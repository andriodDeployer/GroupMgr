package group.transport.processor;

import group.transport.channel.JChannel;
import group.transport.payload.GRequestPayload;
import group.transport.payload.GResponsePayload;

/**
 * Created by DELL on 2018/8/30.
 */
public interface Processor {

    void handleRequest(JChannel channel, GRequestPayload payload);
    void handleResonse(JChannel channel, GResponsePayload payload);
    void shutdown();

}
