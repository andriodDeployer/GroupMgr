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

package group.im.consumer;


import group.im.JRequest;
import group.im.JResponse;
import group.im.tracing.TraceId;
import group.transport.channel.JChannel;

/**
 * jupiter
 * org.jupiter.rpc.consumer
 *
 * @author jiachun.fjc
 */
public interface ConsumerInterceptor {

    /**
     * This code is executed before the request data sent.
     */
    void beforeInvoke(TraceId traceId, JRequest request, JChannel channel);

    /**
     * This code is executed after the server returns the result.
     */
    void afterInvoke(TraceId traceId, JResponse response, JChannel channel);
}
