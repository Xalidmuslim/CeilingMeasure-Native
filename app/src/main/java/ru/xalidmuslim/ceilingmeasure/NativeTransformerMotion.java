package ru.xalidmuslim.ceilingmeasure;

import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;

/**
 * Shared matte Transformer choreography. No bitmap work, animated layouts,
 * blur, or modification of the drawing canvas/measurement storage.
 */
final class NativeTransformerMotion {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final OvershootInterpolator RETURN_CURVE = new OvershootInterpolator(1.10f);

    private NativeTransformerMotion() {}

    static boolean enabled(@NonNull View view) {
        return NativeMotion.animationsEnabled() &&
                NativeTransformerMode.enabled(view.getContext());
    }

    static void reveal(@NonNull View surface, int direction) {
        surface.animate().cancel();
        reset(surface);
        if (!enabled(surface)) {
            NativeMotion.enterCompact(surface);
            return;
        }
        surface.setAlpha(.90f);
        surface.setScaleX(.985f);
        surface.setScaleY(.985f);
        surface.setTranslationX(direction * dp(surface, 18f));
        surface.setTranslationY(dp(surface, 7f));
        surface.post(() -> {
            if (!surface.isAttachedToWindow() || !enabled(surface)) {
                reset(surface);
                return;
            }
            surface.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .translationX(0f).translationY(0f)
                    .setDuration(TransformerMotionProfile.SECTION_REVEAL_MS)
                    .setInterpolator(NativeMotion.easing()).start();
        });
    }

    /** One system-managed, very light tick for an accepted bottom-nav tap.
     *  Never bypasses the device's haptics/accessibility preferences. */
    static void navigationTapHaptic(@NonNull View item) {
        if (Build.VERSION.SDK_INT >= 34) {
            item.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK);
        } else {
            item.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    static void bindNavigation(@NonNull View item, @NonNull View glyph,
            int direction, @NonNull Runnable action) {
        item.setOnClickListener(v -> {
            if (!v.isEnabled()) return;
            navigationTapHaptic(v);
            if (!enabled(v)) {
                action.run();
                return;
            }
            // Lock repeated taps briefly, but dispatch the destination NOW.
            // The icon's motion is purely visual and must never block navigation.
            v.setEnabled(false);
            MAIN.postDelayed(() -> v.setEnabled(true),
                    TransformerMotionProfile.NAV_TAP_LOCK_MS);
            animateGlyph(glyph, direction);
            if (item != glyph) {
                item.animate().cancel();
                item.animate().scaleX(.95f).scaleY(.95f)
                        .setDuration(TransformerMotionProfile.NAV_OUT_MS)
                        .setInterpolator(NativeMotion.easing())
                        .withEndAction(() -> item.animate().scaleX(1f).scaleY(1f)
                                .setDuration(TransformerMotionProfile.NAV_RETURN_MS)
                                .setInterpolator(RETURN_CURVE).start())
                        .start();
            }
            action.run();
        });
    }

    static void selection(@NonNull View glyph, int direction) {
        if (enabled(glyph)) animateGlyph(glyph, direction);
    }

    private static void animateGlyph(@NonNull View glyph, int direction) {
        glyph.animate().cancel();
        reset(glyph);
        glyph.animate()
                .translationX(direction * dp(glyph, TransformerMotionProfile.NAV_X_DP))
                .translationY(-dp(glyph, TransformerMotionProfile.NAV_Y_DP))
                .rotation(direction * 7f).scaleX(.89f).scaleY(.89f)
                .setDuration(TransformerMotionProfile.NAV_OUT_MS)
                .setInterpolator(NativeMotion.easing())
                .withEndAction(() -> glyph.animate()
                        .translationX(0f).translationY(0f).rotation(0f)
                        .scaleX(1f).scaleY(1f)
                        .setDuration(TransformerMotionProfile.NAV_RETURN_MS)
                        .setInterpolator(RETURN_CURVE).start())
                .start();
    }

    private static void reset(@NonNull View v) {
        v.setAlpha(1f);
        v.setTranslationX(0f);
        v.setTranslationY(0f);
        v.setRotation(0f);
        v.setRotationY(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
    }

    private static float dp(@NonNull View v, float amount) {
        return amount * v.getResources().getDisplayMetrics().density;
    }
}
