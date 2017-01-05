package inaka.com.mangosta.xmpp;

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
        if (userName.contains("@")) {
            return userName;
        } else {
            return userName + "@" + XMPPSession.SERVICE_NAME;
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

    public static boolean isAutenticatedUser(User user) {
        if(user == null || user.getLogin() == null) {
            return false;
        }
        return user.getLogin().equals(XMPPUtils.fromJIDToUserName(Preferences.getInstance().getUserXMPPJid()));
    }

    public static boolean isAutenticatedUser(String userName) {
        User user = new User();
        user.setLogin(userName);
        return isAutenticatedUser(user);
    }

    public static boolean isAutenticatedJid(Jid jid) {
        return jid.equals(XMPPSession.getInstance().getUser().asBareJid());
    }

}
