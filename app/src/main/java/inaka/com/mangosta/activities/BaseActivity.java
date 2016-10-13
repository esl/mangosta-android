package inaka.com.mangosta.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.xmpp.XMPPSession;
import io.realm.Realm;

public class BaseActivity extends AppCompatActivity {

    private Realm mRealm;
    private static int mSessionDepth = 0;
    private boolean mIsRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRealm = RealmManager.getRealm();

        String message = getIntent().getStringExtra("notification_message");
        if (!TextUtils.isEmpty(message)) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        ButterKnife.unbind(this);

        if (mRealm != null) {
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
        EventBus.getDefault().register(this);

        mSessionDepth++;

        Log.wtf("activities", String.valueOf(mSessionDepth));

        if (mSessionDepth == 1 && XMPPSession.getInstance().getXMPPConnection().isConnected()) {
            XMPPSession.getInstance().activeCSI();
        }

        MangostaApplication.getInstance().moveToForeground();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);

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
                mRealm = RealmManager.getRealm();
            }
        } catch (Throwable e) {
            mRealm = RealmManager.getRealm();
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
