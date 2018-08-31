package group.im1.server.processor;/**
 * Created by DELL on 2018/8/31.
 */

import group.im.consumer.processor.AbstractProcessor;
import group.transport.JChannelManager;

/**
 * user is lwb
 **/


public abstract class AbstractServerProcessor extends AbstractProcessor{
    private JChannelManager jChannelManager = new JChannelManager();
    public JChannelManager jChannelManager(){
        return jChannelManager;
    }


}
