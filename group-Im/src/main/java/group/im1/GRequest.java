package group.im1;/**
 * Created by DELL on 2018/8/30.
 */

import group.im1.message.Message;
import group.transport.payload.GRequestPayload;

/**
 * user is lwb
 **/


public class GRequest {

    private final GRequestPayload payload;
    private Message message;
    private int messageType;

    public GRequest(){
        this(new GRequestPayload());
    }


    public GRequest(GRequestPayload payload){
        this.payload = payload;
       // this.messageType = messageType;
    }


    public long requestId(){
        return payload.requestId();
    }

    public long timestemp(){
        return payload.timestemp();
    }

    public GRequestPayload payload(){
        return payload;
    }

    public void message(Message message){
        this.message = message;
    }
    public Message message(){
        return message;
    }



}
