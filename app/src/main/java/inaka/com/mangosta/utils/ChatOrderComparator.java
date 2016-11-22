package inaka.com.mangosta.utils;

import java.util.Comparator;

import inaka.com.mangosta.models.Chat;

public class ChatOrderComparator implements Comparator<Chat> {

    @Override
    public int compare(Chat chat1, Chat chat2) {
        Integer position1 = chat1.getSortPosition();
        Integer position2 = chat2.getSortPosition();
        return position1.compareTo(position2);
    }

}