package group.im1.client;/**
 * Created by DELL on 2018/8/29.
 */

import group.im1.message.Message;
import group.transport.JConnection;
import group.transport.JConnector;
import group.transport.UnresolvedAddress;

/**
 * user is lwb
 **/


public interface ImClient {

    ImClient withConnector(JConnector<JConnection> connector);
    void connectServer(UnresolvedAddress address, boolean async);
    void connectServer(String address, boolean async);

    void sentMessage(Message message,MessingSendListener listener);


}
