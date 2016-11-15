package inaka.com.mangosta.chat;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.muclight.MultiUserChatLightManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.realm.Realm;

public class RoomsListManager {

    private static RoomsListManager mInstance;

    public static RoomsListManager getInstance() {
        if (mInstance == null) {
            mInstance = new RoomsListManager();
        }
        return mInstance;
    }

    public static void setSpecialInstanceForTesting(RoomsListManager roomsListManager) {
        mInstance = roomsListManager;
    }

    public void createCommonChat(String jid) {
        createChatIfNotExists(jid, true);
    }

    public ChatManager getChatManager() {
        return ChatManager.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
    }

    public MultiUserChat createMUC(List<User> users, String roomName, String nickName) {
        String roomJID = UUID.randomUUID().toString() + "@" + XMPPSession.MUC_SERVICE_NAME;
        MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        MultiUserChat muc;

        try {
            muc = manager.getMultiUserChat(JidCreate.from(roomJID).asEntityBareJidIfPossible());
            muc.create(Resourcepart.from(nickName));

            Form form = muc.getConfigurationForm();
            Form submitForm = form.createAnswerForm();
            for (FormField field : form.getFields()) {
                if (!FormField.Type.hidden.equals(field.getType()) && field.getVariable() != null) {
                    submitForm.setDefaultAnswer(field.getVariable());
                }
            }
//            submitForm.setAnswer("muc#roomconfig_publicroom", true);
            submitForm.setAnswer("muc#roomconfig_persistentroom", false);
            submitForm.setAnswer("muc#roomconfig_roomname", roomName);

            muc.sendConfigurationForm(submitForm);

            sendInvitations(muc, roomName, users);
        } catch (InterruptedException | XmppStringprepException | XMPPException.XMPPErrorException | SmackException e) {
            e.printStackTrace();
            return null;
        }

        return muc;
    }

    public MultiUserChatLight createMUCLight(List<User> users, String roomName) {
        List<Jid> occupants = new ArrayList<>();

        for (User user : users) {
            String jid = XMPPUtils.fromUserNameToJID(user.getLogin());
            try {
                occupants.add(JidCreate.from(jid));
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
        }

        XMPPTCPConnection connection = XMPPSession.getInstance().getXMPPConnection();
        MultiUserChatLightManager multiUserChatLightManager = MultiUserChatLightManager.getInstanceFor(connection);

        String roomId = UUID.randomUUID().toString();
        String roomJid = roomId + "@" + XMPPSession.MUC_LIGHT_SERVICE_NAME;
        MultiUserChatLight multiUserChatLight = null;

        try {
            multiUserChatLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(roomJid).asEntityBareJidIfPossible());
            multiUserChatLight.create(roomName, occupants);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return multiUserChatLight;
    }

    public void sendInvitations(MultiUserChat multiUserChat, String roomName, List<User> users) {
        for (User user : users) {
            try {
                EntityBareJid userJID = JidCreate.entityBareFrom(XMPPUtils.fromUserNameToJID(user.getLogin()));
                multiUserChat.invite(new Message(), userJID, "I invite you to the room " + roomName);
            } catch (SmackException.NotConnectedException | InterruptedException | XmppStringprepException e) {
                e.printStackTrace();
            }
        }
    }

    public void createChatIfNotExists(String chatJid, final boolean save) {
        if (!RealmManager.getInstance().chatExists(chatJid)) {
            // save chat
            final inaka.com.mangosta.models.Chat chat = new inaka.com.mangosta.models.Chat(chatJid);

            if (save) {

                if (chatJid.contains(XMPPSession.MUC_SERVICE_NAME)) {
                    chat.setType(Chat.TYPE_MUC);
                    findMUCName(chat);
                } else if (chatJid.contains(XMPPSession.MUC_LIGHT_SERVICE_NAME)) {
                    chat.setType(Chat.TYPE_MUC_LIGHT);
                    findMUCLightName(chat);
                } else {
                    chat.setType(Chat.TYPE_1_T0_1);
                    if (!chatJid.equals(Preferences.getInstance().getUserXMPPJid())) {
                        chat.setName("Chat with " + XMPPUtils.fromJIDToUserName(chatJid));
                    }
                }

                chat.setShow(true);
                chat.setDateCreated(new Date());
                RealmManager.getInstance().saveChat(chat);
            }
        }
    }

    private void findMUCName(Chat chat) {
        if (XMPPSession.getInstance().getXMPPConnection().isAuthenticated()) {
            DiscoverItems discoverItems = XMPPSession.getInstance().discoverMUCItems();

            if (discoverItems != null) {
                List<DiscoverItems.Item> items = discoverItems.getItems();

                for (DiscoverItems.Item item : items) {

                    String itemJid = item.getEntityID().toString();
                    if (itemJid.equals(chat.getJid())) {
                        chat.setName(item.getName());
                    }
                }

            }

        }
    }

    private void findMUCLightName(Chat chat) {
        if (XMPPSession.getInstance().getXMPPConnection().isAuthenticated()) {
            DiscoverItems discoverItems = XMPPSession.getInstance().discoverMUCLightItems();

            if (discoverItems != null) {
                List<DiscoverItems.Item> items = discoverItems.getItems();

                for (DiscoverItems.Item item : items) {

                    String itemJid = item.getEntityID().toString();
                    if (itemJid.equals(chat.getJid())) {
                        chat.setName(item.getName());
                    }
                }

            }

        }
    }

    public void setShowChat(Realm realm, Chat mChat) {
        realm.beginTransaction();
        mChat.setShow(true);
        realm.copyToRealmOrUpdate(mChat);
        realm.commitTransaction();
        realm.close();
    }

    public void manageNewChat(Chat chat, Realm realm, String chatName, String chatJid) {
        realm.beginTransaction();
        if (chat == null) {
            chat = new Chat(chatJid);

            if (chatJid.contains(XMPPSession.MUC_SERVICE_NAME)) {
                chat.setType(Chat.TYPE_MUC);
            } else if (chatJid.contains(XMPPSession.MUC_LIGHT_SERVICE_NAME)) {
                chat.setType(Chat.TYPE_MUC_LIGHT);
            } else {
                chat.setType(Chat.TYPE_1_T0_1);
            }

            chat.setDateCreated(new Date());
        }
        chat.setName(chatName);
        realm.copyToRealmOrUpdate(chat);
        realm.commitTransaction();
        realm.close();
    }

}
