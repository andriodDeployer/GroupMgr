package group.im.consumer.processor;/**
 * Created by DELL on 2018/8/30.
 */

import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im.executor.CloseableExecutor;
import group.transport.processor.Processor;

/**
 * user is lwb
 **/


public abstract class AbstractProcessor implements Processor{

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractProcessor.class);
    protected  CloseableExecutor executor; //业务线程




    @Override
    public void shutdown() {
        if (executor != null){
            executor.shutdown();
        }
    }
}
