package inaka.com.mangosta.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.jivesoftware.smack.packet.Presence;

public class User implements Parcelable {

    public User(String jid) {
        setJid(jid);
    }

    public User(String jid, String name) {
        setJid(jid);
        setName(name);
    }

    private String jid;
    private String name;
    private Presence.Type connectionStatus;

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Presence.Type getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(Presence.Type connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.jid);
        dest.writeString(this.name);
        dest.writeInt(this.connectionStatus == null ? -1 : this.connectionStatus.ordinal());
    }

    protected User(Parcel in) {
        this.jid = in.readString();
        this.name = in.readString();
        int tmpConnectionStatus = in.readInt();
        this.connectionStatus = tmpConnectionStatus == -1 ? null : Presence.Type.values()[tmpConnectionStatus];
    }

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        @Override
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

}
