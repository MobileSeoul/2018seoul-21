package org.seoro.seoro;

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.seoro.seoro.model.ImageMemo;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {
    public static final String BUNDLE_CURRENT_ITEM_KEY = "currentItem";
    public static final String BUNDLE_IMAGE_MEMO_LIST_KEY = "imageMemoList";

    private int mCurrentItem;
    private List<ImageMemo> mImageMemoList = new ArrayList<>();

    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            mCurrentItem = bundle.getInt(BUNDLE_CURRENT_ITEM_KEY, 0);
            mImageMemoList = bundle.getParcelableArrayList(BUNDLE_IMAGE_MEMO_LIST_KEY);
        }

        mViewPager = findViewById(R.id.DetailActivity_ViewPager);
        mViewPager.setAdapter(new ExpandingViewPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return ImageMemoFragment.newInstance(mImageMemoList.get(position));
            }

            @Override
            public int getCount() {
                return mImageMemoList.size();
            }
        });

        mViewPager.setCurrentItem(mCurrentItem);
    }
}
