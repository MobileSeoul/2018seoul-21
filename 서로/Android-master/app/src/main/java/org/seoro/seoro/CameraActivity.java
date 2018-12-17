package org.seoro.seoro;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

public class CameraActivity extends AppCompatActivity {
    // https://github.com/googlecreativelab/shadercam
    // https://github.com/yulu/ShaderCam

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.CameraActivity_FrameLayout, CameraFilterFragment.newInstance())
                    .commit();
        }
    }
}
