package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.ViewPagerMainMenuAdapter;
import inaka.com.mangosta.fragments.ChatsListsFragment;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.notifications.RosterNotifications;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class MainMenuActivity extends BaseActivity {

    @BindView(R.id.slidingTabStrip)
    TabLayout mSlidingTabStrip;

    @BindView(R.id.viewpagerMainMenu)
    ViewPager mViewpagerMainMenu;

    @BindView(R.id.createNewChatFloatingButton)
    FloatingActionButton createNewChatFloatingButton;

    @BindView(R.id.createNewBlogFloatingButton)
    FloatingActionButton createNewBlogFloatingButton;

    public boolean mRoomsLoaded = false;

    public static String NEW_BLOG_POST = "newBlogPost";
    public static String NEW_ROSTER_REQUEST = "newRosterRequest";
    public static String NEW_ROSTER_REQUEST_SENDER = "newRosterRequestSender";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        unbinder = ButterKnife.bind(this);

        String tabTitles[] = new String[]{
                getResources().getString(R.string.title_tab_chat),
                getResources().getString(R.string.title_tab_social)};

        mViewpagerMainMenu.setAdapter(new ViewPagerMainMenuAdapter(getSupportFragmentManager(), tabTitles));

        createNewChatFloatingButton.setIcon(R.mipmap.ic_action_create_new_chat_light);
        createNewBlogFloatingButton.setIcon(R.mipmap.ic_add_blog);

        createNewChatFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenuActivity.this, CreateChatActivity.class);
                MainMenuActivity.this.startActivity(intent);
            }
        });

        createNewBlogFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenuActivity.this, CreateBlogActivity.class);
                MainMenuActivity.this.startActivity(intent);
            }
        });

        manageCallFromBlogPostNotification();
        manageCallFromRosterRequestNotification();
    }

    private void manageCallFromRosterRequestNotification() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            boolean newRosterRequest = bundle.getBoolean(NEW_ROSTER_REQUEST, false);
            if (newRosterRequest) {
                try {
                    String sender = bundle.getString(NEW_ROSTER_REQUEST_SENDER);
                    Jid jid = JidCreate.from(sender);
                    RosterNotifications.cancelRosterRequestNotification(this, sender);
                    answerSubscriptionRequest(jid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void manageCallFromBlogPostNotification() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            boolean newBlogPost = bundle.getBoolean(NEW_BLOG_POST, false);
            if (newBlogPost) {
                goToSocialTab();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {

            case R.id.actionSignOut: {
                Preferences.getInstance().deleteAll();
                RealmManager.getInstance().deleteAll();

                XMPPSession.getInstance().logoff();

                ((ViewPagerMainMenuAdapter) mViewpagerMainMenu.getAdapter()).clearFragmentsList();

                Intent splashActivityIntent = new Intent(this, SplashActivity.class);
                splashActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                this.startActivity(splashActivityIntent);
                finish();

                return true;
            }

            case R.id.actionUserOptions: {
                goToMyProfile();
                return true;
            }

            case R.id.actionBlockUsers: {
                Intent blockUsersActivityIntent = new Intent(this, BlockUsersActivity.class);
                this.startActivity(blockUsersActivityIntent);
                return true;
            }

            case R.id.actionManageContacts: {
                Intent intent = new Intent(MainMenuActivity.this, ManageContactsActivity.class);
                MainMenuActivity.this.startActivity(intent);
                return true;
            }

            case R.id.actionAbout: {
                Intent intent = new Intent(MainMenuActivity.this, AboutActivity.class);
                MainMenuActivity.this.startActivity(intent);
                return true;
            }

        }

        return super.onOptionsItemSelected(item);
    }

    private void goToMyProfile() {
        Intent userOptionsActivityIntent = new Intent(this, UserProfileActivity.class);

        String jid = Preferences.getInstance().getUserXMPPJid();
        String name = Preferences.getInstance().getUserNickname();
        User user = new User(jid, name);

        Bundle bundle = new Bundle();
        bundle.putParcelable(UserProfileActivity.USER_PARAMETER, user);

        userOptionsActivityIntent.putExtras(bundle);

        this.startActivity(userOptionsActivityIntent);
    }

    // receives events from EventBus
    @Override
    public void onEvent(Event event) {
        super.onEvent(event);
        switch (event.getType()) {
            case ROOMS_LOADED:
                mRoomsLoaded = true;
                break;
            case GO_BACK_FROM_CHAT:
                ((ViewPagerMainMenuAdapter) mViewpagerMainMenu.getAdapter()).reloadChats();
                break;
            case CONTACTS_CHANGED:
                ((ViewPagerMainMenuAdapter) mViewpagerMainMenu.getAdapter()).syncChats();
                break;
            case BLOG_POST_CREATED:
                goToSocialTab();
                break;
            case REFRESH_UNREAD_MESSAGES_COUNT:
                ((ViewPagerMainMenuAdapter) mViewpagerMainMenu.getAdapter()).reloadChats();
                break;
        }
    }

    private void goToSocialTab() {
        mViewpagerMainMenu.setCurrentItem(ViewPagerMainMenuAdapter.SOCIAL_MEDIA_FRAGMENT_POSITION);
        ((ViewPagerMainMenuAdapter) mViewpagerMainMenu.getAdapter()).reloadBlogPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadChats();
    }

    private void reloadChats() {
        ChatsListsFragment mChatsListsFragment = (ChatsListsFragment) ((ViewPagerMainMenuAdapter) mViewpagerMainMenu.getAdapter())
                .getRegisteredFragment(0);
        if (mChatsListsFragment != null) {
            mChatsListsFragment.loadChats();
        }
    }
}
