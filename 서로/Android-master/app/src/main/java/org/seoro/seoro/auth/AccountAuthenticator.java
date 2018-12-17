package org.seoro.seoro.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.json.JSONException;
import org.json.JSONObject;
import org.seoro.seoro.SignInActivity;
import org.seoro.seoro.model.User;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AccountAuthenticator extends AbstractAccountAuthenticator {
    private Context mContext;

    private OkHttpClient mOkHttpClient;

    public AccountAuthenticator(Context context) {
        super(context);

        ClearableCookieJar clearableCookieJar = new PersistentCookieJar(
                new SetCookieCache(),
                new SharedPrefsCookiePersistor(context.getApplicationContext())
        );

        mContext = context;
        mOkHttpClient = new OkHttpClient.Builder().cookieJar(clearableCookieJar).build();
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Intent intent = new Intent();

        intent.putExtra(SignInActivity.ACCOUNT_TYPE, accountType);
        intent.putExtra(SignInActivity.ACCOUNT_TYPE, authTokenType);
        intent.putExtra(SignInActivity.ARG_ADD_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        AccountManager accountManager = AccountManager.get(mContext);

        String authToken = accountManager.peekAuthToken(account, authTokenType);

        if (TextUtils.isEmpty(authToken)) {
            String password = accountManager.getPassword(account);

            if (password != null) {
                JSONObject jsonObject = new JSONObject();

                try {
                    jsonObject.put("email", account.name);
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

                try {
                    Response response = mOkHttpClient.newCall(request).execute();

                    if (response.body() == null) {
                        throw new NetworkErrorException();
                    }

                    JSONObject responseJSONObject = new JSONObject(response.body().string());
                    User user = new User(responseJSONObject.optJSONObject("data"));

                    if (response.isSuccessful()) {
                        Session.getInstance().setUser(user);
                        String sessionId = response.header("Set-Cookie");

                        Bundle result = new Bundle();
                        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                        result.putString(AccountManager.KEY_AUTHTOKEN, sessionId);

                        return result;
                    } else {
                        if (response.code() == 400) {
                            Intent intent = new Intent(mContext, SignInActivity.class);
                            intent.putExtra(
                                    AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                                    accountAuthenticatorResponse
                            );
                            intent.putExtra(SignInActivity.ACCOUNT_TYPE, account.type);
                            intent.putExtra(SignInActivity.AUTH_TOKEN_TYPE, authTokenType);

                            Bundle bundle = new Bundle();
                            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
                        } else {
                            throw new NetworkErrorException();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    throw new NetworkErrorException();
                } catch (JSONException e) {
                    e.printStackTrace();

                    Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                    result.putString(AccountManager.KEY_AUTHTOKEN, authToken);

                    return result;
                }
            }
        } else {
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);

            return result;
        }

        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        return null;
    }
}
