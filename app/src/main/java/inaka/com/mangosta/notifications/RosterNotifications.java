package inaka.com.mangosta.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.Jid;

import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.MainMenuActivity;
import inaka.com.mangosta.services.XMPPSessionService;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;

public class RosterNotifications {

    public static void rosterRequestNotification(Jid sender) {
        // show notification only if the app is closed
        if (!MangostaApplication.getInstance().isClosed()) {
            answerSubscriptionRequest(sender);
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
                .setAutoCancel(true)
                .setOngoing(true);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(sender.toString(), NotificationsControl.ROSTER_NOTIFICATION, mNotifyBuilder.build());
    }

    public static void answerSubscriptionRequest(final Jid jid) {
        Context context = XMPPSessionService.CONTEXT;

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        builder.setMessage(String.format(Locale.getDefault(), context.getString(R.string.roster_subscription_request), jid.toString()));

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
        AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getResources().getColor(R.color.colorPrimary));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(R.color.colorPrimary));
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
