package org.seoro.seoro;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;

import org.seoro.seoro.auth.Session;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        /*
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        */

        if (!Session.getInstance().isHaveSession()) {
            startActivity(new Intent(this, MapsActivity.class));
            finish();
            return;
        }

        SeekBar seekBar = (SeekBar) findViewById(R.id.SplashActivity_SeekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 100) {
                    seekBar.setEnabled(false);
                    seekBar.setProgress(0);
                    //startActivity(new Intent(SplashActivity.this, MapsActivity.class));
                    finish();
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

    }

}
