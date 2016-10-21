package inaka.com.mangosta.xmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearch;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jxmpp.jid.DomainBareJid;

import java.util.List;

import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.utils.Preferences;

public class XMPPUtils {

    public static boolean userExists(String jid) {
        ProviderManager.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
        ProviderManager.addIQProvider("query", "jabber:iq:vjud", new UserSearch.Provider());
        UserSearchManager searchManager = new UserSearchManager(XMPPSession.getInstance().getXMPPConnection());

        try {
            List<DomainBareJid> services = searchManager.getSearchServices();

            if (services == null || services.size() < 1) {
                return false;
            }

            Form searchForm;
            try {
                searchForm = searchManager.getSearchForm(services.get(0));
                Form answerForm = searchForm.createAnswerForm();

                try {
                    answerForm.setAnswer("user", jid);
                } catch (IllegalStateException ex) {
                    ex.printStackTrace();
                    return false;
                }

                //answerForm.setAnswer("search", jid);
                ReportedData data;
                try {
                    data = searchManager.getSearchResults(answerForm, services.get(0));

                    if (data.getRows() != null) {
                        List<ReportedData.Row> rowList = data.getRows();

                        return rowList.size() > 0;
                    }

                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    return false;
                }


            } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                e.printStackTrace();
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

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
        return userName + "@" + XMPPSession.SERVICE_NAME;
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
        return user.getLogin().equals(XMPPUtils.fromJIDToUserName(Preferences.getInstance().getUserXMPPJid()));
    }

}
