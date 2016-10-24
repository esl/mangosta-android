package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.astuetz.PagerSlidingTabStrip;
import com.getbase.floatingactionbutton.FloatingActionButton;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.ViewPagerMainMenuAdapter;
import inaka.com.mangosta.fragments.ChatsListFragment;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class MainMenuActivity extends BaseActivity {

    @Bind(R.id.slidingTabStrip)
    PagerSlidingTabStrip mSlidingTabStrip;

    @Bind(R.id.viewpagerMainMenu)
    ViewPager mViewpagerMainMenu;

    @Bind(R.id.createNewChatFloatingButton)
    FloatingActionButton createNewChatFloatingButton;

    @Bind(R.id.createNewBlogFloatingButton)
    FloatingActionButton createNewBlogFloatingButton;

    @Bind(R.id.manageFriendsFloatingButton)
    FloatingActionButton manageFriendsFloatingButton;

    public boolean mRoomsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        String tabTitles[] = new String[]{
                getResources().getString(R.string.title_tab_1_to_1_chats),
                getResources().getString(R.string.title_tab_muc_light_chats),
                getResources().getString(R.string.title_tab_muc_chats)};

        mViewpagerMainMenu.setAdapter(new ViewPagerMainMenuAdapter(getSupportFragmentManager(), tabTitles));
        mSlidingTabStrip.setViewPager(mViewpagerMainMenu);

        createNewChatFloatingButton.setIcon(R.mipmap.ic_action_create_new_chat_light);
        createNewBlogFloatingButton.setIcon(R.mipmap.ic_add_blog);
        manageFriendsFloatingButton.setIcon(R.mipmap.ic_friends);

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

        manageFriendsFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenuActivity.this, ManageFriendsActivity.class);
                MainMenuActivity.this.startActivity(intent);
            }
        });

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

                getRealm().beginTransaction();
                getRealm().deleteAll();
                getRealm().commitTransaction();

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

        }

        return super.onOptionsItemSelected(item);
    }

    private void goToMyProfile() {
        Intent userOptionsActivityIntent = new Intent(this, UserProfileActivity.class);

        User user = new User();
        user.setLogin(XMPPUtils.fromJIDToUserName(Preferences.getInstance().getUserXMPPJid()));

        Bundle bundle = new Bundle();
        bundle.putBoolean(UserProfileActivity.AUTH_USER_PARAMETER, true);
        bundle.putParcelable(UserProfileActivity.USER_PARAMETER, user);

        userOptionsActivityIntent.putExtras(bundle);

        this.startActivity(userOptionsActivityIntent);
    }

    // receives events from EventBus
    @Override
    public void onEvent(Event event) {
        switch (event.getType()) {
            case ROOMS_LOADED:
                mRoomsLoaded = true;
                break;
            case GO_BACK_FROM_CHAT:
                ((ViewPagerMainMenuAdapter) mViewpagerMainMenu.getAdapter()).reloadChats();
                break;
            case GO_BACK_FROM_MANAGE_FRIENDS:
                ((ViewPagerMainMenuAdapter) mViewpagerMainMenu.getAdapter()).reloadChats();
                break;
            case BLOG_POST_CREATED:
                goToMyProfile();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadChats();
    }

    private void reloadChats() {
        ChatsListFragment mChatsListFragment = (ChatsListFragment) ((ViewPagerMainMenuAdapter) mViewpagerMainMenu.getAdapter())
                .getRegisteredFragment(mViewpagerMainMenu.getCurrentItem());
        if (mChatsListFragment != null) {
            mChatsListFragment.loadChats();
        }
    }
}
