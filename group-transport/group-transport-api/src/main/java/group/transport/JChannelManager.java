package group.transport;/**
 * Created by DELL on 2018/8/31.
 */

import group.transport.channel.JChannel;

/**
 * user is lwb
 **/


public interface JChannelManager {
    void addJChannel(JChannel channel);
    JChannel getJChannel(String idKey);

}
