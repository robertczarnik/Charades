package robertczarnik.objects;

import java.io.Serializable;

public class Message implements Serializable {
    private String messageType;
    private String message;

    public Message(String messageType, String message) {
        this.messageType = messageType;
        this.message = message;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getMessage() {
        return message;
    }
}
