package org.seoro.seoro.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ShaderProgram {
    public static final float[] ON_SCREEN_POSITIONS = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
    public static final float[] OFF_SCREEN_POSITIONS = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
    public static final float[] COORDINATES = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};
    public static final float[] MIRROR_COORDINATES = {1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};

    private Context mContext;

    private int mProgram;
    private int mVertexShader;
    private int mFragmentShader;

    private String mVertexShaderString;
    private String mFragmentShaderString;


    public ShaderProgram(Context context, int vertexShaderRawResource, int fragmentShaderRawResource) throws Exception {
        mContext = context;

        mVertexShaderString = loadShaderString(vertexShaderRawResource);
        mFragmentShaderString = loadShaderString(fragmentShaderRawResource);
    }

    public void create() throws Exception {
        mProgram = GLES20.glCreateProgram();

        if (mProgram == 0) {
            throw new Exception();
        } else {
            int[] linkStatus = new int[1];
            mVertexShader = compileShader(GLES20.GL_VERTEX_SHADER, mVertexShaderString);
            mFragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShaderString);

            GLES20.glAttachShader(mProgram, mVertexShader);
            GLES20.glAttachShader(mProgram, mFragmentShader);
            GLES20.glLinkProgram(mProgram);
            GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);

            if (linkStatus[0] != GLES20.GL_TRUE) {
                String infoLog = GLES20.glGetProgramInfoLog(mProgram);

                delete();

                throw new Exception(infoLog);
            }
        }
    }

    public int compileShader(int type, String shaderSource) throws Exception {
        int shader = GLES20.glCreateShader(type);

        if (shader == 0) {
            throw new Exception();
        } else {
            int[] shaderStatus = new int[1];

            GLES20.glShaderSource(shader, shaderSource);
            GLES20.glCompileShader(shader);
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, shaderStatus, 0);

            if (shaderStatus[0] == 0) {
                String infoLog = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);

                throw new Exception(infoLog);
            }
        }

        return shader;
    }

    public String loadShaderString(int rawResource) throws IOException {

        InputStream inputStream = mContext.getResources().openRawResource(rawResource);
        String s = IOUtils.toString(inputStream, "UTF-8");

        inputStream.close();

        return s;
    }

    public static void clear() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    public static void viewport(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public static int addTexture(int textureLocation, Bitmap bitmap, String uniformName, boolean recycle) {
        int[] textureId = new int[1];

        GLES20.glActiveTexture(textureLocation);
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        if(recycle) {
            bitmap.recycle();
        }

        return textureId[0];
    }

    public static int addExternalTexture(int textureLocation) {
        int[] textureId = new int[1];

        GLES20.glActiveTexture(textureLocation);
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        return textureId[0];
    }

    public static void updateTexture(int textureLocation, int textureId, Bitmap drawingCache) {
        GLES20.glActiveTexture(textureLocation);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, drawingCache);

        drawingCache.recycle();
    }

    public static void deleteTexture(int textureId) {
        int[] textureIds = new int[] {textureId};

        GLES20.glDeleteTextures(1, textureIds, 0);
    }

    public static void draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public static void drawElements(ShortBuffer shortBuffer, int length) {
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                length,
                GLES20.GL_UNSIGNED_SHORT,
                shortBuffer
        );
    }

    public void use() {
        GLES20.glUseProgram(mProgram);
    }

    public void delete(){
        GLES20.glDeleteShader(mVertexShader);
        GLES20.glDeleteShader(mFragmentShader);
        GLES20.glDeleteProgram(mProgram);

        mProgram = 0;
        mVertexShader = 0;
        mFragmentShader = 0;
    }

    public void addAttributeFloatArray(String attributeName, float[] floats) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * floats.length);
        byteBuffer.order(ByteOrder.nativeOrder());

        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(floats);
        floatBuffer.position(0);

        int attributeLocation = GLES20.glGetAttribLocation(mProgram, attributeName);

        GLES20.glEnableVertexAttribArray(attributeLocation);
        GLES20.glVertexAttribPointer(
                attributeLocation,
                2,
                GLES20.GL_FLOAT,
                false,
                4 * 2,
                floatBuffer
        );
    }

    public void addUniformTexture(int textureLocation, int textureId, String uniformName) {
        int uniformLocation = GLES20.glGetUniformLocation(mProgram, uniformName);

        GLES20.glActiveTexture(textureLocation);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uniformLocation, textureLocation);
    }

    public void addUniformExternalTexture(int textureLocation, int textureId, String uniformName) {
        int uniformLocation = GLES20.glGetUniformLocation(mProgram, uniformName);

        GLES20.glActiveTexture(textureLocation);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(uniformLocation, textureLocation);
    }

    public void addUniformMatrix4v(float[] matrix, String uniformName) {
        int uniformLocation = GLES20.glGetUniformLocation(mProgram, uniformName);

        GLES20.glUniformMatrix4fv(uniformLocation, 1, false, matrix, 0);
    }

    public void disableAttribute(String attributeName) {
        int attributeLocation = GLES20.glGetAttribLocation(mProgram, attributeName);

        GLES20.glDisableVertexAttribArray(attributeLocation);
    }

    public boolean isCreated() {
        return mProgram != 0;
    }
}
