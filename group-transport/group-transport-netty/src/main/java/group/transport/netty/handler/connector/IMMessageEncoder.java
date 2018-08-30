package group.transport.netty.handler.connector;
/**
 * Created by DELL on 2018/8/30.
 */

import group.transport.JProtocolHeader;
import group.transport.payload.GRequestPayload;
import group.transport.payload.GResponsePayload;
import group.transport.payload.PayloadHolder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * user is lwb
 **/

/**
 * 发送消息 编码
 */
@ChannelHandler.Sharable
public class IMMessageEncoder extends MessageToByteEncoder<PayloadHolder>{


    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, PayloadHolder msg, ByteBuf out) throws Exception {
         if(msg instanceof GRequestPayload){
             doEncodeRequest((GRequestPayload)msg,out);
         }else if(msg instanceof GResponsePayload){
             doEncodeResponse((GResponsePayload)msg,out);
         } else {
             throw new IllegalArgumentException("can not encode the type "+msg.getClass());
         }

    }

    private void doEncodeResponse(GResponsePayload response, ByteBuf out) {
        byte sign = JProtocolHeader.toSign(response.serializerCode(),JProtocolHeader.RESPONSE);
        byte status = response.status();
        long responseId = response.responseId();
        byte[] bytes = response.bytes();
        int length = bytes.length;

        out.writeShort(JProtocolHeader.MAGIC)
                .writeByte(sign)
                .writeByte(status)
                .writeLong(responseId)
                .writeInt(length)
                .writeBytes(bytes);
    }

    private void doEncodeRequest(GRequestPayload request, ByteBuf out) {
        byte sign = JProtocolHeader.toSign(request.serializerCode(),JProtocolHeader.REQUEST);
        long requestId = request.requestId();
        byte[] bytes = request.bytes();
        int length = bytes.length;
        out.writeShort(JProtocolHeader.MAGIC)
                .writeByte(sign)
                .writeByte(0X00)
                .writeLong(requestId)
                .writeLong(length)
                .writeBytes(bytes);
    }


}
