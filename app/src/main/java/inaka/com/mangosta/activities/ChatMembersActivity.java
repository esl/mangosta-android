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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import inaka.com.mangosta.xmpp.muclight.MUCLightAffiliation;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLight;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLightManager;

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
    public static String ROOM_TYPE_PARAMETER = "roomType";

    private String mRoomJid;
    private int mRoomType;

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
        mRoomType = bundle.getInt(ROOM_TYPE_PARAMETER);

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
                if (mRoomType == Chat.TYPE_MUC) {
                    loadMUCMembers(roomJid);
                } else if (mRoomType == Chat.TYPE_MUC_LIGHT) {
                    loadMUCLightMembers(roomJid);
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

    private void loadMUCLightMembers(String roomJid) throws XmppStringprepException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException {
        MultiUserChatLightManager multiUserChatLightManager = MultiUserChatLightManager.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        MultiUserChatLight multiUserChatLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(roomJid).asEntityBareJidIfPossible());

        HashMap<Jid, MUCLightAffiliation> occupants = multiUserChatLight.getAffiliations();

        for (Map.Entry<Jid, MUCLightAffiliation> pair : occupants.entrySet()) {
            Jid jid = pair.getKey();
            if (jid != null) {
                obtainUser(XMPPUtils.fromJIDToUserName(jid.toString()));
            }
        }
    }

    private void loadMUCMembers(String roomJid) throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, XmppStringprepException {
        ServiceDiscoveryManager serviceDiscoveryManager =
                ServiceDiscoveryManager.getInstanceFor(XMPPSession.getInstance().getXMPPConnection());
        List<DiscoverItems.Item> items = serviceDiscoveryManager.discoverItems(JidCreate.from(roomJid)).getItems();
        for (DiscoverItems.Item item : items) {
            obtainUser(item.getEntityID().toString().split("/")[1]);
        }
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
        loadMembers(mRoomJid);
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
