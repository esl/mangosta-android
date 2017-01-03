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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ErrorIQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.blocking.element.BlockedErrorExtension;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muclight.MUCLightAffiliation;
import org.jivesoftware.smackx.muclight.MUCLightRoomConfiguration;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.muclight.MultiUserChatLightManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.ChatMessagesAdapter;
import inaka.com.mangosta.adapters.StickersAdapter;
import inaka.com.mangosta.chat.ChatConnection;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.chat.RoomManagerListener;
import inaka.com.mangosta.chat.RoomsListManager;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.notifications.MessageNotifications;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
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

    @Bind(R.id.scrollDownImageButton)
    ImageButton scrollDownImageButton;

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
    private Subscription mConnectionSubscription;
    private Subscription mArchiveQuerySubscription;
    private Subscription mErrorArchiveQuerySubscription;

    boolean mLeaving = false;
    boolean mIsOwner = false;

    Timer mPauseComposeTimer = new Timer();
    private int mMessagesCount;
    private Menu mMenu;

    final private int VISIBLE_BEFORE_LOAD = 10;
    final private int ITEMS_PER_PAGE = 15;
    final private int PAGES_TO_LOAD = 3;

    SwipeRefreshLayout.OnRefreshListener mSwipeRefreshListener;

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

        mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);

        if (isNewChat) {
            RoomsListManager.getInstance().manageNewChat(mChat, getRealm(), chatName, mChatJID);
            mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);
        }

        if (!mChat.isShow()) {
            RoomsListManager.getInstance().setShowChat(getRealm(), mChat);
        }

        getSupportActionBar().setTitle(chatName);
        if (mChat.getSubject() != null) {
            getSupportActionBar().setSubtitle(mChat.getSubject());
        }

        if (mChat.getType() == Chat.TYPE_MUC_LIGHT) {
            manageRoomNameAndSubject();
        } else {
            setOneToOneChatConnectionStatus();
        }

        mRoomManager = RoomManager.getInstance(new RoomManagerChatListener(ChatActivity.this));

        mLayoutManagerMessages = new LinearLayoutManager(this);
        mLayoutManagerMessages.setStackFromEnd(true);

        chatMessagesRecyclerView.setHasFixedSize(true);
        chatMessagesRecyclerView.setLayoutManager(mLayoutManagerMessages);

        if (!RealmManager.isTesting()) {
            mMessages = RealmManager.getInstance().getMessagesForChat(getRealm(), mChatJID);
            mMessages.addChangeListener(mRealmChangeListener);
        }

        List<ChatMessage> messages = ((mMessages == null) ? new ArrayList<ChatMessage>() : mMessages);
        mMessagesAdapter = new ChatMessagesAdapter(this, messages);

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

        mSwipeRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadArchivedMessages();
            }
        };
        loadMessagesSwipeRefreshLayout.setOnRefreshListener(mSwipeRefreshListener);

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

        scrollDownImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollToEnd();
            }
        });

        chatMessagesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                manageScrollButtonVisibility();
                loadMoreMessages(recyclerView, dy);
            }
        });

        cancelMessageNotificationsForChat();
    }

    private void cancelMessageNotificationsForChat() {
        MessageNotifications.cancelChatNotifications(this, mChatJID);
        getRealm().beginTransaction();
        mChat.resetUnreadMessageCount();
        getRealm().commitTransaction();
    }

    private void loadMoreMessages(RecyclerView recyclerView, int dy) {
        int lastVisibleItem = mLayoutManagerMessages.findLastVisibleItemPosition();
        if (dy < 0) {
            int visibleItemCount = recyclerView.getChildCount();
            int totalItemCount = mLayoutManagerMessages.getItemCount();
            boolean countVisibleToLoadMore = (totalItemCount - visibleItemCount
                    - (totalItemCount - lastVisibleItem))
                    <= VISIBLE_BEFORE_LOAD;

            if (countVisibleToLoadMore && !loadMessagesSwipeRefreshLayout.isRefreshing()) {
                loadMessagesSwipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        loadMessagesSwipeRefreshLayout.setRefreshing(true);
                        mSwipeRefreshListener.onRefresh();
                    }
                });
            }
        }
    }

    private void manageScrollButtonVisibility() {
        if (isMessagesListScrolledToBottom()) {
            scrollDownImageButton.setVisibility(View.GONE);
        } else {
            scrollDownImageButton.setVisibility(View.VISIBLE);
        }
    }

    private void setOneToOneChatConnectionStatus() {
        String userName = XMPPUtils.fromJIDToUserName(mChatJID);

        if (RosterManager.getInstance().getStatusFromContact(userName).equals(Presence.Type.available)) {
            getSupportActionBar().setSubtitle(getString(R.string.connected));
        } else {
            getSupportActionBar().setSubtitle("");
        }
    }

    private void schedulePauseTimer() {
        mPauseComposeTimer = new Timer();
        mPauseComposeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);
                        mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.getType());
                    }
                });
            }
        }, 15000);
    }

    private void manageRoomNameAndSubject() {
        Tasks.executeInBackground(this, new BackgroundWork<MUCLightRoomConfiguration>() {
            @Override
            public MUCLightRoomConfiguration doInBackground() throws Exception {
                if (Preferences.isTesting()) {
                    return null;
                } else {
                    MultiUserChatLight multiUserChatLight = XMPPSession.getInstance().getMUCLightManager().getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                    return multiUserChatLight.getConfiguration();
                }
            }
        }, new Completion<MUCLightRoomConfiguration>() {
            @Override
            public void onSuccess(Context context, MUCLightRoomConfiguration mucLightRoomConfiguration) {
                if (mucLightRoomConfiguration != null) {
                    Realm realm = getRealm();
                    realm.beginTransaction();
                    mChat.setName(mucLightRoomConfiguration.getRoomName());
                    mChat.setSubject(mucLightRoomConfiguration.getSubject());
                    realm.copyToRealmOrUpdate(mChat);
                    realm.commitTransaction();
                    realm.close();
                }

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

                            String myUser = XMPPUtils.fromJIDToUserName(XMPPSession.getInstance().getUser().toString());
                            String userSender = "";
                            String messageType = message.getType().name();

                            String[] jidList = message.getFrom().toString().split("/");

                            switch (mChat.getType()) {

                                case Chat.TYPE_1_T0_1:
                                    userSender = XMPPUtils.fromJIDToUserName(jidList[0]);
                                    break;

                                case Chat.TYPE_MUC_LIGHT:
                                    if (jidList.length > 1) {
                                        userSender = XMPPUtils.fromJIDToUserName(jidList[1]);
                                    }
                                    break;
                            }

                            if (!userSender.equals(myUser) && !messageType.equals("error")) {
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

                            XMPPError error = message.getError();

                            if (BlockedErrorExtension.isInside(message)) {
                                Toast.makeText(ChatActivity.this, getString(R.string.message_to_blocked_user), Toast.LENGTH_SHORT).show();
                            } else if (error != null && error.getCondition().equals(XMPPError.Condition.service_unavailable)) {
                                Toast.makeText(ChatActivity.this, getString(R.string.cant_send_message), Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                });
            }

        });

        mConnectionSubscription = XMPPSession.getInstance().subscribeToConnection(new Action1<ChatConnection>() {
            @Override
            public void call(ChatConnection chatConnection) {
                switch (chatConnection.getStatus()) {
                    case Connected:
                    case Authenticated:
                        break;
                    case Connecting:
                    case Disconnected:
                        break;
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);
        if (mChat != null) {
            mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.getType());
        }
        disconnectRoomFromServer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        mMenu = menu;

        menu.findItem(R.id.actionChatMembers).setVisible(mChat.getType() != Chat.TYPE_1_T0_1);

        // can change room name or subject only if it is a MUC Light
        menu.findItem(R.id.actionChangeRoomName).setVisible(mChat.getType() == Chat.TYPE_MUC_LIGHT);
        menu.findItem(R.id.actionChangeSubject).setVisible(mChat.getType() == Chat.TYPE_MUC_LIGHT);

        menu.findItem(R.id.actionDestroyChat).setVisible(false);
        setDestroyButtonVisibility(menu);

        menu.findItem(R.id.actionAddToContacts).setVisible(false);
        menu.findItem(R.id.actionRemoveFromContacts).setVisible(false);
        if (mChat.getType() == Chat.TYPE_1_T0_1) {
            menu.findItem(R.id.actionLeaveChat).setTitle(getString(R.string.action_delete_chat));
            manageLeaveAndContactMenuItems();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);
                mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.getType());

                if (mSessionDepth == 1) {
                    Intent intent = new Intent(this, MainMenuActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    finish();
                }

                EventBus.getDefault().post(new Event(Event.Type.GO_BACK_FROM_CHAT));
                break;

            case R.id.actionChatMembers:
                mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);
                Intent chatMembersIntent = new Intent(ChatActivity.this, ChatMembersActivity.class);
                chatMembersIntent.putExtra(ChatMembersActivity.ROOM_JID_PARAMETER, mChatJID);
                chatMembersIntent.putExtra(ChatMembersActivity.IS_ADMIN_PARAMETER, mIsOwner);
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

            case R.id.actionAddToContacts:
                addChatGuyToContacts();
                break;

            case R.id.actionRemoveFromContacts:
                removeChatGuyFromContacts();
                break;
        }

        return true;
    }

    private void removeChatGuyFromContacts() {
        User userNotContact = new User();
        userNotContact.setLogin(XMPPUtils.fromJIDToUserName(mChat.getJid()));
        try {
            RosterManager.getInstance().removeFromBuddies(userNotContact);
            setMenuChatNotContact();
            Toast.makeText(this, String.format(Locale.getDefault(), getString(R.string.user_removed_from_contacts),
                    userNotContact.getLogin()), Toast.LENGTH_SHORT).show();
        } catch (SmackException.NotLoggedInException | InterruptedException |
                SmackException.NotConnectedException | XMPPException.XMPPErrorException |
                XmppStringprepException | SmackException.NoResponseException e) {
            e.printStackTrace();
        }
    }

    private void addChatGuyToContacts() {
        User userContact = new User();
        userContact.setLogin(XMPPUtils.fromJIDToUserName(mChat.getJid()));
        try {
            RosterManager.getInstance().addToBuddies(userContact);
            setMenuChatWithContact();
            Toast.makeText(this, String.format(Locale.getDefault(), getString(R.string.user_added_to_contacts),
                    userContact.getLogin()), Toast.LENGTH_SHORT).show();
        } catch (SmackException.NotLoggedInException | InterruptedException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | XmppStringprepException | SmackException.NoResponseException e) {
            e.printStackTrace();
        }
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
        roomNameEditText.setHint(getString(R.string.enter_room_name_hint));
        roomNameEditText.setText(getSupportActionBar().getTitle());

        linearLayout.addView(roomNameEditText);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(ChatActivity.this)
                .setTitle(getString(R.string.room_name))
                .setMessage(getString(R.string.enter_new_room_name))
                .setView(linearLayout)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final String chatName = roomNameEditText.getText().toString();

                        Tasks.executeInBackground(ChatActivity.this, new BackgroundWork<Object>() {
                            @Override
                            public Object doInBackground() throws Exception {
                                if (!Preferences.isTesting()) {
                                    MultiUserChatLight multiUserChatLight = XMPPSession.getInstance().getMUCLightManager().getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                                    multiUserChatLight.changeRoomName(chatName);
                                }
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
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
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
        roomSubjectEditText.setHint(getString(R.string.enter_room_subject_hint));
        roomSubjectEditText.setText(getSupportActionBar().getSubtitle());

        linearLayout.addView(roomSubjectEditText);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(ChatActivity.this)
                .setTitle(getString(R.string.room_subject))
                .setMessage(getString(R.string.enter_new_room_subject))
                .setView(linearLayout)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final String subject = roomSubjectEditText.getText().toString();

                        Tasks.executeInBackground(ChatActivity.this, new BackgroundWork<Object>() {
                            @Override
                            public Object doInBackground() throws Exception {
                                if (!Preferences.isTesting()) {
                                    MultiUserChatLight multiUserChatLight = XMPPSession.getInstance().getMUCLightManager().getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                                    multiUserChatLight.changeSubject(subject);
                                }
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
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
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

    }

    private void sendTextMessage() {
        if (!XMPPSession.isInstanceNull()
                && (XMPPSession.getInstance().isConnectedAndAuthenticated() || Preferences.isTesting())) {
            String content = chatSendMessageEditText.getText().toString().trim().replaceAll("\n\n+", "\n\n");

            if (!TextUtils.isEmpty(content)) {
                String messageId = RealmManager.getInstance()
                        .saveMessageLocally(mChat, mChatJID, content, ChatMessage.TYPE_CHAT);
                mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);
                mRoomManager.sendTextMessage(messageId, mChatJID, content, mChat.getType());
                chatSendMessageEditText.setText("");
                refreshMessagesAndScrollToEnd();
            }
        }
    }

    private void leaveChat() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        int positiveButtonTitle;
        if (mChat.getType() == Chat.TYPE_1_T0_1) {
            builder.setMessage(getString(R.string.want_to_delete_chat));
            positiveButtonTitle = R.string.action_delete_chat;
        } else {
            builder.setMessage(getString(R.string.want_to_leave_chat));
            positiveButtonTitle = R.string.action_leave_chat;
        }

        builder.setPositiveButton(positiveButtonTitle, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mLeaving = true;

                Realm realm = getRealm();
                Chat chat = realm.where(Chat.class).equalTo("jid", mChatJID).findFirst();

                switch (chat.getType()) {

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

        AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
    }

    private void destroyChat() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.want_to_destroy_chat));

        builder.setPositiveButton(R.string.action_destroy_chat, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mLeaving = true;
                mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);
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

        AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
    }

    private void loadArchivedMessages() {
        mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);

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

        mRoomManager.loadArchivedMessages(mChatJID, PAGES_TO_LOAD, ITEMS_PER_PAGE);
    }

    private void refreshMessages() {
        XMPPSession.getInstance().deleteMessagesToDelete();
        mMessagesAdapter.notifyDataSetChanged();
    }

    private void scrollToEnd() {
        if (mMessages != null) {
            if (!isMessagesListScrolledToBottom() && chatMessagesRecyclerView != null) {
                chatMessagesRecyclerView.scrollToPosition(mMessages.size() - 1);
            }
        }
        cancelMessageNotificationsForChat();
    }

    private boolean isMessagesListScrolledToBottom() {
        int lastPosition = mLayoutManagerMessages.findLastVisibleItemPosition();
        return !(lastPosition <= mMessages.size() - 2);
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
        super.onEvent(event);
        switch (event.getType()) {
            case STICKER_SENT:
                stickerSent(event.getImageName());
                break;

            case PRESENCE_RECEIVED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mChat.getType() == Chat.TYPE_1_T0_1) {
                            setOneToOneChatConnectionStatus();
                        }
                    }
                });
                break;
        }
    }

    private void stickerSent(String imageName) {
        String messageId = RealmManager.getInstance()
                .saveMessageLocally(mChat, mChatJID, imageName, ChatMessage.TYPE_STICKER);
        mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);
        mRoomManager.sendStickerMessage(messageId, mChatJID, imageName, mChat.getType());
        stickersRecyclerView.setVisibility(View.GONE);
        refreshMessagesAndScrollToEnd();
    }

    private void manageLeaveAndContactMenuItems() {
        if (isChatWithContact()) {
            setMenuChatWithContact();
        } else {
            setMenuChatNotContact();
        }
    }

    private void setMenuChatNotContact() {
        mMenu.findItem(R.id.actionLeaveChat).setVisible(true);
        mMenu.findItem(R.id.actionAddToContacts).setVisible(true);
        mMenu.findItem(R.id.actionRemoveFromContacts).setVisible(false);
    }

    private void setMenuChatWithContact() {
        mMenu.findItem(R.id.actionLeaveChat).setVisible(false);
        mMenu.findItem(R.id.actionAddToContacts).setVisible(false);
        mMenu.findItem(R.id.actionRemoveFromContacts).setVisible(true);
    }

    private boolean isChatWithContact() {
        try {
            HashMap<Jid, Presence.Type> buddies = RosterManager.getInstance().getContacts();
            for (Map.Entry pair : buddies.entrySet()) {
                if (mChat.getJid().equals(pair.getKey().toString())) {
                    return true;
                }
            }
        } catch (SmackException.NotLoggedInException | InterruptedException | SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void setDestroyButtonVisibility(final Menu menu) {
        switch (mChat.getType()) {
            case Chat.TYPE_MUC_LIGHT:
                manageMUCLightAdmins(menu);
                break;
        }
    }

    private void manageMUCLightAdmins(final Menu menu) {
        Tasks.executeInBackground(this, new BackgroundWork<HashMap<Jid, MUCLightAffiliation>>() {
            @Override
            public HashMap<Jid, MUCLightAffiliation> doInBackground() throws Exception {
                if (Preferences.isTesting()) {
                    return null;
                }
                MultiUserChatLightManager multiUserChatLightManager = MultiUserChatLightManager.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
                MultiUserChatLight multiUserChatLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                return multiUserChatLight.getAffiliations();
            }
        }, new Completion<HashMap<Jid, MUCLightAffiliation>>() {
            @Override
            public void onSuccess(Context context, HashMap<Jid, MUCLightAffiliation> occupants) {
                if (occupants != null) {
                    for (Map.Entry<Jid, MUCLightAffiliation> pair : occupants.entrySet()) {
                        Jid key = pair.getKey();
                        if (key != null && key.toString().equals(Preferences.getInstance().getUserXMPPJid())) {
                            MenuItem destroyItem = menu.findItem(R.id.actionDestroyChat);
                            mIsOwner = pair.getValue().equals(MUCLightAffiliation.owner);
                            destroyItem.setVisible(mIsOwner && mChat.getType() == Chat.TYPE_MUC_LIGHT);
                        }
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


