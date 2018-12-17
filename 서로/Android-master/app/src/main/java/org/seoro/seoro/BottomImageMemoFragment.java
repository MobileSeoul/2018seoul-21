package org.seoro.seoro;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.seoro.seoro.model.ImageMemo;

public class BottomImageMemoFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM_IMAGE_MEMO = "imageMemo";

    private TextView mTextView;

    private ImageMemo mImageMemo;

    public BottomImageMemoFragment() {
        // Required empty public constructor
    }

    public static BottomImageMemoFragment newInstance(ImageMemo imageMemo) {
        BottomImageMemoFragment fragment = new BottomImageMemoFragment();
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
        View view = inflater.inflate(R.layout.fragment_bottom_image_memo, container, false);

        mTextView = view.findViewById(R.id.BottomImageMemoFragment_TextView);
        //mTextView.setText(mImageMemo.getLocationName());

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
