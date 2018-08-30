package group.transport.netty.handler.connector;/**
 * Created by DELL on 2018/8/30.
 */

import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.transport.netty.channel.NettyChannel;
import group.transport.payload.GRequestPayload;
import group.transport.payload.GResponsePayload;
import group.transport.processor.Processor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * user is lwb
 **/


public class IMConnectorHandler extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(IMConnectorHandler.class);

    private Processor processor;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        if(msg instanceof GRequestPayload){
            processor.handleRequest(NettyChannel.attachChannel(channel), (GRequestPayload) msg);
        } else if (msg instanceof GResponsePayload){
            processor.handleResonse(NettyChannel.attachChannel(channel), (GResponsePayload) msg);
        } else {
            logger.warn("Unexcepted message type received: {}, channel {}",msg.getClass(),channel);
        }
    }
}
