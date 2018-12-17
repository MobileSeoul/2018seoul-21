package org.seoro.seoro;

import android.app.Application;

import com.mapbox.mapboxsdk.Mapbox;

public class ThisApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Mapbox.getInstance(getApplicationContext(), getString(R.string.mapbox_access_token));
    }
}
