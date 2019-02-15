package inaka.com.mangosta.models.event;

import org.jxmpp.jid.Jid;

public class SendEvent {

    public enum Type {
        SEND_STICKER,
    }

    private SendEvent.Type mType;
    private String mImageName;

    public SendEvent(SendEvent.Type type, String imageName) {
        this.mType = type;
        this.mImageName = imageName;
    }

    public SendEvent.Type getType() {
        return mType;
    }

    public String getImageName() {
        return mImageName;
    }

}
