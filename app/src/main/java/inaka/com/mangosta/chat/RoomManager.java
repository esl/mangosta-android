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
import org.jivesoftware.smackx.bob.BoBHash;
import org.jivesoftware.smackx.bob.element.BoBExtension;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.muclight.MUCLightAffiliation;
import org.jivesoftware.smackx.muclight.MUCLightRoomConfiguration;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.muclight.MultiUserChatLightManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                            ChatMessage chatMessage = RealmManager.getInstance().getFirstMessageForChat(chatRoom.getJid());
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

    public void loadArchivedMessages(final String chatJid, final int pages, final int pageSize) {
        Tasks.executeInBackground(MangostaApplication.getInstance(), new BackgroundWork<Stanza>() {
            @Override
            public Stanza doInBackground() throws Exception {
                Realm realm = RealmManager.getInstance().getRealm();
                Chat chat = realm.where(Chat.class).equalTo("jid", chatJid).findFirst();
                MamManager mamManager = XMPPSession.getInstance().getMamManager();

                Jid jid = JidCreate.from(chatJid);
                MamManager.MamQueryResult mamQueryResult;
                if (chat == null || chat.getLastRetrievedFromMAM() == null) {
                    mamQueryResult = mamManager.queryArchive(pageSize, null, new Date(), jid, null);
                } else {
                    mamQueryResult = mamManager.pageBefore(jid, chat.getLastRetrievedFromMAM(), pageSize);
                }

                int pagesCount = 0;
                while (!mamQueryResult.mamFin.isComplete() && pagesCount < pages) {
                    mamQueryResult = mamManager.pagePrevious(mamQueryResult, pageSize);
                    pagesCount++;
                }

                if (mamQueryResult.forwardedMessages.size() > 0) {

                    if (chat == null || !chat.isValid()) {
                        loadMUCLightRooms();
                    } else {
                        if (!realm.isInTransaction()) {
                            realm.beginTransaction();
                        }
                        chat = realm.where(Chat.class).equalTo("jid", chatJid).findFirst();
                        chat.setLastRetrievedFromMAM(mamQueryResult.mamFin.getRSMSet().getFirst());
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
        if (chatType == Chat.TYPE_MUC_LIGHT) {
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

    public void loadRosterContactsChats() throws SmackException.NotLoggedInException, InterruptedException, SmackException.NotConnectedException {
        try {
            HashMap<Jid, Presence.Type> buddies = RosterManager.getInstance().getBuddies();
            for (Map.Entry pair : buddies.entrySet()) {
                String userJid = pair.getKey().toString();
                RoomsListManager.getInstance().createChatIfNotExists(userJid, true);
            }
        } finally {
            mListener.onRoomsLoaded();
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

    public void addToMUCLight(User user, String chatJID) {
        MultiUserChatLightManager multiUserChatLightManager = XMPPSession.getInstance().getMUCLightManager();
        try {
            MultiUserChatLight mucLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(chatJID).asEntityBareJidIfPossible());

            Jid jid = JidCreate.from(XMPPUtils.fromUserNameToJID(user.getLogin()));

            HashMap<Jid, MUCLightAffiliation> affiliations = new HashMap<>();
            affiliations.put(jid, MUCLightAffiliation.member);

            mucLight.changeAffiliations(affiliations);
        } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        }
    }

    public void removeFromMUCLight(User user, String chatJID) {
        MultiUserChatLightManager multiUserChatLightManager = XMPPSession.getInstance().getMUCLightManager();
        try {
            MultiUserChatLight mucLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(chatJID).asEntityBareJidIfPossible());

            Jid jid = JidCreate.from(XMPPUtils.fromUserNameToJID(user.getLogin()));

            HashMap<Jid, MUCLightAffiliation> affiliations = new HashMap<>();
            affiliations.put(jid, MUCLightAffiliation.none);

            mucLight.changeAffiliations(affiliations);
        } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        }
    }

}
