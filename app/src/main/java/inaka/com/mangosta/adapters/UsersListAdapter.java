package inaka.com.mangosta.adapters;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.EntityBareJid;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.models.event.UserEvent;
import inaka.com.mangosta.utils.NavigateToUserProfile;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class UsersListAdapter extends RecyclerView.Adapter<UsersListAdapter.ViewHolder> {

    private PublishSubject<UserEvent> userEventSubject = PublishSubject.create();

    private List<User> mUsers;
    private Context mContext;
    private boolean mIsLoading;
    private boolean mShowAdd;
    private boolean mShowRemove;

    public UsersListAdapter(Context context, List<User> users, boolean showAdd, boolean showRemove) {
        this.mContext = context;
        this.mUsers = users;
        this.mShowAdd = showAdd;
        this.mShowRemove = showRemove;
    }

    public Observable<UserEvent> getEventObservable() {
        return userEventSubject;
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    public void setIsLoading(boolean isLoading) {
        this.mIsLoading = isLoading;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View view) {
            super(view);
        }
    }

    public class UserViewHolder extends UsersListAdapter.ViewHolder {
        @BindView(R.id.imageUserAvatar)
        ImageView imageUserAvatar;

        @BindView(R.id.textUserLogin)
        TextView textUserLogin;

        @BindView(R.id.textUserName)
        TextView textUserName;

        @BindView(R.id.addUserButton)
        Button addUserButton;

        @BindView(R.id.removeUserButton)
        Button removeUserButton;

        @BindView(R.id.imageConnectionStatus)
        ImageView imageConnectionStatus;

        private Context mContext;

        public UserViewHolder(View view, Context context) {
            super(view);
            this.mContext = context;
            ButterKnife.bind(this, view);
        }

        public void bind(final User user, boolean showAdd, boolean showRemove) {
            Picasso.with(mContext).load(R.mipmap.ic_user).noFade().fit().into(imageUserAvatar);
            textUserLogin.setText(XMPPUtils.fromJIDToUserName(user.getJid()));

            if (user.getName() != null) {
                textUserName.setText(user.getName());
            } else {
                textUserName.setText("");
            }

            if (isAuthorizedXMPPUser(user)) {
                addUserButton.setVisibility(View.INVISIBLE);
                removeUserButton.setVisibility(View.INVISIBLE);
            } else {
                if (showAdd) {
                    addUserButton.setVisibility(View.VISIBLE);
                } else {
                    addUserButton.setVisibility(View.INVISIBLE);
                }

                if (showRemove) {
                    removeUserButton.setVisibility(View.VISIBLE);
                } else {
                    removeUserButton.setVisibility(View.INVISIBLE);
                }
            }

            addUserButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    userEventSubject.onNext(new UserEvent(UserEvent.Type.ADD_USER, user));
                }
            });

            removeUserButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    userEventSubject.onNext(new UserEvent(UserEvent.Type.REMOVE_USER, user));
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NavigateToUserProfile.go(user, mContext);
                }
            });

            if (XMPPUtils.isAuthenticatedUser(user) || isUserConnected(user)) {
                imageConnectionStatus.setImageResource(R.mipmap.ic_connected);
            } else {
                imageConnectionStatus.setImageResource(R.mipmap.ic_disconnected);
            }

        }

    }

    private static boolean isUserConnected(User user) {
        return RosterManager.getInstance().getStatusFromContact(user.getJid()).equals(Presence.Type.available);
    }

    private static boolean isAuthorizedXMPPUser(User user) {
        EntityBareJid loginJid = XMPPSession.getInstance().getUser();
        return loginJid != null && loginJid.equals(user.getJid());
    }

    public class ProgressViewHolder extends UsersListAdapter.ViewHolder {

        @BindView(R.id.progressLoadingItem)
        ProgressBar progressBar;

        public ProgressViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View viewItemUser = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_item_user, parent, false);
        return new UserViewHolder(viewItemUser, mContext);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        if (holder instanceof UserViewHolder) {
            User user = mUsers.get(position);
            UserViewHolder userViewHolder = (UserViewHolder) holder;
            userViewHolder.bind(user, mShowAdd, mShowRemove);
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
    public int getItemCount() {
        return mUsers.size();
    }
}
