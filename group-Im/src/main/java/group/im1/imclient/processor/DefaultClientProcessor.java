package group.im1.imclient.processor;/**
 * Created by DELL on 2018/8/30.
 */

import group.im.consumer.processor.AbstractProcessor;
import group.im.executor.CloseableExecutor;
import group.im1.GRequest;
import group.im1.imclient.processor.task.RequestMessageTask;
import group.transport.channel.JChannel;
import group.transport.payload.GRequestPayload;
import group.transport.payload.GResponsePayload;

/**
 * user is lwb
 **/


public class DefaultClientProcessor extends AbstractProcessor {

    public DefaultClientProcessor(CloseableExecutor executor) {
        super(executor);
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


    }
}
