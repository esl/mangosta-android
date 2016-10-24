package inaka.com.mangosta.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.fragments.BlogsListFragment;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class UserProfileActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.slidingTabStrip)
    PagerSlidingTabStrip slidingTabStrip;

    @Bind(R.id.viewpagerProfile)
    ViewPager viewpagerProfile;

    @Bind(R.id.textNameUserProfile)
    TextView textNameUserProfile;

    @Bind(R.id.textLoginUserProfile)
    TextView textLoginUserProfile;

    @Bind(R.id.imageAvatarUserProfile)
    ImageView imageAvatarUserProfile;

    private User mUser;
    private boolean mIsAuthenticatedUser;

    private BlogsListFragment mBlogsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        Bundle bundle = getIntent().getExtras();
        mIsAuthenticatedUser = bundle.getBoolean("auth_user");
        mUser = bundle.getParcelable("user");

        if (mUser != null) {
            setTitle(mUser.getLogin());
            textLoginUserProfile.setText(mUser.getLogin());
            textNameUserProfile.setText(XMPPUtils.fromUserNameToJID(mUser.getLogin()));
        }

        mBlogsFragment = new BlogsListFragment();

        viewpagerProfile.setAdapter(new UserPagerAdapter(getSupportFragmentManager()));
        slidingTabStrip.setViewPager(viewpagerProfile);
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

    private boolean isTheAuthenticatedUser() {
        return mIsAuthenticatedUser || XMPPUtils.isAutenticatedUser(mUser);
    }

    private class UserPagerAdapter extends FragmentPagerAdapter {

        public UserPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return mBlogsFragment;

                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getText(R.string.title_tab_blogs);

                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return isTheAuthenticatedUser() ? 1 : 0;
        }
    }
}
