package group.im1;

import group.im1.client.MessingSendListener;
import group.im1.message.Message;
import group.transport.channel.JChannel;

/**
 * Created by DELL on 2018/8/31.
 */
public interface EndPoint {
    void sentMessage(Message message, JChannel jChannel,MessingSendListener listener);
}
