package inaka.com.mangosta.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.chat.RoomManagerListener;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.models.event.UserEvent;
import inaka.com.mangosta.utils.NavigateToChat;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class CreateChatActivity extends BaseActivity {

    private static final String TAG = CreateChatActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.searchUserButton)
    ImageButton createChatSearchUserButton;

    @BindView(R.id.searchUserEditText)
    EditText createChatSearchUserEditText;

    @BindView(R.id.searchUserProgressBar)
    ProgressBar createChatSearchUserProgressBar;

    @BindView(R.id.searchResultRecyclerView)
    RecyclerView createChatSearchResultRecyclerView;

    @BindView(R.id.membersRecyclerView)
    RecyclerView createChatMembersRecyclerView;

    @BindView(R.id.continueFloatingButton)
    FloatingActionButton continueFloatingButton;

    private List<User> mSearchUsers;
    private List<User> mMemberUsers;

    UsersListAdapter mMembersAdapter;
    UsersListAdapter mSearchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chat);

        unbinder = ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        LinearLayoutManager layoutManagerSearch = new LinearLayoutManager(this);
        createChatSearchResultRecyclerView.setHasFixedSize(true);
        createChatSearchResultRecyclerView.setLayoutManager(layoutManagerSearch);

        LinearLayoutManager layoutManagerMembers = new LinearLayoutManager(this);
        createChatMembersRecyclerView.setHasFixedSize(true);
        createChatMembersRecyclerView.setLayoutManager(layoutManagerMembers);

        mMemberUsers = new ArrayList<>();
        mSearchUsers = new ArrayList<>();

        mSearchAdapter = new UsersListAdapter(this, mSearchUsers, true, false);
        mMembersAdapter = new UsersListAdapter(this, mMemberUsers, false, true);

        addDisposable(mSearchAdapter.getEventObservable().subscribe(this::onUserEvent));
        addDisposable(mMembersAdapter.getEventObservable().subscribe(this::onUserEvent));

        createChatMembersRecyclerView.setAdapter(mMembersAdapter);
        createChatSearchResultRecyclerView.setAdapter(mSearchAdapter);

        createChatSearchUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createChatSearchUserButton.setVisibility(View.GONE);
                createChatSearchUserProgressBar.setVisibility(View.VISIBLE);
                final String username = createChatSearchUserEditText.getText().toString();
                searchUserBackgroundTask(username);
            }
        });

        continueFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createChat(mMemberUsers);
            }
        });
    }

    private void searchUserBackgroundTask(final String username) {
        Maybe<User> task = Maybe.fromCallable(() -> {
            if (XMPPSession.getInstance().userExists(username)) {
                return new User(XMPPUtils.fromUserNameToJID(username));
            } else {
                return null;
            }
        });

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    createChatSearchUserProgressBar.setVisibility(View.GONE);
                    createChatSearchUserButton.setVisibility(View.VISIBLE);

                    mSearchUsers.clear();
                    mSearchUsers.add(user);
                    mSearchAdapter.notifyDataSetChanged();
                }, error -> {
                    createChatSearchUserProgressBar.setVisibility(View.GONE);
                    createChatSearchUserButton.setVisibility(View.VISIBLE);
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.error), Toast.LENGTH_SHORT).show();
                }, () -> {
                    //user not found
                    createChatSearchUserProgressBar.setVisibility(View.GONE);
                    createChatSearchUserButton.setVisibility(View.VISIBLE);
                    showInviteDialog(username);
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;
        }

        return true;
    }

    private void showInviteDialog(String user) {
        final String message = String.format(Locale.getDefault(), getString(R.string.user_doesnt_exist), user);

        CreateChatActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(CreateChatActivity.this)
                        .setTitle(getString(R.string.invite_to_mangosta))
                        .setMessage(message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // yes
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // no
                            }
                        })
                        .show();
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            }
        });
    }

    private void onUserEvent(UserEvent userEvent) {
        User user = userEvent.getUser();

        switch (userEvent.getType()) {

            case ADD_USER:
                if (!userInList(user, mMemberUsers)) {
                    mMemberUsers.add(user);
                    mMembersAdapter.notifyDataSetChanged();
                    mSearchUsers.clear();
                    mSearchAdapter.notifyDataSetChanged();
                }
                break;

            case REMOVE_USER:
                mMemberUsers.remove(user);
                mMembersAdapter.notifyDataSetChanged();
                break;
        }
    }

    private boolean userInList(User user, List<User> list) {
        boolean userFound = false;
        for (User anUser : list) {
            if (anUser.getJid().equals(user.getJid())) {
                userFound = true;
            }
        }
        return userFound;
    }

    private void createChat(List<User> memberUsers) {

        if (memberUsers.isEmpty()) {
            Toast.makeText(this, getString(R.string.add_people_to_create_chat), Toast.LENGTH_LONG).show();

        } else if (memberUsers.size() == 1) {   // 1 to 1 chat
            String chatJid = memberUsers.get(0).getJid();
            addDisposable(RoomManager.getInstance().createChat(chatJid)
                    .subscribe(() -> {},
                            error -> Log.d(TAG, "query error", error)));
            NavigateToChat.go(chatJid, XMPPUtils.fromJIDToUserName(chatJid), this);

        } else {    // muc or muc light
            showRoomNameDialog(memberUsers);
        }

    }

    private void showRoomNameDialog(final List<User> memberUsers) {

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText roomNameEditText = new EditText(this);
        roomNameEditText.setHint(R.string.enter_room_name_hint);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(10, 0, 10, 0);
        roomNameEditText.setLayoutParams(lp);

        linearLayout.addView(roomNameEditText);

        AlertDialog dialog = new AlertDialog.Builder(CreateChatActivity.this)
                .setTitle(getString(R.string.room_name))
                .setMessage(getString(R.string.enter_room_name))
                .setView(linearLayout)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String roomName = roomNameEditText.getText().toString();
                        RoomManager.createMUCLight(memberUsers, roomName, CreateChatActivity.this,
                                new RoomManagerCreationListener());
                    }
                })
                .show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));

    }


    private class RoomManagerCreationListener extends RoomManagerListener {

        public void onRoomCreated(String roomJid, String roomName) {
            NavigateToChat.go(roomJid, roomName, CreateChatActivity.this);
        }
    }
}
