package inaka.com.mangosta.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.message_correct.element.MessageCorrectExtension;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.database.MangostaDatabase;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.utils.TimeCalculation;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder> {

    private final int VIEW_TYPE_CHAT_MESSAGE = 0;
    private final int VIEW_TYPE_CHAT_ME_MESSAGE = 1;
    private final int VIEW_TYPE_STICKER_MESSAGE = 2;
    private final int VIEW_TYPE_UNREAD_MESSAGES = 3;

    private MangostaDatabase database = MangostaApplication.getInstance().getDatabase();

    private Context mContext;
    private List<ChatMessage> mMessages;
    private Chat mChat;

    public ChatMessagesAdapter(Context context, List<ChatMessage> messages, Chat chat) {
        mContext = context;
        mMessages = messages;
        mChat = chat;
    }

    @Override
    public int getItemViewType(int position) {
        int viewType = 0;

        if (position >= 0) {
            ChatMessage chatMessage = mMessages.get(position);

            switch (chatMessage.getType()) {
                case ChatMessage.TYPE_HEADER:
                    viewType = VIEW_TYPE_UNREAD_MESSAGES;
                    break;

                case ChatMessage.TYPE_CHAT:
                    if (chatMessage.isMeMessage()) {
                        viewType = VIEW_TYPE_CHAT_ME_MESSAGE;
                    } else {
                        viewType = VIEW_TYPE_CHAT_MESSAGE;
                    }
                    break;

                case ChatMessage.TYPE_STICKER:
                    viewType = VIEW_TYPE_STICKER_MESSAGE;
                    break;
            }
        }

        return viewType;
    }

    public void updateList(List<ChatMessage> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatMessagesAdapter.ChatMessagesDiffCallback(this.mMessages, newList));
        diffResult.dispatchUpdatesTo(this);
        this.mMessages = newList;
    }

    @Override
    public ChatMessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ChatMessagesAdapter.ViewHolder viewHolder;

        switch (viewType) {
            case VIEW_TYPE_CHAT_MESSAGE:
                View viewMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_item_message, parent, false);
                viewHolder = new MessageViewHolder(viewMessage, mContext);
                break;

            case VIEW_TYPE_CHAT_ME_MESSAGE:
                View viewMeMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_item_me_message, parent, false);
                viewHolder = new MeMessageViewHolder(viewMeMessage);
                break;

            case VIEW_TYPE_STICKER_MESSAGE:
                View viewStickerMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_item_sticker_message, parent, false);
                viewHolder = new StickerMessageViewHolder(viewStickerMessage, mContext);
                break;

            case VIEW_TYPE_UNREAD_MESSAGES:
                View viewUnreadMessages = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_item_unread_messages, parent, false);
                viewHolder = new UnreadMessagesViewHolder(viewUnreadMessages, mContext);
                break;

            default:
                viewHolder = null;
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ChatMessagesAdapter.ViewHolder holder, int position) {
        if (position >= 0) {
            ChatMessage chatMessage = mMessages.get(position);

            switch (chatMessage.getType()) {
                case ChatMessage.TYPE_CHAT:
                    if (chatMessage.isMeMessage()) {
                        ((MeMessageViewHolder) holder).bind(chatMessage);
                    } else {
                        ((MessageViewHolder) holder).bind(chatMessage, position == mMessages.size() - 1);
                    }
                    break;

                case ChatMessage.TYPE_STICKER:
                    ((StickerMessageViewHolder) holder).bind(chatMessage);
                    break;

                case ChatMessage.TYPE_HEADER:
                    ((UnreadMessagesViewHolder) holder).bind(mChat.getUnreadMessagesCount());
            }

            if (chatMessage.isUnread()) {
                Disposable d = Single.fromCallable(() -> database.chatMessageDao().updateUnread(false, chatMessage.getMessageId()))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(updated -> {
                            if (updated > 0) chatMessage.setUnread(true);
                        });
            }
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
        }
    }

    public class MessageViewHolder extends ChatMessagesAdapter.ViewHolder {

        @BindView(R.id.messageSenderTextView)
        TextView messageSenderTextView;

        @BindView(R.id.messageCreatedAtTextView)
        TextView messageCreatedAtTextView;

        @BindView(R.id.messageContentTextView)
        TextView messageContentTextView;

        @BindView(R.id.messageLayout)
        LinearLayout messageLayout;

        @BindView(R.id.messageMainLayout)
        LinearLayout messageMainLayout;

        @BindView(R.id.imageEditMessage)
        ImageView imageEditMessage;

        private Context mContext;

        private MessageViewHolder(View view, Context context) {
            super(view);
            ButterKnife.bind(this, view);
            this.mContext = context;
        }

        public void bind(final ChatMessage chatMessage, boolean isLastOne) {

            if (chatMessage == null) {
                return;
            }

            messageCreatedAtTextView.setText(TimeCalculation.getTimeStringAgoSinceDate(mContext, chatMessage.getDate()));
            messageContentTextView.setText(chatMessage.getContent());
            messageSenderTextView.setText(chatMessage.getUserSender());
            imageEditMessage.setVisibility(View.GONE);

            settingsDependingOnSender(chatMessage, isLastOne);

            imageEditMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    correctMessage(chatMessage);
                }
            });
        }

        private void settingsDependingOnSender(ChatMessage chatMessage, boolean isLastOne) {
            LinearLayout.LayoutParams paramsGravity = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            LinearLayout.LayoutParams paramsMargins = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            Preferences preferences = Preferences.getInstance();
            boolean messageByUser = chatMessage.getUserSender().equals(XMPPUtils.fromJIDToUserName(preferences.getUserXMPPJid()));

            if (messageByUser) {
                paramsMargins.setMargins(5, 5, 25, 5);
                messageLayout.setBackground(mContext.getResources().getDrawable(R.drawable.balloon_outgoing_normal));
                paramsGravity.gravity = Gravity.END;
                if (messageIsLastOneSentByMe(chatMessage) && isLastOne && TimeCalculation.wasMinutesAgoMax(chatMessage.getDate(), 20)) {
                    imageEditMessage.setVisibility(View.VISIBLE);
                }
            } else {
                paramsMargins.setMargins(25, 5, 5, 5);
                messageLayout.setBackground(mContext.getResources().getDrawable(R.drawable.balloon_incoming_normal));
                paramsGravity.gravity = Gravity.START;
            }

            messageLayout.setLayoutParams(paramsGravity);
            messageMainLayout.setLayoutParams(paramsMargins);
        }

        private void correctMessage(final ChatMessage chatMessage) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            final View viewFixMessage = inflater.inflate(R.layout.view_dialog_fix_message, null);

            final EditText messageEditText = (EditText) viewFixMessage.findViewById(R.id.fixMessageEditText);
            messageEditText.setText(chatMessage.getContent());

            AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setTitle(mContext.getString(R.string.correct_message))
                    .setMessage(mContext.getString(R.string.enter_new_message))
                    .setView(viewFixMessage)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                // prepare correction message
                                Jid roomJid = JidCreate.from(chatMessage.getRoomJid());
                                Message message = new Message(roomJid, messageEditText.getText().toString());

                                // add message correction extension
                                message.addExtension(new MessageCorrectExtension(chatMessage.getMessageId()));

                                if (mChat.getType() == Chat.TYPE_1_T0_1) {
                                    message.setType(Message.Type.chat);
                                } else {
                                    message.setType(Message.Type.groupchat);
                                }

                                // send correction message
                                XMPPSession.getInstance().sendStanza(message);

                                // update message if 1 to 1 chat
                                if (mChat.getType() == Chat.TYPE_1_T0_1) {
                                    Completable.fromAction(() -> database.chatMessageDao().updateContent(message.getBody(), message.getStanzaId()))
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe();
                                }
                            } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
        }
    }

    public static class MeMessageViewHolder extends ChatMessagesAdapter.ViewHolder {

        @BindView(R.id.meMessageContentTextView)
        TextView meMessageContentTextView;

        private MeMessageViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        public void bind(ChatMessage chatMessage) {
            if (chatMessage == null) {
                return;
            }
            meMessageContentTextView.setText(chatMessage.getMeContent());
        }

    }

    public static class StickerMessageViewHolder extends ChatMessagesAdapter.ViewHolder {

        @BindView(R.id.stickerSenderTextView)
        TextView stickerSenderTextView;

        @BindView(R.id.stickerCreatedAtTextView)
        TextView stickerCreatedAtTextView;

        @BindView(R.id.stickerImageView)
        ImageView stickerImageView;

        @BindView(R.id.stickerLayout)
        LinearLayout stickerLayout;

        @BindView(R.id.stickerMainLayout)
        LinearLayout stickerMainLayout;

        private Context mContext;

        private StickerMessageViewHolder(View view, Context context) {
            super(view);
            ButterKnife.bind(this, view);
            this.mContext = context;
        }

        public void bind(ChatMessage chatMessage) {
            if (chatMessage == null) {
                return;
            }

            stickerCreatedAtTextView.setText(TimeCalculation.getTimeStringAgoSinceDate(mContext, chatMessage.getDate()));
            stickerSenderTextView.setText(chatMessage.getUserSender());

            String drawableName = "sticker_" + chatMessage.getContent();
            Resources resources = mContext.getResources();
            final int resourceId = resources.getIdentifier(drawableName, "drawable", mContext.getPackageName());

            if (resourceId != 0) {
                Picasso.with(mContext).load(resourceId).noFade().fit().into(stickerImageView);
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            LinearLayout.LayoutParams paramsMargins = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            Preferences preferences = Preferences.getInstance();
            boolean messageByUser = chatMessage.getUserSender().equals(XMPPUtils.fromJIDToUserName(preferences.getUserXMPPJid()));

            if (messageByUser) {
                paramsMargins.setMargins(0, 5, 15, 5);
                params.gravity = Gravity.END;
                stickerLayout.setBackground(mContext.getResources().getDrawable(R.drawable.balloon_outgoing_normal));
            } else {
                paramsMargins.setMargins(15, 5, 0, 5);
                params.gravity = Gravity.START;
                stickerLayout.setBackground(mContext.getResources().getDrawable(R.drawable.balloon_incoming_normal));
            }

            stickerLayout.setLayoutParams(params);
            stickerMainLayout.setLayoutParams(paramsMargins);
        }

    }

    public static class UnreadMessagesViewHolder extends ChatMessagesAdapter.ViewHolder {

        @BindView(R.id.unreadMessagesContentTextView)
        TextView unreadMessagesContentTextView;

        private Context mContext;

        private UnreadMessagesViewHolder(View view, Context context) {
            super(view);
            mContext = context;
            ButterKnife.bind(this, view);
        }

        public void bind(int count) {
            String unreadMessagesString = mContext.getResources().getQuantityString(R.plurals.unread_messages, count);
            unreadMessagesContentTextView.setText(String.format(Locale.getDefault(), unreadMessagesString, count));
        }

    }

    private static boolean messageIsLastOneSentByMe(ChatMessage chatMessage) {
        return true; //@TODO RealmManager.getInstance().getLastMessageSentByMeForChat(chatMessage.getRoomJid()).getMessageId().equals(chatMessage.getMessageId());
    }


    public class ChatMessagesDiffCallback extends DiffUtil.Callback {
        private List<ChatMessage> mOldList;
        private List<ChatMessage> mNewList;

        public ChatMessagesDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) {
            this.mOldList = oldList;
            this.mNewList = newList;
        }

        @Override
        public int getOldListSize() {
            return mOldList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mNewList.get(newItemPosition).getType() == mOldList.get(oldItemPosition).getType()
                    && Objects.equals(mNewList.get(newItemPosition).getMessageId(), mOldList.get(oldItemPosition).getMessageId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return Objects.equals(mNewList.get(newItemPosition).getContent(), mOldList.get(oldItemPosition).getContent())
                    && Objects.equals(mNewList.get(newItemPosition).getDate(), mOldList.get(oldItemPosition).getDate());
        }
    }
}
