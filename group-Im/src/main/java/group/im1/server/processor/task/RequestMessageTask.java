package group.im1.server.processor.task;/**
 * Created by DELL on 2018/8/30.
 */

import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im1.GRequest;
import group.im1.message.Message;
import group.serialization.Serializer;
import group.serialization.SerializerFactory;
import group.transport.Status;
import group.transport.channel.JChannel;
import group.transport.payload.GRequestPayload;
import group.transport.payload.JResponsePayload;
import group.transport.processor.Processor;

/**
 * user is lwb
 **/


public class RequestMessageTask implements Runnable {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(RequestMessageTask.class);
    private final JChannel jChannel;
    private final Processor processor;
    private final GRequest request;

    public RequestMessageTask(JChannel jChannel, Processor processor, GRequest request) {
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
        JResponsePayload responsePayload = new JResponsePayload(payload.requestId());

        byte[] bytes = serializer.writeObject("ok");
        responsePayload.bytes(serializerCode,bytes);

        responsePayload.status(Status.OK.value());
    }
}