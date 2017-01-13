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
import inaka.com.mangosta.models.UserEvent;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class ManageContactsActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.manageContactsSearchUserButton)
    ImageButton manageContactsSearchUserButton;

    @Bind(R.id.manageContactsSearchUserEditText)
    EditText manageContactsSearchUserEditText;

    @Bind(R.id.manageContactsSearchUserProgressBar)
    ProgressBar manageContactsSearchUserProgressBar;

    @Bind(R.id.manageContactsSearchResultRecyclerView)
    RecyclerView manageContactsSearchResultRecyclerView;

    @Bind(R.id.manageContactsUsersRecyclerView)
    RecyclerView manageContactsUsersRecyclerView;

    @Bind(R.id.manageContactsUsersRemoveAllContactsButton)
    Button manageContactsUsersRemoveAllContactsButton;

    private List<User> mSearchUsers;
    private List<User> mContacts;

    UsersListAdapter mContactsAdapter;
    UsersListAdapter mSearchAdapter;

    private static final Object LOCK_1 = new Object() {
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_contacts);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        LinearLayoutManager layoutManagerSearch = new LinearLayoutManager(this);
        manageContactsSearchResultRecyclerView.setHasFixedSize(true);
        manageContactsSearchResultRecyclerView.setLayoutManager(layoutManagerSearch);

        LinearLayoutManager layoutManagerContacts = new LinearLayoutManager(this);
        manageContactsUsersRecyclerView.setHasFixedSize(true);
        manageContactsUsersRecyclerView.setLayoutManager(layoutManagerContacts);

        mContacts = new ArrayList<>();
        mSearchUsers = new ArrayList<>();

        mSearchAdapter = new UsersListAdapter(this, mSearchUsers, true, false);
        mContactsAdapter = new UsersListAdapter(this, mContacts, false, true);

        manageContactsUsersRecyclerView.setAdapter(mContactsAdapter);
        manageContactsSearchResultRecyclerView.setAdapter(mSearchAdapter);

        manageContactsSearchUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageContactsSearchUserButton.setVisibility(View.GONE);
                manageContactsSearchUserProgressBar.setVisibility(View.VISIBLE);
                String user = manageContactsSearchUserEditText.getText().toString();
                searchUserBackgroundTask(user);
            }
        });

        manageContactsUsersRemoveAllContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeAllContacts();
            }
        });
    }

    private void searchUserBackgroundTask(final String user) {
        Tasks.executeInBackground(ManageContactsActivity.this, new BackgroundWork<Boolean>() {
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
                if (manageContactsSearchUserButton != null && manageContactsSearchUserProgressBar != null) {
                    manageContactsSearchUserProgressBar.setVisibility(View.GONE);
                    manageContactsSearchUserButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                if (!Preferences.isTesting()) {
                    Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show();
                }

                if (manageContactsSearchUserButton != null && manageContactsSearchUserProgressBar != null) {
                    manageContactsSearchUserProgressBar.setVisibility(View.GONE);
                    manageContactsSearchUserButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showNotFoundDialog(String user) {
        String message = String.format(Locale.getDefault(), getString(R.string.user_doesnt_exist), user);
        AlertDialog dialog = new AlertDialog.Builder(ManageContactsActivity.this)
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
                mContacts.add(user);
                mContactsAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        mContacts.clear();
        getContacts();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                new Event(Event.Type.CONTACTS_CHANGED).post();
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
                if (!userInList(user, mContacts)) {
                    addContact(user);
                }
                break;

            case REMOVE_USER:
                removeContact(user);
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
                        mContactsAdapter.notifyDataSetChanged();
                    }
                });
                break;

            case ROSTER_CHANGED:
                synchronized (LOCK_1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mContacts.clear();
                            getContacts();
                        }
                    });
                }
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

    private void addContact(final User user) {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                RosterManager.getInstance().addContact(user);
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (manageContactsUsersRemoveAllContactsButton != null) {
                    mContacts.add(user);
                    mContactsAdapter.notifyDataSetChanged();
                    manageContactsUsersRemoveAllContactsButton.setVisibility(View.VISIBLE);
                    mSearchUsers.clear();
                    mSearchAdapter.notifyDataSetChanged();
                }

                new Event(Event.Type.CONTACTS_CHANGED).post();
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(ManageContactsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

    private void removeContact(final User user) {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                RosterManager.getInstance().removeContact(user);
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (mContacts != null && mContactsAdapter != null && manageContactsUsersRemoveAllContactsButton != null) {
                    mContacts.remove(user);
                    mContactsAdapter.notifyDataSetChanged();
                    if (mContacts.size() == 0) {
                        manageContactsUsersRemoveAllContactsButton.setVisibility(View.INVISIBLE);
                    }
                }

                new Event(Event.Type.CONTACTS_CHANGED).post();
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(ManageContactsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

    private void getContacts() {

        final ProgressDialog progress = ProgressDialog.show(ManageContactsActivity.this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(ManageContactsActivity.this, new BackgroundWork<HashMap<Jid, Presence.Type>>() {
            @Override
            public HashMap<Jid, Presence.Type> doInBackground() throws Exception {
                return RosterManager.getInstance().getContacts();
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
                    if (buddies.size() > 0 && manageContactsUsersRemoveAllContactsButton != null) {
                        manageContactsUsersRemoveAllContactsButton.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(ManageContactsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });
    }

    private void removeAllContacts() {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                RosterManager.getInstance().removeAllContacts();
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                if (progress != null) {
                    progress.dismiss();
                }
                if (mContacts != null && mContactsAdapter != null && manageContactsUsersRemoveAllContactsButton != null) {
                    mContacts.clear();
                    mContactsAdapter.notifyDataSetChanged();
                    manageContactsUsersRemoveAllContactsButton.setVisibility(View.INVISIBLE);
                }

                new Event(Event.Type.CONTACTS_CHANGED).post();
            }

            @Override
            public void onError(Context context, Exception e) {
                if (progress != null) {
                    progress.dismiss();
                }

                if (!Preferences.isTesting()) {
                    Toast.makeText(ManageContactsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }

                e.printStackTrace();
            }
        });

    }

    @Override
    public void onBackPressed() {
        new Event(Event.Type.CONTACTS_CHANGED).post();
        super.onBackPressed();
    }

}
