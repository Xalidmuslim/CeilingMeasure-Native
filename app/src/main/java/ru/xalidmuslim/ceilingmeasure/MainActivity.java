package ru.xalidmuslim.ceilingmeasure;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends ComponentActivity {
    private static final String TAG = "CeilingMeasure";
    public static final String START_URL = "https://appassets.androidplatform.net/assets/index.html";

    public static final String EXTRA_FROM_NATIVE_HOME = "from_native_home";
    public static final String EXTRA_LAUNCH_ACTION = "launch_action";
    public static final String ACTION_NEW_ORDER = "new_order";
    public static final String ACTION_SAVED_MEASUREMENTS = "saved_measurements";
    public static final String ACTION_DRAWING = "drawing";
    public static final String ACTION_SETTINGS = "settings";

    private WebView webView;
    private ValueCallback<Uri[]> pendingFileCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private final List<String> consoleErrors = Collections.synchronizedList(new ArrayList<>());
    private long lastUnhandledBackAtMs = 0L;
    private boolean launchedFromNativeHome = false;
    private boolean launchActionHandled = false;
    @Nullable
    private String launchAction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        launchedFromNativeHome = getIntent().getBooleanExtra(EXTRA_FROM_NATIVE_HOME, false);
        launchAction = getIntent().getStringExtra(EXTRA_LAUNCH_ACTION);

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    ValueCallback<Uri[]> callback = pendingFileCallback;
                    pendingFileCallback = null;
                    if (callback == null) return;

                    Uri[] uris = null;
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Uri uri = data.getData();
                        if (uri != null) {
                            try {
                                final int flags = data.getFlags()
                                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                if ((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                                    getContentResolver().takePersistableUriPermission(
                                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                }
                            } catch (SecurityException ignored) {
                                // Some providers do not offer persistable grants. The one-shot grant is sufficient.
                            }
                            uris = new Uri[]{uri};
                        } else {
                            uris = WebChromeClient.FileChooserParams.parseResult(
                                    result.getResultCode(), data);
                        }
                    }
                    callback.onReceiveValue(uris);
                });

        createWebView();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleSystemBack();
            }
        });
    }

    private void handleSystemBack() {
        if (webView == null) {
            handleUnhandledBack();
            return;
        }

        webView.evaluateJavascript(
                "(() => {" +
                        "const handled=Boolean(window.ceilingHandleAndroidBack && window.ceilingHandleAndroidBack());" +
                        "const home=document.body.classList.contains('app-home');" +
                        "return (handled?'1':'0')+'|'+(home?'1':'0');" +
                        "})()",
                value -> {
                    boolean handled = "\"1|0\"".equals(value) || "\"1|1\"".equals(value);
                    boolean atWebHome = "\"0|1\"".equals(value) || "\"1|1\"".equals(value);

                    if (launchedFromNativeHome && atWebHome) {
                        lastUnhandledBackAtMs = 0L;
                        finish();
                        return;
                    }

                    if (!handled) {
                        if (launchedFromNativeHome) {
                            finish();
                        } else {
                            handleUnhandledBack();
                        }
                    } else {
                        lastUnhandledBackAtMs = 0L;
                    }
                });
    }

    private void handleUnhandledBack() {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastUnhandledBackAtMs <= 1500L) {
            lastUnhandledBackAtMs = 0L;
            moveTaskToBack(true);
            return;
        }

        lastUnhandledBackAtMs = now;
        Toast.makeText(this, "Ещё раз назад — свернуть приложение", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void createWebView() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Android 15+ enforces edge-to-edge for targetSdk 35. Keep the WebView
        // inside the safe system-bar/cutout area so the app header and fixed
        // bottom controls are never drawn underneath status/navigation bars.
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return windowInsets;
        });

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClientCompat() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(
                    @NonNull WebView view,
                    @NonNull WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    @NonNull WebView view,
                    @NonNull WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !("https".equalsIgnoreCase(uri.getScheme())
                        && "appassets.androidplatform.net".equalsIgnoreCase(uri.getHost()));
            }

            @Override
            public void onPageFinished(@NonNull WebView view, @NonNull String url) {
                super.onPageFinished(view, url);
                if (START_URL.equals(url)) {
                    applyNativeLaunchAction();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (pendingFileCallback != null) {
                    pendingFileCallback.onReceiveValue(null);
                }
                pendingFileCallback = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_MIME_TYPES,
                        new String[]{"application/json", "text/json", "text/plain"});

                try {
                    fileChooserLauncher.launch(intent);
                    return true;
                } catch (RuntimeException e) {
                    pendingFileCallback = null;
                    filePathCallback.onReceiveValue(null);
                    Log.e(TAG, "Unable to launch file picker", e);
                    return false;
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String line = consoleMessage.messageLevel() + ": "
                        + consoleMessage.message() + " @"
                        + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber();
                Log.d(TAG, line);
                if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    consoleErrors.add(line);
                }
                return true;
            }
        });

        webView.addJavascriptInterface(new AndroidFileBridge(this), "AndroidFileBridge");

        root.addView(webView);
        setContentView(root);
        ViewCompat.requestApplyInsets(root);
        webView.loadUrl(START_URL);
    }

    private void applyNativeLaunchAction() {
        if (!launchedFromNativeHome || launchActionHandled || webView == null) return;
        launchActionHandled = true;

        String script;
        if (ACTION_NEW_ORDER.equals(launchAction)) {
            script = "(() => {const b=document.getElementById('homeNewOrder');if(!b)return false;b.click();return true})()";
        } else if (ACTION_SAVED_MEASUREMENTS.equals(launchAction)) {
            script = "(() => {const b=document.getElementById('homeSaved');if(!b)return false;b.click();return true})()";
        } else if (ACTION_DRAWING.equals(launchAction)) {
            script = "(() => {const b=document.getElementById('homeDrawing');if(!b)return false;b.click();return true})()";
        } else if (ACTION_SETTINGS.equals(launchAction)) {
            script = "(() => {const b=document.getElementById('homeSettingsBtn');if(!b)return false;b.click();return true})()";
        } else {
            return;
        }

        webView.post(() -> {
            if (webView != null) {
                webView.evaluateJavascript(script, null);
            }
        });
    }

    public WebView getWebViewForTest() {
        return webView;
    }

    public List<String> getConsoleErrorsForTest() {
        synchronized (consoleErrors) {
            return new ArrayList<>(consoleErrors);
        }
    }

    @Override
    protected void onDestroy() {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (isFinishing() && webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    public static final class AndroidFileBridge {
        private final Context appContext;

        AndroidFileBridge(Context context) {
            this.appContext = context.getApplicationContext();
        }

        @JavascriptInterface
        public void saveBase64File(String name, String mime, String base64) {
            Uri uri = null;
            try {
                String safeName = sanitizeFileName(name);
                String safeMime = sanitizeMime(mime);
                byte[] bytes = Base64.decode(base64 == null ? "" : base64, Base64.DEFAULT);
                if (bytes.length == 0) {
                    throw new IllegalArgumentException("Refusing to save an empty file");
                }

                ContentResolver resolver = appContext.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, safeMime);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/CeilingMeasure");
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);

                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IllegalStateException("MediaStore insert returned null");

                try (OutputStream out = resolver.openOutputStream(uri, "w")) {
                    if (out == null) throw new IllegalStateException("Unable to open output stream");
                    out.write(bytes);
                    out.flush();
                }

                ContentValues ready = new ContentValues();
                ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, ready, null, null);

                Uri finalUri = uri;
                if (appContext instanceof Activity) {
                    ((Activity) appContext).runOnUiThread(() ->
                            Toast.makeText(appContext,
                                    "Файл сохранён: " + safeName,
                                    Toast.LENGTH_SHORT).show());
                }
                Log.i(TAG, "Saved " + safeName + " to " + finalUri);
            } catch (Exception e) {
                if (uri != null) {
                    try {
                        appContext.getContentResolver().delete(uri, null, null);
                    } catch (Exception ignored) {
                    }
                }
                Log.e(TAG, "saveBase64File failed", e);
            }
        }

        static String sanitizeFileName(String value) {
            String name = value == null ? "" : value.trim();
            if (name.isEmpty()) name = "file";
            name = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "_");
            if (name.length() > 120) name = name.substring(0, 120);
            return name;
        }

        static String sanitizeMime(String value) {
            String mime = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            if (mime.equals("application/pdf") || mime.equals("application/json")) return mime;
            if (mime.equals("text/json")) return "application/json";
            return "application/octet-stream";
        }
    }
}
