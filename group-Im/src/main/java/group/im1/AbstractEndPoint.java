package group.im1;/**
 * Created by DELL on 2018/8/31.
 */

import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im1.client.DefaultSendFuture;
import group.im1.client.MessingSendListener;
import group.im1.message.Message;
import group.serialization.Serializer;
import group.serialization.SerializerFactory;
import group.serialization.SerializerType;
import group.transport.Status;
import group.transport.channel.JChannel;
import group.transport.channel.JFutureListener;
import group.transport.payload.GRequestPayload;

/**
 * user is lwb
 **/

public abstract class AbstractEndPoint implements EndPoint{
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractEndPoint.class);
    @Override
    public void sentMessage(final Message message, JChannel channel, final MessingSendListener listener) {
        final GRequest request = createRequest(message);
        final DefaultSendFuture future = DefaultSendFuture.with(request.requestId(),channel);
        future.addListener(listener);
        channel.write(request.payload(), new JFutureListener<JChannel>() {
            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                logger.debug("client completed send message：{}",message);
                future.markSent();
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                GResponse response = new GResponse(request.requestId());
                response.status(Status.CLIENT_ERROR);
                DefaultSendFuture.fakeReceived(channel,response);
            }
        });
    }

    private Serializer serializer = SerializerFactory.getSerializer(SerializerType.JAVA.value());;
    private GRequest createRequest(Message message) {
        GRequest request = new GRequest(new GRequestPayload());
        byte s_code = serializer.code();
        byte[] bytes = serializer.writeObject(message);
        request.bytes(s_code,bytes);
        request.messageType(message.type());
        return request;
    }
}
