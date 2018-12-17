package org.seoro.seoro;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.gestures.StandardScaleGestureDetector;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seoro.seoro.auth.Session;
import org.seoro.seoro.glide.GlideApp;
import org.seoro.seoro.model.GroupImageMemo;
import org.seoro.seoro.model.ImageMemo;
import org.seoro.seoro.util.DisplayUnitUtil;

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
import timber.log.Timber;

@SuppressWarnings( {"MissingPermission"})
public class MapsActivity extends FragmentActivity {
    private static final int REQUEST_ACCESS_FINE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CAMERA_ACTIVITY = 2;
    private static final int MAP_DEFAULT_MARGIN_DP = 16;
    private static final int MAP_COMPASS_TOP_MARGIN_DP = 74;
    private static final int MAP_LOGO_BOTTOM_MARGIN_DP = 110;

    private MapView mMapView;
    private MapboxMap mMapboxMap;
    private LocationLayerPlugin mLocationLayerPlugin;
    private LocationEngine mLocationEngine;
    private Session mSession;

    private FloatingActionButton mMyLocationFloatingActionButton;
    private SeekBar mSeekBar;

    private OkHttpClient mOkHttpClient;

    private List<Marker> mMarkerList;
    private List<ImageMemo> mImageMemoList;
    private List<GroupImageMemo> mGroupImageMemoList;

    private boolean mRequestImageMemoState;
    private int mRenderMode = 0;
    private int mCameraMode = CameraMode.NONE;

    private String mZoomLevel = "town";

    private LocationEngineListener mLocationEngineListener = new LocationEngineListener() {
        @Override
        public void onConnected() {
            mLocationEngine.requestLocationUpdates();
        }

        @Override
        public void onLocationChanged(Location location) {

        }
    };

