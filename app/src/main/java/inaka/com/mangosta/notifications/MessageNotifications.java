package inaka.com.mangosta.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import java.util.HashMap;
import java.util.Locale;

import inaka.com.mangosta.R;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.services.XMPPSessionService;

public class MessageNotifications {

    private static HashMap<String, Integer> mChatMessageCounters = new HashMap<>();

    public final static int CHAT_MESSAGE_NOTIFICATION = 7000001;

    public static void chatMessageNotification(String messageId) {
        Context context = XMPPSessionService.CONTEXT;

        ChatMessage chatMessage = RealmManager.getInstance().getChatMessage(messageId);

        Integer count = mChatMessageCounters.get(chatMessage.getRoomJid());
        count++;
        mChatMessageCounters.put(chatMessage.getRoomJid(), count);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Chat chat = RealmManager.getInstance().getChat(chatMessage.getRoomJid());

        String title = chat.getName();
        String text = String.format(Locale.getDefault(), context.getString(R.string.chat_message_notification), count);

        Notification.Builder mNotifyBuilder = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true);

        mNotificationManager.notify(CHAT_MESSAGE_NOTIFICATION, mNotifyBuilder.build());
    }

    public static void cancelChatNotifications(Context context, String chatJid) {
        NotificationsControl.cancelNotifications(context, chatJid, CHAT_MESSAGE_NOTIFICATION);
        mChatMessageCounters.put(chatJid, 0);
    }

}
