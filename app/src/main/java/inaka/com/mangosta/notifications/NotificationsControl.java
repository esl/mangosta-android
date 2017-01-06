package inaka.com.mangosta.notifications;

import android.app.NotificationManager;
import android.content.Context;

public class NotificationsControl {

    public final static int CHAT_MESSAGE_NOTIFICATION = 7000001;

    public static void cancelNotifications(Context ctx, String tag, int id) {
        NotificationManager mNotificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(tag, id);
    }

    public static void cancelNotifications(Context ctx, int id) {
        NotificationManager mNotificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);
    }

}
