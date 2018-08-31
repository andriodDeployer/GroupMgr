package group.transport;/**
 * Created by DELL on 2018/8/31.
 */

import group.common.util.Maps;
import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.transport.channel.JChannel;

import java.util.List;
import java.util.Map;

/**
 * user is lwb
 **/


public class JChannelManager {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(JChannelManager.class);
    private final Map<String,JChannel> allChannels = Maps.newConcurrentMap();
    private final Map<String,List<JChannel>> groups = Maps.newHashMap();
    private InitGroupsListener listener;


    public void addJChannel(final JChannel newChannel, final String id){
        addJChannel(newChannel, id, new AddChannelListener() {
            @Override
            public void channelExists(Map allchannels, JChannel oldJchannel, JChannel newChnanel) {
                oldJchannel.close();
                allchannels.put(id,newChannel);
            }
        });
    }

    public void addJChannel(JChannel newChannel,String id,AddChannelListener listener){
        newChannel.attachChannelId(id);
        synchronized (logger){
            JChannel oldChannel = getJChannel(id);
            if(oldChannel ==  null){
                allChannels.put(id,newChannel);
            }else{
                listener.channelExists(allChannels,oldChannel,newChannel);
            }
        }

    }
    public JChannel getJChannel(String idKey){
        return allChannels.get(idKey);
    }

    public interface AddChannelListener{
        void channelExists(Map allchannels,JChannel oldJchannel,JChannel newChnanel);
    }

    public void addInitGroupListener(InitGroupsListener listener){
        this.listener = listener;
    }

    public interface InitGroupsListener{
        void initGroups(Map<String,List<JChannel>> groups);
    }

}
