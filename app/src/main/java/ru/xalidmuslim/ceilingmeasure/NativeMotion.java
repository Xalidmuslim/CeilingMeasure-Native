package ru.xalidmuslim.ceilingmeasure;

import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

/**
 * Cheap motion only: alpha + translation are GPU-friendly and do not trigger
 * repeated layout passes. Never use this on the drawing canvas during gestures.
 */
final class NativeMotion {
    private static final long ENTER_MS = 140L;
    private static final DecelerateInterpolator EASE = new DecelerateInterpolator(1.35f);

    private NativeMotion() {}

    static void enter(@NonNull View view) {
        float density = view.getResources().getDisplayMetrics().density;
        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(6f * density);
        view.post(() -> view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ENTER_MS)
                .setInterpolator(EASE)
                .start());
    }

    static void enterCompact(@NonNull View view) {
        float density = view.getResources().getDisplayMetrics().density;
        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(3f * density);
        view.post(() -> view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(115L)
                .setInterpolator(EASE)
                .start());
    }
}
