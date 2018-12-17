package org.seoro.seoro.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.seoro.seoro.R;

public class SettingsViewHolder extends RecyclerView.ViewHolder {
    private View mItemView;
    private TextView mTextView;

    public SettingsViewHolder(View itemView) {
        super(itemView);

        mItemView = itemView;
        mTextView = itemView.findViewById(R.id.SettingsView_TextView);
    }

    public View getItemView() {
        return mItemView;
    }

    public TextView getTextView() {
        return mTextView;
    }
}
