package inaka.com.mangosta.chat;

import android.app.Activity;
import android.content.Context;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.Affiliate;

import java.util.List;

import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.xmpp.XMPPSession;

public class RoomManagerListener {

    Context mContext;

    public RoomManagerListener(Context context) {
        this.mContext = context;
    }

    public void onRoomsLoaded() {

    }

    public void onRoomCreated(Chat chat) {

    }

    public void onRoomLeft(String roomJid) {
    }

    public void onInvitationAccepted(String roomJid) {

    }

    public void onRoomMembersLoaded(List<Affiliate> members) {

    }

    public void onMessageSent(Message message) {
        XMPPSession.getInstance().messageSentAlert(message);
    }

    public void onError(final String error) {
        if (mContext instanceof Activity) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    Toast.makeText(MangostaApplication.getInstance(), error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}