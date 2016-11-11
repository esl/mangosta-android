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
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import inaka.com.mangosta.R;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import inaka.com.mangosta.xmpp.bob.BoBHash;
import inaka.com.mangosta.xmpp.bob.elements.BoBExtension;
import inaka.com.mangosta.xmpp.mam.MamManager;
import inaka.com.mangosta.xmpp.muclight.MUCLightAffiliation;
import inaka.com.mangosta.xmpp.muclight.MUCLightRoomConfiguration;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLight;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLightManager;
import io.realm.Realm;


public class RoomManager {

    private RoomManagerListener mListener;
    private static RoomManager mInstance;
    private static boolean mIsTesting;

    private RoomManager(RoomManagerListener listener) {
        mListener = listener;
    }

    public static RoomManager getInstance(RoomManagerListener listener) {
        if (mInstance == null) {
            mInstance = new RoomManager(listener);
        }
        return mInstance;
    }

    public static boolean isTesting() {
        return mIsTesting;
    }

    public static void setSpecialInstanceForTesting(RoomManager roomManager) {
        mInstance = roomManager;
        mIsTesting = true;
    }

    public void loadMUCRooms() {

        final XMPPTCPConnection connection = XMPPSession.getInstance().getXMPPConnection();

        if (connection.isAuthenticated()) {

            DiscoverItems discoverItems = XMPPSession.getInstance().discoverMUCItems();
            if (discoverItems != null) {
                RealmManager.getInstance().hideAllMUCChats();

                List<DiscoverItems.Item> items = discoverItems.getItems();
                Realm realm = RealmManager.getInstance().getRealm();

                try {
                    for (DiscoverItems.Item item : items) {
                        String itemJid = item.getEntityID().toString();
                        String userJid = Preferences.getInstance().getUserXMPPJid();

                        if (itemJid.contains(XMPPSession.MUC_SERVICE_NAME)) {
                            EntityBareJid jid = item.getEntityID().asEntityBareJidIfPossible();
                            MultiUserChat multiUserChat = MultiUserChatManager.getInstanceFor(connection).getMultiUserChat(jid);

                            Chat chatRoom = realm.where(Chat.class).equalTo("jid", itemJid).findFirst();

                            try {

                                if (chatRoom == null) {
                                    chatRoom = new Chat();
                                    chatRoom.setJid(item.getEntityID().toString());
                                    chatRoom.setName(item.getName());
                                    chatRoom.setType(Chat.TYPE_MUC);

                                    realm.beginTransaction();
                                    realm.copyToRealmOrUpdate(chatRoom);
                                    realm.commitTransaction();
                                }

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
                            XMPPSession.getInstance().sendStanza(presence);
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
                RealmManager.getInstance().hideAllMUCLightChats();
                List<DiscoverItems.Item> items = discoverItems.getItems();
                Realm realm = RealmManager.getInstance().getRealm();

                try {
                    for (DiscoverItems.Item item : items) {
                        String itemJid = item.getEntityID().toString();

                        if (itemJid.contains(XMPPSession.MUC_LIGHT_SERVICE_NAME)) {

                            Chat chatRoom = realm.where(Chat.class).equalTo("jid", itemJid).findFirst();

                            if (chatRoom == null) {
                                chatRoom = new Chat();
                                chatRoom.setJid(item.getEntityID().toString());
                                chatRoom.setType(Chat.TYPE_MUC_LIGHT);
                                getSubject(chatRoom);
                            }

                            realm.beginTransaction();
                            chatRoom.setShow(true);
                            chatRoom.setName(item.getName());
                            realm.copyToRealmOrUpdate(chatRoom);
                            realm.commitTransaction();

                            // set last retrieved from MAM
                            ChatMessage chatMessage = RealmManager.getInstance().getLastMessageForChat(chatRoom.getJid());
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

    public void loadArchivedMessages(final String chatJid) {
        Tasks.executeInBackground(MangostaApplication.getInstance(), new BackgroundWork<Stanza>() {
            @Override
            public Stanza doInBackground() throws Exception {
                Realm realm = RealmManager.getInstance().getRealm();
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
            XMPPSession.getInstance().sendStanza(presence);

            Realm realm = RealmManager.getInstance().getRealm();
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

            Realm realm = RealmManager.getInstance().getRealm();
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
        Realm realm = RealmManager.getInstance().getRealm();

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
            ChatManager chatManager = RoomsListManager.getInstance().getChatManager();
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
        if (!Preferences.isTesting()) {
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
    }

    public void loadRosterFriendsChats() throws SmackException.NotLoggedInException, InterruptedException, SmackException.NotConnectedException {
        for (Jid jid : RosterManager.getInstance().getBuddies()) {
            String userJid = jid.toString();
            RoomsListManager.getInstance().createChatIfNotExists(userJid, true);
        }
    }

    public List<String> loadMUCLightMembers(String roomJid) throws XmppStringprepException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException {
        MultiUserChatLightManager multiUserChatLightManager = MultiUserChatLightManager.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        MultiUserChatLight multiUserChatLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(roomJid).asEntityBareJidIfPossible());

        HashMap<Jid, MUCLightAffiliation> occupants = multiUserChatLight.getAffiliations();
        List<String> jids = new ArrayList<>();

        for (Map.Entry<Jid, MUCLightAffiliation> pair : occupants.entrySet()) {
            Jid jid = pair.getKey();
            if (jid != null) {
                jids.add(jid.toString());
            }
        }
        return jids;
    }

}
