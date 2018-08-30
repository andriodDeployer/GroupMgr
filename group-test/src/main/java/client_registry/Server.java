package client_registry;
/**
 * Created by DELL on 2018/8/29.
 */

import group.im1.imserver1.DefaultImServer;
import group.im1.imserver1.ImServer;
import group.transport.netty.JNettyTcpAcceptor;

/**
 * user is lwb
 **/


public class Server {

    public static void main(String[] args){
        ImServer server = new DefaultImServer().withAcceptor(new JNettyTcpAcceptor(8888));
        try {
            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
