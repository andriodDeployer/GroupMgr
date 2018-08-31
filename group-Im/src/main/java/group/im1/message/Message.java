package group.im1.message;/**
 * Created by DELL on 2018/8/30.
 */

import java.io.Serializable;

/**
 * user is lwb
 **/


public class Message implements Serializable{
    public static final int TEXT = 1;
    protected static final int IMAGE = 2;
    protected byte type;
    protected String sender;
    protected String receiver;
    protected String sendTime;

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public byte type(){
        return type;
    }
}
