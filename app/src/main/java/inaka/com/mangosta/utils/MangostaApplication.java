package inaka.com.mangosta.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;

import androidx.multidex.MultiDex;

import androidx.room.Room;
import inaka.com.mangosta.database.MangostaDatabase;
import inaka.com.mangosta.services.XMPPSessionService;
import inaka.com.mangosta.xmpp.XMPPSession;

public class MangostaApplication extends Application {

    private static MangostaApplication CONTEXT;

    private Activity mCurrentActivity = null;
    private MangostaDatabase database;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CONTEXT = this;

        Stetho.initializeWithDefaults(this);

        database = Room.databaseBuilder(getApplicationContext(),
                MangostaDatabase.class, "MangostaDatabase").build();
    }

    public static MangostaApplication getInstance() {
        return CONTEXT;
    }

    public MangostaDatabase getDatabase() {
        return database;
    }

    public void moveToBackground() {
        if (!XMPPSessionService.isRunning()) {
            XMPPSession.startService(this);
        }
    }

    public void moveToForeground() {
        if (!XMPPSessionService.isRunning()) {
            XMPPSession.startService(this);
        }
    }

    public Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    public boolean isClosed() {
        return mCurrentActivity == null;
    }

    public void setCurrentActivity(Activity currentActivity) {
        mCurrentActivity = currentActivity;
    }

}
