package inaka.com.mangosta.database;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import inaka.com.mangosta.models.Chat;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@Dao
public interface ChatDao {
    @Query("SELECT * FROM Chat order by sortPosition, dateCreated desc")
    Flowable<List<Chat>> findAll();

    @Query("SELECT * FROM Chat where jid = :jid")
    Maybe<Chat> findByJid(String jid);

    @Query("SELECT * FROM Chat where show = 1 and type = :type order by sortPosition, dateCreated desc")
    Flowable<List<Chat>> findByType(int type);

    @Query("UPDATE Chat set unreadMessagesCount=0 where unreadMessagesCount>0 and jid = :jid")
    int resetUnreadMessageCount(String jid);

    @Query("UPDATE Chat set show = :show where jid = :jid")
    int updateVisibilityByJid(String jid, boolean show);

    @Query("UPDATE Chat set show = :show where type = :type")
    int updateVisibilityAll(int type, boolean show);

    @Query("UPDATE Chat set name = :chatName where jid = :jid")
    int updateName(String jid, String chatName);

    @Query("UPDATE Chat set subject = :subject where jid = :jid")
    int updateSubject(String jid, String subject);

    @Query("UPDATE Chat set lastTimestampRetrieved = :timestamp where jid = :jid")
    int updateLastTimestamp(String jid, long timestamp);

    @Query("UPDATE Chat set messageBeingComposed = :message where jid = :jid")
    int updateMessageBeingComposed(String jid, String message);

    @Update
    int update(Chat chat);

    @Update
    int updateItems(List<Chat> chats);

    @Query("DELETE FROM Chat where jid = :jid")
    int deleteByJid(String jid);

    @Insert
    long insert(Chat chat);

    @Query("DELETE FROM Chat")
    void deleteAll();
}
