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

package group.im.provider.processor;


import group.common.util.ThrowUtil;
import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im.JRequest;
import group.im.control.FlowController;
import group.im.executor.CloseableExecutor;
import group.im.metadata.ResultWrapper;
import group.im.provider.LookupService;
import group.im.provider.processor.task.MessageTask;
import group.serialization.Serializer;
import group.serialization.SerializerFactory;
import group.transport.Status;
import group.transport.channel.JChannel;
import group.transport.channel.JFutureListener;
import group.transport.payload.JRequestPayload;
import group.transport.payload.JResponsePayload;
import group.transport.processor.ProviderProcessor;

import static group.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */

public abstract class DefaultProviderProcessor implements ProviderProcessor, LookupService, FlowController<JRequest> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultProviderProcessor.class);

    private final CloseableExecutor executor;//业务线程池

    public DefaultProviderProcessor() {
        this(ProviderExecutors.executor());
    }

    public DefaultProviderProcessor(CloseableExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void handleRequest(JChannel channel, JRequestPayload requestPayload) throws Exception {//执行这个方法在io线程中
        MessageTask task = new MessageTask(this, channel, new JRequest(requestPayload));//将业务逻辑封装成task里面,在task中完成一个rpc请求的解析，避免了线程安全的问题。
            if (executor == null) {
            task.run();//在io线程里面执行task的处理。
        } else {
            executor.execute(task);//executor是一个线程池(业务线程池，防止业务时间过程，耗费io线程时间)
        }
    }

    @Override
    public void handleException(JChannel channel, JRequestPayload request, Status status, Throwable cause) {
        logger.error("An exception was caught while processing request: {}, {}.",
                channel.remoteAddress(), stackTrace(cause));

        doHandleException(
                channel, request.invokeId(), request.serializerCode(), status.value(), cause, false);
    }

    @Override
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void handleException(JChannel channel, JRequest request, Status status, Throwable cause) {
        logger.error("An exception was caught while processing request: {}, {}.",
                channel.remoteAddress(), stackTrace(cause));

        doHandleException(
                channel, request.invokeId(), request.serializerCode(), status.value(), cause, false);
    }

    public void handleRejected(JChannel channel, JRequest request, Status status, Throwable cause) {
        if (logger.isWarnEnabled()) {
            logger.warn("Service rejected: {}, {}.", channel.remoteAddress(), stackTrace(cause));
        }

        doHandleException(
                channel, request.invokeId(), request.serializerCode(), status.value(), cause, true);
    }

    private void doHandleException(
            JChannel channel, long invokeId, byte s_code, byte status, Throwable cause, boolean closeChannel) {

        ResultWrapper result = new ResultWrapper();
        // 截断cause, 避免客户端无法找到cause类型而无法序列化
        cause = ThrowUtil.cutCause(cause);
        result.setError(cause);

        Serializer serializer = SerializerFactory.getSerializer(s_code);
        byte[] bytes = serializer.writeObject(result);

        JResponsePayload response = new JResponsePayload(invokeId);
        response.status(status);
        response.bytes(s_code, bytes);

        if (closeChannel) {
            channel.write(response, JChannel.CLOSE);
        } else {
            channel.write(response, new JFutureListener<JChannel>() {

                @Override
                public void operationSuccess(JChannel channel) throws Exception {
                    logger.debug("Service error message sent out: {}.", channel);
                }

                @Override
                public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Service error message sent failed: {}, {}.", channel, stackTrace(cause));
                    }
                }
            });
        }
    }
}
