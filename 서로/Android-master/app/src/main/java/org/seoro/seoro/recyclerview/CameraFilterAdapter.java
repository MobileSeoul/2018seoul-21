package org.seoro.seoro.recyclerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.seoro.seoro.R;

import java.util.List;

public class CameraFilterAdapter extends RecyclerView.Adapter<CameraFilterViewHolder> {
    public interface OnItemClickListener {
        void onClick(View v, int position);
    }

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private List<String> mShaderProgramNameList;

    private OnItemClickListener mOnItemClickListener;

    public CameraFilterAdapter(Context context, List<String> shaderProgramNameList) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        );
        mShaderProgramNameList = shaderProgramNameList;
    }

    @NonNull
    @Override
    public CameraFilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CameraFilterViewHolder(
                mLayoutInflater.inflate(R.layout.view_camera_filter, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull CameraFilterViewHolder holder, int position) {
        holder.getTextView().setText(mShaderProgramNameList.get(position));
        holder.getImageView().setOnClickListener(new View.OnClickListener() {
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
        return mShaderProgramNameList.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }
}
