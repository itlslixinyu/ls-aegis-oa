package com.lsaegis.eoms.engineer.demo;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public final class MainActivity extends Activity {
    private static final String H5_ENTRY_URL = "http://127.0.0.1:25685/engineer/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setBackgroundColor(0xFFF4F6FB);
        setContentView(webView);
        webView.loadUrl(H5_ENTRY_URL);
    }
}
