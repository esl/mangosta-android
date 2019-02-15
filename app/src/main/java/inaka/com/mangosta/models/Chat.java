package inaka.com.mangosta.models;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Chat {

    public static final int TYPE_1_T0_1 = 0;
    public static final int TYPE_MUC_LIGHT = 1;

    @PrimaryKey
    @NonNull
    private String jid;
    private String name;
    private String subject;
    private boolean isMuted;
    private String imageUrl;
    private int type;
    private boolean show;
    private Date dateCreated;
    private long lastTimestampRetrieved;
    private int sortPosition;
    private int unreadMessagesCount;
    private String messageBeingComposed;

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

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getType() {
        return type;
    }

    public boolean isMucLight() {
        return (type == TYPE_MUC_LIGHT);
    }

    public void setType(int type) {
        this.type = type;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public long getLastTimestampRetrieved() {
        return lastTimestampRetrieved;
    }

    public void setLastTimestampRetrieved(long lastTimestampRetrieved) {
        this.lastTimestampRetrieved = lastTimestampRetrieved;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getSortPosition() {
        return sortPosition;
    }

    public void setSortPosition(int sortPosition) {
        this.sortPosition = sortPosition;
    }

    public int getUnreadMessagesCount() {
        return unreadMessagesCount;
    }

    public void addUnreadMessage() {
        this.unreadMessagesCount++;
    }

    public void setUnreadMessagesCount(int unreadMessagesCount) {
        this.unreadMessagesCount = unreadMessagesCount;
    }

    public String getMessageBeingComposed() {
        return messageBeingComposed;
    }

    public void setMessageBeingComposed(String messageBeingComposed) {
        this.messageBeingComposed = messageBeingComposed;
    }

}
