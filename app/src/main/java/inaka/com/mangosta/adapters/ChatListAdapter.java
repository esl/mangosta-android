package inaka.com.mangosta.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.jivesoftware.smack.packet.Presence;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.ui.ViewHolderType;
import inaka.com.mangosta.ui.itemTouchHelper.ItemTouchHelperAdapter;
import inaka.com.mangosta.ui.itemTouchHelper.ItemTouchHelperViewHolder;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    private List<Chat> mChats;
    private Context mContext;
    private ChatClickListener mChatClickListener;

    public ChatListAdapter(List<Chat> chats, Context context, ChatClickListener chatClickListener) {
        this.mChats = chats;
        this.mContext = context;
        this.mChatClickListener = chatClickListener;
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(mChats, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        RealmManager.getInstance().updateChatsSortPosition(mChats);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        mChats.remove(position);
        notifyItemRemoved(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }

    public static class ChatViewHolder extends ChatListAdapter.ViewHolder
            implements ItemTouchHelperViewHolder {

        @Bind(R.id.chatNameTextView)
        TextView chatNameTextView;

        @Bind(R.id.chatMessageTextView)
        TextView chatMessageTextView;

        @Bind(R.id.chatImageView)
        ImageView chatImageView;

        @Bind(R.id.connectionStatusImageView)
        ImageView connectionStatusImageView;

        private Context mContext;
        private View mView;
        private ChatClickListener mChatClickListener;

        public ChatViewHolder(View view, Context context, ChatClickListener chatClickListener) {
            super(view);
            ButterKnife.bind(this, view);
            this.mContext = context;
            this.mView = view;
            this.mChatClickListener = chatClickListener;
        }

        public void bind(final Chat chat) {

            if (chat == null || !chat.isValid()) {
                return;
            }

            chatNameTextView.setText(XMPPUtils.getChatName(chat));

            String jid = chat.getJid();
            ChatMessage chatMessage = RealmManager.getInstance().getLastMessageForChat(jid);

            if (chatMessage != null) {
                manageLastMessage(chatMessage);
            } else {
                chatMessageTextView.setText("");
            }

            if (chat.getType() == Chat.TYPE_1_T0_1) { // 1 to 1
                assignChatImage(chat);
                connectionStatusImageView.setVisibility(View.VISIBLE);
                assignConnectionStatusImage(chat);
            } else { // group chat
                Picasso.with(mContext).load(R.mipmap.ic_groupchat).noFade().fit().into(chatImageView);
                connectionStatusImageView.setVisibility(View.GONE);
            }

            this.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mChatClickListener != null) {
                        mChatClickListener.onChatClicked(chat);
                    }
                }
            });

        }

        private void assignConnectionStatusImage(Chat chat) {
            String userName = XMPPUtils.fromJIDToUserName(chat.getJid());
            if (RosterManager.getInstance().getStatusFromFriend(userName).equals(Presence.Type.available)) {
                connectionStatusImageView.setImageResource(R.mipmap.ic_connected);
            } else {
                connectionStatusImageView.setImageResource(R.mipmap.ic_disconnected);
            }
        }

        private void assignChatImage(Chat chat) {
            if (chat.getImageUrl() != null) {
                Picasso.with(mContext).load(chat.getImageUrl()).noFade().fit().into(chatImageView);
            } else {
                Picasso.with(mContext).load(R.mipmap.ic_user).noFade().fit().into(chatImageView);
            }
        }

        private void manageLastMessage(ChatMessage chatMessage) {
            switch (chatMessage.getType()) {
                case ChatMessage.TYPE_CHAT:
                    if (chatMessage.isMeMessage()) {
                        chatMessageTextView.setText(chatMessage.getMeContent());
                    } else {
                        chatMessageTextView.setText(String.format(Locale.getDefault(), mContext.getString(R.string.chat_last_message),
                                chatMessage.getUserSender(), chatMessage.getContent()));
                    }
                    break;

                case ChatMessage.TYPE_STICKER:
                    chatMessageTextView.setText(String.format(Locale.getDefault(), mContext.getString(R.string.chat_last_message_sticker),
                            chatMessage.getUserSender()));
                    break;
            }

        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(mContext.getResources().getColor(R.color.colorPrimaryLight));
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(mContext.getResources().getColor(R.color.background_chat));
        }

    }

    @Override
    public ChatListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View viewItemChat = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_item_chat, parent, false);
        return new ChatViewHolder(viewItemChat, mContext, mChatClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatViewHolder chatViewHolder = (ChatViewHolder) holder;
        chatViewHolder.bind(mChats.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        return ViewHolderType.VIEW_TYPE_CHAT_ITEM;
    }

    @Override
    public int getItemCount() {
        return mChats.size();
    }

    public interface ChatClickListener {
        void onChatClicked(Chat chat);
    }

}
