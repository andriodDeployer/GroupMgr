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

package group.im.consumer.processor.task;


import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im.JResponse;
import group.im.consumer.future.DefaultInvokeFuture;
import group.im.exception.JupiterSerializationException;
import group.im.metadata.ResultWrapper;
import group.serialization.Serializer;
import group.serialization.SerializerFactory;
import group.serialization.io.InputBuf;
import group.transport.CodecConfig;
import group.transport.Status;
import group.transport.channel.JChannel;
import group.transport.payload.JResponsePayload;

import static group.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc.consumer.processor.task
 *
 * @author jiachun.fjc
 */
public class MessageTask implements Runnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageTask.class);

    private final JChannel channel;
    private final JResponse response;

    public MessageTask(JChannel channel, JResponse response) {
        this.channel = channel;
        this.response = response;
    }

    @Override
    public void run() {
        // stack copy
        final JResponse _response = response;
        final JResponsePayload _responsePayload = _response.payload();

        byte s_code = _response.serializerCode();

        Serializer serializer = SerializerFactory.getSerializer(s_code);
        ResultWrapper wrapper;
        try {
            if (CodecConfig.isCodecLowCopy()) {
                InputBuf inputBuf = _responsePayload.inputBuf();
                wrapper = serializer.readObject(inputBuf, ResultWrapper.class);
            } else {
                byte[] bytes = _responsePayload.bytes();
                wrapper = serializer.readObject(bytes, ResultWrapper.class);
            }
            _responsePayload.clear();
        } catch (Throwable t) {
            logger.error("Deserialize object failed: {}, {}.", channel.remoteAddress(), stackTrace(t));

            _response.status(Status.DESERIALIZATION_FAIL);
            wrapper = new ResultWrapper();
            wrapper.setError(new JupiterSerializationException(t));
        }
        _response.result(wrapper);

        DefaultInvokeFuture.received(channel, _response);//向future发送消息，告知结果已经返回。之后，future的listener中的回掉会被调用，以及future.get方法也会有返回值了。
    }
}
