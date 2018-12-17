package org.seoro.seoro.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class AccountAuthenticatorService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        AccountAuthenticator accountAuthenticator = new AccountAuthenticator(this);

        return accountAuthenticator.getIBinder();
    }
}
