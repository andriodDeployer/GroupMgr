package group.im1;/**
 * Created by DELL on 2018/8/30.
 */

import group.transport.Status;
import group.transport.payload.GResponsePayload;

/**
 * user is lwb
 **/


public class GResponse{

    private final GResponsePayload payload;

    public GResponse(long responseId){
        this.payload = new GResponsePayload(responseId);
    }

    public GResponse(GResponsePayload payload){
        this.payload = payload;
    }

    public long responseId(){
        return payload.responseId();
    }

    public byte status(){
        return payload.status();
    }

    public byte serializerCode() {
        return payload.serializerCode();
    }

    public void status(Status status){
        payload.status(status);
    }


}
