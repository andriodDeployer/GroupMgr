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

package group.im.consumer.invoker;

import org.jupiter.common.util.Maps;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailFastClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailOverClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailSafeClusterInvoker;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.model.metadata.ClusterStrategyConfig;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;

import java.util.List;
import java.util.Map;

/**
 * Jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * 主要就是维护一个map(methodSpecialClusterInvokermapping)存放了一个方法和调用这个方法的容错调用器的map。
 *
 * @author jiachun.fjc
 */
public class ClusterStrategyBridging {

    private final ClusterInvoker defaultClusterInvoker;//默认的defaultClusterInvoker，如果method2clusterInvker中某个方法没有对应的invoker的话，就使用的默认的invoker。
    private final Map<String, ClusterInvoker> methodSpecialClusterInvokerMapping;//method2clusterInvoker

    public ClusterStrategyBridging(Dispatcher dispatcher,
                                   ClusterStrategyConfig defaultStrategy,
                                   List<MethodSpecialConfig> methodSpecialConfigs) {
        this.defaultClusterInvoker = createClusterInvoker(dispatcher, defaultStrategy);
        this.methodSpecialClusterInvokerMapping = Maps.newHashMap();

        for (MethodSpecialConfig config : methodSpecialConfigs) {
            ClusterStrategyConfig strategy = config.getStrategy();
            if (strategy != null) {
                methodSpecialClusterInvokerMapping.put(
                        config.getMethodName(),
                        createClusterInvoker(dispatcher, strategy)
                );
            }
        }
    }

    //根据方法名获取对应的方法调用器。在实现上要注意：如果这个方法没有调用器的话，是返回空，还是一个默认值呢？如果返回空的话，那么调用方要处理这个问题，如果调用方对没有值的情况有特殊需求的话，尽量要有设置一个返回值，主要是为了实现内聚性，也就是自己的事情自己做。
    public ClusterInvoker findClusterInvoker(String methodName) {
        ClusterInvoker invoker = methodSpecialClusterInvokerMapping.get(methodName);
        return invoker != null ? invoker : defaultClusterInvoker;
    }

    private ClusterInvoker createClusterInvoker(Dispatcher dispatcher, ClusterStrategyConfig strategy) {
        ClusterInvoker.Strategy s = strategy.getStrategy();
        switch (s) {
            case FAIL_FAST:
                return new FailFastClusterInvoker(dispatcher);
            case FAIL_OVER:
                return new FailOverClusterInvoker(dispatcher, strategy.getFailoverRetries());
            case FAIL_SAFE:
                return new FailSafeClusterInvoker(dispatcher);
            default:
                throw new UnsupportedOperationException("Unsupported strategy: " + strategy);
        }
    }
}
