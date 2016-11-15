package inaka.com.mangosta.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import io.realm.Realm;

public class BaseActivity extends AppCompatActivity {

    private Realm mRealm;
    private static int mSessionDepth = 0;
    private boolean mIsRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRealm = RealmManager.getInstance().getRealm();
    }

    @Override
    protected void onDestroy() {
        ButterKnife.unbind(this);
        EventBus.getDefault().unregister(this);

        if (mRealm != null && !Preferences.isTesting()) {
            mRealm.close();
        }
        clearReferences();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MangostaApplication.bus.register(this);
        setIsRegistered(true);
        MangostaApplication.getInstance().setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
        clearReferences();
        super.onPause();

        if (isRegistered()) {
            MangostaApplication.bus.unregister(this);
            setIsRegistered(false);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        mSessionDepth++;

        Log.wtf("activities", String.valueOf(mSessionDepth));

        if (!Preferences.isTesting() && mSessionDepth == 1 && XMPPSession.getInstance().getXMPPConnection().isConnected()) {
            XMPPSession.getInstance().activeCSI();
        }

        MangostaApplication.getInstance().moveToForeground();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isRegistered()) {
            MangostaApplication.bus.unregister(this);
            setIsRegistered(false);
        }

        if (mSessionDepth > 0) {
            mSessionDepth--;
        }

        Log.wtf("activities", String.valueOf(mSessionDepth));

        if (mSessionDepth == 0) {
            XMPPSession.getInstance().inactiveCSI();
            MangostaApplication.getInstance().moveToBackground();
        }

    }

    public Realm getRealm() {
        try {
            if (mRealm.isClosed()) {
                mRealm = RealmManager.getInstance().getRealm();
            }
        } catch (Throwable e) {
            mRealm = RealmManager.getInstance().getRealm();
        }
        return mRealm;
    }

    private void setIsRegistered(boolean registered) {
        mIsRegistered = registered;
    }

    private boolean isRegistered() {
        return mIsRegistered;
    }

    private void clearReferences() {
        Activity currActivity = MangostaApplication.getInstance().getCurrentActivity();
        if (this.equals(currActivity)) {
            MangostaApplication.getInstance().setCurrentActivity(null);
        }
    }

    // base method to override
    public void onEvent(Event event) {

    }

}
