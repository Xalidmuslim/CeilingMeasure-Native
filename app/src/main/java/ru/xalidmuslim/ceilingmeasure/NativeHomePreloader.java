package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background-only reads for the launcher. Never constructs or mutates Views.
 * Only the downsampled packaged icon is used on the native Home;
 * navigation, cards and the ceiling drawing are native vectors and are not
 * rasterized or eagerly inflated. Reading saved projects also stays off the UI thread.
 */
final class NativeHomePreloader {
    interface Listener {
        void onLoaded(@NonNull List<NativeProjectStore.ProjectData> saved,
                      @Nullable Bitmap icon);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread worker = new Thread(r, "ceiling-home-preload");
        worker.setPriority(Thread.NORM_PRIORITY - 1);
        return worker;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile Bitmap cachedIcon;

    private NativeHomePreloader() {}

    /** Start decoding while Home's native Views are being constructed. */
    static void prefetchIcon(@NonNull Context appContext) {
        final Resources resources = appContext.getApplicationContext().getResources();
        IO.execute(() -> {
            if (cachedIcon == null || cachedIcon.isRecycled()) {
                cachedIcon = decodeSmallIcon(resources,
                        resources.getDisplayMetrics().density);
            }
        });
    }

    static void load(@NonNull Context appContext, @NonNull Listener listener) {
        final Context application = appContext.getApplicationContext();
        IO.execute(() -> {
            Bitmap icon = cachedIcon;
            if (icon == null || icon.isRecycled()) {
                icon = decodeSmallIcon(application.getResources(),
                        application.getResources().getDisplayMetrics().density);
                if (icon != null) cachedIcon = icon;
            }
            final Bitmap ready = icon;
            final List<NativeProjectStore.ProjectData> saved =
                    new ArrayList<>(new NativeProjectStore(application).loadSaved());
            // Medicine's LazyColumn sorts/prepares data outside composition.
            // The Home renders at most 3 cards, so no UI-thread sort is needed.
            saved.sort((a, b) -> Long.compare(b.savedAt, a.savedAt));
            MAIN.post(() -> listener.onLoaded(saved, ready));
        });
    }

    @Nullable
    private static Bitmap decodeSmallIcon(@NonNull Resources resources, float density) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, R.drawable.app_icon_master, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int target = Math.max(48, Math.round(48f * density));
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= target
                && bounds.outHeight / (sample * 2) >= target) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap image = BitmapFactory.decodeResource(
                resources, R.drawable.app_icon_master, options);
        if (image == null) return null;
        Bitmap transparent = removeWhiteBackdrop(image);
        if (transparent != image) image.recycle();
        return transparent;
    }

    /**
     * The original launcher WebP includes a white square behind the blue
     * construction artwork. Only the white region connected to the bitmap's
     * outer boundary becomes transparent. Blue details and enclosed white
     * highlights remain pixel-for-pixel unchanged.
     *
     * Runs on the image preload executor, never on the UI thread. The source
     * resource is NOT rewritten (launcher artwork stays untouched).
     */
    @NonNull
    static Bitmap removeWhiteBackdrop(@NonNull Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int n = width * height;
        if (n == 0) return source;
        int[] pixels = new int[n];
        source.getPixels(pixels, 0, width, 0, 0, width, height);
        boolean[] seen = new boolean[n];
        int[] queue = new int[n];
        int head = 0;
        int tail = 0;

        for (int x = 0; x < width; x++) {
            int top = x;
            int bottom = (height - 1) * width + x;
            if (isBackdropWhite(pixels[top]) && !seen[top]) {
                seen[top] = true;
                queue[tail++] = top;
            }
            if (isBackdropWhite(pixels[bottom]) && !seen[bottom]) {
                seen[bottom] = true;
                queue[tail++] = bottom;
            }
        }
        for (int y = 0; y < height; y++) {
            int left = y * width;
            int right = left + width - 1;
            if (isBackdropWhite(pixels[left]) && !seen[left]) {
                seen[left] = true;
                queue[tail++] = left;
            }
            if (isBackdropWhite(pixels[right]) && !seen[right]) {
                seen[right] = true;
                queue[tail++] = right;
            }
        }
        if (tail == 0) return source;

        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            int y = index / width;
            pixels[index] &= 0x00ffffff;
            if (x > 0) {
                int neighbor = index - 1;
                if (!seen[neighbor] && isBackdropWhite(pixels[neighbor])) {
                    seen[neighbor] = true;
                    queue[tail++] = neighbor;
                }
            }
            if (x + 1 < width) {
                int neighbor = index + 1;
                if (!seen[neighbor] && isBackdropWhite(pixels[neighbor])) {
                    seen[neighbor] = true;
                    queue[tail++] = neighbor;
                }
            }
            if (y > 0) {
                int neighbor = index - width;
                if (!seen[neighbor] && isBackdropWhite(pixels[neighbor])) {
                    seen[neighbor] = true;
                    queue[tail++] = neighbor;
                }
            }
            if (y + 1 < height) {
                int neighbor = index + width;
                if (!seen[neighbor] && isBackdropWhite(pixels[neighbor])) {
                    seen[neighbor] = true;
                    queue[tail++] = neighbor;
                }
            }
        }

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        output.setPixels(pixels, 0, width, 0, 0, width, height);
        return output;
    }

    private static boolean isBackdropWhite(int color) {
        if (Color.alpha(color) < 16) return false;
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        // The source WebP has a soft off-white/gray *rectangular* matte,
        // not only pure-white pixels. At 227 its gray edge still appears as
        // a white frame on the Home's cool-gray background. Restrict the
        // lower cutoff to pixels connected to the bitmap OUTER boundary:
        // enclosed ceiling whites, blue lines and calculator are preserved.
        // Still runs only in the existing preloader background thread.
        return min >= 205 && max - min <= 28;
    }
}
