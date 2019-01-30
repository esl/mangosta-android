package inaka.com.mangosta.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.models.UserEvent;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class EditChatMemberActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.searchUserButton)
    ImageButton searchUserButton;

    @BindView(R.id.searchUserEditText)
    EditText searchUserEditText;

    @BindView(R.id.searchUserProgressBar)
    ProgressBar searchUserProgressBar;

    @BindView(R.id.searchResultRecyclerView)
    RecyclerView searchResultRecyclerView;

    @BindView(R.id.membersRecyclerView)
    RecyclerView membersRecyclerView;

    @BindView(R.id.continueFloatingButton)
    FloatingActionButton continueFloatingButton;

    private List<User> mSearchUsers;
    private List<User> mMemberUsers;

    UsersListAdapter mMembersAdapter;
    UsersListAdapter mSearchAdapter;

    public static String CHAT_JID_PARAMETER = "chatJid";

    private String mChatJID;
    Chat mChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chat);

        unbinder = ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        mChatJID = getIntent().getStringExtra(CHAT_JID_PARAMETER);
        mChat = RealmManager.getInstance().getChatFromRealm(getRealm(), mChatJID);

        LinearLayoutManager layoutManagerSearch = new LinearLayoutManager(this);
        searchResultRecyclerView.setHasFixedSize(true);
        searchResultRecyclerView.setLayoutManager(layoutManagerSearch);

        LinearLayoutManager layoutManagerMembers = new LinearLayoutManager(this);
        membersRecyclerView.setHasFixedSize(true);
        membersRecyclerView.setLayoutManager(layoutManagerMembers);

        mMemberUsers = new ArrayList<>();
        mSearchUsers = new ArrayList<>();

        mSearchAdapter = new UsersListAdapter(this, mSearchUsers, true, false);
        mMembersAdapter = new UsersListAdapter(this, mMemberUsers, false, true);

        membersRecyclerView.setAdapter(mMembersAdapter);
        searchResultRecyclerView.setAdapter(mSearchAdapter);

        searchUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchUserButton.setVisibility(View.GONE);
                searchUserProgressBar.setVisibility(View.VISIBLE);
                String user = searchUserEditText.getText().toString();
                searchUserBackgroundTask(user);
            }
        });

        continueFloatingButton.setVisibility(View.INVISIBLE);

        try {
            getChatMembers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchUserBackgroundTask(final String user) {
        Single<Boolean> task = Single.fromCallable(() -> XMPPSession.getInstance().userExists(user));

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userExists -> {
                    if (userExists) {
                        searchObtainUser(user);
                    } else {
                        showInviteDialog(user);
                    }
                    searchUserProgressBar.setVisibility(View.GONE);
                    searchUserButton.setVisibility(View.VISIBLE);
                }, error -> {
                    Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                    searchUserProgressBar.setVisibility(View.GONE);
                    searchUserButton.setVisibility(View.VISIBLE);
                }));
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

    private void getChatMembers()
            throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException, XmppStringprepException {
        List<String> jids = RoomManager.getInstance(null).loadMUCLightMembers(mChatJID);
        for (String jid : jids) {
            membersObtainUser(XMPPUtils.fromJIDToUserName(jid));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    private void showInviteDialog(String user) {
        String message = String.format(Locale.getDefault(), getString(R.string.user_doesnt_exist), user);

        AlertDialog dialog = new AlertDialog.Builder(EditChatMemberActivity.this)
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

    private void searchObtainUser(final String userName) {
        User user = new User();
        user.setLogin(userName);
        mSearchUsers.add(user);
        mSearchAdapter.notifyDataSetChanged();
    }

    private void membersObtainUser(final String userName) {
        User user = new User();
        user.setLogin(userName);
        mMemberUsers.add(user);
        mMembersAdapter.notifyDataSetChanged();
    }

    // receives events from EventBus
    public void onEvent(UserEvent userEvent) {
        User user = userEvent.getUser();

        switch (userEvent.getType()) {

            case ADD_USER:
                if (!userInList(user, mMemberUsers)) {
                    mMemberUsers.add(user);
                    mMembersAdapter.notifyDataSetChanged();
                    addUserToChat(user);
                    mSearchUsers.clear();
                    mSearchAdapter.notifyDataSetChanged();
                }
                break;

            case REMOVE_USER:
                mMemberUsers.remove(user);
                mMembersAdapter.notifyDataSetChanged();
                removeUserFromChat(user);
                break;
        }
    }

    @Override
    public void onEvent(Event event) {
        super.onEvent(event);
        switch (event.getType()) {
            case PRESENCE_RECEIVED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSearchAdapter.notifyDataSetChanged();
                        mMembersAdapter.notifyDataSetChanged();
                    }
                });
                break;
        }
    }

    private void removeUserFromChat(User user) {
        RoomManager.getInstance(null).removeFromMUCLight(user, mChatJID);
    }

    private void addUserToChat(User user) {
        RoomManager.getInstance(null).addToMUCLight(user, mChatJID);
    }

    private boolean userInList(User user, List<User> list) {
        boolean userFound = false;
        for (User anUser : list) {
            if (anUser.getLogin().equals(user.getLogin())) {
                userFound = true;
            }
        }
        return userFound;
    }

}
