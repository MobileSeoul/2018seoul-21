package org.seoro.seoro;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.seoro.seoro.model.ImageMemo;

public class TopImageMemoFragment extends Fragment {
    private static final String ARG_PARAM_IMAGE_MEMO = "imageMemo";

    private ImageView mImageView;

    private ImageMemo mImageMemo;

    public TopImageMemoFragment() {
        // Required empty public constructor
    }

    public static TopImageMemoFragment newInstance(ImageMemo imageMemo) {
        TopImageMemoFragment fragment = new TopImageMemoFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAM_IMAGE_MEMO, imageMemo);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mImageMemo = getArguments().getParcelable(ARG_PARAM_IMAGE_MEMO);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_top_image_memo, container, false);
        mImageView = view.findViewById(R.id.TopImageMemoFragment_ImageView);

        Activity activity = getActivity();

        if (activity != null) {
            Glide.with(activity).load(mImageMemo.getImage()).into(mImageView);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
