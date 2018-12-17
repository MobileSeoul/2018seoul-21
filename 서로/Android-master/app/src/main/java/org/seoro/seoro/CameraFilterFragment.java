package org.seoro.seoro;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.seoro.seoro.gl.CameraRenderThread;
import org.seoro.seoro.gl.ShaderProgram;
import org.seoro.seoro.listener.OnCameraTouchListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class CameraFilterFragment extends Fragment {
    private static final String SHARED_PREFERENCES = "org.seoro.seoro.camera";
    private static final String SHARED_PREFERENCES_AUTO_SAVE = "autoSave";
    private static final String SHARED_PREFERENCES_MIRROR_MODE = "mirrorMode";
    private static final String SHARED_PREFERENCES_LENS_FACING = "lensFacing";
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private Context mContext;

    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    private SurfaceTexture mPreviewSurfaceTexture;
    private CameraRenderThread mCameraRenderThread;

    private SharedPreferences mSharedPreferences;
    private ImageReader mImageReader;
    private File mFile;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private Size mPreviewSize;

    private String mCameraId;

    private boolean mCameraOpened;
    private boolean mFlashSupported;
    private int mSensorOrientation;
    private int mWidth;
    private int mHeight;
    private int mShaderProgramIndex = 0;
    private int mState = STATE_PREVIEW;
    private int mLensFacing;

    private TextureView mTextureView;
    private TextView mTextView;
    private LinearLayout mTopLinearLayout;
    private LinearLayout mHoverLinearLayout;
    private Switch mAutoSaveSwitch;
    private Switch mMirrorModeSwitch;
    private Button mHoverButton;
    private Button mChangeCameraButton;
    private Button mCaptureButton;
    private ImageView mImageView;
    private ConstraintLayout mBottomConstraintLayout;

    private List<ShaderProgram> mShaderProgramList;
    private List<String> mShaderProgramNameList;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    unlockFocus();

                    Image image = reader.acquireNextImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];

                    buffer.get(bytes);

                    FileOutputStream output = null;

                    if (mFile == null) {
                        return;
                    }

                    try {
                        output = new FileOutputStream(mFile);
                        output.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        image.close();

                        if (null != output) {
                            try {
                                output.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        Activity activity = getActivity();

                        if (activity != null) {
                            Intent intent = new Intent(activity, PostActivity.class);
                            intent.putExtra(
                                    PostActivity.PARAM_SHADER_PROGRAM_INDEX,
                                    mShaderProgramIndex
                            );
                            intent.putExtra(
                                    PostActivity.PARAM_IMAGE_AUTO_SAVE,
                                    mAutoSaveSwitch.isChecked()
                            );
                            intent.putExtra(
                                    PostActivity.PARAM_CAMERA_TRANSFORM_MATRIX,
                                    mCameraRenderThread.getCameraTransformMatrix()
                            );
                            intent.putExtra(PostActivity.PARAM_IMAGE_FILE, mFile);

                            activity.startActivity(intent);
                            //activity.finish();
                        }
                    }

                    mCaptureButton.setEnabled(true);
                }
            });
        }

    };

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();

            mCameraDevice = cameraDevice;

            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();

            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();

            mCameraDevice = null;

            Activity activity = getActivity();

            if (activity != null) {
                activity.finish();
            }
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null || afState == 0) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mWidth = width;
            mHeight = height;
            mPreviewSurfaceTexture = surface;

            mCameraRenderThread.setPreviewSurfaceTexture(mPreviewSurfaceTexture);
            mCameraRenderThread.setWidth(mWidth);
            mCameraRenderThread.setHeight(mHeight);

            if (!mCameraRenderThread.isAlive()) {
                mCameraRenderThread.start();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraRenderThread.OnRendererReadyListener mOnRendererReadyListener = new CameraRenderThread.OnRendererReadyListener() {
        @Override
        public void onRendererReady() {
            openCamera(mWidth, mHeight);
        }

        @Override
        public void onRendererFinished() {

        }
    };

    public CameraFilterFragment() {
        // Required empty public constructor
    }

    public static CameraFilterFragment newInstance() {
        return new CameraFilterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(
                R.layout.fragment_camera_filter,
                container,
                false
        );

        mTextureView = view.findViewById(R.id.CameraFilterFragment_TextureView);
        mTextView = view.findViewById(R.id.CameraFilterFragment_TextView);
        mTopLinearLayout = view.findViewById(R.id.CameraFilterFragment_TopLinearLayout);
        mAutoSaveSwitch = view.findViewById(R.id.CameraFilterFragment_AutoSaveSwitch);
        mMirrorModeSwitch = view.findViewById(R.id.CameraFilterFragment_MirrorModeSwitch);
        mHoverButton = view.findViewById(R.id.CameraFilterFragment_HoverButton);
        mHoverLinearLayout = view.findViewById(R.id.CameraFilterFragment_HoverLinearLayout);
        mChangeCameraButton = view.findViewById(R.id.CameraFilterFragment_ChangeCameraButton);
        mCaptureButton = view.findViewById(R.id.CameraFilterFragment_CaptureButton);
        mImageView = view.findViewById(R.id.CameraFilterFragment_ImageView);
        mBottomConstraintLayout = view.findViewById(
                R.id.CameraFilterFragment_BottomConstraintLayout
        );

        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        mTextureView.setOnTouchListener(new OnCameraTouchListener(mContext) {
            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();

                if (mShaderProgramIndex != mShaderProgramNameList.size() - 1) {
                    mTextView.animate().cancel();
                    mShaderProgramIndex++;

                    mCameraRenderThread.useShaderProgram(mShaderProgramIndex);

                    mTextView.setText(mShaderProgramNameList.get(mShaderProgramIndex));
                    mTextView.setVisibility(View.VISIBLE);
                    mTextView.animate().setDuration(500).alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);

                            mTextView.animate().setDuration(500).alpha(0).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);

                                    mTextView.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onSwipeRight() {
                super.onSwipeRight();

                if (mShaderProgramIndex != 0) {
                    mTextView.animate().cancel();
                    mShaderProgramIndex--;

                    mCameraRenderThread.useShaderProgram(mShaderProgramIndex);

                    mTextView.setText(mShaderProgramNameList.get(mShaderProgramIndex));
                    mTextView.setVisibility(View.VISIBLE);
                    mTextView.animate().setDuration(500).alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);

                            mTextView.animate().setDuration(500).alpha(0).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);

                                    mTextView.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                }
            }
        });
        mAutoSaveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mSharedPreferences != null) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean(SHARED_PREFERENCES_AUTO_SAVE, isChecked);
                    editor.apply();
                }
            }
        });
        mMirrorModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mCameraRenderThread.setCoordinates(ShaderProgram.MIRROR_COORDINATES);
                } else {
                    mCameraRenderThread.setCoordinates(ShaderProgram.COORDINATES);
                }

                if (mSharedPreferences != null) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean(SHARED_PREFERENCES_MIRROR_MODE, isChecked);
                    editor.apply();
                }
            }
        });
        mHoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mHoverLinearLayout.getVisibility()) {
                    case View.GONE:
                    case View.INVISIBLE:
                        mHoverLinearLayout.setVisibility(View.VISIBLE);

                        break;
                    case View.VISIBLE:
                        mHoverLinearLayout.setVisibility(View.GONE);

                        break;
                }
            }
        });
        mChangeCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTextureView.setSurfaceTextureListener(null);
                closeCamera();
                stopBackgroundThread();

                switch (mLensFacing) {
                    case CameraCharacteristics.LENS_FACING_BACK:
                        mLensFacing = CameraCharacteristics.LENS_FACING_FRONT;

                        break;
                    case CameraCharacteristics.LENS_FACING_FRONT:
                        mLensFacing = CameraCharacteristics.LENS_FACING_BACK;

                        break;
                }

                if (mSharedPreferences != null) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putInt(SHARED_PREFERENCES_LENS_FACING, mLensFacing);
                    editor.apply();
                }

                startBackgroundThread();

                if (mTextureView.isAvailable()) {
                    openCamera(mTextureView.getWidth(), mTextureView.getHeight());
                } else {
                    mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
                }
            }
        });
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCaptureButton.setEnabled(false);
                takePicture();
            }
        });
        mImageView.setClipToOutline(true);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_VIEW);

                startActivity(Intent.createChooser(intent, ""));
            }
        });

        mContext = getContext();

        if (mContext != null) {
            mSharedPreferences = mContext.getSharedPreferences(
                    SHARED_PREFERENCES, Context.MODE_PRIVATE
            );
            mAutoSaveSwitch.setChecked(
                    mSharedPreferences.getBoolean(SHARED_PREFERENCES_AUTO_SAVE, false)
            );
            mMirrorModeSwitch.setChecked(
                    mSharedPreferences.getBoolean(SHARED_PREFERENCES_MIRROR_MODE, false)
            );
            mLensFacing = mSharedPreferences.getInt(
                    SHARED_PREFERENCES_LENS_FACING,
                    CameraCharacteristics.LENS_FACING_BACK
            );
        }

        try {
            mShaderProgramList = new ArrayList<ShaderProgram>() {{
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.basic_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.gangnam_morning_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.gangnam_night_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.namsan_morning_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.namsan_night_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.samchungdong_morning_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.samchungdong_night_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.itaewon_morning_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.itaewon_night_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.hangang_morning_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.hangang_night_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.hongdae_morning_preview_frag
                ));
                add(new ShaderProgram(
                        mContext, R.raw.basic_vert, R.raw.hongdae_night_preview_frag
                ));
            }};
            mShaderProgramNameList = new ArrayList<String>() {{
                add("Default");
                add("Gangnam Morning");
                add("Gangnam Night");
                add("Namsan Morning");
                add("Namsan Night");
                add("Samchung-dong Morning");
                add("Samchung-dong Night");
                add("Itaewon Morning");
                add("Itaewon Night");
                add("Hangang Morning");
                add("Hangang Night");
                add("Hongdae Morning");
                add("Hongdae Night");
            }};
        } catch (Exception e) {
            e.printStackTrace();
        }

        mCameraRenderThread = new CameraRenderThread(mContext, mShaderProgramList);
        mCameraRenderThread.setOnRendererReadyListener(mOnRendererReadyListener);

        if (mMirrorModeSwitch.isChecked()) {
            mCameraRenderThread.setCoordinates(ShaderProgram.MIRROR_COORDINATES);
        } else {
            mCameraRenderThread.setCoordinates(ShaderProgram.COORDINATES);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Uri imageUri = getLatestImageUri();

        if (imageUri != null && imageUri.getPath() != null) {
            File imageFile = new File(imageUri.getPath());

            if (imageFile.exists()) {
                mImageView.setImageURI(imageUri);
            }
        }

        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        int statusBarHeight = 0;
        int navigationBarHeight = 0;
        int statusBarHeightResourceId = getResources()
                .getIdentifier(
                        "status_bar_height",
                        "dimen",
                        "android"
                );
        int navigationBarHeightResourceId = getResources()
                .getIdentifier(
                        "navigation_bar_height",
                        "dimen",
                        "android"
                );

        if (statusBarHeightResourceId > 0) {
            statusBarHeight = getResources()
                    .getDimensionPixelSize(statusBarHeightResourceId);
        }

        if (navigationBarHeightResourceId > 0) {
            navigationBarHeight = getResources()
                    .getDimensionPixelSize(navigationBarHeightResourceId);
        }

        ConstraintLayout.LayoutParams topLinearLayoutParams =
                (ConstraintLayout.LayoutParams) mTopLinearLayout.getLayoutParams();
        topLinearLayoutParams.setMargins(
                topLinearLayoutParams.leftMargin,
                statusBarHeight,
                topLinearLayoutParams.rightMargin,
                topLinearLayoutParams.bottomMargin
        );
        ConstraintLayout.LayoutParams bottomConstraintLayoutParams =
                (ConstraintLayout.LayoutParams) mBottomConstraintLayout.getLayoutParams();
        bottomConstraintLayoutParams.setMargins(
                bottomConstraintLayoutParams.leftMargin,
                bottomConstraintLayoutParams.topMargin,
                bottomConstraintLayoutParams.rightMargin,
                navigationBarHeight
        );

        mTopLinearLayout.setLayoutParams(topLinearLayoutParams);
        mBottomConstraintLayout.setLayoutParams(bottomConstraintLayoutParams);
    }

    @Override
    public void onPause() {
        super.onPause();

        mTextureView.setSurfaceTextureListener(null);
        closeCamera();
        stopBackgroundThread();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        try {
            mFile = File.createTempFile("temp", null, getActivity().getCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length != 1 ||
                        grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestCameraPermission();
                        }
                    }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.moveTaskToBack(true);

                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                        }
                    }).setTitle(R.string.dialog_title_permission)
                            .setMessage(R.string.dialog_message_camera_permission)
                            .show();
                }

                break;

        }
    }

    private void requestCameraPermission() {
        String permission = Manifest.permission.CAMERA;
        Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(
                            activity,
                            new String[]{permission},
                            REQUEST_CAMERA_PERMISSION
                    );
                }
            }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    activity.moveTaskToBack(true);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);
                }
            }).setTitle(R.string.dialog_title_permission)
                    .setMessage(R.string.dialog_message_camera_permission)
                    .show();
        } else if (ActivityCompat.checkSelfPermission(activity, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{permission},
                    REQUEST_CAMERA_PERMISSION
            );
        }
    }

    private Uri getLatestImageUri() {
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = mContext
                .getContentResolver()
                .query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
                );

        if (cursor == null) {
            return null;
        }

        Uri imageUri = null;

        if (cursor.moveToFirst()) {
            imageUri = Uri.parse(cursor.getString(1));
        }

        cursor.close();

        return imageUri;
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Timber.e("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        CameraManager cameraManager = (CameraManager) activity.getSystemService(
                Context.CAMERA_SERVICE
        );

        if (cameraManager == null) {
            return;
        }

        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(
                        cameraId
                );

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (facing != null && facing != mLensFacing) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea()
                );
                mImageReader = ImageReader.newInstance(
                        largest.getWidth(),
                        largest.getHeight(),
                        ImageFormat.JPEG,
                        1
                );
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener,
                        mBackgroundHandler
                );
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;

                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }

                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }

                        break;
                    default:
                        Timber.e("Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();

                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                mPreviewSize = chooseOptimalSize(
                        map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth,
                        rotatedPreviewHeight,
                        maxPreviewWidth,
                        maxPreviewHeight,
                        new Size(height, width)
                );

                int orientation = getResources().getConfiguration().orientation;

                /*
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(),
                            mPreviewSize.getHeight()
                    );
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(),
                            mPreviewSize.getWidth()
                    );
                }
                */

                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;
                mCameraId = cameraId;

                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int width, int height) {
        int permission = ContextCompat.checkSelfPermission(
                mContext,
                Manifest.permission.CAMERA
        );

        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();

            return;
        }

        setUpCameraOutputs(width, height);
        configureTransform(width, height);

        CameraManager cameraManager = (CameraManager) mContext.getSystemService(
                Context.CAMERA_SERVICE
        );

        if (cameraManager == null) {
            return;
        }

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            cameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);

            mCameraOpened = true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();

            if (null != mCaptureSession) {
                mCaptureSession.close();

                mCaptureSession = null;
            }

            if (null != mCameraDevice) {
                mCameraDevice.close();

                mCameraDevice = null;
            }

            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
            mCameraOpened = false;
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");

        mBackgroundThread.start();

        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();

        try {
            mBackgroundThread.join();

            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = mCameraRenderThread.getCameraSurfaceTexture();
            assert surfaceTexture != null;

            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            Surface surface = new Surface(surfaceTexture);
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW
            );

            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(
                    Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (null == mCameraDevice) {
                                return;
                            }

                            mCaptureSession = cameraCaptureSession;

                            try {
                                mPreviewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                );
                                setAutoFlash(mPreviewRequestBuilder);

                                mPreviewRequest = mPreviewRequestBuilder.build();

                                mCaptureSession.setRepeatingRequest(
                                        mPreviewRequest,
                                        mCaptureCallback,
                                        mBackgroundHandler
                                );
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            // TODO: Add AlertDialog.
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        if (null == mPreviewSize) {
            return;
        }

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(
                0,
                0,
                mPreviewSize.getHeight(),
                mPreviewSize.getWidth()
        );
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(
                    centerX - bufferRect.centerX(),
                    centerY - bufferRect.centerY()
            );
            matrix.setRectToRect(viewRect, bufferRect, android.graphics.Matrix.ScaleToFit.FILL);

            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth()
            );

            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }

        //mTextureView.setTransform(matrix);
    }

    private void takePicture() {
        lockFocus();
    }

    private void lockFocus() {
        try {
            mPreviewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START
            );

            mState = STATE_WAITING_LOCK;

            mCaptureSession.capture(
                    mPreviewRequestBuilder.build(),
                    mCaptureCallback,
                    mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            mPreviewRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            );

            mState = STATE_WAITING_PRECAPTURE;

            mCaptureSession.capture(
                    mPreviewRequestBuilder.build(),
                    mCaptureCallback,
                    mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();

            if (null == activity || null == mCameraDevice) {
                mCaptureButton.setEnabled(true);
                return;
            }

            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
            );
            captureBuilder.addTarget(mImageReader.getSurface());

            captureBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            );
            setAutoFlash(captureBuilder);

            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Timber.d(mFile.toString());
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void unlockFocus() {
        try {
            mPreviewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            );
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(
                    mPreviewRequestBuilder.build(),
                    mCaptureCallback,
                    mBackgroundHandler
            );

            mState = STATE_PREVIEW;

            mCaptureSession.setRepeatingRequest(
                    mPreviewRequest,
                    mCaptureCallback,
                    mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            );
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() -
                            (long) rhs.getWidth() * rhs.getHeight()
            );
        }

    }

    public TextureView getTextureView() {
        return mTextureView;
    }
}
