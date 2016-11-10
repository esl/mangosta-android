package inaka.com.mangosta.realm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import inaka.com.mangosta.chat.RoomsListManager;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.BlogPostComment;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class RealmManager {

    private static RealmManager mInstance;
    private static boolean mIsTesting = false;

    public static RealmManager getInstance() {
        if (mInstance == null) {
            mInstance = new RealmManager();
        }
        return mInstance;
    }

    public static void setSpecialInstanceForTesting(RealmManager realmManager) {
        mInstance = realmManager;
        mIsTesting = true;
    }

    public static boolean isTesting() {
        return mIsTesting;
    }

    public void saveChatMessage(ChatMessage chatMessage) {
        Realm realm = getRealm();

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(chatMessage);
        realm.commitTransaction();

        realm.close();
    }

    public void saveChat(Chat chat) {
        Realm realm = getRealm();

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(chat);
        realm.commitTransaction();

        realm.close();
    }

    public List<Chat> getMUCs() {
        List<Chat> chatList = new ArrayList<>();

        Realm realm = getRealm();
        RealmResults<Chat> chats = realm.where(Chat.class)
                .equalTo("show", true)
                .equalTo("type", Chat.TYPE_MUC)
                .findAll();

        for (Chat chat : chats) {
            chatList.add(chat);
        }

        return chatList;
    }

    public List<Chat> getMUCLights() {
        List<Chat> chatList = new ArrayList<>();

        Realm realm = getRealm();
        RealmResults<Chat> chats = realm.where(Chat.class)
                .equalTo("show", true)
                .equalTo("type", Chat.TYPE_MUC_LIGHT)
                .findAll();

        for (Chat chat : chats) {
            chatList.add(chat);
        }

        return chatList;
    }

    public List<Chat> get1to1Chats() {
        List<Chat> chatList = new ArrayList<>();

        Realm realm = getRealm();
        RealmResults<Chat> chats = realm.where(Chat.class)
                .equalTo("show", true)
                .equalTo("type", Chat.TYPE_1_T0_1)
                .notEqualTo("jid", Preferences.getInstance().getUserXMPPJid())
                .findAll();

        for (Chat chat : chats) {
            chatList.add(chat);
        }

        return chatList;
    }

    public boolean chatExists(String chatFromJID) {
        Realm realm = getRealm();

        boolean hasChat = realm.where(Chat.class)
                .equalTo("jid", chatFromJID)
                .count() > 0;

        realm.close();

        return hasChat;
    }

    public boolean chatMessageExists(String messageId) {
        Realm realm = getRealm();

        boolean hasChat = realm.where(ChatMessage.class)
                .equalTo("messageId", messageId)
                .count() > 0;

        realm.close();

        return hasChat;
    }

    public ChatMessage getChatMessage(String messageId) {
        Realm realm = getRealm();
        ChatMessage chatMessage =
                realm.where(ChatMessage.class)
                        .equalTo("messageId", messageId)
                        .findFirst();
        realm.close();

        return chatMessage;
    }

    public Chat getChat(String chatJid) {
        Realm realm = getRealm();
        Chat chat = realm.where(Chat.class)
                .equalTo("jid", chatJid)
                .findFirst();
        realm.close();

        return chat;
    }

    public Realm getRealm() {
        Realm.init(MangostaApplication.getInstance());
        return Realm.getDefaultInstance();
    }

    public RealmResults<ChatMessage> getMessagesForChat(Realm realm, String jid) {
        return realm.where(ChatMessage.class)
                .equalTo("roomJid", jid)
                .isNotEmpty("content")
                .findAllSorted("date", Sort.ASCENDING);
    }

    public ChatMessage getLastMessageSentByMeForChat(String jid) {
        Realm realm = getRealm();
        RealmResults<ChatMessage> chatMessages =
                realm.where(ChatMessage.class)
                        .equalTo("roomJid", jid)
                        .isNotEmpty("content")
                        .equalTo("userSender", XMPPUtils.fromJIDToUserName(Preferences.getInstance().getUserXMPPJid()))
                        .findAllSorted("date", Sort.ASCENDING);
        realm.close();

        return chatMessages.last();
    }

    public ChatMessage getLastMessageForChat(String jid) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (ChatMessage chatMessage : getMessagesForChat(getRealm(), jid)) {
            chatMessages.add(chatMessage);
        }
        if (chatMessages.size() == 0) {
            return null;
        } else {
            return chatMessages.get(chatMessages.size() - 1);
        }
    }

    public void saveBlogPost(BlogPost blogPost) {
        Realm realm = getRealm();

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(blogPost);
        realm.commitTransaction();

        realm.close();
    }

    public List<BlogPost> getBlogPosts() {
        List<BlogPost> blogPosts = new ArrayList<>();

        Realm realm = getRealm();

        RealmResults<BlogPost> blogPostRealmResults = realm.where(BlogPost.class).findAllSorted("updated", Sort.DESCENDING);

        for (BlogPost blogPost : blogPostRealmResults) {
            blogPosts.add(blogPost);
        }

        return blogPosts;
    }

    public void saveBlogPostComment(BlogPostComment comment) {
        Realm realm = getRealm();

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(comment);
        realm.commitTransaction();

        realm.close();
    }

    public List<BlogPostComment> getBlogPostComments(String blogPostId) {
        List<BlogPostComment> comments = new ArrayList<>();

        Realm realm = getRealm();

        RealmResults<BlogPostComment> blogPostComments = realm.where(BlogPostComment.class)
                .equalTo("blogPostId", blogPostId)
                .findAll();

        for (BlogPostComment comment : blogPostComments) {
            comments.add(comment);
        }

        return comments;
    }

    public void deleteMessage(String messageId) {
        Realm realm = getRealm();

        ChatMessage chatMessage = realm.where(ChatMessage.class)
                .equalTo("messageId", messageId)
                .findFirst();

        if (chatMessage != null) {
            realm.beginTransaction();
            chatMessage.deleteFromRealm();
            realm.commitTransaction();
        }

        realm.close();
    }

    public void deleteChatAndItsMessages(String jid) {
        Realm realm = getRealm();

        Chat chat = realm.where(Chat.class)
                .equalTo("jid", jid)
                .findFirst();

        if (chat != null) {
            realm.beginTransaction();
            chat.deleteFromRealm();
            realm.commitTransaction();
        }

//        RealmResults<ChatMessage> chatMessages =
//                realm.where(ChatMessage.class)
//                        .equalTo("roomJid", jid)
//                        .findAll();
//
//        for (ChatMessage chatMessage : chatMessages) {
//            realm.beginTransaction();
//            chatMessage.deleteFromRealm();
//            realm.commitTransaction();
//        }

        realm.close();
    }

    public void removeAllMUCChats() {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.where(Chat.class)
                .equalTo("type", Chat.TYPE_MUC)
                .findAll().deleteAllFromRealm();
        realm.commitTransaction();
        realm.close();
    }

    public void removeAllMUCLightChats() {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.where(Chat.class)
                .equalTo("type", Chat.TYPE_MUC_LIGHT)
                .findAll().deleteAllFromRealm();
        realm.commitTransaction();
        realm.close();
    }

    public void removeAllOneToOneChats() {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.where(Chat.class)
                .equalTo("type", Chat.TYPE_1_T0_1)
                .findAll().deleteAllFromRealm();
        realm.commitTransaction();
        realm.close();
    }

    public void hideAllChatsOfType(int type) {
        Realm realm = getRealm();

        RealmResults<Chat> chats = realm.where(Chat.class)
                .equalTo("type", type)
                .findAll();

        for (Chat chat : chats) {
            realm.beginTransaction();
            chat.setShow(false);
            realm.copyToRealmOrUpdate(chat);
            realm.commitTransaction();
        }

        realm.close();
    }

    public void hideAllMUCChats() {
        hideAllChatsOfType(Chat.TYPE_MUC);
    }

    public void hideAllMUCLightChats() {
        hideAllChatsOfType(Chat.TYPE_MUC_LIGHT);
    }

    public Chat getChatFromRealm(Realm realm, String mChatJID) {
        return realm.where(Chat.class).equalTo("jid", mChatJID).findFirst();
    }

    public String saveMessageLocally(Chat chat, String chatJID, String content, int type) {
        RoomsListManager.getInstance().createChatIfNotExists(chatJID, true);
        chat = RealmManager.getInstance().getChatFromRealm(getRealm(), chatJID);

        String messageId = UUID.randomUUID().toString();

        ChatMessage chatMessage = new ChatMessage();

        chatMessage.setMessageId(messageId);
        chatMessage.setRoomJid(chat.getJid());
        chatMessage.setUserSender(XMPPUtils.fromJIDToUserName(Preferences.getInstance().getUserXMPPJid()));
        chatMessage.setStatus(ChatMessage.STATUS_SENDING);
        chatMessage.setDate(new Date());
        chatMessage.setType(type);
        chatMessage.setContent(content);

        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(chatMessage);
        realm.commitTransaction();
        realm.close();

        return messageId;
    }

}
