package group.transport.payload;/**
 * Created by DELL on 2018/8/30.
 */

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

}