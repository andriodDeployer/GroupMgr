package group.im1.message;/**
 * Created by DELL on 2018/8/30.
 */

/**
 * user is lwb
 **/


public class Message {
    public static final int TEXT = 1;
    protected static final int IMAGE = 2;
    protected int type;
    protected String sender;
    protected String receiver;
    protected String sendTime;

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }
}
