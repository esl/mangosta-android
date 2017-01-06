package inaka.com.mangosta.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import org.jivesoftware.smack.packet.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.ChatActivity;
import inaka.com.mangosta.adapters.ChatListAdapter;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.chat.RoomManagerListener;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.ui.itemTouchHelper.SimpleItemTouchHelperCallback;
import inaka.com.mangosta.utils.ChatOrderComparator;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import rx.Subscription;
import rx.functions.Action1;


public class ChatsListsFragment extends BaseFragment {

    @Bind(R.id.groupChatsRecyclerView)
    RecyclerView groupChatsRecyclerView;

    @Bind(R.id.oneToOneChatsRecyclerView)
    RecyclerView oneToOneChatsRecyclerView;

    @Bind(R.id.expandGroupChatsImage)
    ImageView expandGroupChatsImage;

    @Bind(R.id.expandOneToOneChatsImage)
    ImageView expandOneToOneChatsImage;

    @Bind(R.id.expandGroupChatsLayout)
    LinearLayout expandGroupChatsLayout;

    @Bind(R.id.expandOneToOneChatsLayout)
    LinearLayout expandOneToOneChatsLayout;

    @Bind(R.id.chatsLoading)
    ProgressBar chatsLoading;

    private RoomManager mRoomManager;
    private List<Chat> mGroupChats;
    private List<Chat> mOneToOneChats;

    private ChatListAdapter mGroupChatsAdapter;
    private ChatListAdapter mOneToOneChatsAdapter;

    Subscription mMessageSubscription;
    Subscription mMessageSentAlertSubscription;

