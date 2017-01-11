package inaka.com.mangosta.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.BlogPostsListAdapter;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.notifications.BlogPostNotifications;
import inaka.com.mangosta.realm.RealmManager;

public class BlogsListFragment extends Fragment {

    @Bind(R.id.blogsRecyclerView)
    RecyclerView blogsRecyclerView;

    @Bind(R.id.socialMediaSwipeRefreshLayout)
    SwipeRefreshLayout socialMediaSwipeRefreshLayout;

    List<BlogPost> mBlogPosts;
    private BlogPostsListAdapter mBlogPostsListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blogs_list, container, false);
        ButterKnife.bind(this, view);

        blogsRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        blogsRecyclerView.setLayoutManager(layoutManager);

        mBlogPosts = new ArrayList<>();
        mBlogPostsListAdapter = new BlogPostsListAdapter(mBlogPosts, getActivity());

        blogsRecyclerView.setAdapter(mBlogPostsListAdapter);

        loadBlogPosts();

        socialMediaSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        socialMediaSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadBlogPosts();
            }
        });

        return view;
    }

    public void loadBlogPosts() {
        if (socialMediaSwipeRefreshLayout != null && !socialMediaSwipeRefreshLayout.isRefreshing()) {
            socialMediaSwipeRefreshLayout.setRefreshing(true);
        }

        if (mBlogPosts != null && mBlogPostsListAdapter != null) {
            mBlogPosts.clear();
            mBlogPosts.addAll(RealmManager.getInstance().getBlogPosts());
            mBlogPostsListAdapter.notifyDataSetChanged();
        }

        if (socialMediaSwipeRefreshLayout != null) {
            socialMediaSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBlogPosts();
        BlogPostNotifications.cancelBlogPostNotifications(getActivity());
    }

}
