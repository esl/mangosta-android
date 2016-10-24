package inaka.com.mangosta.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import inaka.com.mangosta.R;
import inaka.com.mangosta.models.Event;

public class StickersAdapter extends RecyclerView.Adapter<StickersAdapter.StickerViewHolder> {

    Context mContext;
    List<String> mStickers;

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

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
        }
    }

    public static class StickerViewHolder extends StickersAdapter.ViewHolder {

        @Bind(R.id.stickerImageView)
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
                    EventBus.getDefault().post(new Event(Event.Type.STICKER_SENT, sticker));
                }
            });
        }
    }

}
