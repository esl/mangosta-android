package inaka.com.mangosta.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
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
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ManageContactsActivity extends BaseActivity {
    private static final String TAG = ManageContactsActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.manageContactsSearchUserButton)
    ImageButton manageContactsSearchUserButton;

    @BindView(R.id.manageContactsSearchUserEditText)
    EditText manageContactsSearchUserEditText;

    @BindView(R.id.manageContactsSearchUserProgressBar)
    ProgressBar manageContactsSearchUserProgressBar;

    @BindView(R.id.manageContactsSearchResultRecyclerView)
    RecyclerView manageContactsSearchResultRecyclerView;

    @BindView(R.id.manageContactsUsersRecyclerView)
    RecyclerView manageContactsUsersRecyclerView;

    @BindView(R.id.manageContactsUsersRemoveAllContactsButton)
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

        unbinder = ButterKnife.bind(this);

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
        Single<Boolean> task = Single.fromCallable(() -> XMPPSession.getInstance().userExists(user));

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userExists -> {
                    if (userExists) {
                        obtainUser(user, true);
                    } else {
                        showNotFoundDialog(user);
                    }
                    if (manageContactsSearchUserButton != null && manageContactsSearchUserProgressBar != null) {
                        manageContactsSearchUserProgressBar.setVisibility(View.GONE);
                        manageContactsSearchUserButton.setVisibility(View.VISIBLE);
                    }
                }, error -> {
                    if (!Preferences.isTesting()) {
                        Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }

                    if (manageContactsSearchUserButton != null && manageContactsSearchUserProgressBar != null) {
                        manageContactsSearchUserProgressBar.setVisibility(View.GONE);
                        manageContactsSearchUserButton.setVisibility(View.VISIBLE);
                    }
                }));
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

        Completable task = Completable.fromCallable(() -> {
            RosterManager.getInstance().addContact(user);
            return null;
        });

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
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
                }, error -> {
                    if (progress != null) {
                        progress.dismiss();
                    }

                    if (!Preferences.isTesting()) {
                        Toast.makeText(ManageContactsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    }

                    Log.d(TAG, "addContact error", error);
                }));
    }

    private void removeContact(final User user) {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Completable task = Completable.fromCallable(() -> {
            RosterManager.getInstance().removeContact(user);
            return null;
        });

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
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
                        }, error -> {
                    if (progress != null) {
                        progress.dismiss();
                    }

                    if (!Preferences.isTesting()) {
                        Toast.makeText(ManageContactsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    Log.d(TAG, "removeContact error", error);
                }));
    }

    private void getContacts() {
        final ProgressDialog progress = ProgressDialog.show(ManageContactsActivity.this, getString(R.string.loading), null, true);

        Single<HashMap<Jid, Presence.Type>> task = Single.fromCallable(() -> {
            return RosterManager.getInstance().getContacts();
        });

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(buddies -> {
                    for (Map.Entry pair : buddies.entrySet()) {
                        obtainUser(XMPPUtils.fromJIDToUserName(pair.getKey().toString()), false);
                    }
                    if (progress != null) {
                        progress.dismiss();
                    }
                    if (buddies.size() > 0 && manageContactsUsersRemoveAllContactsButton != null) {
                        manageContactsUsersRemoveAllContactsButton.setVisibility(View.VISIBLE);
                    }
                }, error -> {
                    if (progress != null) {
                        progress.dismiss();
                    }

                    if (!Preferences.isTesting()) {
                        Toast.makeText(ManageContactsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    }

                    Log.d(TAG, "getContacts error", error);
                }));
    }

    private void removeAllContacts() {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Completable task = Completable.fromCallable(() -> {
            RosterManager.getInstance().removeAllContacts();
            return null;
        });

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    if (progress != null) {
                        progress.dismiss();
                    }
                    if (mContacts != null && mContactsAdapter != null && manageContactsUsersRemoveAllContactsButton != null) {
                        mContacts.clear();
                        mContactsAdapter.notifyDataSetChanged();
                        manageContactsUsersRemoveAllContactsButton.setVisibility(View.INVISIBLE);
                    }

                    new Event(Event.Type.CONTACTS_CHANGED).post();
                }, error -> {
                    if (progress != null) {
                        progress.dismiss();
                    }

                    if (!Preferences.isTesting()) {
                        Toast.makeText(ManageContactsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    }

                    Log.d(TAG, "removeAllContacts error", error);
                }));
    }

    @Override
    public void onBackPressed() {
        new Event(Event.Type.CONTACTS_CHANGED).post();
        super.onBackPressed();
    }

}
