package inaka.com.mangosta.utils;

import android.app.Activity;
import android.content.Intent;

import inaka.com.mangosta.activities.ChatActivity;

public class NavigateToChat {

    public static void go(String chatJid, String chatName, Activity currentActivity) {
        Intent intent = new Intent(currentActivity, ChatActivity.class);
        intent.putExtra(ChatActivity.CHAT_JID_PARAMETER, chatJid);
        intent.putExtra(ChatActivity.CHAT_NAME_PARAMETER, chatName);
        currentActivity.startActivity(intent);
        currentActivity.finish();
    }

}
