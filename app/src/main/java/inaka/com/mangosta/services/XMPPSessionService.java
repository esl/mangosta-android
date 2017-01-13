package inaka.com.mangosta.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.Timer;

import inaka.com.mangosta.xmpp.XMPPSession;

public class XMPPSessionService extends Service {

    private final IBinder binder = new XMPPSessionServiceBinder();
    private final long TIMER_LAPSE = 5 * 60 * 1000; // 5 minutes
    public static Context CONTEXT;
    private static boolean RUNNING;

    public class XMPPSessionServiceBinder extends Binder {
        public XMPPSessionService getService() {
            return XMPPSessionService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RUNNING = true;
        CONTEXT = getApplicationContext();
        Timer timer = new Timer();
        XMPPReconnectTask xmppReconnectTask = new XMPPReconnectTask(CONTEXT);
        timer.schedule(xmppReconnectTask, TIMER_LAPSE, TIMER_LAPSE);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RUNNING = false;
    }

    public static boolean isRunning() {
        return RUNNING;
    }

    public void login(String userName, String password) throws Exception {
        XMPPSession.getInstance().login(userName, password);
    }

    public void relogin() throws Exception {
        XMPPSession.getInstance().relogin();
    }

}
