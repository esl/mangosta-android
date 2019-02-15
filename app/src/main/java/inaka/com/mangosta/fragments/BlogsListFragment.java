package inaka.com.mangosta.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.BlogPostsListAdapter;
import inaka.com.mangosta.database.MangostaDatabase;
import inaka.com.mangosta.notifications.BlogPostNotifications;
import inaka.com.mangosta.utils.MangostaApplication;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BlogsListFragment extends BaseFragment {

    @BindView(R.id.blogsRecyclerView)
    RecyclerView blogsRecyclerView;

    @BindView(R.id.socialMediaSwipeRefreshLayout)
    SwipeRefreshLayout socialMediaSwipeRefreshLayout;

    private BlogPostsListAdapter mBlogPostsListAdapter;

    private MangostaDatabase database = MangostaApplication.getInstance().getDatabase();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blogs_list, container, false);
        ButterKnife.bind(this, view);

        blogsRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        blogsRecyclerView.setLayoutManager(layoutManager);

        mBlogPostsListAdapter = new BlogPostsListAdapter(new ArrayList<>(), getActivity());

        blogsRecyclerView.setAdapter(mBlogPostsListAdapter);

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

        addDisposable(database.blogPostDao().getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {}//@TODO mBlogPostsListAdapter.submitList(list))
                        ));

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
