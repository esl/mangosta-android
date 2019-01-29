package inaka.com.mangosta.activities;

import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.chat.RoomsListManager;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.utils.NavigateToChat;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class UserProfileActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.viewpagerProfile)
    ViewPager viewpagerProfile;

    @BindView(R.id.textNameUserProfile)
    TextView textNameUserProfile;

    @BindView(R.id.textLoginUserProfile)
    TextView textLoginUserProfile;

    @BindView(R.id.imageAvatarUserProfile)
    ImageView imageAvatarUserProfile;

    public final static String USER_PARAMETER = "user";

    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        unbinder = ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        Bundle bundle = getIntent().getExtras();
        mUser = bundle.getParcelable(USER_PARAMETER);

        if (mUser != null) {
            setTitle(mUser.getLogin());
            textLoginUserProfile.setText(mUser.getLogin());
            textNameUserProfile.setText(XMPPUtils.fromUserNameToJID(mUser.getLogin()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_profile, menu);

        MenuItem actionOpenChat = menu.findItem(R.id.actionOpenChat);
        if (XMPPUtils.isAutenticatedUser(mUser)) {
            actionOpenChat.setVisible(false);
        } else {
            actionOpenChat.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;

            case R.id.actionOpenChat:
                if (mUser != null) {
                    String chatJid = XMPPUtils.fromUserNameToJID(mUser.getLogin());
                    RoomsListManager.getInstance().createCommonChat(chatJid);
                    NavigateToChat.go(chatJid, XMPPUtils.fromJIDToUserName(chatJid), this);
                }
                break;
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
