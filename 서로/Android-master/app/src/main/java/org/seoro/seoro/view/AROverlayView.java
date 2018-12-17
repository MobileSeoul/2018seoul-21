package org.seoro.seoro.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.seoro.seoro.glide.GlideApp;
import org.seoro.seoro.model.ImageMemo;
import org.seoro.seoro.util.DisplayUnitUtil;

import java.util.ArrayList;
import java.util.List;

public class AROverlayView extends View {
    private static final int TEXT_SIZE = 16;
    private static final int CARD_WIDTH = 48;
    private static final int CARD_HEIGHT = 48;
    private static final int CARD_MARGIN = 8;
    private static final int CARD_BORDER_RADIUS = 8;

    private final static double WGS84_A = 6378137.0;
    private final static double WGS84_E2 = 0.00669437999014;

    private final Paint mPaint;

    private final int mTextSizePx;
    private final int mWidthHalfPx;
    private final int mHeightHalfPx;
    private final int mMarginPx;
    private final int mRadiusPx;

    private Context mContext;

    private Location mCurrentLocation;
    private List<ImageMemo> mImageMemoList;
    private List<Float> mFloatList;
    private List<float[]> mFloatsList;
    private float[] mRotatedProjectionMatrix;

    public AROverlayView(Context context) {
        super(context);

        mContext = context;

        mTextSizePx = DisplayUnitUtil.spToPx(context, TEXT_SIZE);
        mWidthHalfPx = DisplayUnitUtil.dpToPx(context, CARD_WIDTH);
        mHeightHalfPx = DisplayUnitUtil.dpToPx(context, CARD_HEIGHT);
        mMarginPx = DisplayUnitUtil.dpToPx(context, CARD_MARGIN);
        mRadiusPx = DisplayUnitUtil.dpToPx(context, CARD_BORDER_RADIUS);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        mPaint.setTextSize(mTextSizePx);

        mFloatList = new ArrayList<>();
        mFloatsList = new ArrayList<>();
        mRotatedProjectionMatrix = new float[16];

        init();
    }

    public AROverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        mTextSizePx = DisplayUnitUtil.spToPx(context, TEXT_SIZE);
        mWidthHalfPx = DisplayUnitUtil.dpToPx(context, CARD_WIDTH);
        mHeightHalfPx = DisplayUnitUtil.dpToPx(context, CARD_HEIGHT);
        mMarginPx = DisplayUnitUtil.dpToPx(context, CARD_MARGIN);
        mRadiusPx = DisplayUnitUtil.dpToPx(context, CARD_BORDER_RADIUS);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        mPaint.setTextSize(mTextSizePx);

        mFloatList = new ArrayList<>();
        mFloatsList = new ArrayList<>();
        mRotatedProjectionMatrix = new float[16];

