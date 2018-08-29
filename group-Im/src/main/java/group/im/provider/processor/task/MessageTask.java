/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package group.im.provider.processor.task;


import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import group.common.concurrent.RejectedRunnable;
import group.common.util.Signal;
import group.common.util.SystemClock;
import group.common.util.SystemPropertyUtil;
import group.common.util.internal.UnsafeIntegerFieldUpdater;
import group.common.util.internal.UnsafeUpdater;
import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im.*;
import group.im.control.ControlResult;
import group.im.control.FlowController;
import group.im.exception.*;
import group.im.metadata.MessageWrapper;
import group.im.metadata.ResultWrapper;
import group.im.metadata.ServiceWrapper;
import group.im.metric.Metrics;
import group.im.provider.ProviderInterceptor;
import group.im.provider.processor.DefaultProviderProcessor;
import group.im.tracing.TraceId;
import group.im.tracing.TracingUtil;
import group.serialization.Serializer;
import group.serialization.SerializerFactory;
import group.serialization.io.InputBuf;
import group.serialization.io.OutputBuf;
import group.transport.CodecConfig;
import group.transport.Status;
import group.transport.channel.JChannel;
import group.transport.channel.JFutureListener;
import group.transport.payload.JRequestPayload;
import group.transport.payload.JResponsePayload;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static group.common.util.Preconditions.checkNotNull;


/**
 *
 * jupiter
 * org.jupiter.rpc.provider.processor.task
 *
 * @author jiachun.fjc
 */
