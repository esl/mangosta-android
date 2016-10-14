package inaka.com.mangosta.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.ProgressBar;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.fragments.LoginDialogFragment;

public class SplashActivity extends FragmentActivity {

    @Bind(R.id.progressLoading)
    ProgressBar progressLoading;

    final int WAIT_TIME = 2000;

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
                createLoginDialog();
            }
        }, WAIT_TIME);
    }

    private void createLoginDialog() {
        DialogFragment fragment = LoginDialogFragment.newInstance();
        fragment.show(getSupportFragmentManager(), getString(R.string.title_login));
    }

}
