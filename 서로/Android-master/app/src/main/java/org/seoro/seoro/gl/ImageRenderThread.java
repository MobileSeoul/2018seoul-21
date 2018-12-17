package org.seoro.seoro.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.opengl.GLES20;

import org.seoro.seoro.util.EGLUtil;

import java.nio.ByteBuffer;
import java.util.List;

public class ImageRenderThread extends Thread {
    public interface OnRendererFinishedListener {
        void onRendererFinished(Bitmap bitmap);
    }

    private Context mContext;

    private EGLUtil mEGLUtil;
    private EGLSurface mEGLSurface;

    private OnRendererFinishedListener mOnRendererFinishedListener;

    private List<ShaderProgram> mShaderProgramList;
    private Bitmap mBitmap;

    private float[] mCameraTransformMatrix = new float[16];
    private float[] mCoordinates = ShaderProgram.COORDINATES;

    private int mWidth;
    private int mHeight;
    private int mImageTexture;
    private int mCurrentShaderProgramIndex = 0;

    public ImageRenderThread(Context context, List<ShaderProgram> shaderProgramList) {
        mContext = context;
        mShaderProgramList = shaderProgramList;
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public void run() {
        super.run();

        mEGLUtil = new EGLUtil();
        mEGLSurface = mEGLUtil.createOffscreenSurface(mWidth, mHeight);

        mEGLUtil.makeCurrent(mEGLSurface);

        for (ShaderProgram shaderProgram: mShaderProgramList) {
            try {
                shaderProgram.create();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mImageTexture = ShaderProgram.addTexture(
                GLES20.GL_TEXTURE0,
                mBitmap,
                "imageTexture",
                true
        );

        draw();
        mEGLUtil.swapBuffers(mEGLSurface);

        mShaderProgramList.get(mCurrentShaderProgramIndex).delete();
        mEGLUtil.releaseSurface(mEGLSurface);

        mEGLSurface = EGL14.EGL_NO_SURFACE;

        mEGLUtil.release();
    }

    public void draw() {
        ShaderProgram.clear();
        ShaderProgram.viewport(mWidth, mHeight);

        ShaderProgram shaderProgram = mShaderProgramList.get(mCurrentShaderProgramIndex);

        shaderProgram.use();
        shaderProgram.addAttributeFloatArray(
                "position",
                ShaderProgram.OFF_SCREEN_POSITIONS
        );
        shaderProgram.addAttributeFloatArray(
                "coordinate",
                mCoordinates
        );
        shaderProgram.addUniformMatrix4v(mCameraTransformMatrix, "cameraTransform");

        ShaderProgram.draw();

        ByteBuffer byteBuffer = ByteBuffer.allocate(mWidth * mHeight * 4);
        GLES20.glReadPixels(
                0,
                0,
                mWidth,
                mHeight,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                byteBuffer
        );
        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(byteBuffer);

        shaderProgram.disableAttribute("position");
        shaderProgram.disableAttribute("coordinate");

        mOnRendererFinishedListener.onRendererFinished(bitmap);
    }

    public void useShaderProgram(int shaderProgramIndex) {
        mCurrentShaderProgramIndex = shaderProgramIndex;
    }

    public void setOnRendererFinishedListener(OnRendererFinishedListener onRendererFinishedListener) {
        this.mOnRendererFinishedListener = onRendererFinishedListener;
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public void setCameraTransformMatrix(float[] cameraTransformMatrix) {
        this.mCameraTransformMatrix = cameraTransformMatrix;
    }

    public void setCoordinates(float[] coordinates) {
        this.mCoordinates = coordinates;
    }

    public void setWidth(int width) {
        this.mHeight = width;
    }

    public void setHeight(int height) {
        this.mWidth = height;
    }
}
