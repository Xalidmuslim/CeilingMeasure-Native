package ru.xalidmuslim.ceilingmeasure;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background-only reads for the launcher. Never constructs or mutates Views.
 * The sole bitmap on the native home is the full-size packaged app icon;
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
                    new NativeProjectStore(application).loadSaved();
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
        return BitmapFactory.decodeResource(resources, R.drawable.app_icon_master, options);
    }
}
