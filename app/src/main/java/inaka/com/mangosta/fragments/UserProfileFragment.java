package inaka.com.mangosta.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.models.User;

public class UserProfileFragment extends Fragment {

    @Bind(R.id.textBioUserProfile)
    TextView textBioUserProfile;

    @Bind(R.id.textFollowersCount)
    TextView textFollowersCount;

    @Bind(R.id.textFollowingCount)
    TextView textFollowingCount;

    @Bind(R.id.layoutFollowers)
    LinearLayout layoutFollowers;

    @Bind(R.id.layoutFollowing)
    LinearLayout layoutFollowing;

    User mUser;
    boolean mIsAuthenticatedUser;


    public static UserProfileFragment newInstance(User user, boolean isAuthenticatedUser) {
        Bundle args = new Bundle();
        args.putParcelable("user", user);
        args.putBoolean("isAuthenticated", isAuthenticatedUser);

        UserProfileFragment fragment = new UserProfileFragment();
        fragment.setArguments(args);

        return fragment;
    }


    public UserProfileFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_user_bio, container, false);
        ButterKnife.bind(this, rootView);

        mUser = getArguments().getParcelable("user");
        mIsAuthenticatedUser = getArguments().getBoolean("auth_user");

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        bind();
    }

    public void setUser(User user) {
        mUser = user;
    }

    public void bind() {
        if (mUser != null) {
            textBioUserProfile.setText(mUser.getBio());
            textFollowersCount.setText(String.valueOf(mUser.getFollowers()));
            textFollowingCount.setText(String.valueOf(mUser.getFollowing()));

            layoutFollowers.setClickable(mUser.getFollowers() > 0);
            layoutFollowing.setClickable(mUser.getFollowing() > 0);
        }
    }

    private void createUsersListDialog(int type, String title) {
        DialogFragment fragment = UsersDialogFragment.newInstance();

        Bundle bundle = new Bundle();
        bundle.putParcelable("user", mUser);
        bundle.putString("title", title);
        bundle.putInt("type", type);
        fragment.setArguments(bundle);

        fragment.show(getChildFragmentManager(), title);
    }
}
