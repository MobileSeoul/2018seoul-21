package org.seoro.seoro;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seoro.seoro.auth.Session;
import org.seoro.seoro.glide.GlideApp;
import org.seoro.seoro.model.ImageMemo;
import org.seoro.seoro.model.User;
import org.seoro.seoro.recyclerview.ImageMemoAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UserActivity extends AppCompatActivity {
    public static final String ARG_USER_ID = "userId";

    private static final int DEFAULT_LIMIT = 20;
    private static final int SPAN_COUNT = 3;

    private AppBarLayout mAppBarLayout;
    private ConstraintLayout mConstraintLayout;
    private CircleImageView mCircleImageView;
    private TextView mDisplayNameTextView;
    private TextView mFollowingCountTextView;
    private TextView mFollowerCountTextView;
    private TextView mCollectedCountTextView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private FloatingActionButton mFollowFloatingActionButton;

    private OkHttpClient mOkHttpClient;

    private ImageMemoAdapter mImageMemoAdapter;

    private String mUserId;
    private User mUser;
    private List<ImageMemo> mImageMemoList;

    private boolean mRequestImageMemoState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        Toolbar toolbar = findViewById(R.id.UserActivity_Toolbar);
        setSupportActionBar(toolbar);

        mAppBarLayout = findViewById(R.id.UserActivity_AppBarLayout);
        mConstraintLayout = findViewById(R.id.UserActivity_ConstraintLayout);
        mCircleImageView = findViewById(R.id.UserActivity_CircleImageView);
        mDisplayNameTextView = findViewById(R.id.UserActivity_DisplayNameTextView);
        mFollowingCountTextView = findViewById(R.id.UserActivity_FollowingCountTextView);
        mFollowerCountTextView = findViewById(R.id.UserActivity_FollowerCountTextView);
        mCollectedCountTextView = findViewById(R.id.UserActivity_CollectedCountTextView);
        mSwipeRefreshLayout = findViewById(R.id.UserActivity_SwipeRefreshLayout);
        mRecyclerView = findViewById(R.id.UserActivity_RecyclerView);
        mFollowFloatingActionButton = findViewById(R.id.UserActivity_FollowFloatingActionButton);
        Button button = findViewById(R.id.UserActivity_ConstraintLayoutButton);

        ClearableCookieJar clearableCookieJar = new PersistentCookieJar(
                new SetCookieCache(), new SharedPrefsCookiePersistor(getApplicationContext())
        );
        mOkHttpClient = new OkHttpClient.Builder().cookieJar(clearableCookieJar).build();

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                int maxScroll = appBarLayout.getTotalScrollRange();
                float percentage = (float) Math.abs(verticalOffset) / (float) maxScroll;

                if (percentage > 0.3) {
                    mConstraintLayout.setVisibility(View.INVISIBLE);
                } else {
                    mConstraintLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        mRequestImageMemoState = false;
        mImageMemoList = new ArrayList<>();
        mImageMemoAdapter = new ImageMemoAdapter(this, mImageMemoList);
        mImageMemoAdapter.setOnItemClickListener(new ImageMemoAdapter.OnItemClickListener() {
            @Override
            public void onClick(View v, int position) {
                Intent intent = new Intent(
                        UserActivity.this,
                        ImageMemoActivity.class
                );
                intent.putExtra(
                        ImageMemoActivity.ARG_IMAGE_MEMO_ID,
                        mImageMemoList.get(position).getId()
                );

                startActivity(intent);
            }
        });
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, SPAN_COUNT);

        mRecyclerView.setAdapter(mImageMemoAdapter);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    requestImageMemos(mUserId, mImageMemoList.size(), DEFAULT_LIMIT);
                }
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mImageMemoList.clear();
                requestImageMemos(mUserId, 0, DEFAULT_LIMIT);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UserActivity.this, SettingsActivity.class));
            }
        });

        mUserId = getIntent().getStringExtra(ARG_USER_ID);

        if (mUserId == null) {
            finish();

            return;
        }

        if (mUserId.equals(Session.getInstance().getUser().getId())) {
            mFollowFloatingActionButton.setVisibility(View.GONE);
        }

        requestUser(mUserId);
        requestImageMemos(mUserId, 0, DEFAULT_LIMIT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_user, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.UserActivity_SettingsMenu:
                startActivity(new Intent(this, SettingsActivity.class));

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void requestUser(String userId) {
        Request request = new Request.Builder()
                .url(Session.HOST + "/api/users/" + userId)
                .get()
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        UserActivity.this
                );
                builder.setNeutralButton(R.string.dialog_ok, null)
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(R.string.dialog_message_network_error)
                        .show();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                UserActivity.this
                        );
                        builder.setNeutralButton(R.string.dialog_ok, null)
                                .setTitle(R.string.dialog_title_error)
                                .setMessage(R.string.dialog_message_network_error)
                                .show();

                        return;
                    }

                    String responseString = responseBody.string();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!response.isSuccessful()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                        UserActivity.this
                                );
                                builder.setNeutralButton(R.string.dialog_ok, null)
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_message_server_error)
                                        .show();

                                return;
                            }

                            try {
                                JSONObject jsonObject = new JSONObject(responseString);

                                mUser = new User(jsonObject.optJSONObject("data"));

                                GlideApp.with(UserActivity.this)
                                        .load(mUser.getThumbnail())
                                        .placeholder(R.drawable.placeholder_profile_image)
                                        .error(R.drawable.placeholder_profile_image)
                                        .fallback(R.drawable.placeholder_profile_image)
                                        .into(mCircleImageView);

                                mDisplayNameTextView.setText(mUser.getDisplayName());
                                mCollectedCountTextView.setText(
                                        String.format(
                                                Locale.getDefault(),
                                                "%d",
                                                mUser.getMemoCount()
                                        )
                                );
                                mFollowingCountTextView.setText(
                                        String.format(
                                                Locale.getDefault(),
                                                "%d",
                                                mUser.getFollowingCount()
                                        )
                                );
                                mFollowerCountTextView.setText(
                                        String.format(
                                                Locale.getDefault(),
                                                "%d",
                                                mUser.getFollowerCount()
                                        )
                                );

                                if (mUser.isFollowing()) {
                                    mFollowFloatingActionButton.setImageResource(
                                            android.R.drawable.ic_menu_close_clear_cancel
                                    );
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    private void requestImageMemos(String userId, int skip, int limit) {
        if (mRequestImageMemoState) {
            return;
        }

        mRequestImageMemoState = true;
        Request request = new Request.Builder()
                .url(
                        Session.HOST +
                                "/api/memos?userId=" +
                                userId +
                                "&skip=" +
                                skip +
                                "&limit=" +
                                limit
                )
                .get()
                .build();

        mSwipeRefreshLayout.setRefreshing(true);

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        UserActivity.this
                );
                builder.setNeutralButton(R.string.dialog_ok, null)
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(R.string.dialog_message_network_error)
                        .show();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                mRequestImageMemoState = false;

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSwipeRefreshLayout.setRefreshing(false);

                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                        UserActivity.this
                                );
                                builder.setNeutralButton(R.string.dialog_ok, null)
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_message_network_error)
                                        .show();
                            }
                        });

                        return;
                    }

                    String responseString = responseBody.string();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(false);

                            if (!response.isSuccessful()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                        UserActivity.this
                                );
                                builder.setNeutralButton(R.string.dialog_ok, null)
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_message_server_error)
                                        .show();

                                return;
                            }

                            try {
                                JSONObject jsonObject = new JSONObject(responseString);
                                JSONArray jsonArray = jsonObject.optJSONArray("data");

                                for (int i = 0; i < jsonArray.length(); i++) {
                                    ImageMemo imageMemo = new ImageMemo(
                                            jsonArray.optJSONObject(i)
                                    );
                                    mImageMemoList.add(imageMemo);
                                }

                                mImageMemoAdapter.notifyDataSetChanged();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }
}
