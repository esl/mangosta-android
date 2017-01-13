package inaka.com.mangosta.notifications;

import android.app.NotificationManager;
import android.content.Context;

public class NotificationsControl {

    public final static int CHAT_MESSAGE_NOTIFICATION = 7000001;
    public final static int BLOG_POST_NOTIFICATION = 7000002;
    public final static int ROSTER_NOTIFICATION = 7000003;

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
