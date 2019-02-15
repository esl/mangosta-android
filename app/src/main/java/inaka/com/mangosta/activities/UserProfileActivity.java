package inaka.com.mangosta.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import inaka.com.mangosta.R;
import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.utils.NavigateToChat;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class UserProfileActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.viewpagerProfile)
    ViewPager viewpagerProfile;

    @BindView(R.id.textLoginUserProfile)
    TextView textLoginUserProfile;

    @BindView(R.id.textNameUserProfile)
    TextView textNameUserProfile;

    @BindView(R.id.imageAvatarUserProfile)
    ImageView imageAvatarUserProfile;

    @BindView(R.id.buttonUpdateUserProfile)
    Button buttonUpdateUserProfile;

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

        bindUserValues();

        showSaveButtonIfSelf();
    }

    private void bindUserValues() {
        if (mUser != null) {
            String login = XMPPUtils.fromJIDToUserName(mUser.getJid());

            setTitle(login);
            textLoginUserProfile.setText(mUser.getJid());
            textNameUserProfile.setText(mUser.getName());
        }

    }

    private void showSaveButtonIfSelf() {
        boolean isSelf = (Preferences.getInstance().getUserXMPPJid().equals(mUser.getJid()));
        buttonUpdateUserProfile.setVisibility(isSelf ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_profile, menu);

        MenuItem actionOpenChat = menu.findItem(R.id.actionOpenChat);
        if (XMPPUtils.isAuthenticatedUser(mUser)) {
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
                    RoomManager.getInstance().createChatIfNotExists(mUser.getJid());
                    NavigateToChat.go(mUser.getJid(), XMPPUtils.fromJIDToUserName(mUser.getJid()), this);
                }
                break;
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @OnClick(R.id.buttonUpdateUserProfile)
    public void clickUserProfile() {
        showEnterNicknameDialog();
    }

    private void showEnterNicknameDialog() {
        String currentNickname = mUser.getName();
        if (TextUtils.isEmpty(currentNickname)) {
            currentNickname = XMPPUtils.fromJIDToUserName(mUser.getJid());
        }
        final EditText input = new EditText(this);
        input.setLines(1);
        input.setText(currentNickname);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_activity_user_profile)
                .setMessage(R.string.enter_nickname)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes, (dlg, which) -> {
                    mUser.setName(input.getText().toString());
                    bindUserValues();

                    Preferences.getInstance().setUserNickName(mUser.getName());
                    XMPPSession.getInstance().saveVCard();
                }).create();

        int margin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        alertDialog.setView(input, margin, 0, margin, 0);
        alertDialog.show();
    }
}
