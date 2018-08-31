package group.im1.client.processor;/**
 * Created by DELL on 2018/8/30.
 */

import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im.consumer.processor.ConsumerExecutors;
import group.im.executor.CloseableExecutor;
import group.im1.GRequest;
import group.im1.GResponse;
import group.im1.client.DefaultSendFuture;
import group.im1.client.processor.task.AbstractClientProcessor;
import group.im1.client.processor.task.RequestMessageTask;
import group.transport.channel.JChannel;
import group.transport.payload.GRequestPayload;
import group.transport.payload.GResponsePayload;

/**
 * user is lwb
 **/


public class DefaultClientProcessor extends AbstractClientProcessor {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultClientProcessor.class);
    public DefaultClientProcessor(){
        this(ConsumerExecutors.executor());
    }

    public DefaultClientProcessor(CloseableExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void handleRequest(JChannel channel, GRequestPayload payload) {
        RequestMessageTask task = new RequestMessageTask(this, channel,new GRequest(payload));
        if(executor == null){
            task.run();
        }else{
            executor.execute(task);
        }
    }

    @Override
    public void handleResonse(JChannel channel, GResponsePayload payload) {
        //直接再io线程中处理
        logger.info("client recive response id: {}",payload.responseId());
        DefaultSendFuture.received(channel,new GResponse(payload));

    }
}
