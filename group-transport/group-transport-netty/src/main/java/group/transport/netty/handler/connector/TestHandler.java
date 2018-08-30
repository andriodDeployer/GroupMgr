package group.transport.netty.handler.connector;/**
 * Created by DELL on 2018/8/30.
 */

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * user is lwb
 **/


public class TestHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("testhandler");
        super.write(ctx, msg, promise);
    }
}
