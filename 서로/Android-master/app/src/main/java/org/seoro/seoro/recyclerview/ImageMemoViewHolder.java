package org.seoro.seoro.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import org.seoro.seoro.R;

public class ImageMemoViewHolder extends RecyclerView.ViewHolder {
    private View mItemView;
    private ImageView mImageView;

    public ImageMemoViewHolder(View itemView) {
        super(itemView);

        mItemView = itemView;
        mImageView = itemView.findViewById(R.id.ImageMemoView_ImageView);
    }

    public View getItemView() {
        return mItemView;
    }

    public ImageView getImageView() {
        return mImageView;
    }
}
