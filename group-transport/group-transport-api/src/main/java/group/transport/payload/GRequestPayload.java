package group.transport.payload;/**
 * Created by DELL on 2018/8/30.
 */

import group.common.util.LongSequence;

/**
 * user is lwb
 **/


public class GRequestPayload extends PayloadHolder {

    private static final LongSequence sequence = new LongSequence();
    private final long requestId;
    private transient long timestemp;
    public GRequestPayload(){
        this(sequence.next());
    }
    public GRequestPayload(long requestId){
        this.requestId = requestId;
    }

    public long requestId(){
        return requestId;
    }

    public long timestemp(){
        return timestemp;
    }

    public void timestemp(long timestemp){
        this.timestemp = timestemp;
    }
}
