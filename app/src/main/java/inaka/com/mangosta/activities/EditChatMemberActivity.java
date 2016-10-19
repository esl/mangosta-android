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
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.utils.UserEvent;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import inaka.com.mangosta.xmpp.muclight.MUCLightAffiliation;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLight;
import inaka.com.mangosta.xmpp.muclight.MultiUserChatLightManager;


public class EditChatMemberActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.createChatSearchUserButton)
    ImageButton createChatSearchUserButton;

    @Bind(R.id.createChatSearchUserEditText)
    EditText createChatSearchUserEditText;

    @Bind(R.id.createChatSearchUserProgressBar)
    ProgressBar createChatSearchUserProgressBar;

    @Bind(R.id.createChatSearchResultRecyclerView)
    RecyclerView createChatSearchResultRecyclerView;

    @Bind(R.id.createChatMembersRecyclerView)
    RecyclerView createChatMembersRecyclerView;

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
        mChat = getRealm().where(Chat.class).equalTo("jid", mChatJID).findFirst();

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

        createChatMembersRecyclerView.setAdapter(mMembersAdapter);
        createChatSearchResultRecyclerView.setAdapter(mSearchAdapter);

        createChatSearchUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createChatSearchUserButton.setVisibility(View.GONE);
                createChatSearchUserProgressBar.setVisibility(View.VISIBLE);
                String user = createChatSearchUserEditText.getText().toString();
                searchUserBackgroundTask(user);
            }
        });

        continueFloatingButton.setVisibility(View.INVISIBLE);

        getChatMembers();
    }

    private void searchUserBackgroundTask(final String user) {
        Tasks.executeInBackground(EditChatMemberActivity.this, new BackgroundWork<Boolean>() {
            @Override
            public Boolean doInBackground() throws Exception {
                return XMPPUtils.userExists(user);
            }
        }, new Completion<Boolean>() {
            @Override
            public void onSuccess(Context context, Boolean userExists) {
                if (userExists) {
                    searchObtainUser(user);
                } else {
                    showInviteDialog(user);
                }
                createChatSearchUserProgressBar.setVisibility(View.GONE);
                createChatSearchUserButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Context context, Exception e) {
                Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show();
                createChatSearchUserProgressBar.setVisibility(View.GONE);
                createChatSearchUserButton.setVisibility(View.VISIBLE);
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

    private void getChatMembers() {

        List<Jid> jids = new ArrayList<>();

        switch (mChat.getType()) {

            case Chat.TYPE_MUC_LIGHT:
                jids = getMUCLightMembers();
                break;

            case Chat.TYPE_MUC:
                jids = getMUCMembers();
                break;
        }

        for (Jid jid : jids) {
            membersObtainUser(XMPPUtils.fromJIDToUserName(jid.toString()));
        }

    }

    private List<Jid> getMUCLightMembers() {
        List<Jid> jids = new ArrayList<>();
        MultiUserChatLightManager multiUserChatLightManager = XMPPSession.getInstance().getMUCLightManager();
        try {
            MultiUserChatLight mucLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
            jids.addAll(mucLight.getAffiliations().keySet());
        } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        }
        return jids;
    }

    private List<Jid> getMUCMembers() {
        List<Jid> jids = new ArrayList<>();

        MultiUserChatManager multiUserChatManager = XMPPSession.getInstance().getMUCManager();
        try {
            MultiUserChat muc = multiUserChatManager.getMultiUserChat(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
            List<EntityFullJid> occupants = muc.getOccupants();

            for (Jid jid : occupants) {
                String userName = jid.toString().split("/")[1];
                jids.add(JidCreate.from(XMPPUtils.fromUserNameToJID(userName)));
            }

        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        return jids;
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

        new AlertDialog.Builder(EditChatMemberActivity.this)
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
        switch (mChat.getType()) {

            case Chat.TYPE_MUC:
                removeFromMUC(user);
                break;

            case Chat.TYPE_MUC_LIGHT:
                removeFromMUCLight(user);
                break;
        }
    }

    private void removeFromMUCLight(User user) {
        MultiUserChatLightManager multiUserChatLightManager = XMPPSession.getInstance().getMUCLightManager();
        try {
            MultiUserChatLight mucLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());

            Jid jid = JidCreate.from(XMPPUtils.fromUserNameToJID(user.getLogin()));

            HashMap<Jid, MUCLightAffiliation> affiliations = new HashMap<>();
            affiliations.put(jid, MUCLightAffiliation.none);

            mucLight.changeAffiliations(affiliations);
        } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        }
    }

    private void removeFromMUC(User user) {
        MultiUserChatManager multiUserChatManager = XMPPSession.getInstance().getMUCManager();
        try {
            MultiUserChat muc = multiUserChatManager.getMultiUserChat(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
            muc.kickParticipant(Resourcepart.from(user.getLogin()), "Kicked");
        } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
            e.printStackTrace();
        }
    }

    private void addUserToChat(User user) {
        switch (mChat.getType()) {

            case Chat.TYPE_MUC:
                inviteToMUC(user);
                break;

            case Chat.TYPE_MUC_LIGHT:
                addToMUCLight(user);
                break;
        }
    }

    private void addToMUCLight(User user) {
        MultiUserChatLightManager multiUserChatLightManager = XMPPSession.getInstance().getMUCLightManager();
        try {
            MultiUserChatLight mucLight = multiUserChatLightManager.getMultiUserChatLight(JidCreate.from(mChatJID).asEntityBareJidIfPossible());

            Jid jid = JidCreate.from(XMPPUtils.fromUserNameToJID(user.getLogin()));

            HashMap<Jid, MUCLightAffiliation> affiliations = new HashMap<>();
            affiliations.put(jid, MUCLightAffiliation.member);

            mucLight.changeAffiliations(affiliations);
        } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        }
    }

    private void inviteToMUC(User user) {
        MultiUserChatManager multiUserChatManager = XMPPSession.getInstance().getMUCManager();
        try {
            MultiUserChat muc = multiUserChatManager.getMultiUserChat(JidCreate.from(mChatJID).asEntityBareJidIfPossible());
            muc.invite(XMPPUtils.fromUserNameToJID(user.getLogin()), "I invite you to my chat.");
        } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException e) {
            e.printStackTrace();
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

}