    Activity mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats_lists, container, false);
        ButterKnife.bind(this, view);

        mContext = getActivity();
        mRoomManager = RoomManager.getInstance(new RoomManagerChatListListener(mContext));

        mGroupChats = new ArrayList<>();
        initGroupChatsRecyclerView();

        mOneToOneChats = new ArrayList<>();
        initOneToOneChatsRecyclerView();

        setExpandLayout(expandGroupChatsLayout, groupChatsRecyclerView, expandGroupChatsImage);
        setExpandLayout(expandOneToOneChatsLayout, oneToOneChatsRecyclerView, expandOneToOneChatsImage);

        mMessageSubscription = XMPPSession.getInstance().subscribeToMessages(new Action1<Message>() {
            @Override
            public void call(Message message) {
                loadChats();
            }
        });

        mMessageSentAlertSubscription = XMPPSession.getInstance().subscribeToMessageSent(new Action1<Message>() {
            @Override
            public void call(Message message) {
                loadChats();
            }
        });

        loadChatsBackgroundTask();

        return view;
    }

    private void setExpandLayout(LinearLayout layout, final RecyclerView recyclerView, final ImageView imageView) {
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recyclerView.getVisibility() == View.VISIBLE) {
                    recyclerView.setVisibility(View.GONE);
                    imageView.setImageResource(R.mipmap.ic_expand_less);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    imageView.setImageResource(R.mipmap.ic_expand_more);
                }
            }
        });
    }

    private void initGroupChatsRecyclerView() {
        mGroupChatsAdapter = getGroupChatsAdapter();
        groupChatsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManagerGroupChats = new LinearLayoutManager(mContext);
        groupChatsRecyclerView.setLayoutManager(layoutManagerGroupChats);
        groupChatsRecyclerView.setAdapter(mGroupChatsAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mGroupChatsAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(groupChatsRecyclerView);
    }

    private void initOneToOneChatsRecyclerView() {
        mOneToOneChatsAdapter = getOneToOneChatsAdapter();
        oneToOneChatsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManagerOneToOneChats = new LinearLayoutManager(mContext);
        oneToOneChatsRecyclerView.setLayoutManager(layoutManagerOneToOneChats);
        oneToOneChatsRecyclerView.setAdapter(mOneToOneChatsAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mOneToOneChatsAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(oneToOneChatsRecyclerView);
    }

    public ChatListAdapter getGroupChatsAdapter() {
        return getChatListAdapter(mGroupChats);
    }

    public ChatListAdapter getOneToOneChatsAdapter() {
        return getChatListAdapter(mOneToOneChats);
    }

    private ChatListAdapter getChatListAdapter(List<Chat> chats) {
        return new ChatListAdapter(chats, mContext,
                new ChatListAdapter.ChatClickListener() {
                    @Override
                    public void onChatClicked(Chat chat) {
                        Intent intent = new Intent(mContext, ChatActivity.class);
                        intent.putExtra(ChatActivity.CHAT_JID_PARAMETER, chat.getJid());
                        intent.putExtra(ChatActivity.CHAT_NAME_PARAMETER, XMPPUtils.getChatName(chat));
                        intent.putExtra(ChatActivity.IS_NEW_CHAT_PARAMETER, false);
                        mContext.startActivity(intent);
                    }
                });
    }

    public void loadChats() {
        if (mContext == null) {
            changeChatsList();
        } else {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeChatsList();
                }
            });
        }
    }

    private void changeChatsList() {
        updateChatsList(mGroupChats, RealmManager.getInstance().getMUCLights());
        updateChatsList(mOneToOneChats, RealmManager.getInstance().get1to1Chats());

        Collections.sort(mGroupChats, new ChatOrderComparator());
        Collections.sort(mOneToOneChats, new ChatOrderComparator());

        if (mOneToOneChatsAdapter == null) {
            mOneToOneChatsAdapter = getOneToOneChatsAdapter();
        }

        if (mGroupChatsAdapter == null) {
            mGroupChatsAdapter = getGroupChatsAdapter();
        }

        mOneToOneChatsAdapter.notifyDataSetChanged();
        mGroupChatsAdapter.notifyDataSetChanged();

        if (chatsLoading != null) {
            chatsLoading.setVisibility(View.GONE);
        }
    }

    private void loadChatsAfterRoomLeft(final String roomJid) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RealmManager.getInstance().deleteChat(roomJid);
                mRoomManager.loadMUCLightRoomsInBackground();
            }
        });
    }

    private void updateChatsList(List<Chat> chatsList, List<Chat> updaterList) {
        boolean deletedOneToOneChat = deletedChat(chatsList);
        if (deletedOneToOneChat) {
            chatsList.clear();
            chatsList.addAll(updaterList);
        } else {
            refineChatsList(chatsList, updaterList);
        }
    }

    private boolean deletedChat(List<Chat> chats) {
        for (Chat chat : chats) {
            if (!chat.isValid()) {
                return true;
            }
        }
        return false;
    }

    private void refineChatsList(List<Chat> list, List<Chat> refiner) {
        for (Chat chat : refiner) {
            if (!list.contains(chat)) {
                list.add(chat);
            }
        }
    }

    public void loadChatsBackgroundTask() {
        if (mRoomManager == null) {
            return;
        }

        Tasks.executeInBackground(mContext, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                mRoomManager.loadMUCLightRooms(); // load group chats
                mRoomManager.loadRosterContactsChats(); // load 1 to 1 chats from contacts
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object object) {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadChats();
                    }
                });
            }

            @Override
            public void onError(Context context, Exception e) {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadChats();
                    }
                });
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    // receives events from EventBus
    public void onEvent(Event event) {
        switch (event.getType()) {
            case GO_BACK_FROM_CHAT:
                loadChats();
                break;

            case CONTACTS_CHANGED:
                loadChatsBackgroundTask();
                break;

            case PRESENCE_RECEIVED:
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mOneToOneChatsAdapter.notifyDataSetChanged();
                    }
                });
                break;
        }
    }

    private class RoomManagerChatListListener extends RoomManagerListener {

        public RoomManagerChatListListener(Context context) {
            super(context);
        }

        @Override
        public void onRoomLeft(String roomJid) {
            loadChatsAfterRoomLeft(roomJid);
        }

        @Override
        public void onRoomsLoaded() {
            loadChats();
        }
    }

}
