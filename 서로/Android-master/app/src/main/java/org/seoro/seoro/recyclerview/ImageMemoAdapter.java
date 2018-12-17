package org.seoro.seoro.recyclerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import org.seoro.seoro.R;
import org.seoro.seoro.model.ImageMemo;

import java.util.List;

public class ImageMemoAdapter extends RecyclerView.Adapter<ImageMemoViewHolder> {
    public interface OnItemClickListener {
        void onClick(View v, int position);
    }

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private ImageMemoAdapter.OnItemClickListener mOnItemClickListener;

    private List<ImageMemo> mImageMemoList;

    public ImageMemoAdapter(Context context, List<ImageMemo> imageMemoList) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        );
        mImageMemoList = imageMemoList;
    }

    @NonNull
    @Override
    public ImageMemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(
                R.layout.view_image_memo,
                parent,
                false
        );

        return new ImageMemoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageMemoViewHolder holder, int position) {
        ImageMemoViewHolder viewHolder = (ImageMemoViewHolder) holder;

        Glide.with(mContext)
                .load(mImageMemoList.get(position).getThumbnail())
                .into(viewHolder.getImageView());

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
        return mImageMemoList.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }
}
