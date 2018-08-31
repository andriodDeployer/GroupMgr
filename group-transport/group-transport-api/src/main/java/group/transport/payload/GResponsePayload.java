package group.transport.payload;/**
 * Created by DELL on 2018/8/30.
 */

import group.transport.Status;

/**
 * user is lwb
 **/


public class GResponsePayload extends PayloadHolder {

    private final long responseId;
    private byte status;
    public GResponsePayload(long responseId){
        this.responseId = responseId;
    }

    public long responseId(){
        return responseId;
    }

    public byte status(){
        return status;
    }

    public void status(Status status){
        this.status =status.value();
    }

    public void status(byte status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "GResponsePayload{" +
                "responseId=" + responseId +
                ", status=" + status +
                '}';
    }
}
