package inaka.com.mangosta.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.ChatActivity;
import inaka.com.mangosta.adapters.ChatListAdapter;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.chat.RoomManagerListener;
import inaka.com.mangosta.database.MangostaDatabase;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.ui.itemTouchHelper.SimpleItemTouchHelperCallback;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ChatsListsFragment extends BaseFragment {

    private static final String TAG = ChatsListsFragment.class.getSimpleName();

    @BindView(R.id.groupChatsRecyclerView)
    RecyclerView groupChatsRecyclerView;

    @BindView(R.id.oneToOneChatsRecyclerView)
    RecyclerView oneToOneChatsRecyclerView;

    @BindView(R.id.expandGroupChatsImage)
    ImageView expandGroupChatsImage;

    @BindView(R.id.expandOneToOneChatsImage)
    ImageView expandOneToOneChatsImage;

    @BindView(R.id.expandGroupChatsLayout)
    LinearLayout expandGroupChatsLayout;

    @BindView(R.id.expandOneToOneChatsLayout)
    LinearLayout expandOneToOneChatsLayout;

    @BindView(R.id.chatsLoading)
    ProgressBar chatsLoading;

    private RoomManager mRoomManager;
    private RoomManagerListener mRoomManagerListener;

    private ChatListAdapter mGroupChatsAdapter;
    private ChatListAdapter mOneToOneChatsAdapter;

    private boolean mOneToOneChatsLoaded = false;
    private boolean mGroupChatsLoaded = false;

    private MangostaDatabase database = MangostaApplication.getInstance().getDatabase();

    Disposable mMessageSubscription;
    Disposable mMessageSentAlertSubscription;

    Activity mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats_lists, container, false);
        ButterKnife.bind(this, view);

        mContext = getActivity();
        mRoomManager = RoomManager.getInstance();
        mRoomManagerListener = new RoomManagerChatListListener();

        updateProgress();

        initGroupChatsRecyclerView();

        initOneToOneChatsRecyclerView();

        final Preferences preferences = Preferences.getInstance();
        setExpandLayout(expandGroupChatsLayout, groupChatsRecyclerView, expandGroupChatsImage,
                preferences.isMenuRoomsExpanded(), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (groupChatsRecyclerView.getVisibility() == View.VISIBLE) {
                            notExpanded(groupChatsRecyclerView, expandGroupChatsImage);
                            preferences.setMenuRoomsExpanded(false);
                        } else {
                            expanded(groupChatsRecyclerView, expandGroupChatsImage);
                            preferences.setMenuRoomsExpanded(true);
                        }
                    }
                });
        setExpandLayout(expandOneToOneChatsLayout, oneToOneChatsRecyclerView, expandOneToOneChatsImage,
                preferences.isMenuPeopleExpanded(), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (oneToOneChatsRecyclerView.getVisibility() == View.VISIBLE) {
                            notExpanded(oneToOneChatsRecyclerView, expandOneToOneChatsImage);
                            preferences.setMenuPeopleExpanded(false);
                        } else {
                            expanded(oneToOneChatsRecyclerView, expandOneToOneChatsImage);
                            preferences.setMenuPeopleExpanded(true);
                        }
                    }
                });

        mRoomManager.loadAllChats();

        addDisposable(database.chatDao().findByType(Chat.TYPE_MUC_LIGHT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mucLightChatRooms -> {
                    mGroupChatsAdapter.updateList(mucLightChatRooms);
                    mGroupChatsLoaded = true;
                    updateProgress();
                }));
        addDisposable(database.chatDao().findByType(Chat.TYPE_1_T0_1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(one2oneChatRooms -> {
                    mOneToOneChatsAdapter.updateList(one2oneChatRooms);
                    mOneToOneChatsLoaded = true;
                    updateProgress();
                }));

        return view;
    }

    private void updateProgress() {
        if (mOneToOneChatsLoaded && mGroupChatsLoaded) {
            chatsLoading.setVisibility(View.GONE);
        } else {
            chatsLoading.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mMessageSubscription != null) {
            mMessageSubscription.dispose();
        }
        if (mMessageSentAlertSubscription != null) {
            mMessageSentAlertSubscription.dispose();
        }
    }

    private void setExpandLayout(LinearLayout layout, final RecyclerView recyclerView, final ImageView imageView,
                                 boolean expanded, View.OnClickListener onClickListener) {
        if (expanded) {
            expanded(recyclerView, imageView);
        } else {
            notExpanded(recyclerView, imageView);
        }
        layout.setOnClickListener(onClickListener);
    }

    private void notExpanded(RecyclerView recyclerView, ImageView imageView) {
        recyclerView.setVisibility(View.GONE);
        imageView.setImageResource(R.mipmap.ic_expand_less);
    }

    private void expanded(RecyclerView recyclerView, ImageView imageView) {
        recyclerView.setVisibility(View.VISIBLE);
        imageView.setImageResource(R.mipmap.ic_expand_more);
    }

    private void initGroupChatsRecyclerView() {
        mGroupChatsAdapter = new ChatListAdapter(new ArrayList<>(), mContext,
                new ChatListAdapter.ChatClickListener() {
                    @Override
                    public void onChatClicked(Chat chat) {
                        Intent intent = new Intent(mContext, ChatActivity.class);
                        intent.putExtra(ChatActivity.CHAT_JID_PARAMETER, chat.getJid());
                        intent.putExtra(ChatActivity.CHAT_NAME_PARAMETER, XMPPUtils.getChatName(chat));
                        mContext.startActivity(intent);
                    }
                });

        groupChatsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManagerGroupChats = new LinearLayoutManager(mContext);
        groupChatsRecyclerView.setLayoutManager(layoutManagerGroupChats);
        groupChatsRecyclerView.setAdapter(mGroupChatsAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mGroupChatsAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(groupChatsRecyclerView);
    }

    private void initOneToOneChatsRecyclerView() {
        mOneToOneChatsAdapter = new ChatListAdapter(new ArrayList<>(), mContext,
                new ChatListAdapter.ChatClickListener() {
                    @Override
                    public void onChatClicked(Chat chat) {
                        Intent intent = new Intent(mContext, ChatActivity.class);
                        intent.putExtra(ChatActivity.CHAT_JID_PARAMETER, chat.getJid());
                        intent.putExtra(ChatActivity.CHAT_NAME_PARAMETER, XMPPUtils.getChatName(chat));
                        mContext.startActivity(intent);
                    }
                });

        oneToOneChatsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManagerOneToOneChats = new LinearLayoutManager(mContext);
        oneToOneChatsRecyclerView.setLayoutManager(layoutManagerOneToOneChats);
        oneToOneChatsRecyclerView.setAdapter(mOneToOneChatsAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mOneToOneChatsAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(oneToOneChatsRecyclerView);
    }

    // receives events from EventBus
//    public void onEvent(Event event) {
//        switch (event.getType()) {
//
//            case CONTACTS_CHANGED:
//                loadChatsBackgroundTask();
//                break;
//
//            case PRESENCE_RECEIVED:
//                mContext.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mOneToOneChatsAdapter.notifyDataSetChanged();
//                    }
//                });
//                break;
//        }
//    }

    private class RoomManagerChatListListener extends RoomManagerListener {

        @Override
        public void onRoomLeft(String roomJid) {
            Disposable d = Completable.fromAction(() -> database.chatDao().deleteByJid(roomJid))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        }

    }

}
