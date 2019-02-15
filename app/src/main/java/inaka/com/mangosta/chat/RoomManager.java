package inaka.com.mangosta.chat;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.bob.BoBHash;
import org.jivesoftware.smackx.bob.element.BoBExtension;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muclight.MUCLightAffiliation;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.muclight.MultiUserChatLightManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import inaka.com.mangosta.R;
import inaka.com.mangosta.database.MangostaDatabase;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.network.MongooseService;
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
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomManager {
    private static final String TAG = RoomManager.class.getSimpleName();

    private static RoomManager instance;
    private static MangostaDatabase database = MangostaApplication.getInstance().getDatabase();

    private boolean mIsTesting;

    public static RoomManager getInstance() {
        if (instance == null) {
            instance = new RoomManager(false);
        }
        return instance;
    }

    public static RoomManager getTestInstance() {
        if (instance == null) {
            instance = new RoomManager(true);
        }
        return instance;
    }

    private RoomManager(boolean isTesting) {
        mIsTesting = isTesting;
    }

    public boolean isTesting() {
        return mIsTesting;
    }

    public void loadAllChats() {
        Completable task = Completable.fromCallable(() -> {
            loadRosterContactsChats(); // load 1 to 1 chats from contacts
            loadMUCLightRooms(); // load group chats
            return null;
        });

        Disposable d = task.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> Log.d(TAG, "loadChatsBackgroundTask complete"),
                        error -> Log.d(TAG, "loadChatsBackgroundTask error", error));
    }

    public void loadMUCLightRooms() {
        final XMPPTCPConnection connection = XMPPSession.getInstance().getXMPPConnection();
        MongooseService mongooseService = MongooseAPI.getInstance().getAuthenticatedService();

        if (connection.isAuthenticated() && mongooseService != null) {

            Call<List<MongooseMUCLight>> call = mongooseService.getMUCLights();

            try {
                List<MongooseMUCLight> rooms = call.execute().body();
                processMUCLightRooms(rooms);
            } catch (Exception e) {
                Log.e(TAG, "loadMUCLightRooms error", e);
            }
        }
    }

    private void processMUCLightRooms(List<MongooseMUCLight> rooms) {

        if (rooms != null) {
            Disposable d = Single.fromCallable(() -> database.chatDao().updateVisibilityAll(Chat.TYPE_MUC_LIGHT, false))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(done -> {
                        for (MongooseMUCLight room : rooms) {
                            String itemJid = room.getId() + "@" + XMPPSession.MUC_LIGHT_SERVICE_NAME;
                            Disposable d1 = database.chatDao().findByJid(itemJid)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(existingChat -> {
                                                //existing chat
                                                existingChat.setShow(true);
                                                existingChat.setName(room.getName());
                                                existingChat.setSubject(room.getSubject());
                                                Completable.fromAction(() -> database.chatDao().update(existingChat))
                                                        .subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe();
                                            }, error -> Log.w(TAG, "processMUCLightRooms error", error),
                                            () -> {
                                                //new chat
                                                Chat chat = new Chat();
                                                chat.setJid(itemJid);
                                                chat.setType(Chat.TYPE_MUC_LIGHT);
                                                chat.setShow(true);
                                                chat.setName(room.getName());
                                                chat.setSubject(room.getSubject());
                                                Completable.fromAction(() -> database.chatDao().insert(chat))
                                                        .subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe();
                                            });

                        }
                    });
        }

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

    public Completable createChat(final String chatJid) {
        return Completable.fromAction(() -> {
            Chat chat = new Chat();
            chat.setJid(chatJid);
            if (chatJid.contains(XMPPSession.MUC_LIGHT_SERVICE_NAME)) {
                chat.setType(Chat.TYPE_MUC_LIGHT);
                //chat.setSortPosition(RealmManager.getInstance().getMUCLights().size());
                findMUCLightName(chat);
            } else {
                chat.setType(Chat.TYPE_1_T0_1);
                //chat.setSortPosition(RealmManager.getInstance().get1to1Chats().size());
                if (!chatJid.equals(Preferences.getInstance().getUserXMPPJid())) {
                    chat.setName(XMPPUtils.fromJIDToUserName(chatJid));
                }
            }

            chat.setShow(true);
            chat.setDateCreated(new Date());

            Log.d(TAG, "createChat " + chatJid);
            database.chatDao().insert(chat);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
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

    public void setShowChat(Chat mChat) {
        Completable.fromAction(() -> database.chatDao().updateVisibilityByJid(mChat.getJid(), true))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    public void manageNewChat(Chat chat, String chatName, String chatJid) {
        if (chat == null) {
            chat = new Chat();
            chat.setJid(chatJid);

            if (chatJid.contains(XMPPSession.MUC_LIGHT_SERVICE_NAME)) {
                chat.setType(Chat.TYPE_MUC_LIGHT);
                //chatRoom.setSortPosition(RealmManager.getInstance().getMUCLights().size());
            } else {
                chat.setType(Chat.TYPE_1_T0_1);
                //chatRoom.setSortPosition(RealmManager.getInstance().get1to1Chats().size());
            }

            chat.setDateCreated(new Date());
        }
        chat.setName(chatName);
        chat.setShow(true);

        final Chat updateChat = chat;
        Completable.fromAction(() -> database.chatDao().update(updateChat))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    public void updateChatsSortPosition(List<Chat> chats) {
        for (int i = 0; i < chats.size(); i++) {
            Chat chat = chats.get(i);
            chat.setSortPosition(i);
        }

        Completable.fromAction(() -> database.chatDao().updateItems(chats));
    }

    public static void createMUCLight(List<User> users, final String roomName,
            Activity context, RoomManagerListener listener) {
        final List<String> occupants = new ArrayList<>();

        for (User user : users) {
            occupants.add(user.getJid());
        }

        final MongooseService mongooseService = MongooseAPI.getInstance().getAuthenticatedService();

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
                            if (Preferences.isTesting() || isLastOccupant(body)) {
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onRoomCreated(mucLightId + "@" + XMPPSession.MUC_LIGHT_SERVICE_NAME, roomName);
                                    }
                                });
                            }
                        }

                        private boolean isLastOccupant(String body) {
                            return body.substring(body.lastIndexOf(":") + 2, body.length() - 2).equals(lastOccupant);
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

    public void loadArchivedMessages(final Chat chat, int pages, int count) {

        long timestamp = chat.getLastTimestampRetrieved();
        int chatType = chat.getType();

        switch (chatType) {
            case Chat.TYPE_MUC_LIGHT:
                getMUCLightMessages(chat.getJid(), pages, count);
                break;

            case Chat.TYPE_1_T0_1:
                getMessages(chat.getJid(), timestamp, pages, count);
                break;
        }

    }

    private void getMessages(final String jid, long timestamp, final int pages, final int count) {
        MongooseService mongooseService = MongooseAPI.getInstance().getAuthenticatedService();

        if (mongooseService != null) {

            Call<List<MongooseMessage>> call;
            if (timestamp == 0) {
                call = mongooseService.getMessages(jid, count);
            } else {
                call = mongooseService.getMessages(jid, count, timestamp);
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

                        if (messages.size() == count) { // get more pages
                            getMessages(jid, lastTimestamp, pages, count);
                        } else { // show list
                            XMPPSession.getInstance().publishQueryArchive(null);
                        }
                    } else {
                        XMPPSession.getInstance().publishQueryArchive(null);
                    }
                }

                private void setLastTimestamp(long lastTimestamp) {
                    Disposable d = Completable.fromAction(() -> database.chatDao().updateLastTimestamp(jid, lastTimestamp))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe();
                }

                @Override
                public void onFailure(Call<List<MongooseMessage>> call, Throwable t) {
                    t.printStackTrace();
                }
            });
        }

    }

    private void getMUCLightMessages(final String jid, final int pages, final int count) {
        MongooseService mongooseService = MongooseAPI.getInstance().getAuthenticatedService();

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

            if (message.getTo().equals("undefined") || message.getBody().contains("xmlns='http://jabber.org/protocol/chatstates'")) {
                continue;
            }

            Disposable d = database.chatMessageDao().findByMessageId(message.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(found -> Log.d(TAG, "message id already exists"),
                    error -> Log.w(TAG, "message lookup error", error),
                    () -> {
                        //no existing messsage found, continue
                        String jidTo = message.getTo().split("/")[0];
                        String me = XMPPSession.getInstance().getXMPPConnection().getUser().toString().split("/")[0];

                        ChatMessage chatMessage = makeChatMessage(message, roomJid);

                        if (jidTo.equals(me)) {
                            chatMessage.setUserSender(XMPPUtils.fromJIDToUserName(roomJid));
                        } else {
                            chatMessage.setUserSender(XMPPUtils.fromJIDToUserName(me));
                        }

                        Disposable d1 = Single.fromCallable(() -> database.chatMessageDao().insert(chatMessage))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(result -> XMPPSession.getInstance().notifyPublished(message));
                    });
        }

    }

    private void saveMUCLightMessages(List<MongooseMUCLightMessage> messages, String roomJid) {

        for (MongooseMUCLightMessage message : messages) {

            if (!message.getType().equals("message") || message.getBody().contains("xmlns='http://jabber.org/protocol/chatstates'")) {
                continue;
            }

            Disposable d = database.chatMessageDao().findByMessageId(message.getId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(found -> Log.d(TAG, "message id already exists"),
                            error -> Log.w(TAG, "message lookup error", error),
                            () -> {
                                String userFrom = message.getFrom().split("/")[0];


                                ChatMessage chatMessage = makeMUCLightMessage(message, userFrom, roomJid);

                                Disposable d1 = Single.fromCallable(() -> database.chatMessageDao().insert(chatMessage))
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(result -> XMPPSession.getInstance().notifyMUCLightPublished(message));
                            });
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

    public void leaveMUCLight(final String jid, RoomManagerListener listener) {
        MongooseService mongooseService = MongooseAPI.getInstance().getAuthenticatedService();
        String authenticatedUser = XMPPSession.getInstance().getXMPPConnection().getUser().asEntityBareJid().toString();

        if (mongooseService != null) {

            Call<Object> call = mongooseService.removeUserFromMUCLight(jid.split("@")[0], authenticatedUser);
            call.enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    Disposable d = Single.fromCallable(() -> database.chatDao().deleteByJid(jid))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(result -> listener.onRoomLeft(jid),
                                    error -> listener.onError(error.getMessage()));
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    Context context = MangostaApplication.getInstance();
                    Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show();
                    listener.onError(t.getLocalizedMessage());
                }
            });
        }

    }

    public void leave1to1Chat(String chatJid, RoomManagerListener listener) {
        Disposable d = Completable.fromAction(() -> database.chatDao().deleteByJid(chatJid))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> listener.onRoomLeft(chatJid),
                        error -> listener.onError(error.getMessage()));
    }

    public void destroyMUCLight(final String jid, final RoomManagerListener listener) {
        Completable task = Completable.fromAction(() -> {
                MultiUserChatLightManager manager = XMPPSession.getInstance().getMUCLightManager();

                MultiUserChatLight multiUserChatLight = manager.getMultiUserChatLight(JidCreate.from(jid).asEntityBareJidIfPossible());
                multiUserChatLight.destroy();
            });
        Disposable d = task.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> listener.onRoomLeft(jid),
                        error -> {
                            //Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show();
                            listener.onError(error.getLocalizedMessage());
                });
    }

    public void sendTextMessage(String jid, boolean isMucLight, String content, RoomManagerListener listener) {
        sendRestMessageDependingOnType(jid, isMucLight, content, ChatMessage.TYPE_CHAT, listener);
    }

    public void sendStickerMessage(String jid, boolean isMucLight, String imageName, RoomManagerListener listener) {
        String messageId = UUID.randomUUID().toString();

        saveChatMessage(jid, imageName, ChatMessage.TYPE_STICKER, messageId);

        Completable task = Completable.fromCallable(() -> {
            Message message = new Message(JidCreate.from(jid), imageName);
            BoBHash bobHash = new BoBHash(Base64.encode(imageName), "base64");
            message.addExtension(new BoBExtension(bobHash, null, null));
            message.setStanzaId(messageId);
            sendXMPPMessageDependingOnType(jid, isMucLight, message, ChatMessage.TYPE_STICKER, listener);
            return null;
        });

        Disposable d = task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> listener.onMessageSent(ChatMessage.TYPE_STICKER),
                        error -> listener.onError(error.getLocalizedMessage())
                );
    }

    private void sendRestMessageDependingOnType(final String jid, boolean isMucLight,
            final String content, int chatType, RoomManagerListener listener) {

        MongooseService mongooseService = MongooseAPI.getInstance().getAuthenticatedService();

        if (mongooseService != null) {

            Call<MongooseIdResponse> call;
            if (isMucLight) {
                call = mongooseService.sendMessageToMUCLight(jid.split("@")[0], new CreateMUCLightMessageRequest(content));
            } else {
                call = mongooseService.sendMessage(new CreateMessageRequest(jid, content));
            }

            call.enqueue(new Callback<MongooseIdResponse>() {
                @Override
                public void onResponse(Call<MongooseIdResponse> call, Response<MongooseIdResponse> response) {
                    MongooseIdResponse idResponse = response.body();
                    if (idResponse != null) {
                        Disposable d = saveMessageLocally(jid, content, chatType, idResponse.getId())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(result -> {
                                        if (result > 0) {
                                           Toast.makeText(MangostaApplication.getInstance(), R.string.message_sent, Toast.LENGTH_SHORT).show();
                                            listener.onMessageSent(chatType);
                                        } else {
                                            Toast.makeText(MangostaApplication.getInstance(), R.string.error_send_message, Toast.LENGTH_SHORT).show();
                                            listener.onError("Error sending message");
                                        }
                                });

                        }
                }

                @Override
                public void onFailure(Call<MongooseIdResponse> call, Throwable t) {
                    Toast.makeText(MangostaApplication.getInstance(), R.string.error_send_message, Toast.LENGTH_SHORT).show();
                    listener.onError("Error sending message");
                }
            });
        }
    }

    private void sendXMPPMessageDependingOnType(String jid, boolean isMucLight,
            Message message, int chatType, RoomManagerListener listener) {
        if (isMucLight) {

            MultiUserChatLightManager manager = XMPPSession.getInstance().getMUCLightManager();

            try {
                MultiUserChatLight multiUserChatLight = manager.getMultiUserChatLight(JidCreate.from(jid).asEntityBareJidIfPossible());
                multiUserChatLight.sendMessage(message);
                listener.onMessageSent(chatType);
            } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException e) {
                listener.onError(e.getLocalizedMessage());
            }

        } else {
            ChatManager chatManager = getChatManager();
            try {
                chatManager.createChat(JidCreate.from(jid).asEntityJidIfPossible()).sendMessage(message);
                listener.onMessageSent(chatType);
            } catch (InterruptedException | XmppStringprepException | SmackException.NotConnectedException e) {
                listener.onError(e.getLocalizedMessage());
            }
        }
    }

    public void updateTypingStatus(final ChatState chatState, final String jid,
                                   final boolean isMucLight, final RoomManagerListener listener) {
        try {
            Message message = new Message(JidCreate.from(jid));
            message.addExtension(new ChatStateExtension(chatState));
            sendXMPPMessageDependingOnType(jid, isMucLight, message,
                    ChatMessage.TYPE_CHAT, listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Single<Long> saveMessageLocally(String chatJID, String content, int type, String messageId) {
        ChatMessage chatMessage = new ChatMessage();

        chatMessage.setMessageId(messageId);
        chatMessage.setRoomJid(chatJID);
        chatMessage.setUserSender(XMPPUtils.fromJIDToUserName(Preferences.getInstance().getUserXMPPJid()));
        chatMessage.setStatus(ChatMessage.STATUS_SENDING);
        chatMessage.setDate(new Date());
        chatMessage.setType(type);
        chatMessage.setContent(content);

        return Single.fromCallable(() -> database.chatMessageDao().insert(chatMessage));
    }

    public void createChatIfNotExists(String chatJID) {
        Disposable d = database.chatDao().findByJid(chatJID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(found -> {},
                        error -> Log.w(TAG,"query error", error),
                        () -> createChat(chatJID).subscribe()
                );
    }

    public void saveChatMessage(String chatJID, String content, int type, String messageId) {
        Disposable d = database.chatDao().findByJid(chatJID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(id -> saveMessageLocally(chatJID, content, type, messageId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(),
                error -> Log.w(TAG, "query error", error),
                () -> {
                        Log.w(TAG, "create missing chat object");
                        createChat(chatJID)
                                .andThen(saveMessageLocally(chatJID, content, type, messageId))
                                .subscribe();
                });
    }

    public void loadRosterContactsChats() throws SmackException.NotLoggedInException, InterruptedException, SmackException.NotConnectedException {
        HashMap<Jid, Presence.Type> buddies = RosterManager.getInstance().getContacts();
        for (Map.Entry pair : buddies.entrySet()) {
            String userJid = pair.getKey().toString();
            createChatIfNotExists(userJid);
        }
    }

    public List<String> loadMUCLightMembers(String roomJid) throws
            XmppStringprepException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException {
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

            Jid jid = JidCreate.from(user.getJid());

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

            Jid jid = JidCreate.from(user.getJid());

            HashMap<Jid, MUCLightAffiliation> affiliations = new HashMap<>();
            affiliations.put(jid, MUCLightAffiliation.none);

            mucLight.changeAffiliations(affiliations);
        } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        }
    }

}