        init();
    }

    public AROverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;

        mTextSizePx = DisplayUnitUtil.spToPx(context, TEXT_SIZE);
        mWidthHalfPx = DisplayUnitUtil.dpToPx(context, CARD_WIDTH);
        mHeightHalfPx = DisplayUnitUtil.dpToPx(context, CARD_HEIGHT);
        mMarginPx = DisplayUnitUtil.dpToPx(context, CARD_MARGIN);
        mRadiusPx = DisplayUnitUtil.dpToPx(context, CARD_BORDER_RADIUS);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        mPaint.setTextSize(mTextSizePx);

        mFloatList = new ArrayList<>();
        mFloatsList = new ArrayList<>();
        mRotatedProjectionMatrix = new float[16];

        init();
    }

    public AROverlayView(Context context, @Nullable AttributeSet attrs,
                         int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mContext = context;

        mTextSizePx = DisplayUnitUtil.spToPx(context, TEXT_SIZE);
        mWidthHalfPx = DisplayUnitUtil.dpToPx(context, CARD_WIDTH);
        mHeightHalfPx = DisplayUnitUtil.dpToPx(context, CARD_HEIGHT);
        mMarginPx = DisplayUnitUtil.dpToPx(context, CARD_MARGIN);
        mRadiusPx = DisplayUnitUtil.dpToPx(context, CARD_BORDER_RADIUS);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        mPaint.setTextSize(mTextSizePx);

        mFloatList = new ArrayList<>();
        mFloatsList = new ArrayList<>();
        mRotatedProjectionMatrix = new float[16];

        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mCurrentLocation == null) {
            return;
        }

        for (int i = 0; i < mFloatsList.size(); i++) {
            float distance = mFloatList.get(i);
            float[] cameraCoordinateVector = mFloatsList.get(i);
            ImageMemo imageMemo = mImageMemoList.get(i);

            if (cameraCoordinateVector[2] < 0 && distance < 10) {
                double ratio = (1.0 - (distance / 10.0));
                float x  = (0.5f + cameraCoordinateVector[0] / cameraCoordinateVector[3])
                        * canvas.getWidth();
                float y = (0.5f - cameraCoordinateVector[1] / cameraCoordinateVector[3])
                        * canvas.getHeight();

                int halfWidth = (int) (mWidthHalfPx * ratio);
                int halfHeight = (int) (mHeightHalfPx * ratio);
                int radius = (int) (mRadiusPx * ratio);

                mPaint.setAlpha((int) (ratio * 255));
                mPaint.setTextSize((float) (mTextSizePx * ratio));

                if (imageMemo.getTempBitmap() != null) {
                    Rect rect = new Rect(
                            (int) x,
                            (int) y,
                            halfWidth * 2,
                            halfHeight * 2
                    );

                    canvas.drawBitmap(imageMemo.getTempBitmap(), null, rect, mPaint);
                }

                /*
                canvas.drawRoundRect(
                        x - halfWidth,
                        y - halfHeight,
                        x + halfWidth,
                        y + halfHeight,
                        radius,
                        radius,
                        mPaint
                );
                */
            }
        }
    }

    private void init() {
        mImageMemoList = new ArrayList<>();
    }

    public void updateRotatedProjectionMatrix(float[] rotatedProjectionMatrix) {
        mRotatedProjectionMatrix = rotatedProjectionMatrix;

        if (mCurrentLocation != null) {
            mFloatsList.clear();

            for (ImageMemo imageMemo: mImageMemoList) {
                float[] currentLocationInECEF = WSG84toECEF(mCurrentLocation);
                float[] pointInECEF = WSG84toECEF(imageMemo.getLocation());
                float[] pointInENU = ECEFtoENU(
                        mCurrentLocation,
                        currentLocationInECEF,
                        pointInECEF
                );

                float[] cameraCoordinateVector = new float[4];
                Matrix.multiplyMV(
                        cameraCoordinateVector,
                        0,
                        mRotatedProjectionMatrix,
                        0,
                        pointInENU,
                        0
                );

                mFloatsList.add(cameraCoordinateVector);
            }
        }

        invalidate();
    }

    public void updateCurrentLocation(Location currentLocation) {
        mCurrentLocation = currentLocation;

        mFloatList.clear();
        mFloatsList.clear();

        for (ImageMemo imageMemo: mImageMemoList) {
            mFloatList.add(currentLocation.distanceTo(imageMemo.getLocation()));

            float[] currentLocationInECEF = WSG84toECEF(mCurrentLocation);
            float[] pointInECEF = WSG84toECEF(imageMemo.getLocation());
            float[] pointInENU = ECEFtoENU(mCurrentLocation, currentLocationInECEF, pointInECEF);

            float[] cameraCoordinateVector = new float[4];
            Matrix.multiplyMV(
                    cameraCoordinateVector,
                    0,
                    mRotatedProjectionMatrix,
                    0,
                    pointInENU,
                    0
            );

            mFloatsList.add(cameraCoordinateVector);
        }

        invalidate();
    }

    public void updateImageMemoList(List<ImageMemo> imageMemoList) {
        mImageMemoList = imageMemoList;

        mFloatList.clear();
        mFloatsList.clear();

        for (ImageMemo imageMemo: mImageMemoList) {
            mFloatList.add(mCurrentLocation.distanceTo(imageMemo.getLocation()));

            float[] currentLocationInECEF = WSG84toECEF(mCurrentLocation);
            float[] pointInECEF = WSG84toECEF(imageMemo.getLocation());
            float[] pointInENU = ECEFtoENU(mCurrentLocation, currentLocationInECEF, pointInECEF);

            float[] cameraCoordinateVector = new float[4];
            Matrix.multiplyMV(
                    cameraCoordinateVector,
                    0,
                    mRotatedProjectionMatrix,
                    0,
                    pointInENU,
                    0
            );

            mFloatsList.add(cameraCoordinateVector);

            GlideApp.with(mContext)
                    .asBitmap()
                    .load(imageMemo.getThumbnail())
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            imageMemo.setTempBitmap(resource);
                        }
                    });
        }

        invalidate();
    }

    private static float[] WSG84toECEF(Location location) {
        double radLat = Math.toRadians(location.getLatitude());
        double radLon = Math.toRadians(location.getLongitude());

        float clat = (float) Math.cos(radLat);
        float slat = (float) Math.sin(radLat);
        float clon = (float) Math.cos(radLon);
        float slon = (float) Math.sin(radLon);

        float N = (float) (WGS84_A / Math.sqrt(1.0 - WGS84_E2 * slat * slat));

        float x = (float) ((N + location.getAltitude()) * clat * clon);
        float y = (float) ((N + location.getAltitude()) * clat * slon);
        float z = (float) ((N * (1.0 - WGS84_E2) + location.getAltitude()) * slat);

        return new float[] {x , y, z};
    }

    private static float[] ECEFtoENU(Location currentLocation,
                                     float[] ecefCurrentLocation, float[] ecefPOI) {
        double radLat = Math.toRadians(currentLocation.getLatitude());
        double radLon = Math.toRadians(currentLocation.getLongitude());

        float clat = (float)Math.cos(radLat);
        float slat = (float)Math.sin(radLat);
        float clon = (float)Math.cos(radLon);
        float slon = (float)Math.sin(radLon);

        float dx = ecefCurrentLocation[0] - ecefPOI[0];
        float dy = ecefCurrentLocation[1] - ecefPOI[1];
        float dz = ecefCurrentLocation[2] - ecefPOI[2];

        float east = -slon*dx + clon*dy;

        float north = -slat*clon*dx - slat*slon*dy + clat*dz;

        float up = clat*clon*dx + clat*slon*dy + slat*dz;

        return new float[] {east , north, up, 1};
    }
}
