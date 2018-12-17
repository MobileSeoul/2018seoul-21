package org.seoro.seoro.recyclerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.seoro.seoro.R;

public class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnItemClickListener {
        void onClick(View v, int position);
    }

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private SettingsAdapter.OnItemClickListener mOnItemClickListener;

    public SettingsAdapter(Context context) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        );
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case 0: {
                View view = mLayoutInflater.inflate(
                        R.layout.view_settings_header,
                        parent,
                        false
                );

                return new SettingsHeaderViewHolder(view);
            }
            case 1: {
                View view = mLayoutInflater.inflate(
                        R.layout.view_settings,
                        parent,
                        false
                );

                return new SettingsViewHolder(view);
            }
        }

        View view = mLayoutInflater.inflate(
                R.layout.view_settings,
                parent,
                false
        );

        return new SettingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (position) {
            case 0: {
                SettingsHeaderViewHolder viewHolder = (SettingsHeaderViewHolder) holder;

                viewHolder.getTextView().setText(R.string.header_edit_profile);

                break;
            }
            case 1: {
                SettingsViewHolder viewHolder = (SettingsViewHolder) holder;

                viewHolder.getTextView().setText(R.string.text_view_change_profile_image);
                viewHolder.getItemView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onClick(v, position);
                        }
                    }
                });

                break;
            }
            case 2: {
                SettingsViewHolder viewHolder = (SettingsViewHolder) holder;

                viewHolder.getTextView().setText(R.string.text_view_change_display_name);
                viewHolder.getItemView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onClick(v, position);
                        }
                    }
                });

                break;
            }
            case 3: {
                SettingsHeaderViewHolder viewHolder = (SettingsHeaderViewHolder) holder;

                viewHolder.getTextView().setText(R.string.header_account_security);

                break;
            }
            case 4: {
                SettingsViewHolder viewHolder = (SettingsViewHolder) holder;

                viewHolder.getTextView().setText(R.string.text_view_change_password);
                viewHolder.getItemView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onClick(v, position);
                        }
                    }
                });

                break;
            }
            case 5: {
                SettingsViewHolder viewHolder = (SettingsViewHolder) holder;

                viewHolder.getTextView().setText(R.string.text_view_delete_account);
                viewHolder.getItemView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onClick(v, position);
                        }
                    }
                });

                break;
            }
            case 6: {
                SettingsViewHolder viewHolder = (SettingsViewHolder) holder;

                viewHolder.getTextView().setText(R.string.open_source_license);
                viewHolder.getItemView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onClick(v, position);
                        }
                    }
                });

                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return 7;
    }

    @Override
    public int getItemViewType(int position) {
        switch (position) {
            case 0:
            case 3:
                return 0;
            default:
                return 1;
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }
}
