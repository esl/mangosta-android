package inaka.com.mangosta.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.UserEvent;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class EditChatMemberActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.searchUserButton)
    ImageButton searchUserButton;

    @Bind(R.id.searchUserEditText)
    EditText searchUserEditText;

    @Bind(R.id.searchUserProgressBar)
    ProgressBar searchUserProgressBar;

    @Bind(R.id.searchResultRecyclerView)
    RecyclerView searchResultRecyclerView;

    @Bind(R.id.membersRecyclerView)
    RecyclerView membersRecyclerView;

    @Bind(R.id.continueFloatingButton)
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

        ButterKnife.bind(this);

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
        Tasks.executeInBackground(EditChatMemberActivity.this, new BackgroundWork<Boolean>() {
            @Override
            public Boolean doInBackground() throws Exception {
                return XMPPSession.getInstance().userExists(user);
            }
        }, new Completion<Boolean>() {
            @Override
            public void onSuccess(Context context, Boolean userExists) {
                if (userExists) {
                    searchObtainUser(user);
                } else {
                    showInviteDialog(user);
                }
                searchUserProgressBar.setVisibility(View.GONE);
                searchUserButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Context context, Exception e) {
                Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show();
                searchUserProgressBar.setVisibility(View.GONE);
                searchUserButton.setVisibility(View.VISIBLE);
            }
        });
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
