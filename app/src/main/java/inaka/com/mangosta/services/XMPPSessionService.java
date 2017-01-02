package inaka.com.mangosta.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.util.Timer;

public class XMPPSessionService extends Service {
    private Timer mTimer;
    private final long TIMER_LAPSE = 2 * 60 * 1000; // 2 minutes
    public static Context CONTEXT;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CONTEXT = getApplicationContext();
        mTimer = new Timer();
        XMPPReconnectTask xmppReconnectTask = new XMPPReconnectTask(CONTEXT);
        mTimer.schedule(xmppReconnectTask, 0, TIMER_LAPSE);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
