package inaka.com.mangosta.xmpp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import org.jivesoftware.smack.AbstractConnectionClosedListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ErrorIQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tbr.TBRManager;
import org.jivesoftware.smack.tbr.TBRTokens;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.blocking.BlockingCommandManager;
import org.jivesoftware.smackx.bob.BoBData;
import org.jivesoftware.smackx.bob.BoBHash;
import org.jivesoftware.smackx.bob.BoBManager;
import org.jivesoftware.smackx.bob.element.BoBExtension;
import org.jivesoftware.smackx.bob.element.BoBIQ;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.csi.ClientStateIndicationManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.jiveproperties.packet.JivePropertiesExtension;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.message_correct.element.MessageCorrectExtension;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.muclight.MUCLightAffiliation;
import org.jivesoftware.smackx.muclight.MultiUserChatLightManager;
import org.jivesoftware.smackx.muclight.element.MUCLightElements;
import org.jivesoftware.smackx.pep.PEPListener;
import org.jivesoftware.smackx.pep.PEPManager;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.EventElementType;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearch;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppDateTime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import inaka.com.mangosta.chat.ChatConnection;
import inaka.com.mangosta.chat.RoomsListManager;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.utils.TimeCalculation;
import inaka.com.mangosta.xmpp.microblogging.elements.PostEntryExtension;
import inaka.com.mangosta.xmpp.microblogging.providers.PostEntryProvider;
import io.realm.Realm;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class XMPPSession {

    private static XMPPSession mInstance;
    private XMPPTCPConnection mXMPPConnection;

    private static final boolean XMPP_DEBUG_MODE = true;
    private static final String XMPP_TAG = "XMPP";

    public static final String SERVER_NAME = "xmpp.erlang-solutions.com";
    public static final String SERVICE_NAME = "erlang-solutions.com";
    public static final String MUC_LIGHT_SERVICE_NAME = "muclight.erlang-solutions.com";

    // received
    private PublishSubject<Message> mMessagePublisher = PublishSubject.create();
    private PublishSubject<Presence> mPresencePublisher = PublishSubject.create();
    private PublishSubject<ChatConnection> mConnectionPublisher = PublishSubject.create();
    private PublishSubject<String> mArchiveQueryPublisher = PublishSubject.create();
    private PublishSubject<ErrorIQ> mErrorPublisher = PublishSubject.create();

    // sent
    private PublishSubject<Message> mMessageSentAlert = PublishSubject.create();

    private ReconnectionManager mReconnectionManager;

    private HashMap<Message, Date> lastCorrectionMessages = new HashMap<>();
    private List<String> messagesToDeleteIds = new ArrayList<>();

    private boolean connectionDoneOnce = false;

    public XMPPTCPConnection getXMPPConnection() {
        return mXMPPConnection;
    }

    public static XMPPSession getInstance() {
        if (mInstance == null) {
            mInstance = new XMPPSession();
        }
        return mInstance;
    }

    public static void setSpecialInstanceForTesting(XMPPSession xmppSession) {
        mInstance = xmppSession;
    }

    public static void clearInstance() {
        mInstance = null;
    }

    private XMPPSession() {
        SmackConfiguration.setDefaultPacketReplyTimeout(15000);
        XMPPTCPConnectionConfiguration config = null;
        try {
            XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder()
                    .setDebuggerEnabled(XMPP_DEBUG_MODE)
                    .setXmppDomain(JidCreate.from(SERVICE_NAME).asDomainBareJid())
                    .setHost(SERVER_NAME)
                    .setPort(5222)
                    .setSendPresence(true)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

            KeyStore keyStore = configKeyStore(builder);

            configSSLContext(builder, keyStore);

            config = builder.build();

        } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | XmppStringprepException e) {
            e.printStackTrace();
        }

        XMPPTCPConnection.setUseStreamManagementResumptionDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);
        mXMPPConnection = new XMPPTCPConnection(config);

        Roster roster = Roster.getInstanceFor(mXMPPConnection);
        roster.setRosterLoadedAtLogin(false);
        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        mXMPPConnection.addConnectionListener(new AbstractConnectionClosedListener() {
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                super.authenticated(connection, resumed);
                Preferences.getInstance().setLoggedIn(true);
                mConnectionPublisher.onNext(new ChatConnection(ChatConnection.ChatConnectionStatus.Authenticated));
                getXOAUTHTokens();
                subscribeToMyBlogPosts();
                connectionDoneOnce = true;
            }

            @Override
            public void connected(XMPPConnection connection) {
                Log.w(XMPP_TAG, "Connection Successful");
                backgroundRelogin();
                mConnectionPublisher.onNext(new ChatConnection(ChatConnection.ChatConnectionStatus.Connected));
                activeCSI();
            }

            @Override
            public void connectionTerminated() {
                Log.w(XMPP_TAG, "Connection Terminated");
                mConnectionPublisher.onNext(new ChatConnection(ChatConnection.ChatConnectionStatus.Disconnected));
            }

            @Override
            public void reconnectingIn(int seconds) {
                Log.w(XMPP_TAG, String.format("Reconnecting in %d seconds", seconds));
                mConnectionPublisher.onNext(new ChatConnection(ChatConnection.ChatConnectionStatus.Connecting));
            }

            @Override
            public void reconnectionFailed(Exception e) {
                super.reconnectionFailed(e);
                Preferences.getInstance().setXmppOauthAccessToken("");
                mXMPPConnection.avoidTokenReconnection();
                try {
                    mXMPPConnection.connect();
                } catch (SmackException | IOException | XMPPException | InterruptedException exception) {
                    exception.printStackTrace();
                }
            }

        });

        mReconnectionManager = ReconnectionManager.getInstanceFor(mXMPPConnection);
        mReconnectionManager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.RANDOM_INCREASING_DELAY);
        mReconnectionManager.enableAutomaticReconnection();

        StanzaListener mainStanzaListener = new StanzaListener() {
            @Override
            public void processPacket(Stanza stanza) throws SmackException.NotConnectedException {
                if (stanza instanceof Message) {
                    Message message = (Message) stanza;

                    // error
                    if (message.getType() == Message.Type.error) {
                        messagesToDeleteIds.add(message.getStanzaId());

                        // if not a chat state
                    } else if (!message.hasExtension(ChatStateExtension.NAMESPACE) || message.getBody() != null) {

                        if (message.hasExtension(MamElements.MamResultExtension.ELEMENT, MamElements.NAMESPACE)) { // MAM
                            MamElements.MamResultExtension mamResultExtension = MamElements.MamResultExtension.from(message);
                            Message forwardedMessage = (Message) mamResultExtension.getForwarded().getForwardedStanza();
                            Date date = mamResultExtension.getForwarded().getDelayInformation().getStamp();

                            // if not a chat state
                            if (!forwardedMessage.hasExtension(ChatStateExtension.NAMESPACE) || forwardedMessage.getBody() != null) {
                                saveMamMessage(forwardedMessage, date);
                            }

                        } else { // normal message
                            saveMessage(message);
                        }

                    }

                    mMessagePublisher.onNext(message);

                } else if (stanza instanceof Presence) {
                    Presence presence = (Presence) stanza;
                    // ...

                } else if (stanza instanceof ErrorIQ) {
                    ErrorIQ errorIq = (ErrorIQ) stanza;
                    mErrorPublisher.onNext(errorIq);

                    // if it is a BoB IQ
                    // TODO: this method is not necessary because the stickers are stored in the app, but I show how this could be used
                } else if (stanza instanceof BoBIQ) {
//                    BoBIQ bobIQ = (BoBIQ) stanza;
//
//                    if (bobIQ.getType().equals(IQ.Type.get)) { // BoB request
//                        sendResponse(bobIQ);
//                    } else if (bobIQ.getType().equals(IQ.Type.result)) { // BoB response
//                        try {
//                            saveSticker(bobIQ);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
                }

            }
        };

        MultiUserChatManager.getInstanceFor(mXMPPConnection).addInvitationListener(new InvitationListener() {
            @Override
            public void invitationReceived(XMPPConnection conn, MultiUserChat multiUserChat, EntityFullJid jid, String inviter, String reason, Message message, MUCUser.Invite invite) {
                try {
                    String userName = XMPPUtils.fromJIDToUserName(Preferences.getInstance().getUserXMPPJid());
                    multiUserChat.join(Resourcepart.from(userName));
                } catch (XmppStringprepException | SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException | MultiUserChatException.NotAMucServiceException e) {
                    e.printStackTrace();
                }
            }
        });

        StanzaFilter mainStanzaFilter = new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                return true;
            }
        };

        mXMPPConnection.addAsyncStanzaListener(mainStanzaListener, mainStanzaFilter);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mXMPPConnection.connect();
                } catch (SmackException.AlreadyConnectedException ace) {
                    Log.w("SMACK", "Already Connected");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();

        addExtensions();

        receiveBlogPosts();
    }

    public void backgroundRelogin() {
        Preferences preferences = Preferences.getInstance();
        if (!preferences.getUserXMPPJid().equals("") && !preferences.getUserXMPPPassword().equals("")) {
            backgroundLogin(XMPPUtils.fromJIDToUserName(preferences.getUserXMPPJid()), preferences.getUserXMPPPassword());
        }
    }

    /**
     * This method is not necessary on this app, is only to show how BoB could be used
     */
    private void saveSticker(BoBIQ bobIQ) throws IOException {
        // get content
        byte[] content = bobIQ.getBoBData().getContent();

        // make bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(content, 0, content.length);

        // SD card storage directory
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();

        // set a file name
        String filename = "sticker_" + bobIQ.getBoBHash() + ".png";

        // save file
        File file = new File(extStorageDirectory, filename);
        FileOutputStream outStream = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        outStream.flush();
        outStream.close();
    }

    /**
     * This method is not necessary on this app, is only to show how BoB could be used
     */
    private void sendResponse(BoBIQ bobIQ) {
        BoBHash bobHash = bobIQ.getBoBHash();

        Resources resources = MangostaApplication.getInstance().getResources();
        final int resourceId = resources.getIdentifier("sticker_" + Base64.decodeToString(bobHash.getHash()), "drawable",
                MangostaApplication.getInstance().getPackageName());
        Drawable drawable = resources.getDrawable(resourceId);

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream);
        byte[] bitMapData = stream.toByteArray();

        try {
            BoBData bobData = new BoBData(0, "image/png", bitMapData);
            getBoBManager().responseBoB(bobIQ, bobData);
        } catch (InterruptedException | SmackException.NotConnectedException | SmackException.NotLoggedInException e) {
            e.printStackTrace();
        }
    }

    private void receiveBlogPosts() {
        PEPManager pepManager = PEPManager.getInstanceFor(mXMPPConnection);
        pepManager.addPEPListener(new PEPListener() {
            @Override
            public void eventReceived(EntityBareJid entityBareJid, EventElement eventElement, Message message) {
                if (EventElementType.items == eventElement.getEventType()) {
                    ItemsExtension itemsExtension = (ItemsExtension) eventElement.getExtensions().get(0);
                    PayloadItem payloadItem = (PayloadItem) itemsExtension.getItems().get(0);
                    PostEntryExtension postEntryExtension = (PostEntryExtension) payloadItem.getPayload();

                    String id = postEntryExtension.getId();
                    String jid = entityBareJid.toString();
                    String title = postEntryExtension.getTitle();
                    Date published = postEntryExtension.getPublished();
                    Date updated = postEntryExtension.getUpdated();

                    BlogPost blogPost = new BlogPost(id, jid, null, title, published, updated);
                    RealmManager.getInstance().saveBlogPost(blogPost);
                }
            }
        });
    }

    public void publishQueryArchive(Stanza stanza) {
        String id = (stanza == null) ? null : stanza.getStanzaId();
        mArchiveQueryPublisher.onNext(id);
    }

    public void subscribeToMyBlogPosts() {
        PubSubManager pubSubManager = getPubSubManager();
        String nodeName = "urn:xmpp:microblog:0";

        try { // create node (only 1 time, then it's already created)
            pubSubManager.createNode(nodeName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // subscribe to myself
        String myJIDString = getUser().asEntityBareJid().toString();
        try {
            org.jivesoftware.smackx.pubsub.Subscription subscription = pubSubManager.getNode(nodeName).subscribe(myJIDString);
            Log.wtf("Subscription state", subscription.getState().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getXOAUTHTokens() {
        TBRManager tbrManager = TBRManager.getInstanceFor(getXMPPConnection());
        try {
            Preferences preferences = Preferences.getInstance();
            TBRTokens tbrTokens = tbrManager.getTokens();

            preferences.setXmppOauthAccessToken(tbrTokens.getAccessToken());
            preferences.setXmppOauthRefreshToken(tbrTokens.getRefreshToken());
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    private void configSSLContext(XMPPTCPConnectionConfiguration.Builder builder, KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

        builder.setCustomSSLContext(sslContext);
    }

    private KeyStore configKeyStore(XMPPTCPConnectionConfiguration.Builder builder) throws KeyStoreException {
        KeyStore keyStore;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder.setKeystorePath(null);
            builder.setKeystoreType("AndroidCAStore");
            keyStore = KeyStore.getInstance("AndroidCAStore");
        } else {
            builder.setKeystoreType("BKS");
            keyStore = KeyStore.getInstance("BKS");

            String path = System.getProperty("javax.net.ssl.trustStore");
            if (path == null)
                path = System.getProperty("java.home") + File.separator + "etc"
                        + File.separator + "security" + File.separator
                        + "cacerts.bks";
            builder.setKeystorePath(path);
        }
        return keyStore;
    }

    private void addExtensions() {
        // Microblogging
        ProviderManager.addExtensionProvider(PostEntryExtension.ELEMENT, PostEntryExtension.NAMESPACE, new PostEntryProvider());
    }

    public MultiUserChatManager getMUCManager() {
        return MultiUserChatManager.getInstanceFor(mXMPPConnection);
    }

    public MultiUserChatLightManager getMUCLightManager() {
        return MultiUserChatLightManager.getInstanceFor(mXMPPConnection);
    }

    public MamManager getMamManager() {
        return MamManager.getInstanceFor(mXMPPConnection);
    }

    public BlockingCommandManager getBlockingCommandManager() {
        return BlockingCommandManager.getInstanceFor(mXMPPConnection);
    }

    public PubSubManager getPubSubManager() {
        EntityBareJid myJIDString = getUser();
        return PubSubManager.getInstance(mXMPPConnection, myJIDString);
    }

    public BoBManager getBoBManager() {
        return BoBManager.getInstanceFor(mXMPPConnection);
    }

    public DiscoverItems discoverMUCLightItems() {
        DiscoverItems discoverItems = null;
        try {
            discoverItems = ServiceDiscoveryManager.getInstanceFor(mXMPPConnection).discoverItems(JidCreate.from(MUC_LIGHT_SERVICE_NAME));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return discoverItems;
    }

    public void activeCSI() {
        try {
            ClientStateIndicationManager.active(XMPPSession.getInstance().getXMPPConnection());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inactiveCSI() {
        try {
            ClientStateIndicationManager.inactive(XMPPSession.getInstance().getXMPPConnection());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void backgroundLogin(final String userName, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    login(userName, password);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void login(String userName, String password) throws Exception {
        try {
            mXMPPConnection.connect();
        } catch (SmackException.AlreadyConnectedException ace) {
            Log.w(XMPP_TAG, "Client Already Connected");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Preferences preferences = Preferences.getInstance();
        try {
            String resourceString = Settings.Secure.getString(MangostaApplication.getInstance().getContentResolver(), Settings.Secure.ANDROID_ID);
            Resourcepart resourcepart = Resourcepart.from(resourceString);

            // login
            if (connectionDoneOnce && !preferences.getXmppOauthAccessToken().isEmpty() && tokenSetMinutesAgo(30)) {
                mXMPPConnection.login(preferences.getXmppOauthAccessToken(), resourcepart);
            } else {
                mXMPPConnection.login(userName, password, resourcepart);
            }

            preferences.setUserXMPPJid(XMPPUtils.fromUserNameToJID(userName));
            preferences.setUserXMPPPassword(password);

            mConnectionPublisher.onNext(new ChatConnection(ChatConnection.ChatConnectionStatus.Authenticated));
        } catch (SmackException.AlreadyLoggedInException ale) {
            mConnectionPublisher.onNext(new ChatConnection(ChatConnection.ChatConnectionStatus.Authenticated));
        }

    }

    private boolean tokenSetMinutesAgo(int minutes) throws ParseException {
        return TimeCalculation.wasMinutesAgoMax(XmppDateTime.parseXEP0082Date(Preferences.getInstance().getDateLastTokenUpdate()), minutes);
    }

    public void logoff() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mReconnectionManager.disableAutomaticReconnection();
                    mXMPPConnection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    clearInstance();
                }
            }
        }).start();
    }

    // Room Message Subscription
    public Subscription subscribeRoomToMessages(final String roomJid, final MessageSubscriber subscriber) {
        return mMessagePublisher
                .filter(
                        new Func1<Message, Boolean>() {
                            @Override
                            public Boolean call(Message message) {
                                return message.getFrom().toString().startsWith(roomJid);
                            }
                        }
                )
                .subscribe(
                        new Action1<Message>() {
                            @Override
                            public void call(Message message) {
                                subscriber.onMessageReceived(message);
                            }
                        }
                );
    }

    // All Message Subscription
    public Subscription subscribeToMessages(Action1<Message> subscriber) {
        return mMessagePublisher.subscribe(subscriber);
    }

    // Presence Subscription
    public Subscription subscribeToPresence(Action1<Presence> subscriber) {
        return mPresencePublisher.subscribe(subscriber);
    }

    // Connection Subscription
    public Subscription subscribeToConnection(Action1<ChatConnection> subscriber) {
        return mConnectionPublisher.subscribe(subscriber);
    }

    // Archiver Subscription
    public Subscription subscribeToArchiveQuery(Action1<String> subscriber) {
        return mArchiveQueryPublisher.subscribe(subscriber);
    }

    // Error Publisher
    public Subscription subscribeToError(Action1<ErrorIQ> subscriber) {
        return mErrorPublisher.subscribe(subscriber);
    }

    // Message sent alert
    public Subscription subscribeToMessageSent(Action1<Message> subscriber) {
        return mMessageSentAlert.subscribe(subscriber);
    }

    public void messageSentAlert(Message message) {
        mMessageSentAlert.onNext(message);
    }

    public interface MessageSubscriber {
        void onMessageReceived(Message message);
    }

    public boolean isConnectedAndAuthenticated() {
        return mXMPPConnection != null && mXMPPConnection.isConnected() && mXMPPConnection.isAuthenticated();
    }

    public static boolean isInstanceNull() {
        return mInstance == null;
    }

    private void saveMamMessage(Message message, Date delayDate) {
        String messageId = assignMessageId(message);

        // not processing changes in affiliations or configurations
        if (!hasAffiliationsChangeExtension(message) && !hasConfigurationChangeExtension(message)) {
            if (isMessageCorrection(message)) { // message correction
                saveMessageCorrection(message, delayDate);
            } else { // normal message received
                if (!RealmManager.getInstance().chatMessageExists(messageId)) {
                    manageMessageReceived(message, delayDate, messageId);
                }
            }
        }
    }

    private void saveMessage(Message message) {
        String messageId = assignMessageId(message);

        if (RealmManager.getInstance().chatMessageExists(messageId)) { // message sent confirmation
            manageMessageAlreadyExists(message, null, messageId);

        } else if (isMessageCorrection(message)) { // message correction
            manageMessageCorrection(message, null);

        } else { // normal message received
            manageMessageReceived(message, null, messageId);
        }
    }

    private boolean isMessageCorrection(Message message) {
        return message.hasExtension(MessageCorrectExtension.ELEMENT, MessageCorrectExtension.NAMESPACE);
    }

    private void saveMessageCorrection(Message message, Date delayDate) {
        lastCorrectionMessages.put(message, delayDate);
    }

    public void loadCorrectionMessages() {
        for (Map.Entry<Message, Date> pair : lastCorrectionMessages.entrySet()) {
            manageMessageCorrection(pair.getKey(), pair.getValue());
        }
        lastCorrectionMessages.clear();
    }

    private void manageMessageCorrection(Message message, Date delayDate) {
        MessageCorrectExtension messageCorrectExtension = MessageCorrectExtension.from(message);
        String newMessageBody = message.getBody();
        String idInitialMessage = messageCorrectExtension.getIdInitialMessage();

        Realm realm = RealmManager.getInstance().getRealm();
        realm.beginTransaction();

        ChatMessage chatMessage = realm.where(ChatMessage.class)
                .equalTo("messageId", idInitialMessage)
                .findFirst();

        if (chatMessage != null) {
            chatMessage.setContent(newMessageBody);
            manageDelayDate(delayDate, chatMessage);
            realm.copyToRealmOrUpdate(chatMessage);
        }

        realm.commitTransaction();
        realm.close();
    }

    private void manageMessageReceived(Message message, Date delayDate, String messageId) {
        String[] jidList = message.getFrom().toString().split("/");

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessageId(messageId);

        String chatRoomJID;
        if (!jidList[0].equals(Preferences.getInstance().getUserXMPPJid())) {
            chatRoomJID = jidList[0];
        } else {
            chatRoomJID = message.getTo().toString().split("/")[0];
        }
        chatMessage.setRoomJid(chatRoomJID);

        // not saving messages with my affiliation changes to "none", and delete the chat in that case
        if (checkIfMyIsAffiliationNone(message)) {
            deleteChat(chatRoomJID);
            return;
        }

        if (message.hasExtension(JivePropertiesExtension.ELEMENT, JivePropertiesExtension.NAMESPACE)) {
            return;
        }

        RoomsListManager.getInstance().createChatIfNotExists(chatRoomJID, true);

        manageSender(jidList, chatMessage, chatRoomJID);

        chatMessage.setStatus(ChatMessage.STATUS_SENT);
        chatMessage.setUnread(true);

        if (isBoBMessage(message)) {
            BoBExtension bobExtension = BoBExtension.from(message);
            chatMessage.setContent(Base64.decodeToString(bobExtension.getBoBHash().getHash()));
            chatMessage.setType(ChatMessage.TYPE_STICKER);
        } else {
            chatMessage.setContent(message.getBody());
            chatMessage.setType(ChatMessage.TYPE_CHAT);
        }

        Realm realm = RealmManager.getInstance().getRealm();
        Chat chatRoom = realm.where(Chat.class).equalTo("jid", chatRoomJID).findFirst();
        realm.beginTransaction();

        // room name or subject change
        manageConfigurationsChange(message, chatMessage, chatRoom);

        // not saving invalid messages
        if (chatMessage.getContent() == null || chatMessage.getContent().isEmpty() || chatMessage.getUserSender() == null) {
            realm.commitTransaction();
            realm.close();
            return;
        }

        // assign date
        manageDelayDate(delayDate, chatMessage);

        realm.copyToRealmOrUpdate(chatMessage);
        realm.commitTransaction();
        realm.close();
    }

    private boolean isBoBMessage(Message message) {
        return message.hasExtension(BoBExtension.ELEMENT, BoBExtension.NAMESPACE);
    }

    private String assignMessageId(Message message) {
        String messageId = message.getStanzaId();
        if (TextUtils.isEmpty(messageId)) {
            messageId = UUID.randomUUID().toString();
        }
        return messageId;
    }

    private void manageMessageAlreadyExists(Message message, Date delayDate, String messageId) {
        Realm realm = RealmManager.getInstance().getRealm();
        realm.beginTransaction();

        ChatMessage chatMessage = realm.where(ChatMessage.class).equalTo("messageId", messageId).findFirst();
        chatMessage.setStatus(ChatMessage.STATUS_SENT);

        if (isBoBMessage(message)) {
            BoBExtension bobExtension = BoBExtension.from(message);
            chatMessage.setContent(Base64.decodeToString(bobExtension.getBoBHash().getHash()));
        } else {
            chatMessage.setContent(message.getBody());
        }

        if (delayDate != null) {
            chatMessage.setDate(delayDate);
        }

        realm.copyToRealmOrUpdate(chatMessage);
        realm.commitTransaction();
        realm.close();
    }

    private Realm realmBeginTransaction() {
        Realm realm = RealmManager.getInstance().getRealm();
        realm.beginTransaction();
        return realm;
    }

    private void manageSender(String[] jidList, ChatMessage chatMessage, String chatRoomJid) {
        Realm realm = RealmManager.getInstance().getRealm();
        Chat chat = realm.where(Chat.class).equalTo("jid", chatRoomJid).findFirst();

        if (chat.getType() == Chat.TYPE_MUC_LIGHT) {
            if (jidList.length > 1) {
                chatMessage.setUserSender(XMPPUtils.fromJIDToUserName(jidList[1]));
            }

        } else {
            chatMessage.setUserSender(XMPPUtils.fromJIDToUserName(jidList[0]));

            if (!jidList[0].equals(Preferences.getInstance().getUserXMPPJid())) {
                String roomName = XMPPUtils.fromJIDToUserName(jidList[0]);
                realm.beginTransaction();
                chat.setName(roomName);
                realm.copyToRealmOrUpdate(chat);
                realm.commitTransaction();
            }

        }
    }

    private void deleteChat(String chatRoomJID) {
        Realm realm = realmBeginTransaction();

        Chat chat = realm.where(Chat.class).equalTo("jid", chatRoomJID).findFirst();
        if (chat != null) {
            chat.setShow(false);
            chat.deleteFromRealm();
        }

        realm.commitTransaction();
        realm.close();
    }

    private void manageDelayDate(Date delayDate, ChatMessage chatMessage) {
        if (delayDate == null) {
            chatMessage.setDate(new Date());
        } else {
            chatMessage.setDate(delayDate);
        }
    }

    private void manageConfigurationsChange(Message message, ChatMessage chatMessage, Chat chatRoom) {
        if (hasConfigurationChangeExtension(message)) {
            MUCLightElements.ConfigurationsChangeExtension configurationsChangeExtension = MUCLightElements.ConfigurationsChangeExtension.from(message);

            String roomName = configurationsChangeExtension.getRoomName();
            String subject = configurationsChangeExtension.getSubject();

            if (roomName != null) {
                chatRoom.setName(roomName);
            }

            if (subject != null) {
                chatRoom.setSubject(subject);
            }

            chatMessage.setType(ChatMessage.TYPE_ROOM_NAME_CHANGED);
        }
    }

    public synchronized int deleteMessagesToDelete() {
        int count = 0;
        for (String messageId : messagesToDeleteIds) {
            RealmManager.getInstance().deleteMessage(messageId);
            count++;
        }
        messagesToDeleteIds.clear();
        return count;
    }

    private boolean hasConfigurationChangeExtension(Message message) {
        return message.hasExtension(MUCLightElements.ConfigurationsChangeExtension.ELEMENT, MUCLightElements.ConfigurationsChangeExtension.NAMESPACE);
    }

    private boolean checkIfMyIsAffiliationNone(Message message) {
        if (hasAffiliationsChangeExtension(message)) {
            MUCLightElements.AffiliationsChangeExtension affiliationsChangeExtension = MUCLightElements.AffiliationsChangeExtension.from(message);

            try {
                Jid jid = JidCreate.from(Preferences.getInstance().getUserXMPPJid());
                MUCLightAffiliation affiliation = affiliationsChangeExtension.getAffiliations().get(jid.asEntityJidIfPossible());
                return affiliation == MUCLightAffiliation.none;
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean hasAffiliationsChangeExtension(Message message) {
        return message.hasExtension(MUCLightElements.AffiliationsChangeExtension.ELEMENT, MUCLightElements.AffiliationsChangeExtension.NAMESPACE);
    }

    public void sendStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {
        if (Preferences.isTesting()) {
            try {
                getXMPPConnection().sendStanza(stanza);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            getXMPPConnection().sendStanza(stanza);
        }
    }

    public void createNodeToAllowComments(String blogPostId) {
        String nodeName = "urn:xmpp:microblog:0:comments/" + blogPostId;
        PubSubManager pubSubManager = PubSubManager.getInstance(XMPPSession.getInstance().getXMPPConnection());
        try {
            // create node
            LeafNode node = pubSubManager.createNode(nodeName);

            // subscribe to comments
            String myJIDString = XMPPSession.getInstance().getUser().toString();
            org.jivesoftware.smackx.pubsub.Subscription subscription = node.subscribe(myJIDString);

            Log.wtf("Comments subscription state", subscription.getState().toString());
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public EntityBareJid getUser() {
        return getXMPPConnection().getUser().asEntityBareJid();
    }

    public Jid getPubSubService() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        return PubSubManager.getPubSubService(getXMPPConnection());
    }

    public boolean userExists(String jid) {
        ProviderManager.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
        ProviderManager.addIQProvider("query", "jabber:iq:vjud", new UserSearch.Provider());
        UserSearchManager searchManager = new UserSearchManager(getXMPPConnection());

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

    public void blockContacts(List<Jid> jids)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        getBlockingCommandManager().blockContacts(jids);
    }

    public void unblockContacts(List<Jid> jids)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        getBlockingCommandManager().unblockContacts(jids);
    }

    public List<Jid> getBlockList() throws Exception {
        return getBlockingCommandManager().getBlockList();
    }

    public void unblockAll()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        getBlockingCommandManager().unblockAll();
    }

}