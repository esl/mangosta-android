package inaka.com.mangosta.adapters;

import android.content.Context;
import android.content.res.Resources;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.models.event.SendEvent;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class StickersAdapter extends RecyclerView.Adapter<StickersAdapter.StickerViewHolder> {

    private Context mContext;
    private List<String> mStickers;
    private PublishSubject<SendEvent> stickerSendSubject = PublishSubject.create();

    public StickersAdapter(Context context, List<String> stickersNameList) {
        mContext = context;
        mStickers = stickersNameList;
    }

    @Override
    public StickerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View viewSticker = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_item_sticker, parent, false);
        return new StickerViewHolder(viewSticker, mContext);
    }

    @Override
    public void onBindViewHolder(StickerViewHolder holder, int position) {
        String sticker = mStickers.get(position);
        holder.bind(sticker);
    }

    @Override
    public int getItemCount() {
        return mStickers.size();
    }

    private void sendSticker(String sticker) {
        stickerSendSubject.onNext(new SendEvent(SendEvent.Type.SEND_STICKER, sticker));
    }

    public Observable<SendEvent> getStickerSentObservable() {
        return stickerSendSubject;
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
        }
    }

    public class StickerViewHolder extends StickersAdapter.ViewHolder {

        @BindView(R.id.stickerImageView)
        ImageView stickerImageView;

        private Context mContext;

        private StickerViewHolder(View view, Context context) {
            super(view);
            ButterKnife.bind(this, view);
            this.mContext = context;
        }

        public void bind(final String sticker) {
            String drawableName = "sticker_" + sticker;
            Resources resources = mContext.getResources();
            final int resourceId = resources.getIdentifier(drawableName, "drawable", mContext.getPackageName());
            Picasso.with(mContext).load(resourceId).noFade().fit().into(stickerImageView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendSticker(sticker);
                }
            });
        }
    }

}
