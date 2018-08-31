package group.im1.message;/**
 * Created by DELL on 2018/8/31.
 */

import group.transport.JProtocolHeader;

/**
 * user is lwb
 **/


public class AuthMessage extends Message {

    public AuthMessage(String sender){
        this.sender = sender;
        this.type = JProtocolHeader.AUTH;
    }
}
