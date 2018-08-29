package client_registry;/**
 * Created by DELL on 2018/8/29.
 */

import group.im.imclient.DefaultImClient;
import group.im.imclient.ImClient;
import group.transport.netty.JNettyTcpConnector;

/**
 * user is lwb
 **/


public class Client {

    public static void main(String[] agrs){
        ImClient client = new DefaultImClient().withConnector(new JNettyTcpConnector());

        client.connectServer("127.0.0.1:8888",true);


    }
}
