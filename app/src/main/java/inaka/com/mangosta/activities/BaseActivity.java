package inaka.com.mangosta.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.Jid;

import java.util.Locale;

import butterknife.Unbinder;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.services.XMPPSessionService;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.realm.Realm;

public class BaseActivity extends AppCompatActivity {

    private Realm mRealm;
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
        mRealm = RealmManager.getInstance().getRealm();
        disposables = new CompositeDisposable();
    }

    @Override
    protected void onDestroy() {
        if (unbinder != null) {
            unbinder.unbind();
        }
        EventBus.getDefault().unregister(this);

        if (mRealm != null && !Preferences.isTesting()) {
            mRealm.close();
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

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

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

    private void clearReferences() {
        Activity currActivity = getAppCurrentActivity();
        if (this.equals(currActivity)) {
            MangostaApplication.getInstance().setCurrentActivity(null);
        }
    }

    public void onEvent(Event event) {
        switch (event.getType()) {
            case PRESENCE_SUBSCRIPTION_REQUEST:
                if (getAppCurrentActivity().equals(this)) {
                    answerSubscriptionRequest(event.getJidSender());
                }
                break;
        }
    }

    private Activity getAppCurrentActivity() {
        return MangostaApplication.getInstance().getCurrentActivity();
    }

    protected void answerSubscriptionRequest(final Jid jid) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage(String.format(Locale.getDefault(), getString(R.string.roster_subscription_request), jid.toString()));

        builder.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Presence subscribed = new Presence(Presence.Type.subscribed);
                    subscribed.setTo(jid);
                    XMPPSession.getInstance().sendStanza(subscribed);

                    if (!RosterManager.getInstance().isContact(jid)) {
                        RosterManager.getInstance().addContact(jid.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = builder.show();
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            }
        });

    }

    protected void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

}
