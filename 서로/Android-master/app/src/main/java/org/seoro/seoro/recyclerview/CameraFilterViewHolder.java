package org.seoro.seoro.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.seoro.seoro.R;

public class CameraFilterViewHolder extends RecyclerView.ViewHolder {
    private View mItemView;
    private TextView mTextView;
    private ImageView mImageView;

    public CameraFilterViewHolder(View itemView) {
        super(itemView);

        mItemView = itemView;
        mImageView = itemView.findViewById(R.id.CameraFilterView_ImageView);
        mTextView = itemView.findViewById(R.id.CameraFilterView_TextView);
    }

    public View getItemView() {
        return mItemView;
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public TextView getTextView() {
        return mTextView;
    }
}
