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

package group.im.consumer.dispatcher;


import group.common.util.JConstants;
import group.common.util.Maps;
import group.common.util.SystemClock;
import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im.DispatchType;
import group.im.JClient;
import group.im.JRequest;
import group.im.JResponse;
import group.im.balance.LoadBalancer;
import group.im.consumer.ConsumerInterceptor;
import group.im.consumer.future.DefaultInvokeFuture;
import group.im.exception.JupiterRemoteException;
import group.im.metadata.MessageWrapper;
import group.im.metadata.MethodSpecialConfig;
import group.im.metadata.ResultWrapper;
import group.im.metadata.ServiceMetadata;
import group.im.tracing.TraceId;
import group.serialization.Serializer;
import group.serialization.SerializerFactory;
import group.serialization.SerializerType;
import group.transport.Status;
import group.transport.channel.CopyOnWriteGroupList;
import group.transport.channel.JChannel;
import group.transport.channel.JChannelGroup;
import group.transport.channel.JFutureListener;
import group.transport.payload.JRequestPayload;

import java.util.List;
import java.util.Map;

import static group.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
abstract class AbstractDispatcher implements Dispatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractDispatcher.class);

    private final JClient client;
    private final LoadBalancer loadBalancer;                    // 软负载均衡
    private final Serializer serializerImpl;                    // 序列化/反序列化impl
    private ConsumerInterceptor[] interceptors;                 // 消费者端拦截器
    private long timeoutMillis = JConstants.DEFAULT_TIMEOUT;    // 调用超时时间设置
    // 针对指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private Map<String, Long> methodSpecialTimeoutMapping = Maps.newHashMap();

    public AbstractDispatcher(JClient client, SerializerType serializerType) {
        this(client, null, serializerType);
    }

    public AbstractDispatcher(JClient client, LoadBalancer loadBalancer, SerializerType serializerType) {
        this.client = client;
        this.loadBalancer = loadBalancer;
        this.serializerImpl = SerializerFactory.getSerializer(serializerType.value());
    }

    public Serializer serializer() {
        return serializerImpl;
    }

    public ConsumerInterceptor[] interceptors() {
        return interceptors;
    }

    @Override
    public Dispatcher interceptors(List<ConsumerInterceptor> interceptors) {
        if (interceptors != null && !interceptors.isEmpty()) {
            this.interceptors = interceptors.toArray(new ConsumerInterceptor[interceptors.size()]);
        }
        return this;
    }

    @Override
    public Dispatcher timeoutMillis(long timeoutMillis) {
        if (timeoutMillis > 0) {
            this.timeoutMillis = timeoutMillis;
        }
        return this;
    }

    @Override
    public Dispatcher methodSpecialConfigs(List<MethodSpecialConfig> methodSpecialConfigs) {
        if (!methodSpecialConfigs.isEmpty()) {
            for (MethodSpecialConfig config : methodSpecialConfigs) {
                long timeoutMillis = config.getTimeoutMillis();
                if (timeoutMillis > 0) {//过滤掉超时时间很小的。
                    methodSpecialTimeoutMapping.put(config.getMethodName(), timeoutMillis);
                }
            }
        }
        return this;
    }

    protected long getMethodSpecialTimeoutMillis(String methodName) {
        Long methodTimeoutMillis = methodSpecialTimeoutMapping.get(methodName);
        if (methodTimeoutMillis != null && methodTimeoutMillis > 0) {
            return methodTimeoutMillis;
        }
        return timeoutMillis;
    }

    //根据servicemetadata，根据负载均衡策略选择一个channel
    //因为这个方法，所有的子类共用，所以，这个方法实现在这里，让子类使用，而不是定义在接口中，
    //这也是为什么在实现类和接口之间再增加一个抽象父类的原因。就是为了提供一些，共用的方法。
    protected JChannel select(ServiceMetadata metadata) {
        CopyOnWriteGroupList groups = client
                .connector()
                .directory(metadata);
        JChannelGroup group = loadBalancer.select(groups, metadata);

        if (group != null) {
            if (group.isAvailable()) {
                return group.next();
            }

            // to the deadline (no available channel), the time exceeded the predetermined limit
            long deadline = group.deadlineMillis();
            if (deadline > 0 && SystemClock.millisClock().now() > deadline) {
                boolean removed = groups.remove(group);
                if (removed) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Removed channel group: {} in directory: {} on [select].",
                                group, metadata.directoryString());
                    }
                }
            }
        } else {
            // for 3 seconds, expired not wait
            if (!client.awaitConnections(metadata, 3000)) {
                throw new IllegalStateException("No connections");
            }
        }

        JChannelGroup[] snapshot = groups.getSnapshot();
        for (JChannelGroup g : snapshot) {
            if (g.isAvailable()) {
                return g.next();
            }
        }

        throw new IllegalStateException("No channel");
    }

    protected JChannelGroup[] groups(ServiceMetadata metadata) {
        return client.connector()
                .directory(metadata)
                .getSnapshot();
    }

    @SuppressWarnings("all")
    protected <T> DefaultInvokeFuture<T> write(
            final JChannel channel, final JRequest request, final Class<T> returnType, final DispatchType dispatchType) {
        final MessageWrapper message = request.message();
        final long timeoutMillis = getMethodSpecialTimeoutMillis(message.getMethodName());//超时时间
        final ConsumerInterceptor[] interceptors = interceptors();
        final TraceId traceId = message.getTraceId();
        final DefaultInvokeFuture<T> future = DefaultInvokeFuture
                .with(request.invokeId(), channel, timeoutMillis, returnType, dispatchType)
                .interceptors(interceptors)
                .traceId(traceId);

        if (interceptors != null) {
            for (int i = 0; i < interceptors.length; i++) {
                interceptors[i].beforeInvoke(traceId, request, channel);
            }
        }

        final JRequestPayload payload = request.payload();
        channel.write(payload, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                // 标记已发送
                future.markSent();

                if (dispatchType == DispatchType.ROUND) {
                    payload.clear();
                }
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                if (dispatchType == DispatchType.ROUND) {
                    payload.clear();
                }

                if (logger.isWarnEnabled()) {
                    logger.warn("Writes {} fail on {}, {}.", request, channel, stackTrace(cause));
                }

                ResultWrapper result = new ResultWrapper();
                result.setError(new JupiterRemoteException(cause));

                JResponse response = new JResponse(payload.invokeId());
                response.status(Status.CLIENT_ERROR);
                response.result(result);

                DefaultInvokeFuture.fakeReceived(channel, response, dispatchType);
            }
        });


        return future;
    }
}
