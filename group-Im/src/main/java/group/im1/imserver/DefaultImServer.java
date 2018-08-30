package group.im1.imserver;/**
 * Created by DELL on 2018/8/29.
 */

import group.im.JRequest;
import group.im.control.ControlResult;
import group.im.metadata.ServiceWrapper;
import group.im.provider.processor.DefaultProviderProcessor;
import group.transport.Directory;
import group.transport.JAcceptor;

/**
 * user is lwb
 **/


public class DefaultImServer implements ImServer {
    JAcceptor acceptor;

    @Override
    public ImServer withAcceptor(JAcceptor acceptor) {
        if(acceptor.processor() == null){
            acceptor.withProcessor(new DefaultProviderProcessor() {
                @Override
                public ControlResult flowControl(JRequest jRequest) {
                    return null;
                }
                @Override
                public ServiceWrapper lookupService(Directory directory) {
                    return null;
                }
            });
        }
        this.acceptor = acceptor;
        return this;
    }

    @Override
    public void start() throws InterruptedException {
        acceptor.start();
    }
}
