package inaka.com.mangosta.models;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ChatMessage {

    public static final int TYPE_CHAT = 0;
    public static final int TYPE_ROOM_NAME_CHANGED = 1;
    public static final int TYPE_ROOM_CREATED = 2;
    public static final int TYPE_STICKER = 3;

    public static final int TYPE_HEADER = -1;

    public static final int STATUS_SENDING = 0;
    public static final int STATUS_SENT = 1;

    @PrimaryKey
    @NonNull
    private String messageId;
    private String roomJid;
    private String userSender;
    private String content;
    private boolean unread;
    private int type;
    private Date date;
    private int status; //Sending, Sent

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRoomJid() {
        return roomJid;
    }

    public void setRoomJid(String roomJid) {
        this.roomJid = roomJid;
    }

    public String getUserSender() {
        return userSender;
    }

    public void setUserSender(String userSender) {
        this.userSender = userSender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public boolean isMeMessage() {
        return this.getContent().length() >= 4 && this.getContent().substring(0, 4).equals("/me ");
    }

    public String getMeContent() {
        return this.getUserSender() + this.getContent().substring(3);
    }

}
