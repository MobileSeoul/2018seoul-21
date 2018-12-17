package org.seoro.seoro.recyclerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.seoro.seoro.R;
import org.seoro.seoro.glide.GlideApp;
import org.seoro.seoro.model.EmojiComment;

import java.util.List;

public class EmojiCommentAdapter extends RecyclerView.Adapter<EmojiCommentViewHolder> {
    public interface OnItemClickListener {
        void onClick(View v, int position);
    }

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private EmojiCommentAdapter.OnItemClickListener mOnItemClickListener;

    private List<EmojiComment> mEmojiCommentList;

    public EmojiCommentAdapter(Context context, List<EmojiComment> emojiCommentList) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        );
        mEmojiCommentList = emojiCommentList;
    }

    @NonNull
    @Override
    public EmojiCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(
                R.layout.view_emoji_comment,
                parent,
                false
        );

        return new EmojiCommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiCommentViewHolder holder, int position) {
        EmojiCommentViewHolder viewHolder = (EmojiCommentViewHolder) holder;

        GlideApp.with(mContext)
                .load(mEmojiCommentList.get(position).getUser().getThumbnail())
                .placeholder(R.drawable.placeholder_profile_image)
                .error(R.drawable.placeholder_profile_image)
                .fallback(R.drawable.placeholder_profile_image)
                .into(viewHolder.getCircleImageView());

        viewHolder.getItemView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(v, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mEmojiCommentList.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }
}
