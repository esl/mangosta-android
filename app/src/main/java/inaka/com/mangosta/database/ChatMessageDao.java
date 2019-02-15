package inaka.com.mangosta.database;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import inaka.com.mangosta.models.ChatMessage;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@Dao
public interface ChatMessageDao {
    @Query("SELECT * FROM ChatMessage where messageId = :messageId")
    Maybe<ChatMessage> findByMessageId(String messageId);

    @Query("SELECT * FROM ChatMessage where roomJid = :chatId order by date asc")
    Flowable<List<ChatMessage>> findByChatId(String chatId);

    @Query("UPDATE ChatMessage set content = :content where messageId = :messageId")
    int updateContent(String content, String messageId);

    @Query("UPDATE ChatMessage set unread = :unread where messageId = :messageId")
    int updateUnread(boolean unread, String messageId);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    int update(ChatMessage chatMessage);

    @Query("DELETE FROM ChatMessage where messageId in (:messagesToDeleteIds)")
    int deleteItems(List<String> messagesToDeleteIds);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ChatMessage chatMessage);

    @Query("DELETE FROM ChatMessage")
    void deleteAll();
}
