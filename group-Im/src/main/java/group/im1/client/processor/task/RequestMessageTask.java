package group.im1.client.processor.task;/**
 * Created by DELL on 2018/8/30.
 */

import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im1.GRequest;
import group.im1.client.processor.DefaultClientProcessor;
import group.im1.message.Message;
import group.serialization.Serializer;
import group.serialization.SerializerFactory;
import group.transport.Status;
import group.transport.channel.JChannel;
import group.transport.channel.JFutureListener;
import group.transport.payload.GRequestPayload;
import group.transport.payload.GResponsePayload;


/**
 * user is lwb
 **/


public class RequestMessageTask implements Runnable{

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RequestMessageTask.class);
    private final JChannel jChannel;
    private final DefaultClientProcessor processor;
    private final GRequest request;

    public RequestMessageTask(DefaultClientProcessor processor, JChannel jChannel, GRequest request){
        this.processor = processor;
        this.jChannel = jChannel;
        this.request = request;
    }

    @Override
    public void run() {
        // stack copy
        final DefaultClientProcessor _processor = processor;
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

        logger.info("client received message {}",msg.toString());

        //回复响应
        GResponsePayload responsePayload = new GResponsePayload(payload.requestId());

        byte[] bytes = serializer.writeObject("ok");
        responsePayload.bytes(serializerCode,bytes);
        responsePayload.status(Status.OK.value());
        writeResponse(responsePayload);

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
            }
        });
    }
}
