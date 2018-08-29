package client_registry;/**
 * Created by DELL on 2018/8/29.
 */

import group.im.DefaultClient;
import group.im.JClient;
import group.transport.netty.JNettyTcpConnector;

/**
 * user is lwb
 **/


public class Client {

    public static void main(String[] agrs){
        JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        client.connectToRegistryServer("127.0.0.1:8888");


    }
}
