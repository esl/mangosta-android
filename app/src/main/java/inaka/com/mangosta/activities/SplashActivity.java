package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.widget.ProgressBar;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;

public class SplashActivity extends FragmentActivity {

    final int WAIT_TIME = 2000;

    @Bind(R.id.progressLoading)
    ProgressBar progressLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= 23) {
            progressLoading.getIndeterminateDrawable().setColorFilter(this.getColor(R.color.colorPrimaryDark),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        } else {
            progressLoading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorPrimaryDark),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Preferences.getInstance().setLoggedIn(true);
                loginOnXMPP();
                startApplication();
            }
        }, WAIT_TIME);

    }

    private void loginOnXMPP() {
        XMPPSession.getInstance().login();
    }

    private void startApplication() {
        Intent mainMenuIntent = new Intent(this, MainMenuActivity.class);
        mainMenuIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainMenuIntent);
        finish();
    }

}
