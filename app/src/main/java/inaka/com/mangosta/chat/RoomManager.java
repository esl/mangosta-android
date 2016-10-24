package inaka.com.mangosta.chat;

import android.content.Context;
import android.widget.Toast;

import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
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

import inaka.com.mangosta.R;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import inaka.com.mangosta.xmpp.bob.BoBHash;
import inaka.com.mangosta.xmpp.bob.elements.BoBExtension;
import inaka.com.mangosta.xmpp.mam.MamManager;
import inaka.com.mangosta.xmpp.muclight.MUCLightRoomConfiguration;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLight;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLightManager;
import io.realm.Realm;


public class RoomManager {

    RoomManagerListener mListener;
    private static RoomManager mInstance;

    private RoomManager(RoomManagerListener listener) {
        mListener = listener;
    }

    public static RoomManager getInstance(RoomManagerListener listener) {
        if (mInstance == null) {
            mInstance = new RoomManager(listener);
        }
        return mInstance;
    }

    public void loadMUCRooms() {

        final XMPPTCPConnection connection = XMPPSession.getInstance().getXMPPConnection();

        if (connection.isAuthenticated()) {

            DiscoverItems discoverItems = XMPPSession.getInstance().discoverMUCItems();
            if (discoverItems != null) {
                RealmManager.hideAllMUCChats();

                List<DiscoverItems.Item> items = discoverItems.getItems();
                Realm realm = RealmManager.getRealm();

                try {
                    for (DiscoverItems.Item item : items) {
                        String itemJid = item.getEntityID().toString();
                        String userJid = Preferences.getInstance().getUserXMPPJid();

                        if (itemJid.contains(XMPPSession.MUC_SERVICE_NAME)) {
                            EntityBareJid jid = item.getEntityID().asEntityBareJidIfPossible();
                            MultiUserChat multiUserChat = MultiUserChatManager.getInstanceFor(connection).getMultiUserChat(jid);

                            Chat chatRoom = realm.where(Chat.class).equalTo("jid", itemJid).findFirst();

                            try {

                                realm.beginTransaction();
                                if (chatRoom == null) {
                                    chatRoom = new Chat();
                                    chatRoom.setJid(item.getEntityID().toString());
                                    chatRoom.setName(item.getName());
                                    chatRoom.setType(Chat.TYPE_MUC);
                                }
                                realm.copyToRealmOrUpdate(chatRoom);
                                realm.commitTransaction();

                                if (!chatRoom.isShow()) {
                                    String userName = XMPPUtils.fromJIDToUserName(userJid);
                                    multiUserChat.join(Resourcepart.from(userName));

                                    realm.beginTransaction();
                                    chatRoom.setShow(true);
                                    realm.copyToRealmOrUpdate(chatRoom);
                                    realm.commitTransaction();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();

                                if (chatRoom != null) {
                                    realm.beginTransaction();
                                    chatRoom.setShow(false);
                                    realm.copyToRealmOrUpdate(chatRoom);
                                    realm.commitTransaction();
                                }
                            }
                        }

                        try {
                            Presence presence = new Presence(Presence.Type.available);
                            presence.setTo(JidCreate.from(itemJid));
                            connection.sendStanza(presence);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } finally {
                    realm.close();
                    mListener.onRoomsLoaded();
                }

            }
        }
    }

    public void loadMUCLightRooms() {

        final XMPPTCPConnection connection = XMPPSession.getInstance().getXMPPConnection();

        if (connection.isAuthenticated()) {

            DiscoverItems discoverItems = XMPPSession.getInstance().discoverMUCLightItems();

            if (discoverItems != null) {
                RealmManager.hideAllMUCLightChats();
                List<DiscoverItems.Item> items = discoverItems.getItems();
                Realm realm = RealmManager.getRealm();

                try {
                    for (DiscoverItems.Item item : items) {
                        String itemJid = item.getEntityID().toString();

                        if (itemJid.contains(XMPPSession.MUC_LIGHT_SERVICE_NAME)) {

                            Chat chatRoom = realm.where(Chat.class).equalTo("jid", itemJid).findFirst();

                            realm.beginTransaction();
                            if (chatRoom == null) {
                                chatRoom = new Chat();
                                chatRoom.setJid(item.getEntityID().toString());
                                chatRoom.setType(Chat.TYPE_MUC_LIGHT);
                                getSubject(chatRoom);
                            }
                            chatRoom.setShow(true);
                            chatRoom.setName(item.getName());
                            realm.copyToRealmOrUpdate(chatRoom);
                            realm.commitTransaction();

                            // set last retrieved from MAM
                            ChatMessage chatMessage = RealmManager.getLastMessageForChat(chatRoom.getJid());
                            if (chatMessage != null) {
                                realm.beginTransaction();
                                chatRoom.setLastRetrievedFromMAM(chatMessage.getMessageId());
                                realm.copyToRealmOrUpdate(chatRoom);
                                realm.commitTransaction();
                            }

                        }
                    }

                } finally {
                    realm.close();
                    mListener.onRoomsLoaded();
                }

            }
        }
    }

    private void getSubject(Chat chatRoom) {
        try {
            MultiUserChatLight multiUserChatLight = XMPPSession.getInstance().getMUCLightManager().getMultiUserChatLight(JidCreate.from(chatRoom.getJid()).asEntityBareJidIfPossible());
            MUCLightRoomConfiguration configuration = multiUserChatLight.getConfiguration();
            chatRoom.setSubject(configuration.getSubject());
        } catch (XmppStringprepException | SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String createCommonChat(User user) {
        String jid = XMPPUtils.fromUserNameToJID(user.getLogin());
        createChatIfNotExists(jid, true);
        return jid;
    }

    public static ChatManager getChatManager() {
        return ChatManager.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
    }

    public static MultiUserChat createMUC(List<User> users, String roomName, String nickName) {
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

    public static MultiUserChatLight createMUCLight(List<User> users, String roomName) {
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

    public static void sendInvitations(MultiUserChat multiUserChat, String roomName, List<User> users) {
        for (User user : users) {
            String userJID = XMPPUtils.fromUserNameToJID(user.getLogin());
            try {
                multiUserChat.invite(new Message(), userJID, "I invite you to the room " + roomName);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void createChatIfNotExists(String chatJid, final boolean save) {
        if (!RealmManager.chatExists(chatJid)) {
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
                RealmManager.saveChat(chat);
            }
        }
    }

    private static void findMUCName(Chat chat) {
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

    private static void findMUCLightName(Chat chat) {
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

    public void loadArchivedMessages(final String chatJid) {
        Tasks.executeInBackground(MangostaApplication.getInstance(), new BackgroundWork<Stanza>() {
            @Override
            public Stanza doInBackground() throws Exception {
                Realm realm = RealmManager.getRealm();
                Chat chat = realm.where(Chat.class).equalTo("jid", chatJid).findFirst();
                MamManager mamManager = XMPPSession.getInstance().getMamManager();
                int pageSize = 15;

                Jid jid = JidCreate.from(chatJid);
                MamManager.MamQueryResult mamQueryResult;
                if (chat == null || chat.getLastRetrievedFromMAM() == null) {
                    mamQueryResult = mamManager.queryArchive(pageSize, null, null, jid, null);
                } else {
                    mamQueryResult = mamManager.pageAfter(jid, chat.getLastRetrievedFromMAM(), pageSize);
                }

                while (!mamQueryResult.mamFin.isComplete()) {
                    mamQueryResult = mamManager.pageNext(mamQueryResult, pageSize);
                }

                if (mamQueryResult.forwardedMessages.size() > 0) {

                    if (chat == null || !chat.isValid()) {
                        loadMUCLightRooms();
                    } else {
                        if (!realm.isInTransaction()) {
                            realm.beginTransaction();
                        }
                        chat = realm.where(Chat.class).equalTo("jid", chatJid).findFirst();
                        chat.setLastRetrievedFromMAM(mamQueryResult.mamFin.getRSMSet().getLast());
                        realm.copyToRealmOrUpdate(chat);
                        realm.commitTransaction();
                    }

                }

                realm.close();

                if (mamQueryResult.forwardedMessages.size() > 0) {
                    return mamQueryResult.forwardedMessages.get(mamQueryResult.forwardedMessages.size() - 1).getForwardedStanza();
                } else {
                    return null;
                }
            }
        }, new Completion<Stanza>() {
            @Override
            public void onSuccess(Context context, Stanza result) {
                XMPPSession.getInstance().loadCorrectionMessages();
                XMPPSession.getInstance().publishQueryArchive(result);
            }

            @Override
            public void onError(Context context, Exception e) {
                XMPPSession.getInstance().loadCorrectionMessages();
                XMPPSession.getInstance().publishQueryArchive(null);
            }
        });
    }

    public void leaveMUC(final String jid) {

        try {
            Presence presence = new Presence(Presence.Type.unavailable);
            presence.setTo(JidCreate.from(jid));
            XMPPSession.getInstance().getXMPPConnection().sendStanza(presence);

            Realm realm = RealmManager.getRealm();
            realm.beginTransaction();

            Chat chat = realm.where(Chat.class).equalTo("jid", jid).findFirst();
            if (chat != null) {
                chat.setShow(false);
            }

            realm.commitTransaction();
            realm.close();

        } catch (Exception e) {
            mListener.onError(e.getLocalizedMessage());
        } finally {
            mListener.onRoomLeft();
        }

    }

    public void leaveMUCLight(final String jid) {

        MultiUserChatLightManager manager = XMPPSession.getInstance().getMUCLightManager();

        try {
            MultiUserChatLight multiUserChatLight = manager.getMultiUserChatLight(JidCreate.from(jid).asEntityBareJidIfPossible());
            multiUserChatLight.leave();

            Realm realm = RealmManager.getRealm();
            realm.beginTransaction();

            Chat chat = realm.where(Chat.class).equalTo("jid", jid).findFirst();
            if (chat != null) {
                chat.setShow(false);
                chat.deleteFromRealm();
            }

            realm.commitTransaction();
            realm.close();
        } catch (Exception e) {
            Context context = MangostaApplication.getInstance();
            Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show();
            mListener.onError(e.getLocalizedMessage());
        } finally {
            mListener.onRoomLeft();
        }
    }

    public void leave1to1Chat(String chatJid) {
        Realm realm = RealmManager.getRealm();

        Chat chat = realm.where(Chat.class).equalTo("jid", chatJid).findFirst();

        realm.beginTransaction();
        chat.deleteFromRealm();
        realm.commitTransaction();
        realm.close();

        mListener.onRoomLeft();
    }

    public void destroyMUCLight(final String jid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MultiUserChatLightManager manager = XMPPSession.getInstance().getMUCLightManager();

                try {
                    MultiUserChatLight multiUserChatLight = manager.getMultiUserChatLight(JidCreate.from(jid).asEntityBareJidIfPossible());
                    multiUserChatLight.destroy();
                } catch (Exception e) {
                    Context context = MangostaApplication.getInstance();
                    Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show();
                    mListener.onError(e.getLocalizedMessage());
                } finally {
                    mListener.onRoomLeft();
                }

            }
        }).start();
    }

    public void loadMembers(final String jid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (jid.contains(XMPPSession.MUC_SERVICE_NAME)) {
                    MultiUserChatManager manager = XMPPSession.getInstance().getMUCManager();

                    List<Affiliate> members = new ArrayList<>();

                    try {
                        MultiUserChat muc = manager.getMultiUserChat(JidCreate.from(jid).asEntityBareJidIfPossible());

                        //Members and owners are added to the same list.
                        members.addAll(muc.getMembers());
                        members.addAll(muc.getOwners());
                    } catch (Exception e) {
                        mListener.onError(e.getLocalizedMessage());
                    } finally {
                        mListener.onRoomMembersLoaded(members);
                    }
                }
            }
        }).start();
    }

    public void sendTextMessage(String messageId, String jid, String content, int chatType) {
        sendMessage(messageId, jid, content, chatType, false);
    }

    public void sendStickerMessage(String messageId, String jid, String content, int chatType) {
        sendMessage(messageId, jid, content, chatType, true);
    }

    private void sendMessage(final String messageId, final String jid, final String content, final int chatType, final boolean isSticker) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message message;

                try {
                    message = new Message(JidCreate.from(jid), content);
                    manageBoBExtension(message);
                } catch (XmppStringprepException e) {
                    e.printStackTrace();
                    mListener.onError(e.getLocalizedMessage());
                    return;
                }

                message.setStanzaId(messageId);
                sendMessageDependingOnType(message, jid, chatType);
            }

            private void manageBoBExtension(Message message) {
                if (isSticker) {
                    BoBHash bobHash = new BoBHash(Base64.encode(content), "base64");
                    message.addExtension(new BoBExtension(bobHash, null, null));
                }
            }
        }).start();
    }

    private void sendMessageDependingOnType(Message message, String jid, int chatType) {
        if (chatType == Chat.TYPE_MUC) {
            MultiUserChatManager manager = XMPPSession.getInstance().getMUCManager();

            try {
                MultiUserChat muc = manager.getMultiUserChat(JidCreate.from(jid).asEntityBareJidIfPossible());
                muc.sendMessage(message);
            } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException e) {
                mListener.onError(e.getLocalizedMessage());
            } finally {
                mListener.onMessageSent(message);
            }

        } else if (chatType == Chat.TYPE_MUC_LIGHT) {
            MultiUserChatLightManager manager = XMPPSession.getInstance().getMUCLightManager();

            try {
                MultiUserChatLight multiUserChatLight = manager.getMultiUserChatLight(JidCreate.from(jid).asEntityBareJidIfPossible());
                multiUserChatLight.sendMessage(message);
            } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException e) {
                mListener.onError(e.getLocalizedMessage());
            } finally {
                mListener.onMessageSent(message);
            }

        } else {
            ChatManager chatManager = getChatManager();
            try {
                chatManager.createChat(JidCreate.from(jid).asEntityJidIfPossible()).sendMessage(message);
            } catch (InterruptedException | XmppStringprepException | SmackException.NotConnectedException e) {
                mListener.onError(e.getLocalizedMessage());
            } finally {
                mListener.onMessageSent(message);
            }
        }
    }

    public void updateTypingStatus(final ChatState chatState, final String jid, final int chatType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Message message = new Message(JidCreate.from(jid));
                    message.addExtension(new ChatStateExtension(chatState));

                    if (chatType == Chat.TYPE_1_T0_1) {
                        message.setType(Message.Type.chat);
                    } else {
                        message.setType(Message.Type.groupchat);
                    }

                    sendMessageDependingOnType(message, jid, chatType);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void loadRosterFriendsChats() throws SmackException.NotLoggedInException, InterruptedException, SmackException.NotConnectedException {
        for (RosterEntry entry : RosterManager.getBuddies()) {
            String userJid = entry.getJid().toString();
            RoomManager.createChatIfNotExists(userJid, true);
        }
    }

}
