package org.seoro.seoro;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.seoro.seoro.auth.Session;

public class OpenSourceLicenseActivity extends AppCompatActivity {
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_source_license);

        mWebView = findViewById(R.id.OpenSourceLicenseActivity_WebView);

        mWebView.loadUrl(Session.HOST + "/api/opensource_license");
        mWebView.setWebViewClient(new WebViewClient());
    }
}
