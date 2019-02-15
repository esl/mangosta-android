package inaka.com.mangosta.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.blocking.element.BlockedErrorExtension;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.muclight.MUCLightAffiliation;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.muclight.MultiUserChatLightManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.ChatMessagesAdapter;
import inaka.com.mangosta.adapters.StickersAdapter;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.chat.RoomManagerListener;
import inaka.com.mangosta.database.MangostaDatabase;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.models.MongooseMUCLight;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.models.event.SendEvent;
import inaka.com.mangosta.network.MongooseAPI;
import inaka.com.mangosta.network.MongooseService;
import inaka.com.mangosta.notifications.MessageNotifications;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {

    private final static String TAG = ChatActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.chatMessagesRecyclerView)
    RecyclerView chatMessagesRecyclerView;

    @BindView(R.id.stickersRecyclerView)
    RecyclerView stickersRecyclerView;

    @BindView(R.id.chatSendMessageButton)
    ImageButton chatSendMessageButton;

    @BindView(R.id.stickersMenuImageButton)
    ImageButton stickersMenuImageButton;

    @BindView(R.id.chatSendMessageEditText)
    EditText chatSendMessageEditText;

    @BindView(R.id.loadMessagesSwipeRefreshLayout)
    SwipeRefreshLayout loadMessagesSwipeRefreshLayout;

    @BindView(R.id.chatTypingTextView)
    TextView chatTypingTextView;

    @BindView(R.id.scrollDownImageButton)
    ImageButton scrollDownImageButton;

    private RoomManager mRoomManager;
    private RoomManagerListener mRoomManagerListener;
    private String mChatJID;

    public static String CHAT_JID_PARAMETER = "chatJid";
    public static String CHAT_NAME_PARAMETER = "chatName";

    private Chat mChat;

    private ChatMessagesAdapter mMessagesAdapter;
    private LinearLayoutManager mLayoutManagerMessages;

    private String[] mStickersNameList = {
            "base",
            "pliiz",
            "bigsmile",
            "pleure",
            "snif"
    };
    private StickersAdapter mStickersAdapter;
    private LinearLayoutManager mLayoutManagerStickers;

    private Disposable mMessageSubscription;
    private Disposable mMongooseMessageSubscription;
    private Disposable mMongooseMUCLightMessageSubscription;
    private Disposable mConnectionSubscription;
    private Disposable mArchiveQuerySubscription;
    private Disposable mPresenceSubscription;
    private Disposable mErrorArchiveQuerySubscription;

    private MangostaDatabase database = MangostaApplication.getInstance().getDatabase();

    boolean mLeaving = false;
    boolean mIsOwner = false;

    private Timer mPauseComposeTimer = new Timer();
    private Menu mMenu;

    final private int VISIBLE_BEFORE_LOAD = 10;
    final private int ITEMS_PER_PAGE = 15;
    final private int PAGES_TO_LOAD = 3;

    SwipeRefreshLayout.OnRefreshListener mSwipeRefreshListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        unbinder = ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        mChatJID = getIntent().getStringExtra(CHAT_JID_PARAMETER);
        final String chatName = getIntent().getStringExtra(CHAT_NAME_PARAMETER);

        mRoomManager = RoomManager.getInstance();
        mRoomManagerListener = new RoomManagerChatListener();

        mLayoutManagerMessages = new LinearLayoutManager(this);
        mLayoutManagerMessages.setStackFromEnd(true);

        chatMessagesRecyclerView.setHasFixedSize(true);
        chatMessagesRecyclerView.setLayoutManager(mLayoutManagerMessages);

        addDisposable(database.chatDao().findByJid(mChatJID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(chat -> initChat(chat, chatName),
                        error -> Log.w(TAG, "error loading chat " + mChatJID, error),
                        () -> Log.w(TAG, "no chat found for " + mChatJID)));

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
                if (mChat != null) {
                    if (charSequence.length() > 0) { // compose message
                        mPauseComposeTimer.cancel();
                        mRoomManager.updateTypingStatus(ChatState.composing, mChatJID, mChat.isMucLight(), mRoomManagerListener);
                        schedulePauseTimer();
                    } else { // delete or send message
                        mPauseComposeTimer.cancel();
                        mRoomManager.updateTypingStatus(ChatState.inactive, mChatJID, mChat.isMucLight(), mRoomManagerListener);
                    }
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

        addDisposable(mStickersAdapter.getStickerSentObservable()
                .subscribe(event -> {
                    cancelMessageNotificationsForChat();
                    if (SendEvent.Type.SEND_STICKER.equals(event.getType())) {
                        sendStickerMessage(event.getImageName());
                    }
                }));

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

        chatMessagesRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                cancelMessageNotificationsForChat();
                mMessagesAdapter.notifyDataSetChanged();
                return false;
            }
        });
    }

    private void initChat(Chat chat, String chatName) {
        mChat = chat;

        getSupportActionBar().setTitle(chatName);
        if (mChat.getSubject() != null) {
            getSupportActionBar().setSubtitle(mChat.getSubject());
        }

        if (mChat.getType() == Chat.TYPE_MUC_LIGHT) {
            manageRoomNameAndSubject();
        } else {
            setOneToOneChatConnectionStatus();
        }

        mMessagesAdapter = new ChatMessagesAdapter(this, new ArrayList<>(), mChat);
        chatMessagesRecyclerView.setAdapter(mMessagesAdapter);

        chatSendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View button) {
                sendTextMessage();
            }
        });

        getMessageBeingComposed();

        setMenuItemsVisibility();

        final ChatMessage headerChatMessage = new ChatMessage();
        headerChatMessage.setType(ChatMessage.TYPE_HEADER);
        headerChatMessage.setMessageId("dummy");

        addDisposable(database.chatMessageDao().findByChatId(mChatJID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(messages -> {
                    int unreadMessages = mChat.getUnreadMessagesCount();
                    if (unreadMessages > 0) {
                        messages.add(messages.size() - unreadMessages, headerChatMessage);
                    }
                    mMessagesAdapter.updateList(messages);
                }));
    }

    private void setMenuItemsVisibility() {
        //set menu items
        if (mMenu != null && mChat != null) {
            mMenu.findItem(R.id.actionChatMembers).setVisible(mChat.getType() != Chat.TYPE_1_T0_1);

            // can change room name or subject only if it is a MUC Light
            mMenu.findItem(R.id.actionChangeRoomName).setVisible(mChat.getType() == Chat.TYPE_MUC_LIGHT);
            mMenu.findItem(R.id.actionChangeSubject).setVisible(mChat.getType() == Chat.TYPE_MUC_LIGHT);

            mMenu.findItem(R.id.actionDestroyChat).setVisible(false);
            setDestroyButtonVisibility(mMenu);

            mMenu.findItem(R.id.actionAddToContacts).setVisible(false);
            mMenu.findItem(R.id.actionRemoveFromContacts).setVisible(false);
            if (mChat.getType() == Chat.TYPE_1_T0_1) {
                mMenu.findItem(R.id.actionLeaveChat).setTitle(getString(R.string.action_delete_chat));
                manageLeaveAndContactMenuItems();
            }
        }

    }
    @Override
    protected void onResume() {
        super.onResume();

        mMessageSubscription = XMPPSession.getInstance().subscribeRoomToMessages(mChatJID, message -> {
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

                showTypingStatus(chatState, myUser, userSender, messageType);
            } else {
                String subject = message.getSubject();
                if (subject != null) {
                    setTitle(subject);
                }
                scrollToEnd();

                showErrorToast(message);
            }
        });

        mMongooseMessageSubscription = XMPPSession.getInstance().subscribeRoomToMongooseMessages(mChatJID, message -> {
            scrollToEnd();
        });

        mMongooseMUCLightMessageSubscription = XMPPSession.getInstance().subscribeRoomToMUCLightMongooseMessages(mChatJID, message -> {
            scrollToEnd();
        });

        mConnectionSubscription = XMPPSession.getInstance().subscribeToConnection(chatConnection -> {
            Log.d(TAG, "ChatConnection: " + chatConnection.getStatus());
            switch (chatConnection.getStatus()) {
                case Connected:
                case Authenticated:
                    break;
                case Connecting:
                case Disconnected:
                    break;
            }
        });

        mPresenceSubscription = XMPPSession.getInstance().subscribeToPresence(presence -> {
            if (mChat != null && mChat.getType() == Chat.TYPE_1_T0_1) {
                setOneToOneChatConnectionStatus();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveMessageBeingComposed();

        cancelMessageNotificationsForChat();
        mMessagesAdapter.notifyDataSetChanged();

        sendInactiveTypingStatus();

        disposeConnectionSubscriptions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        mMenu = menu;
        setMenuItemsVisibility();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        cancelMessageNotificationsForChat();

        switch (id) {
            case android.R.id.home:

                sendInactiveTypingStatus();

                if (mSessionDepth == 1) {
                    Intent intent = new Intent(this, MainMenuActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    finish();
                }

                break;

            case R.id.actionChatMembers:
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
                if (mChat != null) {
                    mRoomManager.updateTypingStatus(ChatState.gone, mChatJID, mChat.isMucLight(), mRoomManagerListener);
                }
                leaveChat();
                break;

            case R.id.actionDestroyChat:
                if (mChat != null) {
                    mRoomManager.updateTypingStatus(ChatState.gone, mChatJID, mChat.isMucLight(), mRoomManagerListener);
                }
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        saveMessageBeingComposed();
        sendInactiveTypingStatus();
        cancelMessageNotificationsForChat();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveMessageBeingComposed();
    }

    private void showErrorToast(Message message) {
        XMPPError error = message.getError();
        if (BlockedErrorExtension.isInside(message)) {
            Toast.makeText(ChatActivity.this, getString(R.string.message_to_blocked_user), Toast.LENGTH_SHORT).show();
        } else if (error != null && error.getCondition().equals(XMPPError.Condition.service_unavailable)) {
            Toast.makeText(ChatActivity.this, getString(R.string.cant_send_message), Toast.LENGTH_SHORT).show();
        }
    }

    private void showTypingStatus(ChatState chatState, String myUser, String userSender, String messageType) {
        if (!userSender.equals(myUser) && !messageType.equals("error")) {
            if (chatState.equals(ChatState.composing)) { // typing
                chatTypingTextView.setText(String.format(Locale.getDefault(), getString(R.string.typing), userSender));
                chatTypingTextView.setVisibility(View.VISIBLE);
            } else { // not typing
                chatTypingTextView.setVisibility(View.GONE);
            }
        }
    }

    private void saveMessageBeingComposed() {
        if (!Preferences.isTesting()) {

            if (mChat != null && chatSendMessageEditText != null) {
                String message = chatSendMessageEditText.getText().toString();

                addDisposable(Completable.fromAction(() -> database.chatDao().updateMessageBeingComposed(mChatJID, message))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe());
            }
        }
    }

    private void getMessageBeingComposed() {
        if (!Preferences.isTesting()) {
            String message = mChat.getMessageBeingComposed();
            if (!TextUtils.isEmpty(message)) {
                chatSendMessageEditText.setText(message);
            }
        }
    }

    private void cancelMessageNotificationsForChat() {
        if (!Preferences.isTesting()) {
            MessageNotifications.cancelChatNotifications(this, mChatJID);
            addDisposable(Single.fromCallable(() -> database.chatDao().resetUnreadMessageCount(mChatJID))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> mChat.setUnreadMessagesCount(0)));
        }
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
                        sendInactiveTypingStatus();
                    }
                });
            }
        }, 15000);
    }

    private void manageRoomNameAndSubject() {
        MongooseService mongooseService = MongooseAPI.getInstance().getAuthenticatedService();

        if (mongooseService != null) {
            Call<MongooseMUCLight> call = mongooseService.getMUCLightDetails(mChatJID.split("@")[0]);
            call.enqueue(new Callback<MongooseMUCLight>() {
                @Override
                public void onResponse(Call<MongooseMUCLight> call, Response<MongooseMUCLight> response) {
                    MongooseMUCLight mongooseMUCLight = response.body();

                    if (mongooseMUCLight != null) {
                        mChat.setName(mongooseMUCLight.getName());
                        mChat.setSubject(mongooseMUCLight.getSubject());
                        Completable.fromAction(() -> database.chatDao().update(mChat))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe();

                        if (mChat.getSubject() != null) {
                            getSupportActionBar().setSubtitle(mChat.getSubject());
                        }
                    }

                }

                @Override
                public void onFailure(Call<MongooseMUCLight> call, Throwable t) {
                    Toast.makeText(ChatActivity.this, ChatActivity.this.getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void sendInactiveTypingStatus() {
        if (mChat != null) {
            if (wasComposingMessage()) {
                mRoomManager.updateTypingStatus(ChatState.paused, mChatJID, mChat.isMucLight(), mRoomManagerListener);
            } else {
                mRoomManager.updateTypingStatus(ChatState.inactive, mChatJID, mChat.isMucLight(), mRoomManagerListener);
            }
        }
    }

    private boolean wasComposingMessage() {
        return chatSendMessageEditText != null && chatSendMessageEditText.getText().length() > 0;
    }

    private void removeChatGuyFromContacts() {
        User userNotContact = new User(mChat.getJid());
        try {
            RosterManager.getInstance().removeContact(userNotContact);
            setMenuChatNotContact();
            Toast.makeText(this, String.format(Locale.getDefault(), getString(R.string.user_removed_from_contacts),
                    XMPPUtils.getDisplayName(userNotContact)), Toast.LENGTH_SHORT).show();
        } catch (SmackException.NotLoggedInException | InterruptedException |
                SmackException.NotConnectedException | XMPPException.XMPPErrorException |
                XmppStringprepException | SmackException.NoResponseException e) {
            e.printStackTrace();
        }
    }

    private void addChatGuyToContacts() {
        User userContact = new User(mChat.getJid());
        try {
            RosterManager.getInstance().addContact(userContact);
            setMenuChatWithContact();
            Toast.makeText(this, String.format(Locale.getDefault(), getString(R.string.user_added_to_contacts),
                    XMPPUtils.getDisplayName(userContact)), Toast.LENGTH_SHORT).show();
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
                        renameRoom(chatName);
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

    private void renameRoom(String chatName) {
        Completable task = Completable.fromCallable(() -> {
            if (!Preferences.isTesting()) {
                MultiUserChatLight multiUserChatLight = XMPPSession.getInstance().getMUCLightManager().getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                multiUserChatLight.changeRoomName(chatName);
            }
            return null;
        });

        addDisposable(task
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    Toast.makeText(ChatActivity.this,
                            getString(R.string.room_name_changed),
                            Toast.LENGTH_SHORT).show();

                    Completable.fromAction(() -> database.chatDao().updateName(mChatJID, chatName))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe();

                    getSupportActionBar().setTitle(chatName);
                }, error -> {
                    Toast.makeText(ChatActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "renameRoom error", error);
                }));
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
                        changeRoomSubject(subject);
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

    private void changeRoomSubject(String subject) {
        Completable task = Completable.fromCallable(() -> {
            if (!Preferences.isTesting()) {
                MultiUserChatLight multiUserChatLight = XMPPSession.getInstance().getMUCLightManager().getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
                multiUserChatLight.changeSubject(subject);
            }
            return null;
        });

        addDisposable(task
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    Toast.makeText(ChatActivity.this,
                            getString(R.string.room_subject_changed),
                            Toast.LENGTH_SHORT).show();

                    addDisposable(Completable.fromAction(() -> database.chatDao().updateSubject(mChatJID, subject))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe());

                    getSupportActionBar().setSubtitle(subject);
                }, error -> {
                    Toast.makeText(ChatActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }));
    }

    private void disposeConnectionSubscriptions() {
        if (mMessageSubscription != null)
            mMessageSubscription.dispose();

        if (mConnectionSubscription != null)
            mConnectionSubscription.dispose();

        if (mArchiveQuerySubscription != null)
            mArchiveQuerySubscription.dispose();

        if (mErrorArchiveQuerySubscription != null)
            mErrorArchiveQuerySubscription.dispose();

        if (mMongooseMessageSubscription != null)
            mMongooseMessageSubscription.dispose();

        if (mMongooseMUCLightMessageSubscription != null)
            mMongooseMUCLightMessageSubscription.dispose();

        if (mPresenceSubscription != null)
            mPresenceSubscription.dispose();
    }

    private void sendTextMessage() {
        cancelMessageNotificationsForChat();
        if (!XMPPSession.isInstanceNull()
                && (XMPPSession.getInstance().isConnectedAndAuthenticated() || Preferences.isTesting())) {
            String content = chatSendMessageEditText.getText().toString().trim().replaceAll("\n\n+", "\n\n");

            if (!TextUtils.isEmpty(content)) {
                mRoomManager.sendTextMessage(mChatJID, mChat.isMucLight(), content, mRoomManagerListener);
                chatSendMessageEditText.setText("");
                scrollToEnd();
            }
        }
    }

    private void sendStickerMessage(String imageName) {
        mRoomManager.sendStickerMessage(mChatJID, mChat.isMucLight(),
                imageName, mRoomManagerListener);
        stickersRecyclerView.setVisibility(View.GONE);
        scrollToEnd();
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

                switch (mChat.getType()) {
                    case Chat.TYPE_MUC_LIGHT:
                        disposeConnectionSubscriptions();
                        mRoomManager.leaveMUCLight(mChatJID, mRoomManagerListener);
                        break;

                    case Chat.TYPE_1_T0_1:
                        mRoomManager.leave1to1Chat(mChatJID, mRoomManagerListener);
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
                if (mChat.getType() == Chat.TYPE_MUC_LIGHT) {
                    disposeConnectionSubscriptions();
                    mRoomManager.destroyMUCLight(mChatJID, mRoomManagerListener);
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
        if (mChat == null) {

            if (loadMessagesSwipeRefreshLayout != null) {
                loadMessagesSwipeRefreshLayout.setRefreshing(false);
            }

            if (mErrorArchiveQuerySubscription != null) {
                mErrorArchiveQuerySubscription.dispose();
            }

            if (mArchiveQuerySubscription != null) {
                mArchiveQuerySubscription.dispose();
            }

            return;
        }

        mArchiveQuerySubscription = XMPPSession.getInstance().subscribeToArchiveQuery(s -> {
            if (loadMessagesSwipeRefreshLayout != null) {
                loadMessagesSwipeRefreshLayout.setRefreshing(false);
            }
        });

        mErrorArchiveQuerySubscription = XMPPSession.getInstance().subscribeToError(errorIQ -> {
            if (loadMessagesSwipeRefreshLayout != null) {
                loadMessagesSwipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(ChatActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
        });

        mRoomManager.loadArchivedMessages(mChat, PAGES_TO_LOAD, ITEMS_PER_PAGE);
    }

    private void scrollToEnd() {
        if (mMessagesAdapter != null) {
            if (!isMessagesListScrolledToBottom() && chatMessagesRecyclerView != null) {
                chatMessagesRecyclerView.smoothScrollToPosition(mMessagesAdapter.getItemCount() - 1);
            }
        }
    }

    private boolean isMessagesListScrolledToBottom() {
        int lastPosition = mLayoutManagerMessages.findLastVisibleItemPosition();
        return !(lastPosition <= mMessagesAdapter.getItemCount() - 2);
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
            Log.w(TAG, e);
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
        Single<HashMap<Jid, MUCLightAffiliation>> task = Single.fromCallable(() -> {
            if (Preferences.isTesting()) {
                return null;
            }
            MultiUserChatLightManager multiUserChatLightManager = MultiUserChatLightManager.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
            MultiUserChatLight multiUserChatLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
            return multiUserChatLight.getAffiliations();
        });

        addDisposable(task.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(occupants -> {
                    for (Map.Entry<Jid, MUCLightAffiliation> pair : occupants.entrySet()) {
                        Jid key = pair.getKey();
                        if (key != null && key.toString().equals(Preferences.getInstance().getUserXMPPJid())) {
                            MenuItem destroyItem = menu.findItem(R.id.actionDestroyChat);
                            mIsOwner = pair.getValue().equals(MUCLightAffiliation.owner);
                            destroyItem.setVisible(mIsOwner && mChat.getType() == Chat.TYPE_MUC_LIGHT);
                        }
                    }
                }, error -> Log.w(TAG, error)));
    }

    private class RoomManagerChatListener extends RoomManagerListener {
        @Override
        public void onMessageSent(int chatType) {
            new Handler().postDelayed(() -> scrollToEnd(), 500L);
        }
    }
}

