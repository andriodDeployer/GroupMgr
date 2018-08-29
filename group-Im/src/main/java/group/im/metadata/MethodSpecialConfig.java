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

package group.im.metadata;

import java.io.Serializable;

/**
 * Jupiter
 * org.jupiter.rpc.model.metadata
 *
 * 保存一个方法的配置信息：容错策略，超时时间，方法名称。
 *
 * @author jiachun.fjc
 */
public class MethodSpecialConfig implements Serializable {

    private static final long serialVersionUID = -3689442191636868738L;

    private final String methodName;

    private long timeoutMillis;
    private ClusterStrategyConfig strategy;

    public static MethodSpecialConfig of(String methodName) {
        return new MethodSpecialConfig(methodName);
    }

    private MethodSpecialConfig(String methodName) {
        this.methodName = methodName;
    }

    public MethodSpecialConfig timeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public MethodSpecialConfig strategy(ClusterStrategyConfig strategy) {
        this.strategy = strategy;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public ClusterStrategyConfig getStrategy() {
        return strategy;
    }

    public void setStrategy(ClusterStrategyConfig strategy) {
        this.strategy = strategy;
    }
}
