package org.seoro.seoro;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;

import org.json.JSONException;
import org.json.JSONObject;
import org.seoro.seoro.auth.Session;
import org.seoro.seoro.model.User;
import org.seoro.seoro.util.ResponseErrorUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SignUpActivity extends AppCompatActivity {

    private View mProgressView;
    private ScrollView mScrollView;
    private AutoCompleteTextView mEmailAutoCompleteTextView;
    private EditText mPasswordEditText;
    private EditText mConfirmPasswordEditText;
    private EditText mDisplayNameEditText;
    private CheckBox mEmailCheckBox;
    private CheckBox mPasswordCheckBox;
    private CheckBox mConfirmPasswordCheckBox;
    private CheckBox mDisplayNameCheckBox;
    private SeekBar mSeekBar;

    private OkHttpClient mOkHttpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        Button backButton = findViewById(R.id.SignUpActivity_BackButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mProgressView = findViewById(R.id.SignUpActivity_ProgressBar);
        mScrollView = findViewById(R.id.SignUpActivity_ScrollView);
        mEmailAutoCompleteTextView = findViewById(R.id.SignUpActivity_EmailAutoCompleteTextView);
        mPasswordEditText = findViewById(R.id.SignUpActivity_PasswordEditText);
        mConfirmPasswordEditText = findViewById(R.id.SignUpActivity_ConfirmPasswordEditText);
        mDisplayNameEditText = findViewById(R.id.SignUpActivity_DisplayNameEditText);
        mEmailCheckBox = findViewById(R.id.SignUpActivity_EmailCheckBox);
        mPasswordCheckBox = findViewById(R.id.SignUpActivity_PasswordCheckBox);
        mConfirmPasswordCheckBox = findViewById(R.id.SignUpActivity_ConfirmPasswordCheckBox);
        mDisplayNameCheckBox = findViewById(R.id.SignUpActivity_DisplayNameCheckBox);
        mSeekBar = findViewById(R.id.MapsActivity_SeekBar);

        mEmailAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString()) || !isEmailValid(s.toString())) {
                    mEmailCheckBox.setChecked(false);
                    mSeekBar.setEnabled(true);
                } else {
                    mEmailCheckBox.setChecked(true);
                    mSeekBar.setEnabled(false);
                }
            }
        });
        mPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString()) || !isPasswordValid(s.toString())) {
                    mPasswordCheckBox.setChecked(false);
                    mSeekBar.setEnabled(true);
                } else {
                    mPasswordCheckBox.setChecked(true);
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
                        !mPasswordEditText.getText().toString().equals(s.toString())) {
                    mConfirmPasswordCheckBox.setChecked(false);
                    mSeekBar.setEnabled(true);
                } else {
                    mConfirmPasswordCheckBox.setChecked(true);
                    mSeekBar.setEnabled(false);
                }
            }
        });
        mDisplayNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString()) || !isDisplayNameValid(s.toString())) {
                    mDisplayNameCheckBox.setChecked(false);
                    mSeekBar.setEnabled(true);
                } else {
                    mDisplayNameCheckBox.setChecked(true);
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

                    attemptSignUp();
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

    private void attemptSignUp() {
        mEmailAutoCompleteTextView.setError(null);
        mPasswordEditText.setError(null);

        String email = mEmailAutoCompleteTextView.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String confirmPassword = mConfirmPasswordEditText.getText().toString();
        String displayName = mDisplayNameEditText.getText().toString();

        if (TextUtils.isEmpty(email) || !isEmailValid(email)) {
            mEmailCheckBox.setChecked(false);
            mEmailAutoCompleteTextView.requestFocus();

            mSeekBar.setEnabled(false);

            return;
        }

        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            mPasswordCheckBox.setChecked(false);
            mPasswordEditText.requestFocus();

            mSeekBar.setEnabled(false);

            return;
        }

        if (TextUtils.isEmpty(confirmPassword) || !password.equals(confirmPassword)) {
            mConfirmPasswordCheckBox.setChecked(false);
            mConfirmPasswordEditText.requestFocus();

            mSeekBar.setEnabled(false);

            return;
        }

        if (TextUtils.isEmpty(displayName) || !isDisplayNameValid(displayName)) {
            mDisplayNameCheckBox.setChecked(false);
            mDisplayNameEditText.requestFocus();

            mSeekBar.setEnabled(false);

            return;
        }

        postUser(email, password, displayName);
    }

    private void postUser(String email, String password, String displayName) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("email", email)
                    .put("password", password)
                    .put("displayName", displayName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(Session.HOST + "/api/users")
                .post(requestBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false);

                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                SignUpActivity.this
                        );
                        builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEmailAutoCompleteTextView.requestFocus();
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false);

                        try (ResponseBody responseBody = response.body()) {
                            JSONObject responseJSONObject = new JSONObject();

                            if (responseBody != null) {
                                try {
                                    responseJSONObject = new JSONObject(responseBody.string());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (response.isSuccessful()) {
                                Intent intent = new Intent(
                                        SignUpActivity.this,
                                        WelcomeActivity.class
                                );
                                intent.putExtra(WelcomeActivity.PARAM_EMAIL, email);
                                intent.putExtra(WelcomeActivity.PARAM_PASSWORD, password);

                                startActivity(intent);
                            } else {
                                switch (response.code()) {
                                    case 400: {
                                        String message = responseJSONObject.optString(
                                                "message",
                                                ""
                                        );
                                        String parameter = ResponseErrorUtil
                                                .MESSAGE_PARAMETER_PATTERN
                                                .matcher(message)
                                                .group();

                                        switch (parameter) {
                                            case "email":
                                                mEmailAutoCompleteTextView.requestFocus();
                                                mEmailCheckBox.setChecked(false);

                                                break;
                                            case "password":
                                                mPasswordEditText.requestFocus();
                                                mPasswordCheckBox.setChecked(false);

                                                break;
                                            case "displayName":
                                                mDisplayNameEditText.requestFocus();
                                                mDisplayNameCheckBox.setChecked(false);

                                                break;
                                        }

                                        break;
                                    }
                                    case 409: {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                                SignUpActivity.this
                                        );
                                        builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                mEmailAutoCompleteTextView.requestFocus();
                                            }
                                        }).setTitle(R.string.dialog_title_error)
                                                .setMessage(R.string.dialog_message_conflict_email)
                                                .show();

                                        break;
                                    }
                                    default: {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                                SignUpActivity.this
                                        );
                                        builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                mEmailAutoCompleteTextView.requestFocus();
                                            }
                                        }).setTitle(R.string.dialog_title_error)
                                                .setMessage(R.string.dialog_message_server_error)
                                                .show();
                                    }
                                }
                            }
                        }

                        mSeekBar.setEnabled(true);
                    }
                });
            }
        });
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mScrollView.setVisibility(show ? View.GONE : View.VISIBLE);
        mScrollView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mScrollView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private boolean isEmailValid(String email) {
        return User.EMAIL_PATTERN.matcher(email).find();
    }

    private boolean isPasswordValid(String password) {
        return User.PASSWORD_PATTERN.matcher(password).find();
    }

    private boolean isDisplayNameValid(String displayName) {
        return User.DISPLAY_NAME_PATTERN.matcher(displayName).find();
    }
}
