package inaka.com.mangosta.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class ChatMembersActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.editChatMembersFloatingButton)
    FloatingActionButton editChatMembersFloatingButton;

    @Bind(R.id.chatMembersRecyclerView)
    RecyclerView chatMembersRecyclerView;

    @Bind(R.id.progressLoading)
    ProgressBar progressLoading;

    List<User> mMembers;
    UsersListAdapter mMembersAdapter;

    public static String ROOM_JID_PARAMETER = "roomJid";
    public static String IS_ADMIN_PARAMETER = "isAdmin";

    private String mRoomJid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_members);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        LinearLayoutManager layoutManagerMembers = new LinearLayoutManager(this);
        chatMembersRecyclerView.setHasFixedSize(true);
        chatMembersRecyclerView.setLayoutManager(layoutManagerMembers);

        mMembers = new ArrayList<>();
        mMembersAdapter = new UsersListAdapter(this, mMembers, false, false);

        chatMembersRecyclerView.setAdapter(mMembersAdapter);

        Bundle bundle = getIntent().getExtras();

        mRoomJid = bundle.getString(ROOM_JID_PARAMETER);
        boolean isAdmin = bundle.getBoolean(IS_ADMIN_PARAMETER);

        editChatMembersFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatMembersActivity.this, EditChatMemberActivity.class);
                intent.putExtra(EditChatMemberActivity.CHAT_JID_PARAMETER, mRoomJid);
                ChatMembersActivity.this.startActivity(intent);
            }
        });

        if (!isAdmin) {
            editChatMembersFloatingButton.setVisibility(View.INVISIBLE);
        }

    }

    private void loadMembers(final String roomJid) {
        mMembers.clear();
        progressLoading.setVisibility(View.VISIBLE);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                List<String> jids = RoomManager.getInstance(null).loadMUCLightMembers(roomJid);
                for (String jid : jids) {
                    obtainUser(XMPPUtils.fromJIDToUserName(jid));
                }
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
            }

            @Override
            public void onError(Context context, Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void obtainUser(final String userName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                User user = new User();
                user.setLogin(userName);
                mMembers.add(user);
                mMembersAdapter.notifyDataSetChanged();

                if (progressLoading != null && progressLoading.getVisibility() == View.VISIBLE) {
                    progressLoading.setVisibility(View.GONE);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Preferences.isTesting() && !RealmManager.isTesting()) {
            progressLoading.setVisibility(View.GONE);
        } else {
            loadMembers(mRoomJid);
        }
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

}
