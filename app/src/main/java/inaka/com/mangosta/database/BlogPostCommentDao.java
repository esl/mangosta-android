package inaka.com.mangosta.database;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import inaka.com.mangosta.models.BlogPostComment;
import io.reactivex.Flowable;

@Dao
public interface BlogPostCommentDao {
    @Query("SELECT * from BlogPostComment where blogPostId = :blogPostId")
    Flowable<List<BlogPostComment>> getCommentsForBlogPost(String blogPostId);

    @Update
    int update(BlogPostComment chat);

    @Insert
    long insert(BlogPostComment chat);

    @Query("DELETE FROM BlogPostComment")
    void deleteAll();
}
