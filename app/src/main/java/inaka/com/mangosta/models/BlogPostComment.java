package inaka.com.mangosta.models;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class BlogPostComment {

    @PrimaryKey
    @NonNull
    private String id;
    private String content;
    private String authorName;
    private String authorJid;
    private Date published;
    private String blogPostId;

    public BlogPostComment() {
    }

    public BlogPostComment(String id, String blogPostId, String content, String authorName, String authorJid, Date published) {
        this.id = id;
        this.blogPostId = blogPostId;
        this.content = content;
        this.authorJid = authorJid;
        this.authorName = authorName;
        this.published = published;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorJid() {
        return authorJid;
    }

    public void setAuthorJid(String authorJid) {
        this.authorJid = authorJid;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
    }

    public String getBlogPostId() {
        return blogPostId;
    }

    public void setBlogPostId(String blogPostId) {
        this.blogPostId = blogPostId;
    }

}