    private MapboxMap.InfoWindowAdapter mInfoWindowAdapter = new MapboxMap.InfoWindowAdapter() {
        @Nullable
        @Override
        public View getInfoWindow(@NonNull Marker marker) {
            GroupImageMemo groupImageMemo = null;

            for (GroupImageMemo tempGroupImageMemo: mGroupImageMemoList) {
                if (marker.getTitle().equals(tempGroupImageMemo.getId())) {
                    groupImageMemo = tempGroupImageMemo;
                }
            }

            ConstraintLayout constraintLayout = new ConstraintLayout(MapsActivity.this);
            constraintLayout.setLayoutParams(
                    new ConstraintLayout.LayoutParams(150, 150)
            );
            constraintLayout.setId(View.generateViewId());

            ImageView imageView = new ImageView(MapsActivity.this);
            imageView.setLayoutParams(new ConstraintLayout.LayoutParams(150, 150));
            imageView.setId(View.generateViewId());

            TextView textView = new TextView(MapsActivity.this);
            textView.setLayoutParams(new ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            textView.setId(View.generateViewId());
            textView.setTextColor(Color.parseColor("#ffffff"));
            textView.setBackgroundColor(Color.parseColor("#ff0000"));

            constraintLayout.addView(imageView);
            constraintLayout.addView(textView);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.connect(
                    constraintLayout.getId(),
                    ConstraintSet.TOP,
                    imageView.getId(),
                    ConstraintSet.TOP
            );
            constraintSet.connect(
                    constraintLayout.getId(),
                    ConstraintSet.BOTTOM,
                    imageView.getId(),
                    ConstraintSet.BOTTOM
            );
            constraintSet.connect(
                    constraintLayout.getId(),
                    ConstraintSet.START,
                    imageView.getId(),
                    ConstraintSet.START
            );
            constraintSet.connect(
                    constraintLayout.getId(),
                    ConstraintSet.END,
                    imageView.getId(),
                    ConstraintSet.END
            );
            constraintSet.connect(
                    constraintLayout.getId(),
                    ConstraintSet.TOP,
                    textView.getId(),
                    ConstraintSet.TOP
            );
            constraintSet.connect(
                    constraintLayout.getId(),
                    ConstraintSet.END,
                    textView.getId(),
                    ConstraintSet.END
            );

            constraintSet.applyTo(constraintLayout);

            if (groupImageMemo != null) {
                GlideApp.with(MapsActivity.this)
                        .load(groupImageMemo.getThumbnail())
                        .into(imageView);

                textView.setText(
                        String.format(Locale.getDefault(), "%d", groupImageMemo.getCount())
                );
            } else {
                textView.setText(String.format(Locale.getDefault(), "%d", 0));
            }

            return constraintLayout;
        }
    };

    private MapboxMap.OnCameraMoveListener onCameraMoveListener = new MapboxMap.OnCameraMoveListener() {
        @Override
        public void onCameraMove() {
            double zoom = mMapboxMap.getCameraPosition().zoom;
            String zoomLevel = getZoolLevel(zoom);

            mZoomLevel = zoomLevel;

            for (Marker marker: mMarkerList) {
                mMapboxMap.removeMarker(marker);
            }

            mMarkerList.clear();

            requestGroupImageMemos(
                    zoomLevel,
                    mLocationEngine.getLastLocation().getLongitude(),
                    mLocationEngine.getLastLocation().getLatitude()
            );
        }
    };

    private MapboxMap.OnScaleListener mOnScaleListener = new MapboxMap.OnScaleListener() {
        @Override
        public void onScaleBegin(@NonNull StandardScaleGestureDetector detector) {

        }

        @Override
        public void onScale(@NonNull StandardScaleGestureDetector detector) {

        }

        @Override
        public void onScaleEnd(@NonNull StandardScaleGestureDetector detector) {
            double zoom = mMapboxMap.getCameraPosition().zoom;
            String zoomLevel = getZoolLevel(zoom);

            if (!zoomLevel.equals(mZoomLevel)) {
                mZoomLevel = zoomLevel;

                for (Marker marker: mMarkerList) {
                    mMapboxMap.removeMarker(marker);
                }

                mMarkerList.clear();

                requestGroupImageMemos(
                        zoomLevel,
                        mLocationEngine.getLastLocation().getLongitude(),
                        mLocationEngine.getLastLocation().getLatitude()
                );
            }
        }
    };

    private OnCameraTrackingChangedListener mOnCameraTrackingChangedListener =
            new OnCameraTrackingChangedListener() {
        @Override
        public void onCameraTrackingDismissed() {
            mRenderMode = RenderMode.NORMAL;
            mCameraMode = CameraMode.NONE;

            mLocationLayerPlugin.setRenderMode(mRenderMode);
            mLocationLayerPlugin.setCameraMode(mCameraMode);

            int color = getResources().getColor(R.color.color_primary);

            mMyLocationFloatingActionButton.setBackgroundTintList(
                    ColorStateList.valueOf(color)
            );
        }

        @Override
        public void onCameraTrackingChanged(int currentMode) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ConstraintLayout constraintLayout = findViewById(R.id.MapsActivity_ConstraintLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        mMyLocationFloatingActionButton = findViewById(
                R.id.MapsActivity_MyLocationFloatActionButton
        );

        mMyLocationFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = Color.WHITE;

                switch (mRenderMode) {
                    case RenderMode.NORMAL:
                        mRenderMode = RenderMode.COMPASS;
                        color = getResources().getColor(R.color.color_accent);

                        break;
                    case RenderMode.COMPASS:
                        mRenderMode = RenderMode.NORMAL;
                        color = getResources().getColor(R.color.color_primary);

                        break;
                }

                switch (mCameraMode) {
                    case CameraMode.NONE:
                        mCameraMode = CameraMode.TRACKING;
                        color = getResources().getColor(R.color.color_accent);

                        break;
                    case CameraMode.TRACKING:
                        mCameraMode = CameraMode.TRACKING_COMPASS;
                        color = getResources().getColor(R.color.color_accent);

                        break;
                    case CameraMode.TRACKING_COMPASS:
                        mCameraMode = CameraMode.NONE;
                        color = getResources().getColor(R.color.color_primary);

                        break;
                }

                mLocationLayerPlugin.setRenderMode(mRenderMode);
                mLocationLayerPlugin.setCameraMode(mCameraMode);

                mMyLocationFloatingActionButton.setBackgroundTintList(
                        ColorStateList.valueOf(color)
                );
            }
        });

