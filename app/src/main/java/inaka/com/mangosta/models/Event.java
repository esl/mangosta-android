package inaka.com.mangosta.models;

public class Event {

    public enum Type {
        ROOMS_LOADED,
        ROOM_LEFT,
        GO_BACK_FROM_CHAT,
        STICKER_SENT
    }

    private Type mType;
    private String mImageName;

    public Event() {

    }

    public Event(Type type) {
        this.mType = type;
    }

    public Event(Type type, String imageName) {
        this.mType = type;
        this.mImageName = imageName;
    }

    public Type getType() {
        return mType;
    }

    public String getImageName() {
        return mImageName;
    }

}
