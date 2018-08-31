package group.im1.server;/**
 * Created by DELL on 2018/8/29.
 */

import group.im1.EndPoint;
import group.transport.JAcceptor;

/**
 * user is lwb
 **/


public interface ImServer extends EndPoint{

    ImServer withAcceptor(JAcceptor acceptor);
    void start() throws InterruptedException;
}
