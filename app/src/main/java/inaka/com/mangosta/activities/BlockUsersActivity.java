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

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.utils.UserEvent;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import inaka.com.mangosta.xmpp.blocking.BlockingCommandManager;

public class BlockUsersActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.blockSearchUserButton)
    ImageButton blockSearchUserButton;

    @Bind(R.id.blockSearchUserEditText)
    EditText blockSearchUserEditText;

    @Bind(R.id.blockSearchUserProgressBar)
    ProgressBar blockSearchUserProgressBar;

    @Bind(R.id.blockSearchResultRecyclerView)
    RecyclerView blockSearchResultRecyclerView;

    @Bind(R.id.blockedUsersRecyclerView)
    RecyclerView blockedUsersRecyclerView;

    @Bind(R.id.blockedUsersUnblockAllButton)
    Button blockedUsersUnblockAllButton;

    private List<User> mSearchUsers;
    private List<User> mBlockedUsers;

    UsersListAdapter mBlockedAdapter;
    UsersListAdapter mSearchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_users);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        LinearLayoutManager layoutManagerSearch = new LinearLayoutManager(this);
        blockSearchResultRecyclerView.setHasFixedSize(true);
        blockSearchResultRecyclerView.setLayoutManager(layoutManagerSearch);

        LinearLayoutManager layoutManagerBlocked = new LinearLayoutManager(this);
        blockedUsersRecyclerView.setHasFixedSize(true);
        blockedUsersRecyclerView.setLayoutManager(layoutManagerBlocked);

        mBlockedUsers = new ArrayList<>();
        mSearchUsers = new ArrayList<>();

        mSearchAdapter = new UsersListAdapter(this, mSearchUsers, true, false);
        mBlockedAdapter = new UsersListAdapter(this, mBlockedUsers, false, true);

        blockedUsersRecyclerView.setAdapter(mBlockedAdapter);
        blockSearchResultRecyclerView.setAdapter(mSearchAdapter);

        blockSearchUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blockSearchUserButton.setVisibility(View.GONE);
                blockSearchUserProgressBar.setVisibility(View.VISIBLE);
                String user = blockSearchUserEditText.getText().toString();
                searchUserBackgroundTask(user);
            }
        });

        blockedUsersUnblockAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unblockAll();
            }
        });
    }

    private void searchUserBackgroundTask(final String user) {
        Tasks.executeInBackground(BlockUsersActivity.this, new BackgroundWork<Boolean>() {
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
                if (blockSearchUserButton != null && blockSearchUserProgressBar != null) {
                    blockSearchUserProgressBar.setVisibility(View.GONE);
                    blockSearchUserButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show();
                if (blockSearchUserButton != null && blockSearchUserProgressBar != null) {
                    blockSearchUserProgressBar.setVisibility(View.GONE);
                    blockSearchUserButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showNotFoundDialog(String user) {
        String message = String.format(Locale.getDefault(), getString(R.string.user_doesnt_exist), user);
        AlertDialog dialog = new AlertDialog.Builder(BlockUsersActivity.this)
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
                mBlockedUsers.add(user);
                mBlockedAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        mBlockedUsers.clear();
        getBlockedUsers();
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

    // receives events from EventBus
    public void onEvent(UserEvent userEvent) {
        User user = userEvent.getUser();

        switch (userEvent.getType()) {

            case ADD_USER:
                if (!userInList(user, mBlockedUsers)) {
                    blockUser(user);
                }
                break;

            case REMOVE_USER:
                unblockUser(user);
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

    private void blockUser(final User user) {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                Jid jid = JidCreate.from(XMPPUtils.fromUserNameToJID(user.getLogin()));
                List<Jid> jids = new ArrayList<>();
                jids.add(jid);
                XMPPSession.getInstance().blockContacts(jids);
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                if (progress != null) {
                    progress.dismiss();
                }
                if (blockedUsersUnblockAllButton != null) {
                    mBlockedUsers.add(user);
                    mBlockedAdapter.notifyDataSetChanged();
                    blockedUsersUnblockAllButton.setVisibility(View.VISIBLE);
                    mSearchUsers.clear();
                    mSearchAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(BlockUsersActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

    private void unblockUser(final User user) {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                Jid jid = JidCreate.from(XMPPUtils.fromUserNameToJID(user.getLogin()));
                List<Jid> jids = new ArrayList<>();
                jids.add(jid);
                XMPPSession.getInstance().unblockContacts(jids);
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                if (progress != null) {
                    progress.dismiss();
                }
                if (mBlockedUsers != null && mBlockedAdapter != null && blockedUsersUnblockAllButton != null) {
                    mBlockedUsers.remove(user);
                    mBlockedAdapter.notifyDataSetChanged();
                    if (mBlockedUsers.size() == 0) {
                        blockedUsersUnblockAllButton.setVisibility(View.INVISIBLE);
                    }
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(BlockUsersActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

    private void getBlockedUsers() {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<List<Jid>>() {
            @Override
            public List<Jid> doInBackground() throws Exception {
                return XMPPSession.getInstance().getBlockList();
            }
        }, new Completion<List<Jid>>() {
            @Override
            public void onSuccess(Context context, List<Jid> jids) {
                if (jids != null) {
                    for (Jid jid : jids) {
                        obtainUser(XMPPUtils.fromJIDToUserName(jid.toString()), false);
                    }
                    if (progress != null) {
                        progress.dismiss();
                    }
                    if (jids.size() > 0 && blockedUsersUnblockAllButton != null) {
                        blockedUsersUnblockAllButton.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(BlockUsersActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

    private void unblockAll() {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                XMPPSession.getInstance().unblockAll();
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                if (progress != null) {
                    progress.dismiss();
                }
                if (mBlockedUsers != null && mBlockedAdapter != null && blockedUsersUnblockAllButton != null) {
                    mBlockedUsers.clear();
                    mBlockedAdapter.notifyDataSetChanged();
                    blockedUsersUnblockAllButton.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(BlockUsersActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

}
