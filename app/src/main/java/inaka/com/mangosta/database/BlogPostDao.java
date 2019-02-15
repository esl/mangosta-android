package inaka.com.mangosta.database;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.Chat;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

@Dao
public interface BlogPostDao {
    @Query("SELECT * from BlogPost order by updated desc")
    Flowable<BlogPost> getAll();

    @Query("SELECT * from BlogPost where id = :id")
    Maybe<BlogPost> getById(String id);

    @Update
    int update(BlogPost chat);

    @Insert
    long insert(BlogPost chat);

    @Query("DELETE FROM BlogPost")
    void deleteAll();
}
