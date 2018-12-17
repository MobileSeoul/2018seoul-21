package org.seoro.seoro;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.json.JSONException;
import org.json.JSONObject;
import org.seoro.seoro.auth.Session;
import org.seoro.seoro.gl.ImageRenderThread;
import org.seoro.seoro.gl.ShaderProgram;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PostActivity extends AppCompatActivity {
    public static final String PARAM_SHADER_PROGRAM_INDEX = "shaderProgramIndex";
    public static final String PARAM_IMAGE_AUTO_SAVE = "imageAutoSave";
    public static final String PARAM_IMAGE_MIRROR_MODE = "mirrorMode";
    public static final String PARAM_IMAGE_FILE = "imageFile";
    public static final String PARAM_CAMERA_TRANSFORM_MATRIX = "cameraTransformMatrix";

    private static final int REQUEST_ACCESS_FINE_LOCATION_PERMISSION = 0;

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd_HH:mm:ss",
            Locale.US
    );

    private OkHttpClient mOkHttpClient;

    private ScrollView mScrollView;
    private ProgressBar mProgressBar;
    private ImageView mImageView;
    private EditText mEditText;
    private FloatingActionButton mPostFloatingActionButton;

    private Location mLocation;

    private ImageRenderThread mImageRenderThread;

    private Bitmap mBitmap;
    private File mFile;

    private List<ShaderProgram> mShaderProgramList;

    private float[] mCameraTransformMatrix = new float[16];

    private boolean mImageAutoSave;
    private boolean mMirrorMode;

    private int mShaderProgramIndex = 0;

    private OnLocationUpdatedListener mOnLocationUpdatedListener = new OnLocationUpdatedListener() {
        @Override
        public void onLocationUpdated(Location location) {
            mLocation = location;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        ConstraintLayout constraintLayout = findViewById(R.id.PostActivity_ConstraintLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        ClearableCookieJar clearableCookieJar = new PersistentCookieJar(
                new SetCookieCache(), new SharedPrefsCookiePersistor(getApplicationContext())
        );
        mOkHttpClient = new OkHttpClient.Builder().cookieJar(clearableCookieJar).build();

        mScrollView = findViewById(R.id.PostActivity_ScrollView);
        mProgressBar = findViewById(R.id.PostActivity_ProgressBar);
        mImageView = findViewById(R.id.PostActivity_ImageView);
        mEditText = findViewById(R.id.PostActivity_EditText);
        mPostFloatingActionButton = findViewById(R.id.PostActivity_PostFloatingActionButton);

        Button shareButton = findViewById(R.id.PostActivity_ShareButton);
        
        mPostFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postImageMemo(mEditText.getText().toString(), mLocation, mFile);
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFile));

                startActivity(Intent.createChooser(intent, "Share Image"));
            }
        });

        initLocationService();

        try {
            mShaderProgramList = new ArrayList<ShaderProgram>() {{
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.basic_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.gangnam_morning_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.gangnam_night_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.namsan_morning_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.namsan_night_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.samchungdong_morning_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.samchungdong_night_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.itaewon_morning_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.itaewon_night_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.hangang_morning_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.hangang_night_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.hongdae_morning_frag
                ));
                add(new ShaderProgram(
                        PostActivity.this,
                        R.raw.basic_vert,
                        R.raw.hongdae_night_frag
                ));
            }};
        } catch (Exception e) {
            e.printStackTrace();

            finish();
        }

        mImageRenderThread = new ImageRenderThread(this, mShaderProgramList);
        mShaderProgramIndex = getIntent().getIntExtra(PARAM_SHADER_PROGRAM_INDEX, 0);
        mImageAutoSave = getIntent().getBooleanExtra(PARAM_IMAGE_AUTO_SAVE, false);
        mMirrorMode = getIntent().getBooleanExtra(PARAM_IMAGE_MIRROR_MODE, false);
        mFile = (File) getIntent().getSerializableExtra(PARAM_IMAGE_FILE);
        mCameraTransformMatrix = getIntent().getFloatArrayExtra(PARAM_CAMERA_TRANSFORM_MATRIX);

        if (mFile == null) {
            return;
        }

        mBitmap = BitmapFactory.decodeFile(mFile.getPath());

        if (mMirrorMode) {
            mImageRenderThread.setCoordinates(ShaderProgram.MIRROR_COORDINATES);
        } else {
            mImageRenderThread.setCoordinates(ShaderProgram.COORDINATES);
        }

        mImageRenderThread.setOnRendererFinishedListener(new ImageRenderThread.OnRendererFinishedListener() {
            @Override
            public void onRendererFinished(Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBitmap = bitmap;
                        mImageView.setImageBitmap(bitmap);

                        if (mImageAutoSave) {
                            MediaStore.Images.Media.insertImage(
                                    getContentResolver(),
                                    bitmap,
                                    "",
                                    ""
                            );
                        }

                        try {
                            mFile = File.createTempFile(
                                    "temp",
                                    null,
                                    getExternalCacheDir()
                            );
                            FileOutputStream fileOutputStream = new FileOutputStream(mFile);

                            mBitmap.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    100,
                                    fileOutputStream
                            );
                            fileOutputStream.flush();
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();

                            finish();
                        }
                    }
                });
            }
        });
        mImageRenderThread.setCameraTransformMatrix(mCameraTransformMatrix);
        mImageRenderThread.setBitmap(mBitmap);
        mImageRenderThread.setWidth(mBitmap.getWidth());
        mImageRenderThread.setHeight(mBitmap.getHeight());
        mImageRenderThread.useShaderProgram(mShaderProgramIndex);
        mImageRenderThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        initLocationService();
    }

    @Override
    protected void onPause() {
        super.onPause();

        SmartLocation.with(this).location().stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_ACCESS_FINE_LOCATION_PERMISSION:
                if (grantResults.length != 1 ||
                        grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestAccessFineLocationPermission();
                        }
                    }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            moveTaskToBack(true);
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                        }
                    }).setTitle(R.string.dialog_title_permission)
                            .setMessage(R.string.dialog_message_location_permission)
                            .show();
                } else {
                    initLocationService();
                }

                break;

        }
    }

    private void initLocationService() {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;

        if (ActivityCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            requestAccessFineLocationPermission();

            return;
        }

        SmartLocation.with(this).location().start(mOnLocationUpdatedListener);
        mLocation = SmartLocation.with(this).location().getLastLocation();
    }

    private void requestAccessFineLocationPermission() {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(
                            PostActivity.this,
                            new String[]{permission},
                            REQUEST_ACCESS_FINE_LOCATION_PERMISSION
                    );
                }
            }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    moveTaskToBack(true);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);
                }
            }).setTitle(R.string.dialog_title_permission)
                    .setMessage(R.string.dialog_message_location_permission)
                    .show();
        } else if (ActivityCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{permission},
                    REQUEST_ACCESS_FINE_LOCATION_PERMISSION
            );
        }
    }

    private void postImageMemo(String memo, Location location, File file) {
        mPostFloatingActionButton.setEnabled(false);
        showProgress(true);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "img",
                        SIMPLE_DATE_FORMAT.format(new Date()) + ".jpg",
                        RequestBody.create(MediaType.parse("image/jpeg"), file))
                .addFormDataPart("text", memo)
                .addFormDataPart("loc[type]", "Point")
                .addFormDataPart(
                        "loc[coordinates][]",
                        Double.toString(location.getLongitude())
                )
                .addFormDataPart(
                        "loc[coordinates][]",
                        Double.toString(location.getLatitude())
                )
                .build();
        Request request = new Request.Builder()
                .url(Session.HOST + "/api/memos")
                .post(requestBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false);
                        mPostFloatingActionButton.setEnabled(true);

                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                PostActivity.this
                        );
                        builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEditText.requestFocus();
                            }
                        }).setTitle(R.string.dialog_title_error)
                                .setMessage(R.string.dialog_message_network_error)
                                .show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String responseString = null;
                    JSONObject responseJSONObject = new JSONObject();

                    if (responseBody != null) {
                        try {
                            responseString = responseBody.string();

                            responseJSONObject = new JSONObject(
                                    responseString
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showProgress(false);
                            mPostFloatingActionButton.setEnabled(true);

                            if (response.isSuccessful()) {
                                finish();
                            } else {
                                if (response.code() == 400) {
                                    mEditText.requestFocus();
                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(
                                            PostActivity.this
                                    );
                                    builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mEditText.requestFocus();
                                        }
                                    }).setTitle(R.string.dialog_title_error)
                                            .setMessage(R.string.dialog_message_server_error)
                                            .show();
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mScrollView.setVisibility(show ? View.GONE : View.VISIBLE);
        mScrollView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mScrollView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}
