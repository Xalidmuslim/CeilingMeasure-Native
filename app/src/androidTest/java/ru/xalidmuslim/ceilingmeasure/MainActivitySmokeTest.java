package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.WebView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {

    private static MainActivity activity(ActivityScenario<MainActivity> scenario) {
        AtomicReference<MainActivity> ref = new AtomicReference<>();
        scenario.onActivity(ref::set);
        return ref.get();
    }

    private static String eval(MainActivity activity, String js) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            WebView webView = activity.getWebViewForTest();
            webView.evaluateJavascript(js, value -> {
                result.set(value);
                latch.countDown();
            });
        });
        assertTrue("JavaScript evaluation timed out", latch.await(10, TimeUnit.SECONDS));
        return result.get();
    }

    private static void awaitReady(MainActivity activity) throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            String ready = eval(activity, "location.href + '|' + document.readyState");
            if (("\"" + MainActivity.START_URL + "|complete\"").equals(ready)) return;
            Thread.sleep(150);
        }
        throw new AssertionError("Golden-master WebView did not finish loading at " + MainActivity.START_URL);
    }

    @Test
    public void goldenMasterLoadsWithCoreControlsAndNoJsErrors() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            MainActivity activity = activity(scenario);
            assertNotNull(activity);
            awaitReady(activity);

            // The web app implementation is intentionally scoped inside an IIFE, so this
            // instrumentation test verifies the public DOM/bridge contract. Internal
            // implementation tokens and the exact golden-master bytes are verified by
            // scripts/verify_webapp.py before the emulator test starts.
            String result = eval(activity,
                    "(() => [" +
                            "location.href === 'https://appassets.androidplatform.net/assets/index.html'," +
                            "!!document.getElementById('savedBtn')," +
                            "!!document.getElementById('newOrder')," +
                            "!!document.getElementById('uiSettingsBtn')," +
                            "!!document.getElementById('wallSheet')," +
                            "!!document.getElementById('diagSheet')," +
                            "!!document.getElementById('savedMeasurements')," +
                            "!!document.getElementById('backupExport')," +
                            "!!document.getElementById('backupImport')," +
                            "!!document.getElementById('clientEstimatePdf')," +
                            "!!document.getElementById('historyNav')," +
                            "!!document.getElementById('globalUndo')," +
                            "!!document.getElementById('globalRedo')," +
                            "!!document.getElementById('drawToolsNav')," +
                            "!!document.getElementById('homeScreen')," +
                            "!!document.getElementById('homeNewOrder')," +
                            "!!document.getElementById('homeSaved')," +
                            "!!document.getElementById('homeDrawing')," +
                            "!!document.getElementById('homeBtn')," +
                            "document.body.classList.contains('app-home')," +
                            "typeof AndroidFileBridge === 'object'," +
                            "typeof window.ceilingHandleAndroidBack === 'function'" +
                            "].every(Boolean))()");
            assertEquals("true", result);

            assertEquals("true", eval(activity,
                    "document.getElementById('roomPicker').classList.add('open');" +
                    "window.ceilingHandleAndroidBack()"));
            assertEquals("false", eval(activity,
                    "document.getElementById('roomPicker').classList.contains('open')"));

            // Regression guard: verify each edit mode separately. Spacing the
            // renders avoids batching three full drawing re-renders on one UI frame.
            assertEquals("true", eval(activity,
                    "document.body.classList.remove('app-home');" +
                    "(() => { const b=document.querySelector('[data-quick-mode=\\\"move\\\"]');" +
                    "if(!b||b.disabled)return false;b.click();return true; })()"));
            Thread.sleep(180);
            assertEquals("true", eval(activity,
                    "document.querySelector('[data-quick-mode=\\\"move\\\"]')?.classList.contains('active')"));

            assertEquals("true", eval(activity,
                    "(() => { const b=document.querySelector('[data-quick-mode=\\\"diag\\\"]');" +
                    "if(!b||b.disabled)return false;b.click();return true; })()"));
            Thread.sleep(180);
            assertEquals("true", eval(activity,
                    "document.querySelector('[data-quick-mode=\\\"diag\\\"]')?.classList.contains('active')"));

            assertEquals("true", eval(activity,
                    "(() => { const b=document.querySelector('[data-quick-mode=\\\"labels\\\"]');" +
                    "if(!b)return false;b.click();return true; })()"));
            Thread.sleep(180);
            assertEquals("true", eval(activity,
                    "document.querySelector('[data-quick-mode=\\\"labels\\\"]')?.classList.contains('active')"));

            Thread.sleep(500);
            assertTrue("WebView console contains errors: " + activity.getConsoleErrorsForTest(),
                    activity.getConsoleErrorsForTest().isEmpty());
        }
    }

    @Test
    public void localStorageSurvivesActivityRecreation() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            MainActivity activity = activity(scenario);
            awaitReady(activity);

            assertEquals("\"ok\"", eval(activity,
                    "localStorage.setItem('__apk_smoke_persist','persisted'); 'ok'"));

            scenario.recreate();
            activity = activity(scenario);
            awaitReady(activity);

            assertEquals("\"persisted\"", eval(activity,
                    "localStorage.getItem('__apk_smoke_persist')"));
            eval(activity, "localStorage.removeItem('__apk_smoke_persist'); 'ok'");
        }
    }

    @Test
    public void androidBridgeWritesJsonAndPdfIntoDownloads() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String jsonName = "ceiling_smoke_" + suffix + ".json";
        String pdfName = "ceiling_smoke_" + suffix + ".pdf";

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            MainActivity activity = activity(scenario);
            awaitReady(activity);

            String js = "AndroidFileBridge.saveBase64File(" +
                    "'" + jsonName + "','application/json',btoa('{\"ok\":true}'));" +
                    "AndroidFileBridge.saveBase64File(" +
                    "'" + pdfName + "','application/pdf',btoa('%PDF-1.4\\n%%EOF\\n'));" +
                    "'sent'";
            assertEquals("\"sent\"", eval(activity, js));

            Uri jsonUri = waitForDownload(jsonName, 10_000);
            Uri pdfUri = waitForDownload(pdfName, 10_000);
            assertNotNull("JSON export was not written", jsonUri);
            assertNotNull("PDF export was not written", pdfUri);

            assertTrue(readPrefix(jsonUri, 1).length > 0);
            String pdfPrefix = new String(readPrefix(pdfUri, 5), StandardCharsets.US_ASCII);
            assertEquals("%PDF-", pdfPrefix);

            ContentResolver resolver = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getContentResolver();
            resolver.delete(jsonUri, null, null);
            resolver.delete(pdfUri, null, null);
        }
    }

    private static Uri waitForDownload(String displayName, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Uri found = findDownload(displayName);
            if (found != null) return found;
            Thread.sleep(150);
        }
        return null;
    }

    private static Uri findDownload(String displayName) {
        ContentResolver resolver = InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getContentResolver();
        String[] projection = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE};
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
        String[] args = {displayName};
        try (Cursor cursor = resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                args,
                MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
                if (size <= 0) return null;
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        String.valueOf(id));
            }
        }
        return null;
    }

    private static byte[] readPrefix(Uri uri, int count) throws Exception {
        ContentResolver resolver = InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getContentResolver();
        try (InputStream in = resolver.openInputStream(uri)) {
            assertNotNull(in);
            byte[] out = new byte[count];
            int read = in.read(out);
            assertFalse("File is empty", read < 0);
            if (read == count) return out;
            byte[] smaller = new byte[Math.max(0, read)];
            System.arraycopy(out, 0, smaller, 0, smaller.length);
            return smaller;
        }
    }
}
