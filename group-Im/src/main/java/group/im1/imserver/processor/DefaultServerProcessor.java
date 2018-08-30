package group.im1.imserver.processor;

import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im.consumer.processor.AbstractProcessor;
import group.im.executor.CloseableExecutor;
import group.im1.GRequest;
import group.im1.imserver.processor.task.RequestMessageTask;
import group.transport.channel.JChannel;
import group.transport.payload.GRequestPayload;
import group.transport.payload.GResponsePayload;

/**
 * Created by DELL on 2018/8/30.
 */
public class DefaultServerProcessor extends AbstractProcessor{

    private final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultServerProcessor.class);
    public DefaultServerProcessor(CloseableExecutor executor) {
        super(executor);
    }

    @Override
    public void handleRequest(JChannel channel, GRequestPayload payload) {
        RequestMessageTask task =  new RequestMessageTask(channel,this,new GRequest(payload));
        if(executor == null){
            task.run();
        }else {
            executor.execute(task);
        }
    }

    @Override
    public void handleResonse(JChannel channel, GResponsePayload payload) {
        //直接在io线程处理
        long id = payload.responseId();
        byte status = payload.status();
        logger.info("server received response id: {},stutus: {}",id,status);
    }
}
