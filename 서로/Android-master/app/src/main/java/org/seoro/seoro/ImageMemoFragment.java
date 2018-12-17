package org.seoro.seoro;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.seoro.seoro.model.ImageMemo;


public class ImageMemoFragment extends ExpandingFragment {
    private static final String ARG_PARAM_IMAGE_MEMO = "imageMemo";

    private ImageMemo mImageMemo;

    private TopImageMemoFragment mTopImageMemoFragment;
    private BottomImageMemoFragment mBottomImageMemoFragment;

    public ImageMemoFragment() {
        // Required empty public constructor
    }

    public static ImageMemoFragment newInstance(ImageMemo imageMemo) {
        ImageMemoFragment fragment = new ImageMemoFragment();
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

            mTopImageMemoFragment = TopImageMemoFragment.newInstance(mImageMemo);
            mBottomImageMemoFragment = BottomImageMemoFragment.newInstance(mImageMemo);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public Fragment getFragmentTop() {
        return mTopImageMemoFragment;
    }

    @Override
    public Fragment getFragmentBottom() {
        return mBottomImageMemoFragment;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
