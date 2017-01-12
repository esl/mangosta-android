package inaka.com.mangosta.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.jxmpp.jid.Jid;

import java.util.Locale;

import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.MainMenuActivity;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.services.XMPPSessionService;
import inaka.com.mangosta.utils.MangostaApplication;

public class RosterNotifications {

    public static void rosterRequestNotification(Jid sender) {
        // show notification only if the app is closed
        if (!MangostaApplication.getInstance().isClosed()) {
            EventBus.getDefault().post(new Event(Event.Type.PRESENCE_SUBSCRIPTION_REQUEST, sender));
            return;
        }

        Context context = XMPPSessionService.CONTEXT;
        String text = String.format(Locale.getDefault(), context.getString(R.string.roster_subscription_request), sender.toString());

        PendingIntent chatPendingIntent = preparePendingIntent(context, sender.toString());

        Notification.Builder mNotifyBuilder = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.roster_subscription_request_notification_title))
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_empty_back)
                .setContentIntent(chatPendingIntent)
                .setVibrate(new long[]{0, 200, 0, 200})
                .setOngoing(true);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(sender.toString(), NotificationsControl.ROSTER_NOTIFICATION, mNotifyBuilder.build());
    }

    private static PendingIntent preparePendingIntent(Context context, String requestSender) {
        Intent intent = new Intent(context, MainMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        Bundle bundle = new Bundle();
        bundle.putBoolean(MainMenuActivity.NEW_ROSTER_REQUEST, true);
        bundle.putString(MainMenuActivity.NEW_ROSTER_REQUEST_SENDER, requestSender);
        intent.putExtras(bundle);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void cancelRosterRequestNotification(Context context, String requestSender) {
        NotificationsControl.cancelNotifications(context, requestSender, NotificationsControl.CHAT_MESSAGE_NOTIFICATION);
    }

}
