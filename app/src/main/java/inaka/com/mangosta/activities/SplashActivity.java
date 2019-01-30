package inaka.com.mangosta.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import inaka.com.mangosta.R;
import inaka.com.mangosta.fragments.LoginDialogFragment;
import inaka.com.mangosta.services.XMPPSessionService;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SplashActivity extends FragmentActivity {

    @BindView(R.id.progressLoading)
    ProgressBar progressLoading;

    private Unbinder unbinder;

    public static final int WAIT_TIME = 1500;

    private XMPPSessionService xmppSessionService;
    protected ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            xmppSessionService = ((XMPPSessionService.XMPPSessionServiceBinder) binder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            xmppSessionService = null;
        }
    };

    public XMPPSessionService getService() {
        return xmppSessionService;
    }

    public void bindService() {
        Intent serviceIntent = new Intent(this, XMPPSessionService.class);
        serviceIntent.setPackage("com.nanoscopia.services");
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        unbinder = ButterKnife.bind(this);

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

    @Override
    protected void onResume() {
        if (xmppSessionService == null) {
            bindService();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (xmppSessionService != null) {
            unbindService(mServiceConnection);
            xmppSessionService = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }

    private void xmppReloginAndStart() {
        Completable task = Completable.fromCallable(() -> {
            xmppSessionService.relogin();
            Thread.sleep(XMPPSession.REPLY_TIMEOUT);
            return null;
        });

        Disposable d = task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::startApplication, error -> {
                    XMPPSession.getInstance().getXMPPConnection().disconnect();
                    XMPPSession.clearInstance();
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.error_login), Toast.LENGTH_SHORT).show();
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
