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

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.models.UserEvent;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BlockUsersActivity extends BaseActivity {
    private static final String TAG = BlockUsersActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.blockSearchUserButton)
    ImageButton blockSearchUserButton;

    @BindView(R.id.blockSearchUserEditText)
    EditText blockSearchUserEditText;

    @BindView(R.id.blockSearchUserProgressBar)
    ProgressBar blockSearchUserProgressBar;

    @BindView(R.id.blockSearchResultRecyclerView)
    RecyclerView blockSearchResultRecyclerView;

    @BindView(R.id.blockedUsersRecyclerView)
    RecyclerView blockedUsersRecyclerView;

    @BindView(R.id.blockedUsersUnblockAllButton)
    Button blockedUsersUnblockAllButton;

    private List<User> mSearchUsers;
    private List<User> mBlockedUsers;

    UsersListAdapter mBlockedAdapter;
    UsersListAdapter mSearchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_users);

        unbinder = ButterKnife.bind(this);

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
                String username = blockSearchUserEditText.getText().toString();
                searchUserBackgroundTask(username);
            }
        });

        blockedUsersUnblockAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unblockAll();
            }
        });
    }

    private void searchUserBackgroundTask(final String username) {
        Single<Boolean> task = Single.fromCallable(() -> XMPPSession.getInstance().userExists(username));

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userExists -> {
                    if (userExists) {
                        obtainUser(username, true);
                    } else {
                        showNotFoundDialog(username);
                    }
                    if (blockSearchUserButton != null && blockSearchUserProgressBar != null) {
                        blockSearchUserProgressBar.setVisibility(View.GONE);
                        blockSearchUserButton.setVisibility(View.VISIBLE);
                    }
                }, error -> {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.error), Toast.LENGTH_SHORT).show();
                    if (blockSearchUserButton != null && blockSearchUserProgressBar != null) {
                        blockSearchUserProgressBar.setVisibility(View.GONE);
                        blockSearchUserButton.setVisibility(View.VISIBLE);
                    }
                }));
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
        User user = new User(XMPPUtils.fromUserNameToJID(userName));

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

    @Override
    public void onEvent(Event event) {
        super.onEvent(event);
        switch (event.getType()) {
            case PRESENCE_RECEIVED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSearchAdapter.notifyDataSetChanged();
                        mBlockedAdapter.notifyDataSetChanged();
                    }
                });
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

    private void blockUser(final User user) {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Completable task = Completable.fromCallable(() -> {
            Jid jid = JidCreate.from(user.getJid());
            List<Jid> jids = new ArrayList<>();
            jids.add(jid);
            XMPPSession.getInstance().blockContacts(jids);
            return null;
        });

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
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
                }, error -> {
                    if (progress != null) {
                        progress.dismiss();
                    }

                    if (!Preferences.isTesting()) {
                        Toast.makeText(BlockUsersActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    }

                    Log.d(TAG, "blockUser error", error);
                }));
    }

    private void unblockUser(final User user) {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Completable task = Completable.fromCallable(() -> {
            Jid jid = JidCreate.from(user.getJid());
            List<Jid> jids = new ArrayList<>();
            jids.add(jid);
            XMPPSession.getInstance().unblockContacts(jids);
            return null;
        });

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
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
                }, error -> {
                    if (progress != null) {
                        progress.dismiss();
                    }

                    if (!Preferences.isTesting()) {
                        Toast.makeText(BlockUsersActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    Log.d(TAG, "unblockUser error", error);

                }));
        }

    private void getBlockedUsers() {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Single<List<Jid>> task = Single.fromCallable(() -> {
            return XMPPSession.getInstance().getBlockList();
        });

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(jids -> {
                    for (Jid jid : jids) {
                        obtainUser(XMPPUtils.fromJIDToUserName(jid.toString()), false);
                    }
                    if (progress != null) {
                        progress.dismiss();
                    }
                    if (jids.size() > 0 && blockedUsersUnblockAllButton != null) {
                        blockedUsersUnblockAllButton.setVisibility(View.VISIBLE);
                    }
                }, error -> {
                    if (progress != null) {
                        progress.dismiss();
                    }

                    if (!Preferences.isTesting()) {
                        Toast.makeText(BlockUsersActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    }

                    Log.d(TAG, "getBlockedUsers error", error);
                }));
    }

    private void unblockAll() {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading), null, true);

        Completable task = Completable.fromCallable(() -> {
            XMPPSession.getInstance().unblockAll();
            return null;
        });

        addDisposable(task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    if (progress != null) {
                        progress.dismiss();
                    }
                    if (mBlockedUsers != null && mBlockedAdapter != null && blockedUsersUnblockAllButton != null) {
                        mBlockedUsers.clear();
                        mBlockedAdapter.notifyDataSetChanged();
                        blockedUsersUnblockAllButton.setVisibility(View.INVISIBLE);
                    }
                }, error -> {
                    if (progress != null) {
                        progress.dismiss();
                    }

                    if (!Preferences.isTesting()) {
                        Toast.makeText(BlockUsersActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    }

                    Log.d(TAG, "unblockAll error", error);
                }));
    }

}
