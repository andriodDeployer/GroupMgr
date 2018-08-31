package group.im1.client;/**
 * Created by DELL on 2018/8/29.
 */

import group.im1.EndPoint;
import group.transport.JConnection;
import group.transport.JConnector;
import group.transport.UnresolvedAddress;

/**
 * user is lwb
 **/


public interface ImClient extends EndPoint{

    ImClient withConnector(JConnector<JConnection> connector);
    void connectServer(UnresolvedAddress address, boolean async);
    void connectServer(String address, boolean async);




}
