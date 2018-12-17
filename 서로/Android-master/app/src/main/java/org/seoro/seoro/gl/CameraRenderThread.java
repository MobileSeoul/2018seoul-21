package org.seoro.seoro.gl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.seoro.seoro.util.EGLUtil;

import java.lang.ref.WeakReference;
import java.util.List;

public class CameraRenderThread extends Thread {
    public interface OnRendererReadyListener {
        void onRendererReady();
        void onRendererFinished();
    }

    public static class CameraRenderThreadHandler extends Handler {
        private static final int MESSAGE_SHUTDOWN = 0;

        private WeakReference<CameraRenderThread> mCameraRenderThreadWeakReference;

        public CameraRenderThreadHandler(CameraRenderThread cameraRenderThread) {
            mCameraRenderThreadWeakReference = new WeakReference<CameraRenderThread>(
                    cameraRenderThread
            );
        }

        public void sendShutdownMessage() {
            sendMessage(obtainMessage(MESSAGE_SHUTDOWN));
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            CameraRenderThread cameraRenderThread = mCameraRenderThreadWeakReference.get();

            switch (msg.what) {
                case MESSAGE_SHUTDOWN: {
                    cameraRenderThread.shutdown();

                    break;
                }
            }
        }
    }

    private Context mContext;

    private SurfaceTexture mCameraSurfaceTexture;
    private SurfaceTexture mPreviewSurfaceTexture;

    private EGLUtil mEGLUtil;
    private EGLSurface mEGLSurface;

    private CameraRenderThreadHandler mCameraRenderThreadHandler;
    private OnRendererReadyListener mOnRendererReadyListener;

    private List<ShaderProgram> mShaderProgramList;

    private float[] mCameraTransformMatrix = new float[16];
    private float[] mCoordinates = ShaderProgram.COORDINATES;

    private int mWidth;
    private int mHeight;
    private int mCameraTexture;
    private int mCurrentShaderProgramIndex;

    private SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            synchronized (this) {
                if (mShaderProgramList.get(mCurrentShaderProgramIndex).isCreated()) {
                    mEGLUtil.makeCurrent(mEGLSurface);

                    mCameraSurfaceTexture.updateTexImage();
                    mCameraSurfaceTexture.getTransformMatrix(mCameraTransformMatrix);

                    draw();

                    mEGLUtil.swapBuffers(mEGLSurface);
                }
            }
        }
    };

    public CameraRenderThread(Context context, List<ShaderProgram> shaderProgramList) {
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

        Looper.prepare();

        mCameraRenderThreadHandler = new CameraRenderThreadHandler(this);

        mEGLUtil = new EGLUtil();
        mEGLSurface = mEGLUtil.createWindowSurfaceTexture(mPreviewSurfaceTexture);

        mEGLUtil.makeCurrent(mEGLSurface);

        for (ShaderProgram shaderProgram: mShaderProgramList) {
            try {
                shaderProgram.create();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mCameraTexture = ShaderProgram.addExternalTexture(GLES20.GL_TEXTURE0);
        mCameraSurfaceTexture = new SurfaceTexture(mCameraTexture);
        mCameraSurfaceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);

        if (mOnRendererReadyListener != null) {
            mOnRendererReadyListener.onRendererReady();
        }

        Looper.loop();

        mShaderProgramList.get(mCurrentShaderProgramIndex).delete();
        mCameraSurfaceTexture.release();
        mCameraSurfaceTexture.setOnFrameAvailableListener(null);
        mEGLUtil.releaseSurface(mEGLSurface);

        mEGLSurface = EGL14.EGL_NO_SURFACE;

        mEGLUtil.release();

        if (mOnRendererReadyListener != null) {
            mOnRendererReadyListener.onRendererFinished();
        }
    }

    public void draw() {
        ShaderProgram.clear();
        ShaderProgram.viewport(mWidth, mHeight);

        ShaderProgram shaderProgram = mShaderProgramList.get(mCurrentShaderProgramIndex);

        shaderProgram.use();
        shaderProgram.addAttributeFloatArray(
                "position",
                ShaderProgram.ON_SCREEN_POSITIONS
        );
        shaderProgram.addAttributeFloatArray(
                "coordinate",
                mCoordinates
        );
        shaderProgram.addUniformMatrix4v(mCameraTransformMatrix, "cameraTransform");

        ShaderProgram.draw();

        shaderProgram.disableAttribute("position");
        shaderProgram.disableAttribute("coordinate");
    }

    public void shutdown() {
        Looper looper = Looper.myLooper();

        if (looper != null) {
            looper.quit();
        }
    }

    public void useShaderProgram(int shaderProgramIndex) {
        mCurrentShaderProgramIndex = shaderProgramIndex;
    }

    public SurfaceTexture getCameraSurfaceTexture() {
        return mCameraSurfaceTexture;
    }

    public float[] getCameraTransformMatrix() {
        return mCameraTransformMatrix;
    }

    public CameraRenderThreadHandler getCameraRenderThreadHandler() {
        return mCameraRenderThreadHandler;
    }

    public void setPreviewSurfaceTexture(SurfaceTexture previewSurfaceTexture) {
        synchronized (this) {
            this.mPreviewSurfaceTexture = previewSurfaceTexture;

            if (mEGLUtil != null) {
                mEGLUtil.releaseSurface(mEGLSurface);
                mEGLSurface = mEGLUtil.createWindowSurfaceTexture(mPreviewSurfaceTexture);
                //mEGLUtil.makeCurrent(mEGLSurface);
            }
        }
    }

    public void setOnRendererReadyListener(OnRendererReadyListener onRendererReadyListener) {
        this.mOnRendererReadyListener = onRendererReadyListener;
    }

    public void setCoordinates(float[] coordinates) {
        this.mCoordinates = coordinates;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }
}
