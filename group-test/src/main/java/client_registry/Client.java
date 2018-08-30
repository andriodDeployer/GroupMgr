package client_registry;
/**
 * Created by DELL on 2018/8/29.
 */


import group.im1.client.DefaultImClient;
import group.im1.client.ImClient;
import group.im1.client.MessingSendListener;
import group.im1.message.Message;
import group.im1.message.TextMessage;
import group.transport.netty.JNettyTcpConnector;

/**
 * user is lwb
 **/


public class Client {

    public static void main(String[] agrs){
        ImClient client = new DefaultImClient().withConnector(new JNettyTcpConnector());

        client.connectServer("127.0.0.1:8888",true);
        Message text = new TextMessage("sender","receiver","hellowork");
        client.sentMessage(text, new MessingSendListener() {
            @Override
            public void sendSuccessful() {
                System.out.println("发送成功");
            }

            @Override
            public void sendFailure() {
                System.out.println("发送失败");
            }
        });


    }
}
