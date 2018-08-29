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


import group.common.util.JServiceLoader;
import group.common.util.SystemPropertyUtil;
import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im.consumer.processor.ConsumerExecutors;
import group.im.executor.CloseableExecutor;
import group.im.executor.ExecutorFactory;
import group.im.executor.ThreadPoolExecutorFactory;

import static group.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */
public class ProviderExecutors {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConsumerExecutors.class);

    private static final CloseableExecutor executor;

    static {
        String factoryName = SystemPropertyUtil.get("jupiter.executor.factory.provider.factory_name", "threadPool");
        ExecutorFactory factory;
        try {
            factory = (ExecutorFactory) JServiceLoader.load(ProviderExecutorFactory.class)
                    .find(factoryName);
        } catch (Throwable t) {
            logger.warn("Failed to load provider's executor factory [{}], cause: {}, " +
                    "[ThreadPoolExecutorFactory] will be used as default.", factoryName, stackTrace(t));

            factory = new ThreadPoolExecutorFactory();
        }

        executor = factory.newExecutor(ExecutorFactory.Target.PROVIDER, "jupiter-provider-processor");
    }

    public static CloseableExecutor executor() {
        return executor;
    }

    public static void execute(Runnable r) {
        executor.execute(r);
    }
}
