package ru.xalidmuslim.ceilingmeasure;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;

/**
 * Sculpted Transformer Motion for the existing Java View hierarchy.
 * Only animates transient RenderNode properties (translation, rotation, scale,
 * opacity). Never animates layout height, redraws the drawing canvas, allocates
 * blur layers or moves the Activity/window background.
 */
final class NativeTransformerMotion {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final OvershootInterpolator RETURN_CURVE =
            new OvershootInterpolator(1.15f);

    private NativeTransformerMotion() {}

    static void reveal(@NonNull View surface, int direction) {
        surface.animate().cancel();
        reset(surface);
        if (!NativeMotion.animationsEnabled()) return;

        surface.setAlpha(.975f);
        surface.setScaleX(TransformerMotionProfile.START_SCALE);
        surface.setScaleY(TransformerMotionProfile.START_SCALE);
        surface.setTranslationX(direction * dp(surface, TransformerMotionProfile.ENTRY_X_DP));
        surface.setTranslationY(dp(surface, TransformerMotionProfile.ENTRY_Y_DP));
        surface.post(() -> {
            if (!surface.isAttachedToWindow() || !NativeMotion.animationsEnabled()) {
                reset(surface);
                return;
            }
            surface.animate()
                    .translationX(0f).translationY(0f)
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setInterpolator(NativeMotion.easing())
                    .setDuration(TransformerMotionProfile.ENTRY_MS)
                    .start();
        });
    }

    /**
     * A small alternating card-fold choreography: only the first few chrome
     * surfaces move, never the drawing canvas or a potentially huge list.
     */
    static void staggerVisible(@NonNull ViewGroup parent, int maximum) {
        if (!NativeMotion.animationsEnabled()) return;
        int count = Math.min(Math.max(0, maximum), parent.getChildCount());
        for (int i = 0; i < count; i++) {
            View card = parent.getChildAt(i);
            if (card.getVisibility() != View.VISIBLE) continue;
            final int side = TransformerMotionProfile.directionForIndex(i, count);
            final long delay = i * 44L;
            card.animate().cancel();
            card.setTranslationX(side * dp(card, 11f));
            card.setTranslationY(dp(card, 4f));
            card.setRotationY(side * 2.2f);
            card.setScaleX(.994f);
            card.post(() -> {
                if (!card.isAttachedToWindow() || !NativeMotion.animationsEnabled()) {
                    reset(card);
                    return;
                }
                card.animate().translationX(0f).translationY(0f)
                        .rotationY(0f).scaleX(1f)
                        .setStartDelay(delay)
                        .setDuration(TransformerMotionProfile.ENTRY_MS)
                        .setInterpolator(NativeMotion.easing()).start();
            });
        }
    }

    /**
     * Wraps the existing navigation callback without changing its destination.
     * A bounded 95ms gesture gives the icon time to move before a screen switch.
     * Debounces rapid taps; a disabled animation scale executes immediately.
     */
    static void bindNavigation(@NonNull View item, @NonNull View glyph,
            int direction, @NonNull Runnable action) {
        item.setOnClickListener(v -> {
            if (!v.isEnabled()) return;
            if (!NativeMotion.animationsEnabled()) {
                action.run();
                return;
            }
            v.setEnabled(false);
            animateGlyph(glyph, direction);
            if (item != glyph) {
                item.animate().cancel();
                item.animate().scaleX(.97f).scaleY(.97f)
                        .setDuration(TransformerMotionProfile.NAV_OUT_MS)
                        .setInterpolator(NativeMotion.easing())
                        .withEndAction(() -> item.animate().scaleX(1f).scaleY(1f)
                                .setDuration(TransformerMotionProfile.NAV_RETURN_MS)
                                .setInterpolator(RETURN_CURVE).start())
                        .start();
            }
            MAIN.postDelayed(() -> {
                v.setEnabled(true);
                action.run();
            }, TransformerMotionProfile.CLICK_COMMIT_MS);
        });
    }

    /** In-place feedback for room tabs and toggles: never delays data changes. */
    static void selection(@NonNull View glyph, int direction) {
        if (!NativeMotion.animationsEnabled()) return;
        animateGlyph(glyph, direction);
    }

    private static void animateGlyph(@NonNull View glyph, int direction) {
        glyph.animate().cancel();
        reset(glyph);
        glyph.animate()
                .translationX(direction * dp(glyph, TransformerMotionProfile.NAV_X_DP))
                .translationY(-dp(glyph, TransformerMotionProfile.NAV_Y_DP))
                .rotation(direction * 7f)
                .scaleX(.89f).scaleY(.89f)
                .setDuration(TransformerMotionProfile.NAV_OUT_MS)
                .setInterpolator(NativeMotion.easing())
                .withEndAction(() -> glyph.animate()
                        .translationX(0f).translationY(0f).rotation(0f)
                        .scaleX(1f).scaleY(1f)
                        .setDuration(TransformerMotionProfile.NAV_RETURN_MS)
                        .setInterpolator(RETURN_CURVE)
                        .start())
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

    private static float dp(@NonNull View view, float amount) {
        return amount * view.getResources().getDisplayMetrics().density;
    }
}
