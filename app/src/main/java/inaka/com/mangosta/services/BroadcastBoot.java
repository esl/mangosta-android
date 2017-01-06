package inaka.com.mangosta.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import inaka.com.mangosta.xmpp.XMPPSession;

public class BroadcastBoot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        XMPPSession.startService(context);
    }

}
