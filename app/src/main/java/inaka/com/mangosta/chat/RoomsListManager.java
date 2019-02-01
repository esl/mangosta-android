package inaka.com.mangosta.chat;

import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.muclight.MultiUserChatLightManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
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

    public MultiUserChatLight createMUCLight(List<User> users, String roomName) {
        List<Jid> occupants = new ArrayList<>();

        for (User user : users) {
            try {
                occupants.add(JidCreate.from(user.getJid()));
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

    public void createChatIfNotExists(String chatJid, final boolean save) {
        if (!RealmManager.getInstance().chatExists(chatJid)) {
            // save chat
            final inaka.com.mangosta.models.Chat chat = new inaka.com.mangosta.models.Chat(chatJid);

            if (save) {

                if (chatJid.contains(XMPPSession.MUC_LIGHT_SERVICE_NAME)) {
                    chat.setType(Chat.TYPE_MUC_LIGHT);
                    chat.setSortPosition(RealmManager.getInstance().getMUCLights().size());
                    findMUCLightName(chat);
                } else {
                    chat.setType(Chat.TYPE_1_T0_1);
                    chat.setSortPosition(RealmManager.getInstance().get1to1Chats().size());
                    if (!chatJid.equals(Preferences.getInstance().getUserXMPPJid())) {
                        chat.setName(XMPPUtils.fromJIDToUserName(chatJid));
                    }
                }

                chat.setShow(true);
                chat.setDateCreated(new Date());
                RealmManager.getInstance().saveChat(chat);
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

            if (chatJid.contains(XMPPSession.MUC_LIGHT_SERVICE_NAME)) {
                chat.setType(Chat.TYPE_MUC_LIGHT);
                chat.setSortPosition(RealmManager.getInstance().getMUCLights().size());
            } else {
                chat.setType(Chat.TYPE_1_T0_1);
                chat.setSortPosition(RealmManager.getInstance().get1to1Chats().size());
            }

            chat.setDateCreated(new Date());
        }
        chat.setName(chatName);
        realm.copyToRealmOrUpdate(chat);
        realm.commitTransaction();
        realm.close();
    }

}
