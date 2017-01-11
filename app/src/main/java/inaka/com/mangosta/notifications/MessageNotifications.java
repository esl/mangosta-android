package inaka.com.mangosta.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.ChatActivity;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.services.XMPPSessionService;
import inaka.com.mangosta.utils.MangostaApplication;

public class MessageNotifications {

    private static HashMap<String, Integer> mChatMessageCounters = new HashMap<>();

    public static void chatMessageNotification(String messageId) {
        // show notification only if the app is closed
        if (!MangostaApplication.getInstance().isClosed()) {
            EventBus.getDefault().post(new Event(Event.Type.REFRESH_UNREAD_MESSAGES_COUNT));
            return;
        }

        Context context = XMPPSessionService.CONTEXT;
        ChatMessage chatMessage = RealmManager.getInstance().getChatMessage(messageId);
        String chatJid = chatMessage.getRoomJid();

        Integer count = updateMessageCounters(chatJid);

        Chat chat = RealmManager.getInstance().getChat(chatJid);
        String chatName = chat.getName();
        String text = String.format(Locale.getDefault(), context.getResources().getQuantityString(R.plurals.chat_message_notification, count), count);

        PendingIntent chatPendingIntent = preparePendingIntent(context, chatJid, chatName);

        Notification.Builder mNotifyBuilder = new Notification.Builder(context)
                .setContentTitle(chatName)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_empty_back)
                .setContentIntent(chatPendingIntent)
                .setVibrate(new long[]{0, 200, 0, 200})
                .setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(chatJid, NotificationsControl.CHAT_MESSAGE_NOTIFICATION, mNotifyBuilder.build());
    }

    private static Integer updateMessageCounters(String chatJid) {
        Integer count = mChatMessageCounters.get(chatJid);
        if (count == null) {
            count = 0;
        }
        count++;
        mChatMessageCounters.put(chatJid, count);
        return count;
    }

    private static PendingIntent preparePendingIntent(Context context, String chatJid, String chatName) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.CHAT_JID_PARAMETER, chatJid);
        bundle.putString(ChatActivity.CHAT_NAME_PARAMETER, chatName);
        bundle.putBoolean(ChatActivity.IS_NEW_CHAT_PARAMETER, false);
        intent.putExtras(bundle);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void cancelChatNotifications(Context context, String chatJid) {
        NotificationsControl.cancelNotifications(context, chatJid, NotificationsControl.CHAT_MESSAGE_NOTIFICATION);
        mChatMessageCounters.put(chatJid, 0);
    }

}