        FloatingActionButton arFloatingActionButton = findViewById(
                R.id.MapsActivity_ARFloatActionButton
        );
        arFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MapsActivity.this, ARCameraActivity.class));
            }
        });

        Button myPageButton = findViewById(R.id.MapsActivity_MyPageButton);
        myPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, UserActivity.class);
                intent.putExtra(UserActivity.ARG_USER_ID, Session.getInstance().getUser().getId());

                startActivity(intent);
            }
        });

        mSeekBar = findViewById(R.id.MapsActivity_SeekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 100) {
                    seekBar.setEnabled(false);
                    seekBar.setProgress(0);

                    Intent intent = new Intent(
                            MapsActivity.this,
                            CameraActivity.class
                    );

                    startActivityForResult(intent, REQUEST_CAMERA_ACTIVITY);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() != 100) {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        seekBar.setProgress(0, true);
                    } else {
                        seekBar.setProgress(0);
                    }
                }
            }
        });

        mMapView = findViewById(R.id.MapsActivity_MapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(new com.mapbox.mapboxsdk.maps.OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mMapboxMap = mapboxMap;
                mMapboxMap.setStyle(Style.LIGHT);
                mMapboxMap.addOnScaleListener(mOnScaleListener);
                mMapboxMap.addOnCameraMoveListener(onCameraMoveListener);
                mapboxMap.setInfoWindowAdapter(mInfoWindowAdapter);

                mLocationEngine = new LocationEngineProvider(MapsActivity.this)
                        .obtainBestLocationEngineAvailable();
                mLocationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
                mLocationEngine.addLocationEngineListener(mLocationEngineListener);
                mLocationEngine.activate();

                mLocationLayerPlugin = new LocationLayerPlugin(
                        mMapView,
                        mapboxMap,
                        mLocationEngine
                );
                LocalizationPlugin localizationPlugin = new LocalizationPlugin(mMapView, mapboxMap);

                try {
                    localizationPlugin.matchMapLanguageWithDeviceDefault();
                } catch (RuntimeException exception) {
                    Timber.d(exception.toString());
                }

                int defaultMargin = DisplayUnitUtil.dpToPx(
                        MapsActivity.this,
                        MAP_DEFAULT_MARGIN_DP
                );
                int compassTopMargin = DisplayUnitUtil.dpToPx(
                        MapsActivity.this,
                        MAP_COMPASS_TOP_MARGIN_DP
                );
                int logoBottomMargin = DisplayUnitUtil.dpToPx(
                        MapsActivity.this,
                        MAP_LOGO_BOTTOM_MARGIN_DP
                );
                int leftMargin = mMapboxMap.getUiSettings().getAttributionMarginLeft() -
                        mMapboxMap.getUiSettings().getLogoMarginLeft() +
                        defaultMargin;
                mMapboxMap.getUiSettings().setCompassMargins(
                        defaultMargin,
                        compassTopMargin,
                        defaultMargin,
                        defaultMargin
                );
                mMapboxMap.getUiSettings().setLogoMargins(
                        defaultMargin,
                        defaultMargin,
                        defaultMargin,
                        logoBottomMargin
                );
                mMapboxMap.getUiSettings().setAttributionMargins(
                        leftMargin,
                        defaultMargin,
                        defaultMargin,
                        logoBottomMargin
                );

                mLocationLayerPlugin.addOnCameraTrackingChangedListener(
                        mOnCameraTrackingChangedListener
                );
            }
        });

        mSession = Session.getInstance();

        ClearableCookieJar clearableCookieJar = new PersistentCookieJar(
                new SetCookieCache(), new SharedPrefsCookiePersistor(getApplicationContext())
        );
        mOkHttpClient = new OkHttpClient.Builder().cookieJar(clearableCookieJar).build();

        mMarkerList = new ArrayList<>();
        mImageMemoList = new ArrayList<>();
        mGroupImageMemoList = new ArrayList<>();

        mRequestImageMemoState = false;

        if (mSession.isHaveSession()) {
            startActivity(new Intent(this, SplashActivity.class));
        } else {
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            startActivity(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();

        if (mLocationEngine != null) {
            mLocationEngine.addLocationEngineListener(mLocationEngineListener);
            if (mLocationEngine.isConnected()) {
                mLocationEngine.requestLocationUpdates();
            } else {
                mLocationEngine.activate();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();

        requestAccessFineLocationPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();

        if (mLocationEngine != null) {
            mLocationEngine.removeLocationEngineListener(mLocationEngineListener);
            mLocationEngine.removeLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();

        if (mLocationEngine != null) {
            mLocationEngine.deactivate();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_ACCESS_FINE_LOCATION_PERMISSION: {
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
                            .setMessage(R.string.dialog_message_permission)
                            .show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CAMERA_ACTIVITY:
                mSeekBar.setEnabled(true);

                break;
        }
    }

    private String getZoolLevel(double zoom) {
        if (zoom < 6) {
            return "country";
        } else if (zoom < 9) {
            return "state";
        } else if (zoom < 12) {
            return "city";
        } else {
            return "town";
        }
    }

    private void requestAccessFineLocationPermission() {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(
                            MapsActivity.this,
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

    private void requestGroupImageMemos(String type, double lng, double lat) {
        if (mRequestImageMemoState) {
            return;
        }

        mRequestImageMemoState = true;
        Request request = new Request.Builder()
                .url(
                        Session.HOST +
                                "/api/memos/group/" +
                                type +
                                "?lng=" +
                                Double.toString(lng) +
                                "&lat=" +
                                Double.toString(lat)
                )
                .get()
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        MapsActivity.this
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
                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                        MapsActivity.this
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
                            if (!response.isSuccessful()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                        MapsActivity.this
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
                                    GroupImageMemo groupImageMemo = new GroupImageMemo(
                                            jsonArray.optJSONObject(i)
                                    );
                                    mGroupImageMemoList.add(groupImageMemo);

                                    Location location = groupImageMemo.getLocation();
                                    LatLng latLng = new LatLng(
                                            location.getLatitude(),
                                            location.getLongitude()
                                    );
                                    Marker marker = mMapboxMap.addMarker(
                                            new MarkerOptions()
                                                    .setTitle(groupImageMemo.getId())
                                                    .setPosition(latLng)
                                    );

                                    mMarkerList.add(marker);
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
}
