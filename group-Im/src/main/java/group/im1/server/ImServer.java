package group.im1.server;/**
 * Created by DELL on 2018/8/29.
 */

import group.transport.JAcceptor;

/**
 * user is lwb
 **/


public interface ImServer {

    ImServer withAcceptor(JAcceptor acceptor);
    void start() throws InterruptedException;
}
