package org.seoro.seoro;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;
import org.seoro.seoro.auth.Session;
import org.seoro.seoro.model.EmojiComment;
import org.seoro.seoro.model.ImageMemo;
import org.seoro.seoro.recyclerview.EmojiCommentAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ImageMemoActivity extends AppCompatActivity {
    public static final String ARG_IMAGE_MEMO_ID = "imageMemoId";

    private static final int DEFAULT_LIMIT = 20;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ImageView mImageView;
    private TextView mDisplayNameTextView;
    private TextView mTimestampTextView;
    private TextView mTextView;
    private Button mDeleteButton;
    private Button mEmojiButton;
    private TextView mEmojiTextView;
    private RecyclerView mRecyclerView;

    private EmojiCommentAdapter mEmojiCommentAdapter;
    private OkHttpClient mOkHttpClient;
    private PrettyTime mPrettyTime;

    private ImageMemo mImageMemo;
    private String mImageMemoId;

    private List<EmojiComment> mEmojiCommentList;

    private boolean mRequestImageMemoState;
    private boolean mRequestEmojiCommentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_memo);

        mSwipeRefreshLayout = findViewById(R.id.ImageMemoActivity_SwipeRefreshLayout);
        mImageView = findViewById(R.id.ImageMemoActivity_ImageView);
        mDisplayNameTextView = findViewById(R.id.ImageMemoActivity_DisplayNameTextView);
        mTimestampTextView = findViewById(R.id.ImageMemoActivity_TimestampTextView);
        mTextView = findViewById(R.id.ImageMemoActivity_TextView);
        mDeleteButton = findViewById(R.id.ImageMemoActivity_DeleteButton);
        mEmojiButton = findViewById(R.id.ImageMemoActivity_EmojiButton);
        mEmojiTextView = findViewById(R.id.ImageMemoActivity_EmojiTextView);
        mRecyclerView = findViewById(R.id.ImageMemoActivity_RecyclerView);

        ClearableCookieJar clearableCookieJar = new PersistentCookieJar(
                new SetCookieCache(), new SharedPrefsCookiePersistor(getApplicationContext())
        );
        mOkHttpClient = new OkHttpClient.Builder().cookieJar(clearableCookieJar).build();
        mPrettyTime = new PrettyTime();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestImageMemo(mImageMemoId);
            }
        });
        mDisplayNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        ImageMemoActivity.this,
                        UserActivity.class
                );
                intent.putExtra(UserActivity.ARG_USER_ID, mImageMemo.getUser().getId());

                startActivity(intent);
            }
        });

        mEmojiCommentList = new ArrayList<>();
        mEmojiCommentAdapter = new EmojiCommentAdapter(this, mEmojiCommentList);
        mEmojiCommentAdapter.setOnItemClickListener(new EmojiCommentAdapter.OnItemClickListener() {
            @Override
            public void onClick(View v, int position) {
                Intent intent = new Intent(
                        ImageMemoActivity.this,
                        UserActivity.class
                );
                intent.putExtra(
                        UserActivity.ARG_USER_ID,
                        mEmojiCommentList.get(position).getUser().getId()
                );

                startActivity(intent);
            }
        });

        mRecyclerView.setAdapter(mEmojiCommentAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    requestEmojiComments(mImageMemoId, mEmojiCommentList.size(), DEFAULT_LIMIT);
                }
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mEmojiCommentList.clear();

                requestImageMemo(mImageMemoId);
                requestEmojiComments(mImageMemoId, 0, DEFAULT_LIMIT);
            }
        });

        mRequestImageMemoState = false;
        mImageMemoId = getIntent().getStringExtra(ARG_IMAGE_MEMO_ID);

        if (mImageMemoId == null) {
            finish();

            return;
        }

        requestImageMemo(mImageMemoId);
        requestEmojiComments(mImageMemoId, 0, DEFAULT_LIMIT);
    }

    private void requestImageMemo(String imageMemoId) {
        if (mRequestImageMemoState) {
            return;
        }

        mRequestImageMemoState = true;
        Request request = new Request.Builder()
                .url(Session.HOST + "/api/memos/" + imageMemoId)
                .get()
                .build();

        mSwipeRefreshLayout.setRefreshing(true);

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        ImageMemoActivity.this
                );
                builder.setNeutralButton(R.string.dialog_ok, null)
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(R.string.dialog_message_network_error)
                        .show();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                mRequestImageMemoState = false;
                mSwipeRefreshLayout.setRefreshing(false);

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                ImageMemoActivity.this
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
                                        ImageMemoActivity.this
                                );
                                builder.setNeutralButton(R.string.dialog_ok, null)
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_message_server_error)
                                        .show();

                                return;
                            }

                            try {
                                JSONObject jsonObject = new JSONObject(responseString);
                                mImageMemo = new ImageMemo(
                                        jsonObject.optJSONObject("data")
                                );

                                String commentCountString = String.format(
                                        Locale.getDefault(),
                                        "%d",
                                        mImageMemo.getCommentCount()
                                );

                                Glide.with(ImageMemoActivity.this)
                                        .load(mImageMemo.getImage())
                                        .into(mImageView);
                                mDisplayNameTextView.setText(
                                        mImageMemo.getUser().getDisplayName()
                                );
                                mTimestampTextView.setText(
                                        mPrettyTime.format(mImageMemo.getTimestamp())
                                );
                                mTextView.setText(mImageMemo.getMemo());
                                mEmojiTextView.setText(commentCountString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    private void requestEmojiComments(String imageMemoId, int skip, int limit) {
        if (mRequestEmojiCommentState) {
            return;
        }

        mRequestEmojiCommentState = true;
        Request request = new Request.Builder()
                .url(
                        Session.HOST +
                                "/api/comments/" +
                                imageMemoId +
                                "?skip=" +
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
                        ImageMemoActivity.this
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
                                        ImageMemoActivity.this
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
                                        ImageMemoActivity.this
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
                                    EmojiComment emojiComment = new EmojiComment(
                                            jsonArray.optJSONObject(i)
                                    );
                                    mEmojiCommentList.add(emojiComment);
                                }

                                mEmojiCommentAdapter.notifyDataSetChanged();
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
