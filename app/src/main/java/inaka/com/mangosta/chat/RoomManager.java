package inaka.com.mangosta.chat;

import android.app.Activity;
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
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import inaka.com.mangosta.R;
import inaka.com.mangosta.interfaces.MongooseService;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.models.MongooseMUCLight;
import inaka.com.mangosta.models.MongooseMUCLightMessage;
import inaka.com.mangosta.models.MongooseMessage;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.models.requests.AddUserRequest;
import inaka.com.mangosta.models.requests.CreateMUCLightMessageRequest;
import inaka.com.mangosta.models.requests.CreateMUCLightRequest;
import inaka.com.mangosta.models.requests.CreateMessageRequest;
import inaka.com.mangosta.models.responses.MongooseIdResponse;
import inaka.com.mangosta.network.MongooseAPI;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.NavigateToChat;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import inaka.com.mangosta.xmpp.bob.BoBHash;
import inaka.com.mangosta.xmpp.bob.elements.BoBExtension;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLight;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLightManager;
import io.realm.Realm;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


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
        MongooseService mongooseService = MongooseAPI.getAuthenticatedService();

        if (connection.isAuthenticated() && mongooseService != null) {

            Call<List<MongooseMUCLight>> call = mongooseService.getMUCLights();

            try {
                List<MongooseMUCLight> rooms = call.execute().body();
                processMUCLightRooms(rooms);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mListener.onRoomsLoaded();
            }

        }
    }

    public void loadMUCLightRoomsInBackground() {
        final XMPPTCPConnection connection = XMPPSession.getInstance().getXMPPConnection();
        MongooseService mongooseService = MongooseAPI.getAuthenticatedService();

        if (connection.isAuthenticated() && mongooseService != null) {
            Call<List<MongooseMUCLight>> call = mongooseService.getMUCLights();
            call.enqueue(new Callback<List<MongooseMUCLight>>() {
                @Override
                public void onResponse(Call<List<MongooseMUCLight>> call, Response<List<MongooseMUCLight>> response) {
                    processMUCLightRooms(response.body());
                    mListener.onRoomsLoaded();
                }

                @Override
                public void onFailure(Call<List<MongooseMUCLight>> call, Throwable t) {
                    t.printStackTrace();
                    mListener.onRoomsLoaded();
                }
            });
        }
    }

    private void processMUCLightRooms(List<MongooseMUCLight> rooms) {

        if (rooms != null) {
            RealmManager.hideAllMUCLightChats();
            Realm realm = RealmManager.getRealm();

            for (MongooseMUCLight room : rooms) {
                String itemJid = room.getId() + "@" + XMPPSession.MUC_LIGHT_SERVICE_NAME;


                Chat chatRoom = realm.where(Chat.class).equalTo("jid", itemJid).findFirst();

                if (chatRoom == null) {
                    chatRoom = new Chat();
                    chatRoom.setJid(itemJid);
                    chatRoom.setType(Chat.TYPE_MUC_LIGHT);
                }

                realm.beginTransaction();
                chatRoom.setShow(true);
                chatRoom.setName(room.getName());
                chatRoom.setSubject(room.getSubject());
                realm.copyToRealmOrUpdate(chatRoom);
                realm.commitTransaction();

                // set last retrieved from MAM
                ChatMessage chatMessage = RealmManager.getLastMessageForChat(chatRoom.getJid());
                if (chatMessage != null) {
                    realm.beginTransaction();
                    chatRoom.setLastTimestampRetrieved(chatMessage.getDate().getTime());
                    realm.copyToRealmOrUpdate(chatRoom);
                    realm.commitTransaction();
                }
            }

            realm.close();
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

    public static void createMUCLightAndGo(List<User> users, final String roomName, final Activity context) {
        final List<String> occupants = new ArrayList<>();

        for (User user : users) {
            String jid = XMPPUtils.fromUserNameToJID(user.getLogin());
            occupants.add(jid);
        }

        final MongooseService mongooseService = MongooseAPI.getAuthenticatedService();

        if (mongooseService != null) {

            Call<MongooseIdResponse> callCreate = mongooseService.createMUCLight(new CreateMUCLightRequest("", roomName));
            callCreate.enqueue(new Callback<MongooseIdResponse>() {
                @Override
                public void onResponse(Call<MongooseIdResponse> call, Response<MongooseIdResponse> response) {
                    final MongooseIdResponse idResponse = response.body();

                    if (idResponse != null) {
                        final String lastOccupant = occupants.get(occupants.size() - 1);
                        for (String jid : occupants) {
                            addOccupant(idResponse.getId(), lastOccupant, jid);
                        }
                    }

                }

                private void addOccupant(final String mucLightId, final String lastOccupant, String jid) {
                    final Call<Object> callAddUser = mongooseService.addUserToMUCLight(mucLightId, new AddUserRequest(jid));
                    callAddUser.enqueue(new Callback<Object>() {
                        @Override
                        public void onResponse(Call<Object> call, Response<Object> response) {
                            String body = bodyToString(callAddUser.request().body());
                            if (body.substring(body.lastIndexOf(":") + 2, body.length() - 2).equals(lastOccupant)) {
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        NavigateToChat.go(mucLightId + "@" + XMPPSession.MUC_LIGHT_SERVICE_NAME, roomName, context);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<Object> call, Throwable t) {
                            Toast.makeText(context, context.getString(R.string.error_create_chat), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(Call<MongooseIdResponse> call, Throwable t) {
                    Toast.makeText(context, context.getString(R.string.error_create_chat), Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    private static String bodyToString(final okhttp3.RequestBody request) {
        try {
            final okhttp3.RequestBody copy = request;
            final Buffer buffer = new Buffer();
            if (copy != null)
                copy.writeTo(buffer);
            else
                return "";
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "";
        }
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
        Realm realm = RealmManager.getRealm();
        Chat chat = realm.where(Chat.class).equalTo("jid", chatJid).findFirst();

        long timestamp = chat.getLastTimestampRetrieved();
        int chatType = chat.getType();

        realm.close();

        switch (chatType) {
            case Chat.TYPE_MUC_LIGHT:
                getMUCLightMessages(chatJid);
                break;

            case Chat.TYPE_1_T0_1:
                getMessages(chatJid, timestamp);
                break;
        }

    }

    private void getMessages(final String jid, long timestamp) {
        final int pageSize = 15;

        MongooseService mongooseService = MongooseAPI.getAuthenticatedService();

        if (mongooseService != null) {

            Call<List<MongooseMessage>> call;
            if (timestamp == 0) {
                call = mongooseService.getMessages(jid, pageSize);
            } else {
                call = mongooseService.getMessages(jid, pageSize, timestamp);
            }

            call.enqueue(new Callback<List<MongooseMessage>>() {
                @Override
                public void onResponse(Call<List<MongooseMessage>> call, Response<List<MongooseMessage>> response) {
                    List<MongooseMessage> messages = response.body();

                    if (messages != null) {
                        long lastTimestamp = 0;
                        if (messages.size() > 0) {
                            lastTimestamp = messages.get(0).getTimestamp();
                        }

                        if (lastTimestamp != 0) {
                            setLastTimestamp(lastTimestamp);
                        }

                        saveMessages(messages, jid);

                        if (messages.size() == pageSize) { // get more pages
                            getMessages(jid, lastTimestamp);
                        } else { // show list
                            XMPPSession.getInstance().publishQueryArchive(null);
                        }
                    } else {
                        XMPPSession.getInstance().publishQueryArchive(null);
                    }

                }

                private void setLastTimestamp(long lastTimestamp) {
                    Realm realm = RealmManager.getRealm();
                    realm.beginTransaction();
                    Chat chat = realm.where(Chat.class).equalTo("jid", jid).findFirst();
                    chat.setLastTimestampRetrieved(lastTimestamp);
                    realm.copyToRealmOrUpdate(chat);
                    realm.commitTransaction();
                    realm.close();
                }

                @Override
                public void onFailure(Call<List<MongooseMessage>> call, Throwable t) {
                    t.printStackTrace();
                }
            });
        }

    }

    private void getMUCLightMessages(final String jid) {
        MongooseService mongooseService = MongooseAPI.getAuthenticatedService();

        if (mongooseService != null) {
            Call<List<MongooseMUCLightMessage>> call = mongooseService.getMUCLightMessages(jid.split("@")[0]);
            call.enqueue(new Callback<List<MongooseMUCLightMessage>>() {
                @Override
                public void onResponse(Call<List<MongooseMUCLightMessage>> call, Response<List<MongooseMUCLightMessage>> response) {
                    List<MongooseMUCLightMessage> messages = response.body();

                    if (messages != null) {
                        saveMUCLightMessages(messages, jid);
                    }

                    XMPPSession.getInstance().publishQueryArchive(null);
                }

                @Override
                public void onFailure(Call<List<MongooseMUCLightMessage>> call, Throwable t) {
                    t.printStackTrace();
                }
            });
        }

    }

    private void saveMessages(List<MongooseMessage> messages, String roomJid) {

        for (MongooseMessage message : messages) {

            if (!RealmManager.chatMessageExists(message.getId()) &&
                    !message.getTo().equals("undefined") &&
                    !message.getBody().contains("xmlns='http://jabber.org/protocol/chatstates'")) {

                String jidTo = message.getTo().split("/")[0];
                String me = XMPPSession.getInstance().getXMPPConnection().getUser().toString().split("/")[0];

                Realm realm = RealmManager.getRealm();
                realm.beginTransaction();

                ChatMessage chatMessage = makeChatMessage(message, roomJid);

                if (jidTo.equals(me)) {
                    chatMessage.setUserSender(XMPPUtils.fromJIDToUserName(roomJid));
                } else {
                    chatMessage.setUserSender(XMPPUtils.fromJIDToUserName(me));
                }

                realm.copyToRealmOrUpdate(chatMessage);
                realm.commitTransaction();
                realm.close();

                XMPPSession.getInstance().mMongooseMessagePublisher.onNext(message);
            }
        }

    }

    private void saveMUCLightMessages(List<MongooseMUCLightMessage> messages, String roomJid) {

        for (MongooseMUCLightMessage message : messages) {

            if (!RealmManager.chatMessageExists(message.getId()) &&
                    message.getType().equals("message") &&
                    !message.getBody().contains("xmlns='http://jabber.org/protocol/chatstates'")) {

                String userFrom = message.getFrom().split("/")[0];

                Realm realm = RealmManager.getRealm();
                realm.beginTransaction();

                ChatMessage chatMessage = makeMUCLightMessage(message, userFrom, roomJid);

                realm.copyToRealmOrUpdate(chatMessage);
                realm.commitTransaction();
                realm.close();

                XMPPSession.getInstance().mMongooseMUCLightMessagePublisher.onNext(message);
            }
        }

    }

    private ChatMessage makeChatMessage(MongooseMessage message, String roomJid) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessageId(message.getId());
        chatMessage.setRoomJid(roomJid);
        chatMessage.setStatus(ChatMessage.STATUS_SENT);
        chatMessage.setUnread(true);
        chatMessage.setContent(message.getBody());
        chatMessage.setType(ChatMessage.TYPE_CHAT);
        chatMessage.setDate(getDate(message.getTimestamp()));
        return chatMessage;
    }

    private ChatMessage makeMUCLightMessage(MongooseMUCLightMessage message, String userFrom, String roomJid) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessageId(message.getId());
        chatMessage.setRoomJid(roomJid);
        chatMessage.setStatus(ChatMessage.STATUS_SENT);
        chatMessage.setUnread(true);
        chatMessage.setContent(message.getBody());
        chatMessage.setType(ChatMessage.TYPE_CHAT);
        chatMessage.setDate(getDate(message.getTimestamp()));
        chatMessage.setUserSender(XMPPUtils.fromJIDToUserName(userFrom));
        return chatMessage;
    }

    private Date getDate(long time) {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone(); // get the local time zone.
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault());
        sdf.setTimeZone(tz); // set time zone.
        String localTime = sdf.format(new Date(time));
        Date date = new Date();
        try {
            date = sdf.parse(localTime); // get local date
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
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
            mListener.onRoomLeft(jid);
        }

    }

    public void leaveMUCLight(final String jid) {
        MongooseService mongooseService = MongooseAPI.getAuthenticatedService();
        String authenticatedUser = XMPPSession.getInstance().getXMPPConnection().getUser().asEntityBareJid().toString();

        try {
            if (mongooseService != null) {

                Call<Object> call = mongooseService.removeUserFromMUCLight(jid.split("@")[0], authenticatedUser);
                call.enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        Realm realm = RealmManager.getRealm();
                        realm.beginTransaction();

                        Chat chat = realm.where(Chat.class).equalTo("jid", jid).findFirst();
                        if (chat != null) {
                            chat.setShow(false);
                            chat.deleteFromRealm();
                        }

                        realm.commitTransaction();
                        realm.close();
                    }

                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        Context context = MangostaApplication.getInstance();
                        Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show();
                        mListener.onError(t.getLocalizedMessage());
                    }
                });
            }

        } finally {
            mListener.onRoomLeft(jid);
        }
    }

    public void leave1to1Chat(String chatJid) {
        Realm realm = RealmManager.getRealm();

        Chat chat = realm.where(Chat.class).equalTo("jid", chatJid).findFirst();

        realm.beginTransaction();
        chat.deleteFromRealm();
        realm.commitTransaction();
        realm.close();

        mListener.onRoomLeft(chatJid);
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
                    mListener.onRoomLeft(jid);
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
        if (chatType == Chat.TYPE_MUC) {
            Message message = new Message();
            message.setBody(content);
            message.setStanzaId(messageId);
            sendXMPPMessageDependingOnType(message, jid, chatType);
        } else {
            sendRestMessageDependingOnType(content, jid, chatType);
        }
    }

    public void sendStickerMessage(final String messageId, final String jid, final String content, final int chatType) {
        Tasks.executeInBackground(MangostaApplication.getInstance(), new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                Message message = new Message(JidCreate.from(jid), content);
                BoBHash bobHash = new BoBHash(Base64.encode(content), "base64");
                message.addExtension(new BoBExtension(bobHash, null, null));
                message.setStanzaId(messageId);
                sendXMPPMessageDependingOnType(message, jid, chatType);
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                mListener.onMessageSent(null);
            }

            @Override
            public void onError(Context context, Exception e) {
                mListener.onError(e.getLocalizedMessage());
            }
        });
    }

    private void sendRestMessageDependingOnType(final String content, final String jid, int chatType) {

        try {
            MongooseService mongooseService = MongooseAPI.getAuthenticatedService();

            if (mongooseService != null) {

                Call<MongooseIdResponse> call;
                if (chatType == Chat.TYPE_MUC_LIGHT) {
                    call = mongooseService.sendMessageToMUCLight(jid.split("@")[0], new CreateMUCLightMessageRequest(content));
                } else {
                    call = mongooseService.sendMessage(new CreateMessageRequest(jid, content));
                }

                call.enqueue(new Callback<MongooseIdResponse>() {
                    @Override
                    public void onResponse(Call<MongooseIdResponse> call, Response<MongooseIdResponse> response) {
                        MongooseIdResponse idResponse = response.body();
                        if (idResponse != null) {
                            saveMessageLocally(jid, content, idResponse.getId());
                            Toast.makeText(MangostaApplication.getInstance(), R.string.message_sent, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MongooseIdResponse> call, Throwable t) {
                        Toast.makeText(MangostaApplication.getInstance(), R.string.error_send_message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } finally {
            mListener.onMessageSent(null);
        }
    }

    private void sendXMPPMessageDependingOnType(Message message, String jid, int chatType) {
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
        try {
            Message message = new Message(JidCreate.from(jid));
            message.addExtension(new ChatStateExtension(chatState));
            sendXMPPMessageDependingOnType(message, jid, chatType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String saveMessageLocally(String chatJid, String content, String messageId) {
        RoomManager.createChatIfNotExists(chatJid, true);

        Realm realm = RealmManager.getRealm();
        Chat chat = realm.where(Chat.class).equalTo("jid", chatJid).findFirst();

        ChatMessage chatMessage = new ChatMessage();

        chatMessage.setMessageId(messageId);
        chatMessage.setRoomJid(chat.getJid());
        chatMessage.setUserSender(XMPPUtils.fromJIDToUserName(Preferences.getInstance().getUserXMPPJid()));
        chatMessage.setStatus(ChatMessage.STATUS_SENDING);
        chatMessage.setDate(new Date());
        chatMessage.setType(ChatMessage.TYPE_CHAT);
        chatMessage.setContent(content);

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(chatMessage);
        realm.commitTransaction();

        realm.close();

        return messageId;
    }

    public void loadRosterFriendsChats() throws SmackException.NotLoggedInException, InterruptedException, SmackException.NotConnectedException {
        for (RosterEntry entry : RosterManager.getBuddies()) {
            String userJid = entry.getJid().toString();
            RoomManager.createChatIfNotExists(userJid, true);
        }
    }

}
