package inaka.com.mangosta.adapters;


import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.models.BlogPostComment;
import inaka.com.mangosta.ui.ViewHolderType;
import inaka.com.mangosta.utils.TimeCalculation;

public class BlogPostCommentsListAdapter extends RecyclerView.Adapter<BlogPostCommentsListAdapter.ViewHolder> {

    private List<BlogPostComment> mComments;
    private Context mContext;

    public BlogPostCommentsListAdapter(List<BlogPostComment> blogPostComments, Context context) {
        this.mComments = blogPostComments;
        this.mContext = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }

    public static class CommentViewHolder extends BlogPostCommentsListAdapter.ViewHolder {

        @BindView(R.id.textUserName)
        TextView textUserName;

        @BindView(R.id.textCommentCreatedAt)
        TextView textCommentCreatedAt;

        @BindView(R.id.textCommentBody)
        TextView textCommentBody;

        private Context mContext;

        public CommentViewHolder(View view, Context context) {
            super(view);
            ButterKnife.bind(this, view);
            this.mContext = context;
        }

        public void bind(final BlogPostComment comment) {
            textUserName.setText(comment.getAuthorName());
            textCommentBody.setText(comment.getContent());
            textCommentCreatedAt.setText(TimeCalculation.getTimeStringAgoSinceDate(mContext, comment.getPublished()));
        }

    }

    @Override
    public BlogPostCommentsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BlogPostCommentsListAdapter.ViewHolder viewHolder;
        View viewItemGist = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_item_comment, parent, false);
        viewHolder = new CommentViewHolder(viewItemGist, mContext);
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
