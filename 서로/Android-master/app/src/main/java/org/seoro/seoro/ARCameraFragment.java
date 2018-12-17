package org.seoro.seoro;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
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

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seoro.seoro.auth.Session;
import org.seoro.seoro.model.ImageMemo;
import org.seoro.seoro.view.AROverlayView;
import org.seoro.seoro.view.AutoFitTextureView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class ARCameraFragment extends Fragment {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_ACCESS_FINE_LOCATION_PERMISSION = 0;
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private static final int DEFAULT_LIMIT = 30;
    private static final float Z_NEAR = 0.5f;
    private static final float Z_FAR = 2000;

    private Context mContext;

    private AutoFitTextureView mTextureView;
    private AROverlayView mArOverlayView;

    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    private SensorManager mSensorManager;

    private OkHttpClient mOkHttpClient;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private Size mPreviewSize;

    private String mCameraId;

    private List<ImageMemo> mImageMemoList;

    private float[] mProjectionMatrix = new float[16];

    private boolean mRequestImageMemoState;
    private boolean mFlashSupported;
    private int mSensorOrientation;

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
            generateProjectionMatrix();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
            generateProjectionMatrix();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {

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

            if (null != activity) {
                activity.finish();
            }
        }

    };

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] rotationMatrixFromVector = new float[16];
                float[] rotatedProjectionMatrix = new float[16];

                SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, event.values);

                Matrix.multiplyMM(
                        rotatedProjectionMatrix,
                        0,
                        mProjectionMatrix,
                        0,
                        rotationMatrixFromVector,
                        0
                );
                mArOverlayView.updateRotatedProjectionMatrix(rotatedProjectionMatrix);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private OnLocationUpdatedListener mOnLocationUpdatedListener = new OnLocationUpdatedListener() {
        @Override
        public void onLocationUpdated(Location location) {
            updateLatestLocation(location);
        }
    };

    public static ARCameraFragment newInstance() {
        return new ARCameraFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ar_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, Bundle savedInstanceState) {
        mContext = getContext();

        mTextureView = view.findViewById(R.id.ARCameraFragment_TextureView);
        mArOverlayView = view.findViewById(R.id.ARCameraFragment_OverlayView);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        ClearableCookieJar clearableCookieJar = new PersistentCookieJar(
                new SetCookieCache(),
                new SharedPrefsCookiePersistor(mContext.getApplicationContext())
        );
        mOkHttpClient = new OkHttpClient.Builder().cookieJar(clearableCookieJar).build();

        mImageMemoList = new ArrayList<>();
        mRequestImageMemoState = false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        initLocationService();
        registerSensors();
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();

        Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        SmartLocation.with(activity).location().stop();

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length != 1 ||
                        grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Activity activity = getActivity();

                    if (activity == null) {
                        return;
                    }

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
            case REQUEST_ACCESS_FINE_LOCATION_PERMISSION:
                if (grantResults.length != 1 ||
                        grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Activity activity = getActivity();

                    if (activity == null) {
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestAccessFineLocationPermission();
                        }
                    }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.moveTaskToBack(true);
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

    private void requestAccessFineLocationPermission() {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;
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
                            REQUEST_ACCESS_FINE_LOCATION_PERMISSION
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
                    .setMessage(R.string.dialog_message_location_permission)
                    .show();
        } else if (ActivityCompat.checkSelfPermission(activity, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{permission},
                    REQUEST_ACCESS_FINE_LOCATION_PERMISSION
            );
        }
    }

    private void registerSensors() {
        mSensorManager.registerListener(
                mSensorEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST
        );
    }

    private void initLocationService() {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;
        Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(activity, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            requestAccessFineLocationPermission();

            return;
        }

        SmartLocation.with(activity).location().start(mOnLocationUpdatedListener);
        updateLatestLocation(SmartLocation.with(activity).location().getLastLocation());
    }

    private void updateLatestLocation(Location location) {
        if (mArOverlayView != null && location != null) {
            mArOverlayView.updateCurrentLocation(location);
            mImageMemoList.clear();

            requestNearImageMemos(
                    location.getLatitude(),
                    location.getLongitude(),
                    0,
                    DEFAULT_LIMIT
            );
        }
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
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(
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

                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
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

                Activity activity = getActivity();

                if (activity == null) {
                    continue;
                }

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
        Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        int permission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
        );

        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();

            return;
        }

        setUpCameraOutputs(width, height);
        configureTransform(width, height);

        CameraManager cameraManager = (CameraManager) activity.getSystemService(
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
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
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
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            Surface surface = new Surface(texture);
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW
            );

            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Collections.singletonList(surface),
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
                                        null,
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

        if (null == mTextureView || null == mPreviewSize || null == activity) {
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

        mTextureView.setTransform(matrix);
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            );
        }
    }

    private void generateProjectionMatrix() {
        float ratio = (float) mPreviewSize.getWidth() / mPreviewSize.getHeight();
        final int OFFSET = 0;
        final float BOTTOM = -1;
        final float TOP = 1;

        Matrix.frustumM(mProjectionMatrix, OFFSET, -ratio, ratio, BOTTOM, TOP, Z_NEAR, Z_FAR);
    }

    private void requestNearImageMemos(double lat, double lng, int skip, int limit) {
        if (mRequestImageMemoState) {
            return;
        }

        mRequestImageMemoState = true;
        Request request = new Request.Builder()
                .url(
                        Session.HOST +
                                "/api/memos/near?lat=" +
                                lat +
                                "&lng=" +
                                lng +
                                "&skip=" +
                                skip +
                                "&limit=" +
                                limit
                )
                .get()
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Activity activity = getActivity();

                if (activity == null) {
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setNeutralButton(R.string.dialog_ok, null)
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(R.string.dialog_message_network_error)
                        .show();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                mRequestImageMemoState = false;

                try (ResponseBody responseBody = response.body()) {
                    Activity activity = getActivity();

                    if (activity == null) {
                        return;
                    }

                    if (responseBody == null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                builder.setNeutralButton(R.string.dialog_ok, null)
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_message_network_error)
                                        .show();
                            }
                        });

                        return;
                    }

                    String responseString = responseBody.string();

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!response.isSuccessful()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                        activity
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

                                mArOverlayView.updateImageMemoList(mImageMemoList);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
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
}
