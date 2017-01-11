package inaka.com.mangosta.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Locale;

import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.MainMenuActivity;
import inaka.com.mangosta.services.XMPPSessionService;
import inaka.com.mangosta.utils.MangostaApplication;

public class BlogPostNotifications {

    private static Integer mBlogPostCounter = 0;

    public static void newBlogPostNotification() {
        // show notification only if the app is closed
        if (!MangostaApplication.getInstance().isClosed()) {
            return;
        }

        Context context = XMPPSessionService.CONTEXT;

        mBlogPostCounter++;

        String text = String.format(Locale.getDefault(), context.getResources().getQuantityString(R.plurals.blog_post_notification, mBlogPostCounter), mBlogPostCounter);

        PendingIntent chatPendingIntent = preparePendingIntent(context);

        Notification.Builder mNotifyBuilder = new Notification.Builder(context)
                .setContentTitle(text)
//                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_empty_back)
                .setContentIntent(chatPendingIntent)
                .setVibrate(new long[]{0, 200, 0, 200})
                .setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NotificationsControl.BLOG_POST_NOTIFICATION, mNotifyBuilder.build());
    }

    private static PendingIntent preparePendingIntent(Context context) {
        Intent intent = new Intent(context, MainMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        Bundle bundle = new Bundle();
        bundle.putBoolean(MainMenuActivity.NEW_BLOG_POST, true);
        intent.putExtras(bundle);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void cancelBlogPostNotifications(Context context) {
        NotificationsControl.cancelNotifications(context, NotificationsControl.BLOG_POST_NOTIFICATION);
        mBlogPostCounter = 0;
    }

}
