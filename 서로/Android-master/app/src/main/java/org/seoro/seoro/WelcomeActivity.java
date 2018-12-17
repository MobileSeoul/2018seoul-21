package org.seoro.seoro;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.json.JSONException;
import org.json.JSONObject;
import org.seoro.seoro.auth.Session;
import org.seoro.seoro.model.User;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WelcomeActivity extends AppCompatActivity {
    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_PASSWORD = "password";

    private OkHttpClient mOkHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        ConstraintLayout constraintLayout = findViewById(R.id.WelcomeActivity_ConstraintLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        ClearableCookieJar clearableCookieJar = new PersistentCookieJar(
                new SetCookieCache(), new SharedPrefsCookiePersistor(getApplicationContext())
        );
        mOkHttpClient = new OkHttpClient.Builder().cookieJar(clearableCookieJar).build();

        String email = getIntent().getStringExtra(PARAM_EMAIL);
        String password = getIntent().getStringExtra(PARAM_PASSWORD);

        requestLogin(email, password);
    }

    private void requestLogin(String email, String password) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("email", email);
            jsonObject.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(Session.HOST + "/api/auth/session")
                .post(requestBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                WelcomeActivity.this
                        );
                        builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                moveTaskToBack(true);
                                android.os.Process.killProcess(
                                        android.os.Process.myPid()
                                );
                                System.exit(1);
                            }
                        }).setTitle(R.string.dialog_title_error)
                                .setMessage(R.string.dialog_message_network_error)
                                .show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                        WelcomeActivity.this
                                );
                                builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        moveTaskToBack(true);
                                        android.os.Process.killProcess(
                                                android.os.Process.myPid()
                                        );
                                        System.exit(1);
                                    }
                                }).setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_message_network_error)
                                        .show();
                            }
                        });

                        return;
                    }

                    String responseBodyString = responseBody.string();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!response.isSuccessful()) {
                                moveTaskToBack(true);
                                android.os.Process.killProcess(
                                        android.os.Process.myPid()
                                );
                                System.exit(1);

                                return;
                            }

                            try {
                                JSONObject responseJSONObject = new JSONObject(responseBodyString);
                                User user = new User(responseJSONObject.optJSONObject("data"));

                                Session.getInstance().setUser(user);

                                Intent intent = new Intent(
                                        WelcomeActivity.this,
                                        MapsActivity.class
                                );

                                startActivity(intent);
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
