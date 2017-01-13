package inaka.com.mangosta.models;

import de.greenrobot.event.EventBus;

public class UserEvent {

    public enum Type {
        ADD_USER,
        REMOVE_USER
    }

    private Type mType;
    private User mUser;

    public UserEvent(Type type, User user) {
        this.mType = type;
        this.mUser = user;
    }

    public UserEvent(Type type) {
        this.mType = type;
    }

    public Type getType() {
        return mType;
    }

    public User getUser() {
        return mUser;
    }

    public void post() {
        EventBus.getDefault().post(this);
    }

}
