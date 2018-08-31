package client_registry;
/**
 * Created by DELL on 2018/8/29.
 */


import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im1.client.DefaultImClient;
import group.im1.client.MessingSendListener;
import group.im1.message.Message;
import group.im1.message.TextMessage;
import group.transport.netty.JNettyTcpConnector;

/**
 * user is lwb
 **/


public class Client {

    public static void main(String[] agrs){
        final InternalLogger logger = InternalLoggerFactory.getInstance(Client.class);
        final DefaultImClient client = new DefaultImClient().withConnector(new JNettyTcpConnector());

        client.connectServer("127.0.0.1:8888",true);
        final String id = "zhangsan";
        final String receiver = "lisi";
        client.auth(id, new MessingSendListener() {
            @Override
            public void sendSuccessful() {
                logger.info("client: {} login success",id);
                final Message text = new TextMessage(id,id,"hellowork");
                client.sentMessage(text, new MessingSendListener() {
                    @Override
                    public void sendSuccessful() {
                        logger.info("message: {} send successfully",text);
                    }

                    @Override
                    public void sendFailure() {
                        logger.error("message: {} send fail",text);
                    }
                });




            }

            @Override
            public void sendFailure() {

            }
        });




    }
}
