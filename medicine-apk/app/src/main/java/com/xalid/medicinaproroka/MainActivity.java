package com.xalid.medicinaproroka;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String START_URL = "https://medicina-proroka.netlify.app/";
    private static final String APP_HOST = "medicina-proroka.netlify.app";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(16, 22, 17));
        getWindow().setNavigationBarColor(Color.rgb(16, 22, 17));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(16, 22, 17));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        WebView.setWebContentsDebuggingEnabled(false);
        webView.addJavascriptInterface(new ShareBridge(this), "AndroidShare");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                String host = uri.getHost();

                if ("https".equalsIgnoreCase(scheme) && APP_HOST.equalsIgnoreCase(host)) {
                    return false;
                }

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                injectNativeShareBridge(view);
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void injectNativeShareBridge(WebView view) {
        String script =
            "(function(){" +
            "if(!window.AndroidShare)return;" +
            "try{" +
            "Object.defineProperty(navigator,'share',{configurable:true,value:function(data){" +
            "data=data||{};" +
            "AndroidShare.share(String(data.title||document.title||''),String(data.text||''),String(data.url||location.href));" +
            "return Promise.resolve();" +
            "}});" +
            "}catch(e){" +
            "navigator.share=function(data){" +
            "data=data||{};" +
            "AndroidShare.share(String(data.title||document.title||''),String(data.text||''),String(data.url||location.href));" +
            "return Promise.resolve();" +
            "};" +
            "}" +
            "})();";
        view.evaluateJavascript(script, null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private static class ShareBridge {
        private final Context context;

        ShareBridge(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void share(String title, String text, String url) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, title);

            StringBuilder body = new StringBuilder();
            if (text != null && !text.trim().isEmpty()) {
                body.append(text.trim());
            }
            if (url != null && !url.trim().isEmpty()) {
                if (body.length() > 0) body.append("\n\n");
                body.append(url.trim());
            }
            intent.putExtra(Intent.EXTRA_TEXT, body.toString());

            Intent chooser = Intent.createChooser(intent, "Поделиться");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        }
    }
}
