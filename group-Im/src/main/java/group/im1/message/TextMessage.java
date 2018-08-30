package group.im1.message;/**
 * Created by DELL on 2018/8/30.
 */

import java.io.Serializable;

/**
 * user is lwb
 **/


public class TextMessage extends Message implements Serializable{

    private String text;
    public TextMessage(){
        this.type = TEXT;
    }

    public TextMessage(String sender,String receiver){
        this(sender,receiver,"");
    }

    public TextMessage(String sender,String receiver,String text){
        this.type = TEXT;
        this.sender = sender;
        this.receiver = receiver;
        this.text = text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "TextMessage{" +
                "text='" + text + '\'' +
                '}';
    }
}
