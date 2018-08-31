package group.im1.server.processor.task;/**
 * Created by DELL on 2018/8/30.
 */

import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im1.AbstractEndPoint;
import group.im1.GRequest;
import group.im1.client.MessingSendListener;
import group.im1.message.Message;
import group.im1.server.processor.AbstractServerProcessor;
import group.serialization.Serializer;
import group.serialization.SerializerFactory;
import group.transport.JChannelManager;
import group.transport.JProtocolHeader;
import group.transport.Status;
import group.transport.channel.JChannel;
import group.transport.channel.JFutureListener;
import group.transport.payload.GRequestPayload;
import group.transport.payload.GResponsePayload;

import java.util.Map;

/**
 * user is lwb
 **/


public class RequestMessageTask extends AbstractEndPoint implements Runnable {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(RequestMessageTask.class);
    private final JChannel jChannel;
    private final AbstractServerProcessor processor;
    private final GRequest request;

    public RequestMessageTask(JChannel jChannel, AbstractServerProcessor processor, GRequest request) {
        this.jChannel = jChannel;
        this.processor = processor;
        this.request = request;
    }

    @Override
    public void run() {
        // stack copy
       // final Processor _processor = processor;
        final GRequest _request = request;
        Message msg;
        GRequestPayload payload = _request.payload();
        byte serializerCode = payload.serializerCode();
        Serializer serializer = SerializerFactory.getSerializer(serializerCode);

        try{

            byte[] bytes = payload.bytes();
            msg = serializer.readObject(bytes, Message.class);
            payload.clear();

            _request.message(msg);
        }catch (Throwable t){
            //todo 发送过来的消息无法序列化
            return;
        }

        logger.info("Server received message {}",msg.toString());

        //回复响应
        GResponsePayload responsePayload = new GResponsePayload(payload.requestId());

        byte[] bytes = serializer.writeObject("ok");
        responsePayload.bytes(serializerCode,bytes);
        responsePayload.status(Status.OK.value());
        writeResponse(responsePayload);

        process(msg);
    }

    private void process(Message msg) {
        //将消息进行转发
        JChannelManager jChannelManager = processor.jChannelManager();

        if(msg.type() == JProtocolHeader.AUTH){
            final String sender = msg.getSender();
            jChannelManager.addJChannel(jChannel, sender, new JChannelManager.AddChannelListener() {
                @Override
                public void channelExists(Map allchannels, JChannel oldJchannel, JChannel newChnanel) {
                    oldJchannel.close();
                    allchannels.put(sender,newChnanel);
                }
            });
        }else if(msg.type() == JProtocolHeader.TEXT){
            String receiver = msg.getReceiver();
            JChannel reciverChannel = jChannelManager.getJChannel(receiver);
            if(reciverChannel == null){
                //接收者不在线

            }else{
                //进行转发
                sentMessage(msg, reciverChannel, new MessingSendListener() {
                    @Override
                    public void sendSuccessful() {

                    }

                    @Override
                    public void sendFailure() {

                    }
                });

            }

        }




    }

    private void writeResponse(final GResponsePayload response) {
        jChannel.write(response, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                logger.info("response send success responseId: {}, sender: {}, receiver: {}",request.requestId(),request.message().getSender(),request.message().getReceiver());
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {

                logger.info("response send faild responseId: {}, sender: {}, receiver: {}",request.requestId(),request.message().getSender(),request.message().getReceiver());
                logger.error(cause.getCause());
            }
        });
    }
}
