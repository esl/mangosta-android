package inaka.com.mangosta.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.BlogPostComment;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;

@Database(entities = {Chat.class, ChatMessage.class, BlogPost.class, BlogPostComment.class},
        version = 1)
@TypeConverters({DateTypeConverter.class})
public abstract class MangostaDatabase extends RoomDatabase {

    public abstract ChatDao chatDao();

    public abstract ChatMessageDao chatMessageDao();

    public abstract BlogPostDao blogPostDao();

    public abstract BlogPostCommentDao blogPostCommentDao();
}