public class MessageTask implements RejectedRunnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageTask.class);

    private static final boolean METRIC_NEEDED = SystemPropertyUtil.getBoolean("jupiter.metric.needed", false);

    private static final Signal INVOKE_ERROR = null;//Signal.valueOf(MessageTask.class, "INVOKE_ERROR");

    private static final UnsafeIntegerFieldUpdater<TraceId> traceNodeUpdater =
            UnsafeUpdater.newIntegerFieldUpdater(TraceId.class, "node");

    private final DefaultProviderProcessor processor;
    private final JChannel channel;
    private final JRequest request;

    public MessageTask(DefaultProviderProcessor processor, JChannel channel, JRequest request) {
        this.processor = processor;
        this.channel = channel;
        this.request = request;
    }

    //在业务线程中执行。
    @Override
    public void run() {
        // stack copy
        final DefaultProviderProcessor _processor = processor;
        final JRequest _request = request;

        // 全局流量控制
        ControlResult ctrl = _processor.flowControl(_request);
        if (!ctrl.isAllowed()) {
            rejected(Status.APP_FLOW_CONTROL, new JupiterFlowControlException(String.valueOf(ctrl)));
            return;
        }

        MessageWrapper msg;
        try {
            JRequestPayload _requestPayload = _request.payload();

            byte s_code = _requestPayload.serializerCode();
            Serializer serializer = SerializerFactory.getSerializer(s_code);

            // 在业务线程中反序列化, 减轻IO线程负担
            if (CodecConfig.isCodecLowCopy()) {
                InputBuf inputBuf = _requestPayload.inputBuf();
                msg = serializer.readObject(inputBuf, MessageWrapper.class);
            } else {
                byte[] bytes = _requestPayload.bytes();
                msg = serializer.readObject(bytes, MessageWrapper.class);
            }
            _requestPayload.clear();

            _request.message(msg);
        } catch (Throwable t) {
            rejected(Status.BAD_REQUEST, new JupiterBadRequestException("reading request failed", t));
            return;
        }

        // 查找服务
        final ServiceWrapper service =null;// _processor.lookupService(msg.getMetadata());
        if (service == null) {
            rejected(Status.SERVICE_NOT_FOUND, new JupiterServiceNotFoundException(String.valueOf(msg)));
            return;
        }

        // provider私有流量控制
        FlowController<JRequest> childController = service.getFlowController();//每个服务可以单独设置一个流量控制器。
        if (childController != null) {
            ctrl = childController.flowControl(_request);//有
            if (!ctrl.isAllowed()) {
                rejected(Status.PROVIDER_FLOW_CONTROL, new JupiterFlowControlException(String.valueOf(ctrl)));
                return;
            }
        }

        // processing
        Executor childExecutor = service.getExecutor();//调用处理线程，也就是该服务专有线程处理
        if (childExecutor == null) {
            process(service);
        } else {
            // provider私有线程池执行
            childExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    process(service);
                }
            });
        }
    }

    @Override
    public void rejected() {
        rejected(Status.SERVER_BUSY, new JupiterServerBusyException(String.valueOf(request)));
    }

    private void rejected(Status status, JupiterRemoteException cause) {
        if (METRIC_NEEDED) {
            MetricsHolder.rejectionMeter.mark();
        }

        // 当服务拒绝方法被调用时一般分以下几种情况:
        //  1. 非法请求, close当前连接;
        //  2. 服务端处理能力出现瓶颈, close当前连接, jupiter客户端会自动重连, 在加权负载均衡的情况下权重是一点一点升上来的.
        processor.handleRejected(channel, request, status, cause);
    }

    private void process(ServiceWrapper service) {
        // stack copy
        final JRequest _request = request;

        Context invokeCtx = new Context(service);

        if (TracingUtil.isTracingNeeded()) {
            setCurrentTraceId(_request.message().getTraceId());
        }

        try {
            Object invokeResult = Chains.invoke(_request, invokeCtx)
                    .getResult();

            ResultWrapper result = new ResultWrapper();
            result.setResult(invokeResult);
            byte s_code = _request.serializerCode();
            Serializer serializer = SerializerFactory.getSerializer(s_code);

            JResponsePayload responsePayload = new JResponsePayload(_request.invokeId());

            if (CodecConfig.isCodecLowCopy()) {
                OutputBuf outputBuf =
                        serializer.writeObject(channel.allocOutputBuf(), result);
                responsePayload.outputBuf(s_code, outputBuf);
            } else {
                byte[] bytes = serializer.writeObject(result);
                responsePayload.bytes(s_code, bytes);
            }

            responsePayload.status(Status.OK.value());

            handleWriteResponse(responsePayload);
        } catch (Throwable t) {
            if (INVOKE_ERROR == t) {
                // handle biz exception
                handleException(invokeCtx.getExpectCauseTypes(), invokeCtx.getCause());
            } else {
                processor.handleException(channel, _request, Status.SERVER_ERROR, t);
            }
        } finally {
            if (TracingUtil.isTracingNeeded()) {
                TracingUtil.clearCurrent();
            }
        }
    }

    private void handleWriteResponse(JResponsePayload response) {
        channel.write(response, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                if (METRIC_NEEDED) {
                    long duration = SystemClock.millisClock().now() - request.timestamp();
                    MetricsHolder.processingTimer.update(duration, TimeUnit.MILLISECONDS);
                }
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                long duration = SystemClock.millisClock().now() - request.timestamp();
                logger.error("Response sent failed, trace: {}, duration: {} millis, channel: {}, cause: {}.",
                        request.getTraceId(), duration, channel, cause);
            }
        });
    }

    private void handleException(Class<?>[] exceptionTypes, Throwable failCause) {
        if (exceptionTypes != null && exceptionTypes.length > 0) {
            Class<?> failType = failCause.getClass();
            for (Class<?> eType : exceptionTypes) {
                // 如果抛出声明异常的子类, 客户端可能会因为不存在子类类型而无法序列化, 会在客户端抛出无法反序列化异常
                if (eType.isAssignableFrom(failType)) {
                    // 预期内的异常
                    processor.handleException(channel, request, Status.SERVICE_EXPECTED_ERROR, failCause);
                    return;
                }
            }
        }

        // 预期外的异常
        processor.handleException(channel, request, Status.SERVICE_UNEXPECTED_ERROR, failCause);
    }

    private static Object invoke(MessageWrapper msg, Context invokeCtx) throws Signal {
        return null;
//        ServiceWrapper service = invokeCtx.getService();
//        Object provider = service.getServiceProvider();
//        String methodName = msg.getMethodName();
//        Object[] args = msg.getArgs();
//
//        Timer.Context timerCtx = null;
//        if (METRIC_NEEDED) {
//            timerCtx = Metrics.timer(msg.getOperationName()).time();
//        }
//
//        Class<?>[] expectCauseTypes = null;
//        try {
//            List<Pair<Class<?>[], Class<?>[]>> methodExtension = service.getMethodExtension(methodName);
//            if (methodExtension == null) {
//                throw new NoSuchMethodException(methodName);
//            }
//
//            // 根据JLS方法调用的静态分派规则查找最匹配的方法parameterTypes
//            Pair<Class<?>[], Class<?>[]> bestMatch = Reflects.findMatchingParameterTypesExt(methodExtension, args);
//            Class<?>[] parameterTypes = bestMatch.getFirst();
//            expectCauseTypes = bestMatch.getSecond();
//
//            return Reflects.fastInvoke(provider, methodName, parameterTypes, args);
//        } catch (Throwable t) {
//            invokeCtx.setCauseAndExpectTypes(t, expectCauseTypes);
//            throw INVOKE_ERROR;
//        } finally {
//            if (METRIC_NEEDED) {
//                timerCtx.stop();
//            }
//        }
    }

    @SuppressWarnings("all")
    private static void handleBeforeInvoke(ProviderInterceptor[] interceptors,
                                           TraceId traceId,
                                           Object provider,
                                           String methodName,
                                           Object[] args) {

//        for (int i = 0; i < interceptors.length; i++) {
//            try {
//                interceptors[i].beforeInvoke(traceId, provider, methodName, args);
//            } catch (Throwable t) {
//                logger.error("Interceptor[{}#beforeInvoke]: {}.", Reflects.simpleClassName(interceptors[i]), stackTrace(t));
//            }
//        }
    }

    @SuppressWarnings("all")
    private static void handleAfterInvoke(ProviderInterceptor[] interceptors,
                                          TraceId traceId,
                                          Object provider,
                                          String methodName,
                                          Object[] args,
                                          Object invokeResult,
                                          Throwable failCause) {

//        for (int i = interceptors.length - 1; i >= 0; i--) {
//            try {
//                interceptors[i].afterInvoke(traceId, provider, methodName, args, invokeResult, failCause);
//            } catch (Throwable t) {
//                logger.error("Interceptor[{}#afterInvoke]: {}.", Reflects.simpleClassName(interceptors[i]), stackTrace(t));
//            }
//        }
    }

    private static void setCurrentTraceId(TraceId traceId) {
        if (traceId != null && traceId != TraceId.NULL_TRACE_ID) {
            assert traceNodeUpdater != null;
            traceNodeUpdater.set(traceId, traceId.getNode() + 1);
        }
        TracingUtil.setCurrent(traceId);
    }

    public static class Context implements JFilterContext {

        private final ServiceWrapper service;

        private Object result;                  // 服务调用结果
        private Throwable cause;                // 业务异常
        private Class<?>[] expectCauseTypes;    // 预期内的异常类型

        public Context(ServiceWrapper service) {
            this.service = checkNotNull(service, "service");
        }

        public ServiceWrapper getService() {
            return service;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public Throwable getCause() {
            return cause;
        }

        public Class<?>[] getExpectCauseTypes() {
            return expectCauseTypes;
        }

        public void setCauseAndExpectTypes(Throwable cause, Class<?>[] expectCauseTypes) {
            this.cause = cause;
            this.expectCauseTypes = expectCauseTypes;
        }

        @Override
        public JFilter.Type getType() {
            return JFilter.Type.PROVIDER;
        }
    }

    static class InterceptorsFilter implements JFilter {

        @Override
        public Type getType() {
            return Type.PROVIDER;
        }

        @Override
        public <T extends JFilterContext> void doFilter(JRequest request, T filterCtx, JFilterChain next) throws Throwable {
            Context invokeCtx = (Context) filterCtx;
            ServiceWrapper service = invokeCtx.getService();
            // 拦截器
         //   ProviderInterceptor[] interceptors = service.getInterceptors();

//            if (interceptors == null || interceptors.length == 0) {
//                next.doFilter(request, filterCtx);
//            } else {
//                TraceId traceId = TracingUtil.getCurrent();
//                Object provider = service.getServiceProvider();
//
//                MessageWrapper msg = request.message();
//                String methodName = msg.getMethodName();
//                Object[] args = msg.getArgs();
//
//                handleBeforeInvoke(interceptors, traceId, provider, methodName, args);
//                try {
//                    next.doFilter(request, filterCtx);
//                } finally {
//                    handleAfterInvoke(
//                            interceptors, traceId, provider, methodName, args, invokeCtx.getResult(), invokeCtx.getCause());
//                }
//            }
        }
    }

    static class InvokeFilter implements JFilter {

        @Override
        public Type getType() {
            return Type.PROVIDER;
        }

        @Override
        public <T extends JFilterContext> void doFilter(JRequest request, T filterCtx, JFilterChain next) throws Throwable {
            MessageWrapper msg = request.message();
//            Context invokeCtx = (Context) filterCtx;
//
//            Object invokeResult = MessageTask.invoke(msg, invokeCtx);
//
//            invokeCtx.setResult(invokeResult);
        }
    }

    static class Chains {

        private static final JFilterChain headChain;

        static {
            JFilterChain invokeChain = new DefaultFilterChain(new InvokeFilter(), null);
            JFilterChain interceptChain = new DefaultFilterChain(new MessageTask.InterceptorsFilter(), invokeChain);
            headChain = JFilterLoader.loadExtFilters(interceptChain, JFilter.Type.PROVIDER);
        }

        static <T extends JFilterContext> T invoke(JRequest request, T invokeCtx) throws Throwable {
            headChain.doFilter(request, invokeCtx);
            return invokeCtx;
        }
    }

    // - Metrics -------------------------------------------------------------------------------------------------------
    static class MetricsHolder {
        // 请求处理耗时统计(从request被解码开始, 到response数据被刷到OS内核缓冲区为止)
        static final Timer processingTimer              = Metrics.timer("processing");
        // 请求被拒绝次数统计
        static final Meter rejectionMeter               = Metrics.meter("rejection");
    }


}
