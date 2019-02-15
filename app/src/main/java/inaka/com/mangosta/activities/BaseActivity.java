package inaka.com.mangosta.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import butterknife.Unbinder;
import inaka.com.mangosta.services.XMPPSessionService;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class BaseActivity extends AppCompatActivity {

    public static int mSessionDepth = 0;

    protected Unbinder unbinder;

    private CompositeDisposable disposables;

    private XMPPSessionService myService;
    protected ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            myService = ((XMPPSessionService.XMPPSessionServiceBinder) binder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myService = null;
        }
    };

    public void bindService() {
        Intent serviceIntent = new Intent(this, XMPPSessionService.class);
        serviceIntent.setPackage("com.nanoscopia.services");
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disposables = new CompositeDisposable();
    }

    @Override
    protected void onDestroy() {
        if (unbinder != null) {
            unbinder.unbind();
        }
        clearReferences();
        disposables.dispose();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if (myService == null) {
            bindService();
        }
        super.onResume();
        MangostaApplication.getInstance().setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
        if (myService != null) {
            unbindService(mServiceConnection);
            myService = null;
        }

        clearReferences();
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mSessionDepth++;

        Log.wtf("activities", String.valueOf(mSessionDepth));

        if (!Preferences.isTesting() && mSessionDepth == 1 && XMPPSession.getInstance().getXMPPConnection().isConnected()) {
//            XMPPSession.getInstance().activeCSI();
        }

        MangostaApplication.getInstance().moveToForeground();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mSessionDepth > 0) {
            mSessionDepth--;
        }

        Log.wtf("activities", String.valueOf(mSessionDepth));

        if (mSessionDepth == 0) {
            MangostaApplication.getInstance().moveToBackground();
        }

    }

    private void clearReferences() {
        Activity currActivity = getAppCurrentActivity();
        if (this.equals(currActivity)) {
            MangostaApplication.getInstance().setCurrentActivity(null);
        }
    }

    private Activity getAppCurrentActivity() {
        return MangostaApplication.getInstance().getCurrentActivity();
    }

    protected void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

}
