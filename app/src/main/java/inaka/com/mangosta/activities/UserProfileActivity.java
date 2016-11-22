package inaka.com.mangosta.activities;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
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

    public final static String USER_PARAMETER = "user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        Bundle bundle = getIntent().getExtras();
        User user = bundle.getParcelable(USER_PARAMETER);

        if (user != null) {
            setTitle(user.getLogin());
            textLoginUserProfile.setText(user.getLogin());
            textNameUserProfile.setText(XMPPUtils.fromUserNameToJID(user.getLogin()));
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

    @Override
    protected void onResume() {
        super.onResume();
    }

}
