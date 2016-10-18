package inaka.com.mangosta.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import org.jivesoftware.smack.packet.ErrorIQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.ChatMessagesAdapter;
import inaka.com.mangosta.adapters.StickersAdapter;
import inaka.com.mangosta.chat.ChatConnection;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.chat.RoomManagerListener;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.models.MongooseMUCLightMessage;
import inaka.com.mangosta.models.MongooseMessage;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import inaka.com.mangosta.xmpp.muclight.MUCLightAffiliation;
import inaka.com.mangosta.xmpp.muclight.MUCLightRoomConfiguration;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLight;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLightManager;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import rx.Subscription;
import rx.functions.Action1;

public class ChatActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.chatMessagesRecyclerView)
    RecyclerView chatMessagesRecyclerView;

    @Bind(R.id.stickersRecyclerView)
    RecyclerView stickersRecyclerView;

    @Bind(R.id.chatSendMessageButton)
    ImageButton chatSendMessageButton;

    @Bind(R.id.stickersMenuImageButton)
    ImageButton stickersMenuImageButton;

    @Bind(R.id.chatSendMessageEditText)
    EditText chatSendMessageEditText;

    @Bind(R.id.loadMessagesSwipeRefreshLayout)
    SwipeRefreshLayout loadMessagesSwipeRefreshLayout;

    @Bind(R.id.chatTypingTextView)
    TextView chatTypingTextView;

    private RoomManager mRoomManager;
    private String mChatJID;

    public static String CHAT_JID_PARAMETER = "chatJid";
    public static String CHAT_NAME_PARAMETER = "chatName";
    public static String IS_NEW_CHAT_PARAMETER = "isNew";

    Chat mChat;

    private RealmResults<ChatMessage> mMessages;
    private ChatMessagesAdapter mMessagesAdapter;
    LinearLayoutManager mLayoutManagerMessages;

    private String[] mStickersNameList = {
            "base",
            "pliiz",
            "bigsmile",
            "pleure",
            "snif"
    };
    private StickersAdapter mStickersAdapter;
    LinearLayoutManager mLayoutManagerStickers;

    private Subscription mMessageSubscription;
    private Subscription mMongooseMessageSubscription;
    private Subscription mMongooseMUCLightMessageSubscription;
    private Subscription mConnectionSubscription;
    private Subscription mArchiveQuerySubscription;
    private Subscription mErrorArchiveQuerySubscription;

    boolean mLeaving = false;
    boolean mIsOwner = false;

    Timer mPauseComposeTimer = new Timer();
    private int mMessagesCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        mChatJID = getIntent().getStringExtra(CHAT_JID_PARAMETER);
        String chatName = getIntent().getStringExtra(CHAT_NAME_PARAMETER);
        boolean isNewChat = getIntent().getBooleanExtra(IS_NEW_CHAT_PARAMETER, false);

        mChat = getChatFromRealm();

        if (isNewChat) {
            manageNewChat(chatName);
        }

        if (!mChat.isShow()) {
            setShowChat();
        }

        getSupportActionBar().setTitle(chatName);
        if (mChat.getSubject() != null) {
            getSupportActionBar().setSubtitle(mChat.getSubject());
        }

        if (mChat.getType() == Chat.TYPE_MUC_LIGHT) {
            manageRoomNameAndSubject();
        }

        mRoomManager = RoomManager.getInstance(new RoomManagerChatListener(ChatActivity.this));

        mLayoutManagerMessages = new LinearLayoutManager(this);
        mLayoutManagerMessages.setStackFromEnd(true);

        chatMessagesRecyclerView.setHasFixedSize(true);
        chatMessagesRecyclerView.setLayoutManager(mLayoutManagerMessages);

        mMessages = RealmManager.getMessagesForChat(getRealm(), mChatJID);

        mMessages.addChangeListener(mRealmChangeListener);

        mMessagesAdapter = new ChatMessagesAdapter(this, mMessages);

        chatMessagesRecyclerView.setAdapter(mMessagesAdapter);

        chatSendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View button) {
                sendTextMessage();
            }
        });

        loadMessagesSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        loadMessagesSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadArchivedMessages();
            }
        });

        chatSendMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (charSequence.length() > 0) { // compose message
                    mPauseComposeTimer.cancel();

                    mRoomManager.updateTypingStatus(ChatState.composing, mChatJID, mChat.getType());

                    schedulePauseTimer();

                } else { // delete or send message
                    mPauseComposeTimer.cancel();
                    mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.getType());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        stickersMenuImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stickersRecyclerView.getVisibility() == View.VISIBLE) {
                    stickersRecyclerView.setVisibility(View.GONE);
                } else {
                    stickersRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });

        mLayoutManagerStickers = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        stickersRecyclerView.setHasFixedSize(true);
        stickersRecyclerView.setLayoutManager(mLayoutManagerStickers);

        mStickersAdapter = new StickersAdapter(this, Arrays.asList(mStickersNameList));

        stickersRecyclerView.setAdapter(mStickersAdapter);
    }

    private void schedulePauseTimer() {
        mPauseComposeTimer = new Timer();
        mPauseComposeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mChat = getChatFromRealm();
                        mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.getType());
                    }
                });
            }
        }, 15000);
    }

    private Chat getChatFromRealm() {
        return getRealm().where(Chat.class).equalTo("jid", mChatJID).findFirst();
    }

    private void manageRoomNameAndSubject() {
        Tasks.executeInBackground(this, new BackgroundWork<MUCLightRoomConfiguration>() {
            @Override
            public MUCLightRoomConfiguration doInBackground() throws Exception {
                MultiUserChatLight multiUserChatLight = XMPPSession.getInstance().getMUCLightManager().getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                return multiUserChatLight.getConfiguration();
            }
        }, new Completion<MUCLightRoomConfiguration>() {
            @Override
            public void onSuccess(Context context, MUCLightRoomConfiguration mucLightRoomConfiguration) {
                Realm realm = getRealm();
                realm.beginTransaction();
                mChat.setName(mucLightRoomConfiguration.getRoomName());
                mChat.setSubject(mucLightRoomConfiguration.getSubject());
                realm.copyToRealmOrUpdate(mChat);
                realm.commitTransaction();
                realm.close();

                if (mChat.getSubject() != null) {
                    getSupportActionBar().setSubtitle(mChat.getSubject());
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void setShowChat() {
        Realm realm = getRealm();
        realm.beginTransaction();
        mChat.setShow(true);
        realm.copyToRealmOrUpdate(mChat);
        realm.commitTransaction();
        realm.close();
    }

    private void manageNewChat(String chatName) {
        Realm realm = getRealm();
        realm.beginTransaction();
        if (mChat == null) {
            mChat = new Chat(mChatJID);

            if (mChatJID.contains(XMPPSession.MUC_SERVICE_NAME)) {
                mChat.setType(Chat.TYPE_MUC);
            } else if (mChatJID.contains(XMPPSession.MUC_LIGHT_SERVICE_NAME)) {
                mChat.setType(Chat.TYPE_MUC_LIGHT);
            } else {
                mChat.setType(Chat.TYPE_1_T0_1);
            }

            mChat.setDateCreated(new Date());
        }
        mChat.setName(chatName);
        realm.copyToRealmOrUpdate(mChat);
        realm.commitTransaction();
        realm.close();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mMessageSubscription = XMPPSession.getInstance().subscribeRoomToMessages(mChatJID, new XMPPSession.MessageSubscriber() {
            @Override
            public void onMessageReceived(final Message message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (message.hasExtension(ChatStateExtension.NAMESPACE)) {
                            ChatStateExtension chatStateExtension = (ChatStateExtension) message.getExtension(ChatStateExtension.NAMESPACE);
                            ChatState chatState = chatStateExtension.getChatState();

                            String myUser = XMPPUtils.fromJIDToUserName(XMPPSession.getInstance().getXMPPConnection().getUser().toString());
                            String userSender = "";

                            String[] jidList = message.getFrom().toString().split("/");

                            switch (mChat.getType()) {

                                case Chat.TYPE_1_T0_1:
                                    userSender = XMPPUtils.fromJIDToUserName(jidList[0]);
                                    break;

                                case Chat.TYPE_MUC:
                                    if (jidList.length > 1) {
                                        userSender = jidList[1];
                                    }
                                    break;

                                case Chat.TYPE_MUC_LIGHT:
                                    if (jidList.length > 1) {
                                        userSender = XMPPUtils.fromJIDToUserName(jidList[1]);
                                    }
                                    break;
                            }

                            if (!userSender.equals(myUser)) {
                                if (chatState.equals(ChatState.composing)) { // typing
                                    chatTypingTextView.setText(String.format(Locale.getDefault(), getString(R.string.typing), userSender));
                                    chatTypingTextView.setVisibility(View.VISIBLE);
                                } else { // not typing
                                    chatTypingTextView.setVisibility(View.GONE);
                                }
                            }

                        } else {
                            String subject = message.getSubject();
                            if (subject != null) {
                                setTitle(subject);
                            }
                            refreshMessagesAndScrollToEnd();
                        }

                    }
                });
            }

        });

        mMongooseMessageSubscription = XMPPSession.getInstance().subscribeRoomToMongooseMessages(mChatJID, new XMPPSession.MongooseMessageSubscriber() {
            @Override
            public void onMessageReceived(MongooseMessage message) {
                refreshMessagesAndScrollToEnd();
            }
        });

        mMongooseMUCLightMessageSubscription = XMPPSession.getInstance().subscribeRoomToMUCLightMongooseMessages(mChatJID,
                new XMPPSession.MongooseMUCLightMessageSubscriber() {
                    @Override
                    public void onMessageReceived(MongooseMUCLightMessage message) {
                        refreshMessagesAndScrollToEnd();
                    }
                });

        mConnectionSubscription = XMPPSession.getInstance().subscribeToConnection(new Action1<ChatConnection>() {
            @Override
            public void call(ChatConnection chatConnection) {
                switch (chatConnection.getStatus()) {
                    case Connected:
                    case Authenticated:
                        mRoomManager.loadMembers(mChatJID);
                        break;
                    case Connecting:
                    case Disconnected:
                        break;
                }
            }
        });

        if (XMPPSession.getInstance().isConnectedAndAuthenticated()) {
            mRoomManager.loadMembers(mChatJID);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        mChat = getChatFromRealm();
        if (mChat != null) {
            mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.getType());
        }
        disconnectRoomFromServer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);

        MenuItem chatMembersItem = menu.getItem(0);
        chatMembersItem.setVisible(!(mChat.getType() == Chat.TYPE_1_T0_1));

        MenuItem destroyItem = menu.getItem(4);
        destroyItem.setVisible(false);

        // can change room name or subject only if it is a muc light
        menu.getItem(1).setVisible(mChat.getType() == Chat.TYPE_MUC_LIGHT);
        menu.getItem(2).setVisible(mChat.getType() == Chat.TYPE_MUC_LIGHT);

        setDestroyButtonVisibility(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                mChat = getChatFromRealm();
                mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.getType());
                finish();
                EventBus.getDefault().post(new Event(Event.Type.GO_BACK_FROM_CHAT));
                break;

            case R.id.actionChatMembers:
                mChat = getChatFromRealm();
                Intent chatMembersIntent = new Intent(ChatActivity.this, ChatMembersActivity.class);
                chatMembersIntent.putExtra(ChatMembersActivity.ROOM_JID_PARAMETER, mChatJID);
                chatMembersIntent.putExtra(ChatMembersActivity.IS_ADMIN_PARAMETER, mIsOwner);
                chatMembersIntent.putExtra(ChatMembersActivity.ROOM_TYPE_PARAMETER, mChat.getType());
                startActivity(chatMembersIntent);
                break;

            case R.id.actionChangeRoomName:
                changeMUCLightRoomName();
                break;

            case R.id.actionChangeSubject:
                changeMUCLightRoomSubject();
                break;

            case R.id.actionLeaveChat:
                mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.getType());
                leaveChat();
                break;

            case R.id.actionDestroyChat:
                mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.getType());
                destroyChat();
                break;
        }

        return true;
    }

    private void changeMUCLightRoomName() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText roomNameEditText = new EditText(this);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(10, 0, 10, 0);
        roomNameEditText.setLayoutParams(lp);
        roomNameEditText.setText(getSupportActionBar().getTitle());

        linearLayout.addView(roomNameEditText);

        new android.app.AlertDialog.Builder(ChatActivity.this)
                .setTitle(getString(R.string.room_name))
                .setMessage(getString(R.string.enter_new_room_name))
                .setView(linearLayout)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final String chatName = roomNameEditText.getText().toString();

                        Tasks.executeInBackground(ChatActivity.this, new BackgroundWork<Object>() {
                            @Override
                            public Object doInBackground() throws Exception {
                                MultiUserChatLight multiUserChatLight = XMPPSession.getInstance().getMUCLightManager().getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                                multiUserChatLight.changeRoomName(chatName);
                                return null;
                            }
                        }, new Completion<Object>() {
                            @Override
                            public void onSuccess(Context context, Object result) {
                                Toast.makeText(ChatActivity.this,
                                        getString(R.string.room_name_changed),
                                        Toast.LENGTH_SHORT).show();

                                Realm realm = getRealm();
                                realm.beginTransaction();
                                mChat.setName(chatName);
                                realm.commitTransaction();
                                realm.close();

                                getSupportActionBar().setTitle(chatName);
                            }

                            @Override
                            public void onError(Context context, Exception e) {
                                Toast.makeText(ChatActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        });
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void changeMUCLightRoomSubject() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText roomSubjectEditText = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(10, 0, 10, 0);
        roomSubjectEditText.setLayoutParams(lp);
        roomSubjectEditText.setText(getSupportActionBar().getSubtitle());

        linearLayout.addView(roomSubjectEditText);

        new android.app.AlertDialog.Builder(ChatActivity.this)
                .setTitle(getString(R.string.room_subject))
                .setMessage(getString(R.string.enter_new_room_subject))
                .setView(linearLayout)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final String subject = roomSubjectEditText.getText().toString();

                        Tasks.executeInBackground(ChatActivity.this, new BackgroundWork<Object>() {
                            @Override
                            public Object doInBackground() throws Exception {
                                MultiUserChatLight multiUserChatLight = XMPPSession.getInstance().getMUCLightManager().getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                                multiUserChatLight.changeSubject(subject);
                                return null;
                            }
                        }, new Completion<Object>() {
                            @Override
                            public void onSuccess(Context context, Object result) {
                                Toast.makeText(ChatActivity.this,
                                        getString(R.string.room_subject_changed),
                                        Toast.LENGTH_SHORT).show();

                                Realm realm = getRealm();
                                realm.beginTransaction();
                                mChat.setSubject(subject);
                                realm.commitTransaction();
                                realm.close();

                                getSupportActionBar().setSubtitle(subject);
                            }

                            @Override
                            public void onError(Context context, Exception e) {
                                Toast.makeText(ChatActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                            }
                        });
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void disconnectRoomFromServer() {
        if (mMessageSubscription != null) {
            mMessageSubscription.unsubscribe();
        }

        if (mConnectionSubscription != null) {
            mConnectionSubscription.unsubscribe();
        }

        if (mArchiveQuerySubscription != null) {
            mArchiveQuerySubscription.unsubscribe();
        }

        if (mErrorArchiveQuerySubscription != null) {
            mErrorArchiveQuerySubscription.unsubscribe();
        }

        if (mMongooseMessageSubscription != null) {
            mMongooseMessageSubscription.unsubscribe();
        }

        if (mMongooseMUCLightMessageSubscription != null) {
            mMongooseMUCLightMessageSubscription.unsubscribe();
        }

    }

    private void sendTextMessage() {
        if (!XMPPSession.isInstanceNull() && XMPPSession.getInstance().isConnectedAndAuthenticated()) {
            String content = chatSendMessageEditText.getText().toString().trim().replaceAll("\n\n+", "\n\n");

            if (!TextUtils.isEmpty(content)) {
                String messageId = saveMessageLocally(content, ChatMessage.TYPE_CHAT);
                mChat = getChatFromRealm();
                mRoomManager.sendTextMessage(messageId, mChatJID, content, mChat.getType());
                chatSendMessageEditText.setText("");
                refreshMessagesAndScrollToEnd();
            }
        }
    }

    private String saveMessageLocally(String content, int type) {
        RoomManager.createChatIfNotExists(mChatJID, true);
        mChat = getChatFromRealm();

        String messageId = UUID.randomUUID().toString();

        ChatMessage chatMessage = new ChatMessage();

        chatMessage.setMessageId(messageId);
        chatMessage.setRoomJid(mChat.getJid());
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

    private void leaveChat() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.want_to_leave_chat));

        builder.setPositiveButton(R.string.action_leave_chat, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mLeaving = true;

                Realm realm = getRealm();
                Chat chat = realm.where(Chat.class).equalTo("jid", mChatJID).findFirst();

                switch (chat.getType()) {

                    case Chat.TYPE_MUC:
                        realm.close();
                        disconnectRoomFromServer();
                        mRoomManager.leaveMUC(mChatJID);
                        break;

                    case Chat.TYPE_MUC_LIGHT:

                        realm.close();
                        disconnectRoomFromServer();
                        mRoomManager.leaveMUCLight(mChatJID);
                        break;

                    case Chat.TYPE_1_T0_1:
                        realm.close();
                        mRoomManager.leave1to1Chat(mChatJID);
                        break;
                }

                finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void destroyChat() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.want_to_destroy_chat));

        builder.setPositiveButton(R.string.action_destroy_chat, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mLeaving = true;
                mChat = getChatFromRealm();
                if (mChat.getType() == Chat.TYPE_MUC_LIGHT) {
                    disconnectRoomFromServer();
                    mRoomManager.destroyMUCLight(mChatJID);
                    finish();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void loadArchivedMessages() {
        mChat = getChatFromRealm();

        if (mChat == null || !mChat.isValid()) {

            if (loadMessagesSwipeRefreshLayout != null) {
                loadMessagesSwipeRefreshLayout.setRefreshing(false);
            }

            if (mErrorArchiveQuerySubscription != null) {
                mErrorArchiveQuerySubscription.unsubscribe();
            }

            if (mArchiveQuerySubscription != null) {
                mArchiveQuerySubscription.unsubscribe();
            }

            return;
        }

        mArchiveQuerySubscription = XMPPSession.getInstance().subscribeToArchiveQuery(new Action1<String>() {
            @Override
            public void call(String s) {
                mArchiveQuerySubscription.unsubscribe();

                if (mErrorArchiveQuerySubscription != null) {
                    mErrorArchiveQuerySubscription.unsubscribe();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadMessagesSwipeRefreshLayout != null) {
                            loadMessagesSwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });
            }
        });

        mErrorArchiveQuerySubscription = XMPPSession.getInstance().subscribeToError(new Action1<ErrorIQ>() {
            @Override
            public void call(ErrorIQ errorIQ) {
                mErrorArchiveQuerySubscription.unsubscribe();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadMessagesSwipeRefreshLayout != null) {
                            loadMessagesSwipeRefreshLayout.setRefreshing(false);
                        }
                        Toast.makeText(ChatActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        mRoomManager.loadArchivedMessages(mChat.getJid());
    }

    private void refreshMessages() {
        XMPPSession.getInstance().deleteMessagesToDelete();
        mMessagesAdapter.notifyDataSetChanged();
    }

    private void scrollToEnd() {
        int lastPosition = mLayoutManagerMessages.findLastVisibleItemPosition();
        if (lastPosition <= mMessages.size() - 2 && chatMessagesRecyclerView != null) {
            chatMessagesRecyclerView.scrollToPosition(mMessages.size() - 1);
        }
    }

    private void refreshMessagesAndScrollToEnd() {
        refreshMessages();
        scrollToEnd();
    }

    RealmChangeListener<RealmResults<ChatMessage>> mRealmChangeListener = new RealmChangeListener<RealmResults<ChatMessage>>() {
        @Override
        public void onChange(RealmResults<ChatMessage> messages) {
            if (mMessagesAdapter != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!XMPPSession.isInstanceNull() && XMPPSession.getInstance().isConnectedAndAuthenticated()) {
                            if (mMessages.size() == 0 && !mLeaving) {
                                loadArchivedMessages();
                            }
                        }

                        if (mMessagesCount != mMessages.size()) {
                            refreshMessagesAndScrollToEnd();
                            mMessagesCount = mMessages.size();
                        } else {
                            refreshMessages();
                        }

                    }
                });
            }
        }
    };

    private class RoomManagerChatListener extends RoomManagerListener {

        public RoomManagerChatListener(Context context) {
            super(context);
        }

        @Override
        public void onMessageSent(Message message) {
            super.onMessageSent(message);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshMessagesAndScrollToEnd();
                }
            });
        }

        @Override
        public void onRoomMembersLoaded(final List<Affiliate> members) {
            super.onRoomMembersLoaded(members);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.getType());
        EventBus.getDefault().post(new Event(Event.Type.GO_BACK_FROM_CHAT));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // receives events from EventBus
    public void onEvent(Event event) {
        switch (event.getType()) {
            case STICKER_SENT:
                stickerSent(event.getImageName());
                break;
        }
    }

    private void stickerSent(String imageName) {
        String messageId = saveMessageLocally(imageName, ChatMessage.TYPE_STICKER);
        mChat = getChatFromRealm();
        mRoomManager.sendStickerMessage(messageId, mChatJID, imageName, mChat.getType());
        stickersRecyclerView.setVisibility(View.GONE);
        refreshMessagesAndScrollToEnd();
    }

    private void setDestroyButtonVisibility(final Menu menu) {
        switch (mChat.getType()) {
            case Chat.TYPE_MUC_LIGHT:
                manageMUCLightAdmins(menu);
                break;
            case Chat.TYPE_MUC:
                manageMUCAdmins();
                break;
        }
    }

    private void manageMUCLightAdmins(final Menu menu) {
        Tasks.executeInBackground(this, new BackgroundWork<HashMap<Jid, MUCLightAffiliation>>() {
            @Override
            public HashMap<Jid, MUCLightAffiliation> doInBackground() throws Exception {
                MultiUserChatLightManager multiUserChatLightManager = MultiUserChatLightManager.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
                MultiUserChatLight multiUserChatLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                return multiUserChatLight.getAffiliations();
            }
        }, new Completion<HashMap<Jid, MUCLightAffiliation>>() {
            @Override
            public void onSuccess(Context context, HashMap<Jid, MUCLightAffiliation> occupants) {
                for (Map.Entry<Jid, MUCLightAffiliation> pair : occupants.entrySet()) {

                    Jid key = pair.getKey();
                    if (key != null && key.toString().equals(Preferences.getInstance().getUserXMPPJid())) {
                        MenuItem destroyItem = menu.getItem(4);
                        mIsOwner = pair.getValue().equals(MUCLightAffiliation.owner);
                        destroyItem.setVisible(mIsOwner && mChat.getType() == Chat.TYPE_MUC_LIGHT);
                    }
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void manageMUCAdmins() {
        Tasks.executeInBackground(this, new BackgroundWork<List<Affiliate>>() {
            @Override
            public List<Affiliate> doInBackground() throws Exception {
                MultiUserChatManager mucManager = XMPPSession.getInstance().getMUCManager();
                MultiUserChat muc = mucManager.getMultiUserChat(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                List<Affiliate> admins = muc.getAdmins();
                admins.addAll(muc.getOwners());
                return admins;
            }
        }, new Completion<List<Affiliate>>() {
            @Override
            public void onSuccess(Context context, List<Affiliate> admins) {
                for (Affiliate affiliate : admins) {
                    if (affiliate.getJid().toString().equals(Preferences.getInstance().getUserXMPPJid())) {
                        mIsOwner = true;
                    }
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                e.printStackTrace();
            }
        });
    }

}


