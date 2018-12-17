package org.seoro.seoro;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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

public class MainActivity extends AppCompatActivity {
    private OkHttpClient mOkHttpClient;

    private AccountManager mAccountManager;


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ClearableCookieJar clearableCookieJar = new PersistentCookieJar(
                new SetCookieCache(), new SharedPrefsCookiePersistor(getApplicationContext())
        );
        mOkHttpClient = new OkHttpClient.Builder().cookieJar(clearableCookieJar).build();

        mAccountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAccountManager == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_title_permission)
                    .setMessage(R.string.dialog_message_account_service)
                    .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();

            return;
        }

        if (mAccountManager.getAccountsByType(SignInActivity.ACCOUNT_TYPE).length != 0) {
            Account account = mAccountManager.getAccountsByType(SignInActivity.ACCOUNT_TYPE)[0];

            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("email", account.name);
                jsonObject.put("password", mAccountManager.getPassword(account));
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
                                    MainActivity.this
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
                                            MainActivity.this
                                    );
                                    builder.setNeutralButton(R.string.dialog_ok, null)
                                            .setTitle(R.string.dialog_title_error)
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
                                // showProgress(false);

                                if (!response.isSuccessful()) {
                                    if (response.code() == 400) {
                                        Intent intent = new Intent(
                                                MainActivity.this,
                                                SignInActivity.class
                                        );
                                        intent.putExtra(
                                                SignInActivity.ARG_ADD_ACCOUNT,
                                                true)
                                        ;

                                        startActivity(intent);
                                    } else {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                                MainActivity.this
                                        );
                                        builder.setNeutralButton(R.string.dialog_ok, null)
                                                .setTitle(R.string.dialog_title_error)
                                                .setMessage(R.string.dialog_message_server_error)
                                                .show();
                                    }

                                    return;
                                }

                                try {
                                    JSONObject responseJSONObject = new JSONObject(responseBodyString);
                                    User user = new User(responseJSONObject.optJSONObject("data"));

                                    Session.getInstance().setUser(user);

                                    Intent intent = new Intent(
                                            MainActivity.this,
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
        } else {
            startActivity(new Intent(this, MapsActivity.class));
        }
    }
}
