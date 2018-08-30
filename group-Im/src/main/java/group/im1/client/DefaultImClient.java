package group.im1.client;

import group.common.util.Strings;
import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im1.client.processor.DefaultClientProcessor;
import group.transport.JConnection;
import group.transport.JConnector;
import group.transport.UnresolvedAddress;
import group.transport.exception.ConnectFailedException;

import static group.common.util.Preconditions.checkNotNull;

/**
 * Created by DELL on 2018/8/29.
 */

/**
 * user is lwb
 **/


public class DefaultImClient implements ImClient{

    private JConnector<JConnection> connector;
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultImClient.class);

    @Override
    public ImClient withConnector(JConnector<JConnection> connector) {
        if(connector.processor() == null){
            connector.withProcessor(new DefaultClientProcessor());
        }
        this.connector = connector;
        return this;
    }

    @Override
    public void connectServer(UnresolvedAddress address, boolean async) {
       for(int i=0;i<3;i++){
           try {
               connector.connect(address);
               return;
           }catch (ConnectFailedException ex){
               if(i == 2){
                   throw ex;
               }
               logger.warn("connect address ["+address+"] in ["+i+"] times.");
           }
       }
    }

    @Override
    public void connectServer(String address, boolean async) {
        checkNotNull(address,"address can not be null");
        String[] addressStr = Strings.split(address, ':');
        String host = addressStr[0];
        int port = Integer.parseInt(addressStr[1]);
        UnresolvedAddress unresolvedAddress = new UnresolvedAddress(host, port);
        connectServer(unresolvedAddress,async);
    }
}
