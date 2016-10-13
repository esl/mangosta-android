package inaka.com.mangosta.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class BlogPost extends RealmObject implements Parcelable {

    @PrimaryKey
    private String id;
    private String ownerJid;
    private String ownerAvatarUrl;
    private String content;
    private Date published;
    private Date updated;

    public BlogPost() {

    }

    public BlogPost(String id, String ownerJid, String ownerAvatarUrl, String content, Date published, Date updated) {
        this.id = id;
        this.ownerJid = ownerJid;
        this.content = content;
        this.published = published;
        this.updated = updated;
        this.ownerAvatarUrl = ownerAvatarUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerJid() {
        return ownerJid;
    }

    public void setOwnerJid(String ownerJid) {
        this.ownerJid = ownerJid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getOwnerAvatarUrl() {
        return ownerAvatarUrl;
    }

    public void setOwnerAvatarUrl(String ownerAvatarUrl) {
        this.ownerAvatarUrl = ownerAvatarUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.ownerJid);
        dest.writeString(this.ownerAvatarUrl);
        dest.writeString(this.content);
        dest.writeLong(this.published != null ? this.published.getTime() : -1);
        dest.writeLong(this.updated != null ? this.updated.getTime() : -1);
    }

    protected BlogPost(Parcel in) {
        this.id = in.readString();
        this.ownerJid = in.readString();
        this.ownerAvatarUrl = in.readString();
        this.content = in.readString();
        long tmpPublished = in.readLong();
        this.published = tmpPublished == -1 ? null : new Date(tmpPublished);
        long tmpUpdated = in.readLong();
        this.updated = tmpUpdated == -1 ? null : new Date(tmpUpdated);
    }

    public static final Creator<BlogPost> CREATOR = new Creator<BlogPost>() {
        @Override
        public BlogPost createFromParcel(Parcel source) {
            return new BlogPost(source);
        }

        @Override
        public BlogPost[] newArray(int size) {
            return new BlogPost[size];
        }
    };
}
