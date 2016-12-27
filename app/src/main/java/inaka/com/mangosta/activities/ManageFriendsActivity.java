package inaka.com.mangosta.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.utils.UserEvent;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class ManageFriendsActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.manageFriendsSearchUserButton)
    ImageButton manageFriendsSearchUserButton;

    @Bind(R.id.manageFriendsSearchUserEditText)
    EditText manageFriendsSearchUserEditText;

    @Bind(R.id.manageFriendsSearchUserProgressBar)
    ProgressBar manageFriendsSearchUserProgressBar;

    @Bind(R.id.manageFriendsSearchResultRecyclerView)
    RecyclerView manageFriendsSearchResultRecyclerView;

    @Bind(R.id.manageFriendsUsersRecyclerView)
    RecyclerView manageFriendsUsersRecyclerView;

    @Bind(R.id.manageFriendsUsersUnfriendAllButton)
    Button manageFriendsUsersUnfriendAllButton;

    private List<User> mSearchUsers;
    private List<User> mFriends;

    UsersListAdapter mFriendsAdapter;
    UsersListAdapter mSearchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_friends);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        LinearLayoutManager layoutManagerSearch = new LinearLayoutManager(this);
        manageFriendsSearchResultRecyclerView.setHasFixedSize(true);
        manageFriendsSearchResultRecyclerView.setLayoutManager(layoutManagerSearch);

        LinearLayoutManager layoutManagerFriends = new LinearLayoutManager(this);
        manageFriendsUsersRecyclerView.setHasFixedSize(true);
        manageFriendsUsersRecyclerView.setLayoutManager(layoutManagerFriends);

        mFriends = new ArrayList<>();
        mSearchUsers = new ArrayList<>();

        mSearchAdapter = new UsersListAdapter(this, mSearchUsers, true, false);
        mFriendsAdapter = new UsersListAdapter(this, mFriends, false, true);

        manageFriendsUsersRecyclerView.setAdapter(mFriendsAdapter);
        manageFriendsSearchResultRecyclerView.setAdapter(mSearchAdapter);

        manageFriendsSearchUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageFriendsSearchUserButton.setVisibility(View.GONE);
                manageFriendsSearchUserProgressBar.setVisibility(View.VISIBLE);
                String user = manageFriendsSearchUserEditText.getText().toString();
                searchUserBackgroundTask(user);
            }
        });

        manageFriendsUsersUnfriendAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unfriendAll();
            }
        });
    }

    private void searchUserBackgroundTask(final String user) {
        Tasks.executeInBackground(ManageFriendsActivity.this, new BackgroundWork<Boolean>() {
            @Override
            public Boolean doInBackground() throws Exception {
                return XMPPSession.getInstance().userExists(user);
            }
        }, new Completion<Boolean>() {
            @Override
            public void onSuccess(Context context, Boolean userExists) {
                if (userExists) {
                    obtainUser(user, true);
                } else {
                    showNotFoundDialog(user);
                }
                if (manageFriendsSearchUserButton != null && manageFriendsSearchUserProgressBar != null) {
                    manageFriendsSearchUserProgressBar.setVisibility(View.GONE);
                    manageFriendsSearchUserButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                if (!Preferences.isTesting()) {
                    Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show();
                }

                if (manageFriendsSearchUserButton != null && manageFriendsSearchUserProgressBar != null) {
                    manageFriendsSearchUserProgressBar.setVisibility(View.GONE);
                    manageFriendsSearchUserButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showNotFoundDialog(String user) {
        String message = String.format(Locale.getDefault(), getString(R.string.user_doesnt_exist), user);
        AlertDialog dialog = new AlertDialog.Builder(ManageFriendsActivity.this)
                .setTitle(getString(R.string.user_not_found))
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // yes
                    }
                })
                .show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
    }

    private void obtainUser(final String userName, final boolean isSearch) {
        User user = new User();
        user.setLogin(userName);

        if (mSearchUsers != null && mSearchAdapter != null) {
            if (isSearch) {
                mSearchUsers.clear();
                mSearchUsers.add(user);
                mSearchAdapter.notifyDataSetChanged();
            } else {
                mFriends.add(user);
                mFriendsAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        mFriends.clear();
        getFriends();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                EventBus.getDefault().post(new Event(Event.Type.FRIENDS_CHANGED));
                finish();
                break;
        }

        return true;
    }

    // receives events from EventBus
    public void onEvent(UserEvent userEvent) {
        User user = userEvent.getUser();

        switch (userEvent.getType()) {

            case ADD_USER:
                if (!userInList(user, mFriends)) {
                    addFriend(user);
                }
                break;

            case REMOVE_USER:
                unfriend(user);
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
                        mFriendsAdapter.notifyDataSetChanged();
                    }
                });
                break;
        }
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

    private void addFriend(final User user) {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                RosterManager.getInstance().addToBuddies(user);
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (manageFriendsUsersUnfriendAllButton != null) {
                    mFriends.add(user);
                    mFriendsAdapter.notifyDataSetChanged();
                    manageFriendsUsersUnfriendAllButton.setVisibility(View.VISIBLE);
                    mSearchUsers.clear();
                    mSearchAdapter.notifyDataSetChanged();
                }

                EventBus.getDefault().post(new Event(Event.Type.FRIENDS_CHANGED));
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(ManageFriendsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

    private void unfriend(final User user) {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                RosterManager.getInstance().removeFromBuddies(user);
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (mFriends != null && mFriendsAdapter != null && manageFriendsUsersUnfriendAllButton != null) {
                    mFriends.remove(user);
                    mFriendsAdapter.notifyDataSetChanged();
                    if (mFriends.size() == 0) {
                        manageFriendsUsersUnfriendAllButton.setVisibility(View.INVISIBLE);
                    }
                }

                EventBus.getDefault().post(new Event(Event.Type.FRIENDS_CHANGED));
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(ManageFriendsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

    private void getFriends() {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<HashMap<Jid, Presence.Type>>() {
            @Override
            public HashMap<Jid, Presence.Type> doInBackground() throws Exception {
                return RosterManager.getInstance().getBuddies();
            }
        }, new Completion<HashMap<Jid, Presence.Type>>() {
            @Override
            public void onSuccess(Context context, HashMap<Jid, Presence.Type> buddies) {
                if (buddies != null) {
                    for (Map.Entry pair : buddies.entrySet()) {
                        obtainUser(XMPPUtils.fromJIDToUserName(pair.getKey().toString()), false);
                    }
                    if (progress != null) {
                        progress.dismiss();
                    }
                    if (buddies.size() > 0 && manageFriendsUsersUnfriendAllButton != null) {
                        manageFriendsUsersUnfriendAllButton.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(ManageFriendsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

    private void unfriendAll() {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                RosterManager.getInstance().removeAllFriends();
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                if (progress != null) {
                    progress.dismiss();
                }
                if (mFriends != null && mFriendsAdapter != null && manageFriendsUsersUnfriendAllButton != null) {
                    mFriends.clear();
                    mFriendsAdapter.notifyDataSetChanged();
                    manageFriendsUsersUnfriendAllButton.setVisibility(View.INVISIBLE);
                }

                EventBus.getDefault().post(new Event(Event.Type.FRIENDS_CHANGED));
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(ManageFriendsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

    @Override
    public void onBackPressed() {
        EventBus.getDefault().post(new Event(Event.Type.FRIENDS_CHANGED));
        super.onBackPressed();
    }

}
