package inaka.com.mangosta.chat;

import org.jivesoftware.smackx.muc.Affiliate;

import java.util.List;

import inaka.com.mangosta.xmpp.XMPPSession;

public abstract class RoomManagerListener {

    public void onRoomsLoaded() {
    }

    public void onRoomCreated(String roomJid, String roomName) {
    }

    public void onRoomLeft(String roomJid) {
    }

    public void onInvitationAccepted(String roomJid) {
    }

    public void onRoomMembersLoaded(List<Affiliate> members) {
    }

    public void onMessageSent(int chatType) {
        XMPPSession.getInstance().messageSentAlert(chatType);
    }

    public void onError(final String error) {
    }

}