package inaka.com.mangosta.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import inaka.com.mangosta.realm.RealmManager;

public class BlogsListFragment extends Fragment {

    @Bind(R.id.blogsRecyclerView)
    RecyclerView blogsRecyclerView;

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
        return view;
    }

    public void loadBlogPosts() {
        mBlogPosts.clear();
        mBlogPosts.addAll(RealmManager.getBlogPosts());
        mBlogPostsListAdapter.notifyDataSetChanged();
    }

}
