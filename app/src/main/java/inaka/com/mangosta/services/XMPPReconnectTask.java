package inaka.com.mangosta.services;

import android.content.Context;

import java.util.TimerTask;

import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.xmpp.XMPPSession;

public class XMPPReconnectTask extends TimerTask {

    private Context mContext;
    private static final Object LOCK_1 = new Object() {
    };

    public XMPPReconnectTask(Context context) {
        this.mContext = context;
    }

    @Override
    public void run() {
        synchronized (LOCK_1) {
            if (MangostaApplication.getInstance().getCurrentActivity() == null) {
                XMPPSession.getInstance().backgroundRelogin();
            }
        }
    }

}
