package org.seoro.seoro;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.json.JSONException;
import org.json.JSONObject;
import org.seoro.seoro.auth.Session;
import org.seoro.seoro.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A login screen that offers login via email/password.
 */
public class SignInActivity extends AppCompatActivity {
    public static final String ACCOUNT_TYPE = "org.seoro.seoro";
    public static final String AUTH_TOKEN_TYPE = "session";
    public static final String ARG_ADD_ACCOUNT = "addAccount";

    private static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.GET_ACCOUNTS
    };

    private OkHttpClient mOkHttpClient;

    private AccountManager mAccountManager;

    private AutoCompleteTextView mEmailAutoCompleteTextView;
    private EditText mPasswordEditText;
    private ProgressBar mProgressBar;
    private ScrollView mScrollView;
    private LinearLayout mLinearLayout;
    private Button mSignInButton;
    private Button mSignUpButton;
    private Button mForgotPasswordButton;
    private CheckBox mAutoSignInCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        ConstraintLayout constraintLayout = findViewById(R.id.SignInActivity_ConstraintLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        ClearableCookieJar clearableCookieJar = new PersistentCookieJar(
                new SetCookieCache(), new SharedPrefsCookiePersistor(getApplicationContext())
        );
        mOkHttpClient = new OkHttpClient.Builder().cookieJar(clearableCookieJar).build();

        mAccountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);

        mEmailAutoCompleteTextView = findViewById(R.id.SignInActivity_EmailAutoCompleteTextView);
        mPasswordEditText = findViewById(R.id.SignInActivity_PasswordEditText);
        mSignInButton = findViewById(R.id.SignInActivity_SignInButton);
        mSignUpButton = findViewById(R.id.SignInActivity_SignUpButton);
        mAutoSignInCheckBox = findViewById(R.id.SignInActivity_AutoSignInCheckBox);
        mProgressBar = findViewById(R.id.SignInActivity_ProgressBar);
        mLinearLayout = findViewById(R.id.SignInActivity_LinearLayout);
        mScrollView = findViewById(R.id.SignInActivity_ScrollView);
        mForgotPasswordButton = findViewById(R.id.SignInActivity_ForgotPasswordButton);

        mPasswordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignInButton.setEnabled(false);
                attemptLogin();
            }
        });

        mSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            }
        });

        mForgotPasswordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        if (getIntent().getBooleanExtra(ARG_ADD_ACCOUNT, false)) {
            mAutoSignInCheckBox.setChecked(true);
            mAutoSignInCheckBox.setEnabled(false);
        }

        List<String> permissionList = new ArrayList<>();

        for (String permission: REQUIRED_PERMISSIONS) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission) &&
                    ContextCompat.checkSelfPermission(this, permission) !=
                            PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }

        requestPermissions(permissionList);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);

        super.onBackPressed();
    }

    private void requestPermissions(List<String> permissionList) {
        if (permissionList.size() > 0) {
            String[] permissions = new String[permissionList.size()];
            permissionList.toArray(permissions);

            ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    REQUIRED_PERMISSIONS_REQUEST_CODE
            );
        }
    }

    private void attemptLogin() {
        mEmailAutoCompleteTextView.setError(null);
        mPasswordEditText.setError(null);

        String email = mEmailAutoCompleteTextView.getText().toString();
        String password = mPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(email)) {
            mEmailAutoCompleteTextView.setError(getString(R.string.error_field_required));
            mEmailAutoCompleteTextView.requestFocus();

            return;
        } else if (!isEmailValid(email)) {
            mEmailAutoCompleteTextView.setError(getString(R.string.error_invalid_email));
            mEmailAutoCompleteTextView.requestFocus();

            return;
        }

        postSession(email, password);
    }

    private void postSession(String email, String password) {
        showProgress(true);

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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false);
                        mSignInButton.setEnabled(true);

                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                SignInActivity.this
                        );
                        builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEmailAutoCompleteTextView.requestFocus();
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
                                        SignInActivity.this
                                );
                                builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mEmailAutoCompleteTextView.requestFocus();
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
                            showProgress(false);
                            mSignInButton.setEnabled(true);

                            if (!response.isSuccessful()) {
                                if (response.code() == 400) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(
                                            SignInActivity.this
                                    );
                                    builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mPasswordEditText.requestFocus();
                                        }
                                    }).setTitle(R.string.dialog_title_error)
                                            .setMessage(R.string.dialog_message_sign_in_failed)
                                            .show();
                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(
                                            SignInActivity.this
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

                                return;
                            }

                            try {
                                JSONObject responseJSONObject = new JSONObject(responseBodyString);
                                User user = new User(responseJSONObject.optJSONObject("data"));

                                Session.getInstance().setUser(user);
                                String sessionId = response.header("Set-Cookie");

                                if (mAutoSignInCheckBox.isChecked() &&
                                        mAccountManager.getAccountsByType("org.seoro.seoro").length ==
                                                0) {
                                    Account account = new Account(email, ACCOUNT_TYPE);
                                    mAccountManager.addAccountExplicitly(
                                            account,
                                            password,
                                            null
                                    );
                                    mAccountManager.setAuthToken(
                                            account,
                                            AUTH_TOKEN_TYPE,
                                            sessionId
                                    );
                                }

                                finish();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    private boolean isEmailValid(String email) {
        return User.EMAIL_PATTERN.matcher(email).find();
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

        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}

