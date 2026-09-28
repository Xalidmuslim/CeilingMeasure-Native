package com.xalid.meditsinaproroka;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://medicina-proroka.netlify.app/";
    private static final String APP_HOST = "medicina-proroka.netlify.app";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(20, 29, 24));
        getWindow().setNavigationBarColor(Color.rgb(20, 29, 24));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(20, 29, 24));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " MedicinaProrokaAndroid/1.0");

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, false);

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidApp");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (APP_HOST.equals(uri.getHost())) {
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
                injectAndroidBridge(view);
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (!request.isForMainFrame()) {
                    return;
                }
                String html = "<!doctype html><html lang='ru'>"
                        + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                        + "<body style='background:#141d18;color:#f1ede4;font-family:sans-serif;padding:32px;line-height:1.5'>"
                        + "<h2>Нет соединения</h2>"
                        + "<p>Не удалось открыть приложение. Проверь интернет и повтори.</p>"
                        + "<button onclick=\"location.href='" + APP_URL + "'\" "
                        + "style='padding:14px 20px;border:0;border-radius:12px;background:#8fb8a0;color:#102018;font-weight:700'>"
                        + "Повторить</button></body></html>";
                view.loadDataWithBaseURL(APP_URL, html, "text/html", "utf-8", null);
            }
        });

        setContentView(webView);
        webView.loadUrl(APP_URL);
    }

    private void injectAndroidBridge(WebView view) {
        String script = "(() => { try {"
                + "window.__MEDICINA_ANDROID__ = true;"
                + "if (!navigator.share) {"
                + "navigator.share = ({title='',text='',url=''}={}) => {"
                + "AndroidApp.share(String(title), String(text), String(url));"
                + "return Promise.resolve();};}"
                + "} catch (_) {} })();";
        view.evaluateJavascript(script, null);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    public final class AndroidBridge {
        @JavascriptInterface
        public void share(String title, String text, String url) {
            runOnUiThread(() -> {
                String content = text == null ? "" : text.trim();
                if (url != null && !url.trim().isEmpty()) {
                    content = content.isEmpty() ? url.trim() : content + "\n\n" + url.trim();
                }

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                shareIntent.putExtra(Intent.EXTRA_TEXT, content);

                try {
                    startActivity(Intent.createChooser(shareIntent, "Поделиться"));
                } catch (Exception e) {
                    Toast.makeText(
                            MainActivity.this,
                            "Не удалось открыть меню «Поделиться»",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }
    }
}
