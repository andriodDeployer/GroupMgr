package group.im.imclient;/**
 * Created by DELL on 2018/8/29.
 */

import group.transport.JConnection;
import group.transport.JConnector;
import group.transport.UnresolvedAddress;

/**
 * user is lwb
 **/


public interface ImClient {

    ImClient withConnector(JConnector<JConnection> connector);
    void connectServer(UnresolvedAddress address,boolean async);
    void connectServer(String address,boolean async);
}
