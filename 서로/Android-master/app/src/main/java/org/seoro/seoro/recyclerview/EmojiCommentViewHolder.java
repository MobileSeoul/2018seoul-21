package org.seoro.seoro.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.seoro.seoro.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class EmojiCommentViewHolder extends RecyclerView.ViewHolder {
    private View mItemView;
    private CircleImageView mCircleImageView;
    private ImageView mImageView;
    private TextView mTextView;

    public EmojiCommentViewHolder(View itemView) {
        super(itemView);

        mItemView = itemView;
        mCircleImageView = itemView.findViewById(R.id.EmojiCommentView_CircleImageView);
        mImageView = itemView.findViewById(R.id.EmojiCommentView_ImageView);
        mTextView = itemView.findViewById(R.id.EmojiCommentView_TextView);
    }

    public View getItemView() {
        return mItemView;
    }

    public CircleImageView getCircleImageView() {
        return mCircleImageView;
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public TextView getTextView() {
        return mTextView;
    }
}
