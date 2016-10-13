package inaka.com.mangosta.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.squareup.picasso.Picasso;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.fragments.BlogsListFragment;
import inaka.com.mangosta.fragments.UserProfileFragment;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.utils.TimeCalculation;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class UserProfileActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.slidingTabStrip)
    PagerSlidingTabStrip slidingTabStrip;

    @Bind(R.id.viewpagerProfile)
    ViewPager viewpagerProfile;

    @Bind(R.id.textUserEmail)
    TextView textUserEmail;

    @Bind(R.id.textNameUserProfile)
    TextView textNameUserProfile;

    @Bind(R.id.imageAvatarUserProfile)
    ImageView imageAvatarUserProfile;

    @Bind(R.id.textCreatedAtUserProfile)
    TextView textCreatedAtUserProfile;

    @Bind(R.id.textLocationUserProfile)
    TextView textLocationUserProfile;

    private User mUser;
    private boolean mIsAuthenticatedUser;

    private UserProfileFragment mUserProfileFragment;
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

            if (mUser.getAvatarUrl() != null) {
                Picasso.with(this).load(mUser.getAvatarUrl()).noFade().fit().into(imageAvatarUserProfile);
            }
        }

        textCreatedAtUserProfile.setText(String.format(Locale.getDefault(), this.getResources().getString(R.string.label_created), TimeCalculation.getTimeStringAgoSinceStringDate(this, mUser.getCreatedAt())));

        mUserProfileFragment = UserProfileFragment.newInstance(mUser, mIsAuthenticatedUser);

        mBlogsFragment = new BlogsListFragment();

        viewpagerProfile.setAdapter(new UserPagerAdapter(getSupportFragmentManager()));
        slidingTabStrip.setViewPager(viewpagerProfile);
    }

    private void textViewGoneifEmpty(TextView textView) {
        if (textView.getText().toString().equals("")) {
            textView.setVisibility(View.GONE);
        }
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
                    return mUserProfileFragment;

                case 1:
                    return mBlogsFragment;

                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getText(R.string.title_tab_bio);

                case 1:
                    return getText(R.string.title_tab_blogs);

                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return isTheAuthenticatedUser() ? 2 : 1;
        }
    }
}
