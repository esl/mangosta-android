package inaka.com.mangosta.fragments;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
import inaka.com.mangosta.adapters.UsersListAdapter;
import inaka.com.mangosta.models.User;

public class UsersDialogFragment extends DialogFragment {

    @Bind(R.id.recyclerviewUsers)
    RecyclerView recyclerviewUsers;

    public static UsersDialogFragment newInstance() {
        return new UsersDialogFragment();
    }

    private User mUser;
    private List<User> mUsers;
    private int mDialogType;
    private UsersListAdapter mUsersListAdapter;

    private int mVisibleItemCount;
    private int mTotalItemCount;
    private int mFirstVisibleItem;

    final private int VISIBLE_BEFORE_LOAD = 12;
    final private int ITEMS_PER_PAGE = 30;

    private int mPagesLoaded;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users_dialog, container, false);
        ButterKnife.bind(this, view);

        Bundle bundle = getArguments();
        mUser = bundle.getParcelable("user");
        mDialogType = bundle.getInt("type");
        String title = bundle.getString("title");

        mPagesLoaded = 1;
        mUsers = new ArrayList<>();
        getDialog().setTitle(title);

        final LinearLayoutManager layoutManager = new org.solovyev.android.views.llm.LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false);
        recyclerviewUsers.setLayoutManager(layoutManager);

        mUsersListAdapter = new UsersListAdapter(getActivity(), mUsers, false, false);
        recyclerviewUsers.setAdapter(mUsersListAdapter);

        recyclerviewUsers.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mFirstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                if (dy > 0) {
                    mVisibleItemCount = recyclerView.getChildCount();
                    mTotalItemCount = layoutManager.getItemCount();

                    if (!mUsersListAdapter.isLoading() && ((mTotalItemCount - mVisibleItemCount) <= (mFirstVisibleItem + VISIBLE_BEFORE_LOAD))) {
                        mUsersListAdapter.setIsLoading(true);

                    }
                }
            }
        });

        return view;
    }

    private void onLoadUsersSuccess(List<User> users) {
        mUsers.addAll(users);
        mUsersListAdapter.notifyDataSetChanged();
        mUsersListAdapter.setIsLoading(false);
        mPagesLoaded++;
    }

}
