package inaka.com.mangosta.xmpp;

import android.text.TextUtils;

import org.jxmpp.jid.Jid;

import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.utils.Preferences;

public class XMPPUtils {

    public static String fromJIDToUserName(String jidString) {
        int position = jidString.indexOf("@");

        if (position >= 0 && !jidString.equals("")) {

            if (jidString.substring(position + 1).startsWith(XMPPSession.SERVICE_NAME)) {
                return jidString.substring(0, position);
            } else {
                return jidString;
            }

        } else {
            return null;
        }

    }

    public static String fromUserNameToJID(String userName) {
        if (userName != null) {
            if (userName.contains("@")) {
                return userName;
            } else {
                return userName + "@" + XMPPSession.SERVICE_NAME;
            }
        } else {
            return "";
        }
    }

    public static String getChatName(Chat chat) {
        String chatName = "";

        if (chat.getName() != null) {
            int endPosition = chat.getName().lastIndexOf("(");
            if (endPosition != -1) {
                chatName = chat.getName().substring(0, endPosition);
            } else {
                chatName = chat.getName();
            }
        }

        return chatName;
    }

    /** show nickname if defined, or show fallback to localpart of jid */
    public static String getDisplayName(User user) {
        return TextUtils.isEmpty(user.getName())?
                XMPPUtils.fromJIDToUserName(user.getJid()):user.getName();
    }

    public static boolean isAuthenticatedUser(User user) {
        if (user == null || user.getJid() == null) {
            return false;
        }
        return user.getJid().equals(Preferences.getInstance().getUserXMPPJid());
    }

    public static boolean isAutenticatedJid(Jid jid) {
        return jid.equals(XMPPSession.getInstance().getUser().asBareJid());
    }

}
