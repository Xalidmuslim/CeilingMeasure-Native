package ru.xalidmuslim.ceilingmeasure;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import androidx.annotation.NonNull;

/**
 * Native Java adaptation of Medicine Smooth Motion (golden/medicine-smooth-2026-10-10).
 *
 * Compose AnimatedContent cannot be used directly in this Java View app.
 * We use native ViewPropertyAnimator with Compose's FastOutSlowInEasing
 * cubic curve. Activity roots never translate/scale or crossfade against a
 * second screen; the approved layout remains fully visible on the first frame.
 * Closing full-screen dialogs uses a separate single-surface alpha transition.
 */
final class NativeMotion {
    static final long ENTER_MS = 215L;
    static final long COMPACT_ENTER_MS = 115L;
    static final long MICRO_ENTER_MS = 110L;
    static final long DIALOG_ENTER_MS = 215L;
    static final long DIALOG_EXIT_MS = 165L;

    // Jetpack Compose FastOutSlowInEasing = cubicBezier(0.4, 0.0, 0.2, 1.0).
    private static final Interpolator EASE =
            new PathInterpolator(.4f, 0f, .2f, 1f);

    private NativeMotion() {}

    static Interpolator easing() { return EASE; }

    static boolean animationsEnabled() {
        // Respects Android accessibility/developer system animator duration = 0.
        return ValueAnimator.areAnimatorsEnabled();
    }

    static void enter(@NonNull View view) {
        fadeIn(view, ENTER_MS);
    }

    static void enterCompact(@NonNull View view) {
        fadeIn(view, COMPACT_ENTER_MS);
    }

    private static void fadeIn(@NonNull View view, long duration) {
        view.animate().cancel();
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        if (!animationsEnabled()) {
            view.setAlpha(1f);
            return;
        }
        // Do not fade a parent from transparent (or put it in an offscreen
        // compositing layer). Keep content visible while the first frame is
        // drawn. No slide for Home<->drawing to prevent double-background ghosting.
        view.setAlpha(.96f);
        view.post(() -> {
            if (!view.isAttachedToWindow() || !animationsEnabled()) {
                view.setAlpha(1f);
                return;
            }
            view.animate().alpha(1f)
                    .setDuration(duration)
                    .setInterpolator(EASE)
                    .start();
        });
    }
}
