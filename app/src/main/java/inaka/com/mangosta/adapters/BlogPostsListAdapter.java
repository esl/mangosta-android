package inaka.com.mangosta.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.BlogPostDetailsActivity;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.ui.ViewHolderType;
import inaka.com.mangosta.utils.TimeCalculation;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class BlogPostsListAdapter extends RecyclerView.Adapter<BlogPostsListAdapter.ViewHolder> {

    private List<BlogPost> mBlogPosts;
    private boolean mIsLoading;
    private Context mContext;

    public BlogPostsListAdapter(List<BlogPost> blogPosts, Context context) {
        this.mBlogPosts = blogPosts;
        this.mContext = context;
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    public void setIsLoading(boolean isLoading) {
        this.mIsLoading = isLoading;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View view) {
            super(view);
        }
    }

    public static class BlogPostViewHolder extends BlogPostsListAdapter.ViewHolder {

        @BindView(R.id.blogPostOwnerTextView)
        TextView blogPostOwnerTextView;

        @BindView(R.id.blogPostDateTextView)
        TextView blogPostDateTextView;

        @BindView(R.id.blogPostContentTextView)
        TextView blogPostContentTextView;

        private Context mContext;
        private View mView;

        public BlogPostViewHolder(View view, Context context) {
            super(view);
            ButterKnife.bind(this, view);
            this.mContext = context;
            this.mView = view;
        }

        public void bind(final BlogPost blogPost) {
            blogPostOwnerTextView.setText(XMPPUtils.fromJIDToUserName(blogPost.getOwnerJid()));
            blogPostContentTextView.setText(blogPost.getContent());
            blogPostDateTextView.setText(TimeCalculation.getTimeStringAgoSinceDate(mContext, blogPost.getUpdated()));

            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    goToBlogPostDetails(blogPost);
                }
            });
        }

        public void goToBlogPostDetails(BlogPost blogPost) {
            Intent blogPostActivityIntent = new Intent(mContext, BlogPostDetailsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putParcelable(BlogPostDetailsActivity.BLOG_POST_PARAMETER, blogPost);
            blogPostActivityIntent.putExtras(bundle);
            mContext.startActivity(blogPostActivityIntent);
        }

    }

    public static class ProgressViewHolder extends BlogPostsListAdapter.ViewHolder {

        @BindView(R.id.progressLoadingItem)
        ProgressBar progressBar;

        public ProgressViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BlogPostsListAdapter.ViewHolder viewHolder;

        View viewItemLoading = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_item_loading, parent, false);

        View viewItemBlogPost = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_item_blog_post, parent, false);

        switch (viewType) {
            case ViewHolderType.VIEW_TYPE_BLOG_POST_ITEM:
                viewHolder = new BlogPostViewHolder(viewItemBlogPost, mContext);
                break;

            case ViewHolderType.VIEW_TYPE_PROGRESS:
                viewHolder = new ProgressViewHolder(viewItemLoading);
                break;

            default:
                viewHolder = new ProgressViewHolder(viewItemLoading);
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder instanceof BlogPostViewHolder) {
            BlogPostViewHolder blogPostViewHolder = (BlogPostViewHolder) holder;
            BlogPost blogPost = mBlogPosts.get(position);
            blogPostViewHolder.bind(blogPost);

        } else {
            ProgressViewHolder progressViewHolder = (ProgressViewHolder) holder;
            progressViewHolder.progressBar.setIndeterminate(true);

            if (isLoading()) {
                progressViewHolder.progressBar.setVisibility(View.VISIBLE);
            } else {
                progressViewHolder.progressBar.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= mBlogPosts.size()) {
            return ViewHolderType.VIEW_TYPE_PROGRESS;
        } else {
            return ViewHolderType.VIEW_TYPE_BLOG_POST_ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return mBlogPosts.size() + 1;
    }

}
