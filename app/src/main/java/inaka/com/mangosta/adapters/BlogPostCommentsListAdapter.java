package inaka.com.mangosta.adapters;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.BlogPostComment;
import inaka.com.mangosta.ui.ViewHolderType;
import inaka.com.mangosta.utils.TimeCalculation;

public class BlogPostCommentsListAdapter extends RecyclerView.Adapter<BlogPostCommentsListAdapter.ViewHolder> {

    private List<BlogPostComment> mComments;
    private Context mContext;
    private BlogPost mBlogPost;

    public BlogPostCommentsListAdapter(BlogPost blogPost, List<BlogPostComment> blogPostComments, Context context) {
        this.mBlogPost = blogPost;
        this.mComments = blogPostComments;
        this.mContext = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }

    public static class CommentViewHolder extends BlogPostCommentsListAdapter.ViewHolder {

        @Bind(R.id.imageUserAvatar)
        ImageView imageUserAvatar;

        @Bind(R.id.textUserName)
        TextView textUserName;

        @Bind(R.id.textCommentCreatedAt)
        TextView textCommentCreatedAt;

        @Bind(R.id.textCommentBody)
        TextView textCommentBody;

        @Bind(R.id.layoutEditOrDeleteComment)
        LinearLayout layoutEditOrDeleteComment;

        @Bind(R.id.imageEditComment)
        ImageView imageEditComment;

        @Bind(R.id.imageDeleteComment)
        ImageView imageDeleteComment;

        private Context mContext;
        private BlogPost mBlogPost;

        public CommentViewHolder(View view, Context context, BlogPost blogPost) {
            super(view);
            ButterKnife.bind(this, view);
            this.mContext = context;
            this.mBlogPost = blogPost;
        }

        public void bind(final BlogPostComment comment) {
            textUserName.setText(comment.getAuthorName());
            layoutEditOrDeleteComment.setVisibility(View.INVISIBLE);
            textCommentBody.setText(comment.getContent());
            textCommentCreatedAt.setText(TimeCalculation.getTimeStringAgoSinceDate(mContext, comment.getPublished()));

            String avatarUrl = comment.getAuthorAvatarUrl();
            if (avatarUrl != null) {
                Picasso.with(mContext).load(avatarUrl).noFade().fit().into(imageUserAvatar);
            }
        }

    }

    @Override
    public BlogPostCommentsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BlogPostCommentsListAdapter.ViewHolder viewHolder;
        View viewItemGist = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_item_comment, parent, false);
        viewHolder = new CommentViewHolder(viewItemGist, mContext, mBlogPost);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(BlogPostCommentsListAdapter.ViewHolder holder, int position) {
        BlogPostComment comment = mComments.get(position);
        CommentViewHolder postViewHolder = (CommentViewHolder) holder;
        postViewHolder.bind(comment);
    }

    @Override
    public int getItemViewType(int position) {
        return ViewHolderType.VIEW_TYPE_BLOG_POST_ITEM;
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

}
