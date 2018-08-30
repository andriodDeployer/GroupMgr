package group.im1.server;/**
 * Created by DELL on 2018/8/29.
 */

import group.im1.server.processor.DefaultServerProcessor;
import group.transport.JAcceptor;

/**
 * user is lwb
 **/


public class DefaultImServer implements ImServer {
    JAcceptor acceptor;

    @Override
    public ImServer withAcceptor(JAcceptor acceptor) {
        if(acceptor.processor() == null){
            acceptor.withProcessor(new DefaultServerProcessor());
        }
        this.acceptor = acceptor;
        return this;
    }

    @Override
    public void start() throws InterruptedException {
        acceptor.start();
    }
}
