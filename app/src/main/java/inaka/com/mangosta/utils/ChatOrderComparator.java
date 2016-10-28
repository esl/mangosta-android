package inaka.com.mangosta.utils;

import java.util.Comparator;
import java.util.Date;

import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.realm.RealmManager;

public class ChatOrderComparator implements Comparator<Chat> {

    @Override
    public int compare(Chat chat1, Chat chat2) {
        ChatMessage chatMessage1 = RealmManager.getLastMessageForChat(chat1.getJid());
        ChatMessage chatMessage2 = RealmManager.getLastMessageForChat(chat2.getJid());

        Date date1 = null;
        Date date2 = null;

        if (chatMessage1 != null) {
            date1 = chatMessage1.getDate();
        }

        if (chatMessage2 != null) {
            date2 = chatMessage2.getDate();
        }

        if (date1 == null) {
            return 1;
        } else if (date2 == null) {
            return 0;
        } else {
            return date2.compareTo(date1);
        }
    }
}