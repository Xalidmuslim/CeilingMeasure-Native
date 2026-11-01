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
                    .setDuration(TransformerMotionProfile.SCATTER_MS)
                    .setInterpolator(NativeMotion.easing()).start();
        });
    }

    /** Stagger children in a deliberately scattered order, including buttons. */
    static void staggerVisible(@NonNull ViewGroup parent, int maximum) {
        if (!enabled(parent)) return;
        final int count = Math.min(Math.max(0, maximum), parent.getChildCount());
        for (int i = 0; i < count; i++) {
            View child = parent.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            int side = i % 2 == 0 ? -1 : 1;
            if (count % 2 == 1 && i == count / 2) side = 0;
            long delay = TransformerMotionProfile.SCATTER_STEP_MS
                    * TransformerMotionProfile.scatterRank(i, count);
            child.animate().cancel();
            child.setAlpha(.26f);
            child.setTranslationX(side * dp(child, TransformerMotionProfile.SCATTER_X_DP));
            child.setTranslationY(dp(child, TransformerMotionProfile.SCATTER_Y_DP));
            child.setRotationY(side * TransformerMotionProfile.SCATTER_ROTATION_Y);
            child.setScaleX(.92f);
            child.setScaleY(.96f);
            child.post(() -> {
                if (!child.isAttachedToWindow() || !enabled(child)) {
                    reset(child);
                    return;
                }
                child.animate().alpha(1f).translationX(0f).translationY(0f)
                        .rotationY(0f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay)
                        .setDuration(TransformerMotionProfile.SCATTER_MS)
                        .setInterpolator(NativeMotion.easing()).start();
            });
        }
    }

    /** Home wraps the five buttons in a horizontal row; other docks are rows. */
    static void staggerDock(@NonNull View dock) {
        if (!(dock instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) dock;
        ViewGroup slots = group;
        if (group.getChildCount() == 1 && group.getChildAt(0) instanceof ViewGroup) {
            slots = (ViewGroup) group.getChildAt(0);
        }
        staggerVisible(slots, Math.min(6, slots.getChildCount()));
    }

    /** Separately assemble choice buttons in Settings (font, weight, motion). */
    static void staggerSettingsButtons(@NonNull ViewGroup sheet) {
        if (!enabled(sheet)) return;
        for (int i = 0; i < sheet.getChildCount(); i++) {
            View child = sheet.getChildAt(i);
            if (!(child instanceof ViewGroup)) continue;
            ViewGroup section = (ViewGroup) child;
            for (int j = 0; j < section.getChildCount(); j++) {
                View candidate = section.getChildAt(j);
                if (!(candidate instanceof ViewGroup)) continue;
                ViewGroup row = (ViewGroup) candidate;
                if (row.getChildCount() >= 2 && row.getChildCount() <= 4
                        && row.getChildAt(0) instanceof Button) {
                    staggerVisible(row, row.getChildCount());
                }
            }
        }
    }

    /**
     * Noticeable reverse assembly: controls peel apart first, then the sheet
     * folds away while the scrim clears. done() actually dismisses the dialog.
     */
    static void closeSettings(@NonNull ViewGroup sheet, @NonNull View backdrop,
            @NonNull Runnable done) {
        if (!enabled(sheet)) {
            done.run();
            return;
        }
        int count = Math.min(sheet.getChildCount(), 8);
        for (int i = 0; i < count; i++) {
            View child = sheet.getChildAt(i);
            child.animate().cancel();
            int direction = i % 2 == 0 ? -1 : 1;
            child.animate().translationX(direction * dp(child, 37f))
                    .translationY(dp(child, 13f))
                    .rotationY(direction * -6f).scaleX(.94f).alpha(.1f)
                    .setStartDelay(i * 27L).setDuration(230L)
                    .setInterpolator(NativeMotion.easing()).start();
        }
        sheet.animate().cancel();
        sheet.animate().translationY(dp(sheet, 26f))
                .scaleX(.91f).scaleY(.94f).rotationY(-6f)
                .setDuration(405L).setInterpolator(NativeMotion.easing()).start();
        backdrop.animate().cancel();
        backdrop.animate().alpha(0f).setStartDelay(130L)
                .setDuration(340L).setInterpolator(NativeMotion.easing())
                .withEndAction(done).start();
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
            v.setEnabled(false);
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
            MAIN.postDelayed(() -> {
                v.setEnabled(true);
                action.run();
            }, TransformerMotionProfile.CLICK_COMMIT_MS);
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
