package inaka.com.mangosta.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.fragments.LoginDialogFragment;
import inaka.com.mangosta.services.XMPPSessionService;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class SplashActivity extends FragmentActivity {

    @Bind(R.id.progressLoading)
    ProgressBar progressLoading;

    public static final int WAIT_TIME = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= 23) {
            progressLoading.getIndeterminateDrawable().setColorFilter(this.getColor(R.color.colorPrimary),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        } else {
            progressLoading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorPrimary),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Preferences.getInstance().isLoggedIn()) {
                    xmppReloginAndStart();
                } else {
                    createLoginDialog();
                    progressLoading.setVisibility(View.INVISIBLE);
                }
            }
        }, WAIT_TIME);

        XMPPSession.startService(this);
    }

    private void xmppReloginAndStart() {
        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                if (!XMPPSessionService.isRunning()) {
                    Preferences preferences = Preferences.getInstance();
                    XMPPSession.getInstance().login(XMPPUtils.fromJIDToUserName(preferences.getUserXMPPJid()), preferences.getUserXMPPPassword());
                }
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                startApplication();
            }

            @Override
            public void onError(Context context, Exception e) {
                XMPPSession.getInstance().getXMPPConnection().disconnect();
                XMPPSession.clearInstance();
                Toast.makeText(context, getString(R.string.error_login), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createLoginDialog() {
        if (!isFinishing() && !isDestroyed()) {
            DialogFragment fragment = LoginDialogFragment.newInstance();
            fragment.setCancelable(false);
            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(fragment, getString(R.string.title_login));
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    public void startApplication() {
        Intent mainMenuIntent = new Intent(this, MainMenuActivity.class);
        mainMenuIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainMenuIntent);
        finish();
    }

}
