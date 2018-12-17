package org.seoro.seoro;

import android.content.DialogInterface;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;

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

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText mCurrentPasswordEditText;
    private EditText mNewPasswordEditText;
    private EditText mConfirmPasswordEditText;
    private CheckBox mNewPasswordCheckBox;
    private CheckBox mConfirmPasswordCheckBox;
    private SeekBar mSeekBar;
    private ProgressBar mProgressBar;

    private OkHttpClient mOkHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
        );
        setContentView(R.layout.activity_change_password);

        mCurrentPasswordEditText = findViewById(
                R.id.ChangePasswordActivity_CurrentPasswordEditText
        );
        mNewPasswordEditText = findViewById(
                R.id.ChangePasswordActivity_NewPasswordEditText
        );
        mConfirmPasswordEditText = findViewById(
                R.id.ChangePasswordActivity_ConfirmPasswordEditText
        );
        mNewPasswordCheckBox = findViewById(
                R.id.ChangePasswordActivity_NewPasswordCheckBox
        );
        mConfirmPasswordCheckBox = findViewById(
                R.id.ChangePasswordActivity_ConfirmPasswordCheckBox
        );
        mSeekBar = findViewById(R.id.ChangePasswordActivity_SeekBar);
        mProgressBar = findViewById(R.id.ChangePasswordActivity_ProgressBar);

        mNewPasswordCheckBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString()) || !isPasswordValid(s.toString())) {
                    mNewPasswordCheckBox.setChecked(false);
                    mSeekBar.setEnabled(true);
                } else {
                    mNewPasswordCheckBox.setChecked(true);
                    mSeekBar.setEnabled(false);
                }
            }
        });
        mConfirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString()) ||
                        !mNewPasswordCheckBox.getText().toString().equals(s.toString())) {
                    mConfirmPasswordCheckBox.setChecked(false);
                    mSeekBar.setEnabled(true);
                } else {
                    mConfirmPasswordCheckBox.setChecked(true);
                    mSeekBar.setEnabled(false);
                }
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 100) {
                    seekBar.setEnabled(false);
                    seekBar.setProgress(0);

                    attemptChangePassword();
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

        ClearableCookieJar clearableCookieJar = new PersistentCookieJar(
                new SetCookieCache(), new SharedPrefsCookiePersistor(getApplicationContext())
        );
        mOkHttpClient = new OkHttpClient.Builder().cookieJar(clearableCookieJar).build();
    }

    private void attemptChangePassword() {
        String currentPassword = mCurrentPasswordEditText.getText().toString();
        String newPassword = mNewPasswordCheckBox.getText().toString();
        String confirmPassword = mConfirmPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(newPassword) || !isPasswordValid(newPassword)) {
            mNewPasswordCheckBox.setChecked(false);
            mNewPasswordEditText.requestFocus();

            mSeekBar.setEnabled(false);

            return;
        }

        if (TextUtils.isEmpty(confirmPassword) || !newPassword.equals(confirmPassword)) {
            mConfirmPasswordCheckBox.setChecked(false);
            mConfirmPasswordEditText.requestFocus();

            mSeekBar.setEnabled(false);

            return;
        }

        postChangePassword(
                Session.getInstance().getUser().getEmail(),
                currentPassword,
                newPassword
        );
    }

    private void postChangePassword(String email, String currentPassword, String newPassword) {
        mProgressBar.setVisibility(View.VISIBLE);
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("email", email)
                    .put("password", currentPassword)
                    .put("newPassword", newPassword);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(Session.HOST + "/api/users/password")
                .post(requestBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.GONE);

                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                ChangePasswordActivity.this
                        );
                        builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mNewPasswordEditText.requestFocus();
                            }
                        }).setTitle(R.string.dialog_title_error)
                                .setMessage(R.string.dialog_message_network_error)
                                .show();

                        mSeekBar.setEnabled(true);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.setVisibility(View.GONE);

                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                        ChangePasswordActivity.this
                                );
                                builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mNewPasswordEditText.requestFocus();
                                    }
                                }).setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_message_network_error)
                                        .show();

                                mSeekBar.setEnabled(true);
                            }
                        });

                        return;
                    }

                    String responseBodyString = responseBody.string();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setVisibility(View.GONE);
                            mSeekBar.setEnabled(true);

                            try {
                                if (response.isSuccessful()) {
                                    finish();

                                    return;
                                }

                                JSONObject responseJSONObject = new JSONObject(responseBodyString);

                                if (response.code() == 400) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(
                                            ChangePasswordActivity.this
                                    );
                                    builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mCurrentPasswordEditText.requestFocus();
                                        }
                                    }).setTitle(R.string.dialog_title_error)
                                            .setMessage(R.string.dialog_message_sign_in_failed)
                                            .show();
                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(
                                            ChangePasswordActivity.this
                                    );
                                    builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mCurrentPasswordEditText.requestFocus();
                                        }
                                    }).setTitle(R.string.dialog_title_error)
                                            .setMessage(R.string.dialog_message_server_error)
                                            .show();
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

    private boolean isPasswordValid(String password) {
        return User.PASSWORD_PATTERN.matcher(password).find();
    }
}
