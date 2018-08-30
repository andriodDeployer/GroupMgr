package group.transport.netty.handler.acceptor;/**
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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * user is lwb
 **/


public class IMAcceptorHandler extends ChannelInboundHandlerAdapter{

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(IMAcceptorHandler.class);
    private Processor processor;
    private static final AtomicInteger channelCounter = new AtomicInteger(0);

    public void processor(Processor processor){
        this.processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        if(msg instanceof GRequestPayload){
            processor.handleRequest(NettyChannel.attachChannel(channel), (GRequestPayload) msg);
        }else if (msg instanceof GResponsePayload){
            processor.handleResonse(NettyChannel.attachChannel(channel), (GResponsePayload) msg);
        }else{
            logger.warn("Unexcepted message type received: {},channel: {}",msg.getClass(),channel);
        }
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int count = channelCounter.incrementAndGet();

        logger.info("Connects with {} as the {}th channel.", ctx.channel(), count);

        super.channelActive(ctx);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        int count = channelCounter.getAndDecrement();

        logger.warn("Disconnects with {} as the {}th channel.", ctx.channel(), count);

        super.channelInactive(ctx);
    }
}
