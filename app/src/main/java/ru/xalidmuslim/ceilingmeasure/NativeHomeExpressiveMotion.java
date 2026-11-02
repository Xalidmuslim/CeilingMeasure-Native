package ru.xalidmuslim.ceilingmeasure;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.transition.ChangeBounds;
import android.transition.ChangeClipBounds;
import android.transition.ChangeTransform;
import android.transition.TransitionSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/** Interactions run on native render-node transforms; no canvas rerender loop. */
final class NativeHomeExpressiveMotion {
    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(.72f);

    private NativeHomeExpressiveMotion() {}

    static boolean enabled(@NonNull View v) {
        return NativeTransformerMotion.enabled(v);
    }

    private static void spring(@NonNull View view, float target,
            float damping, boolean allowOrdinary) {
        if (!NativeMotion.animationsEnabled()
                || (!allowOrdinary && !enabled(view))) return;
        SpringForce forceX = new SpringForce(target)
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
                .setDampingRatio(damping);
        SpringForce forceY = new SpringForce(target)
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
                .setDampingRatio(damping);
        new SpringAnimation(view, DynamicAnimation.SCALE_X).setSpring(forceX).start();
        new SpringAnimation(view, DynamicAnimation.SCALE_Y).setSpring(forceY).start();
    }

    /** A press is physical. Existing OnClick semantics remain, no touch consumption. */
    static void bindCard(@NonNull View card, @Nullable View icon, @Nullable View chevron,
            @NonNull Runnable action, boolean mainCta, boolean scanner,
            @Nullable Runnable onPrimaryPress, @Nullable Runnable onPrimaryCancel) {
        final boolean[] navigating = {false};
        final boolean[] installationStartedOnDown = {false};
        card.setOnTouchListener((v, event) -> {
            // The blue New Measurement illustration is a button microinteraction,
            // NOT a transformer page transition. Keep it in Ordinary mode.
            if (!NativeMotion.animationsEnabled()
                    || (!mainCta && !enabled(v))) return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    spring(v, .975f, SpringForce.DAMPING_RATIO_NO_BOUNCY, mainCta);
                    if (mainCta && icon != null && !navigating[0]) {
                        icon.animate().cancel();
                        icon.animate()
                                .translationX(-dp(icon,
                                        NativeHomeExpressiveProfile.CTA_ART_PRESS_X_DP))
                                .translationY(-dp(icon,
                                        NativeHomeExpressiveProfile.CTA_ART_Y_DP))
                                .rotation(-2.5f)
                                .scaleX(NativeHomeExpressiveProfile.CTA_ART_PRESS_SCALE)
                                .scaleY(NativeHomeExpressiveProfile.CTA_ART_PRESS_SCALE)
                                .setDuration(NativeHomeExpressiveProfile.CTA_ART_PRESS_MS)
                                .setInterpolator(NativeMotion.easing()).start();
                        installationStartedOnDown[0] = true;
                        if (onPrimaryPress != null) onPrimaryPress.run();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    installationStartedOnDown[0] = false;
                    spring(v, 1f, .59f, mainCta);
                    if (mainCta && icon != null && !navigating[0]) {
                        icon.animate().cancel();
                        icon.animate().translationX(0f).translationY(0f)
                                .rotation(0f).scaleX(1f).scaleY(1f)
                                .setDuration(NativeHomeExpressiveProfile.CTA_ART_RETURN_MS)
                                .setInterpolator(NativeMotion.easing()).start();
                        if (onPrimaryCancel != null) onPrimaryCancel.run();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    // Don't reset artwork on UP: Android dispatches click
                    // immediately afterwards. Reset would cause a visible jerk.
                    spring(v, 1f, .59f, mainCta);
                    break;
                default:
                    break;
            }
            return false;
        });
        card.setOnClickListener(v -> {
            if (!NativeMotion.animationsEnabled()
                    || (!mainCta && !enabled(v))) {
                action.run();
                return;
            }
            if (mainCta) {
                if (navigating[0]) return; // Ignore double taps during handoff.
                navigating[0] = true;
                // Accessibility performClick has no ACTION_DOWN; start
                // assembly here too, but not twice on a regular touch.
                if (!installationStartedOnDown[0] && onPrimaryPress != null) {
                    onPrimaryPress.run();
                }
                installationStartedOnDown[0] = false;
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                if (icon != null) {
                    // This one illustration gets a visible, measured glide.
                    // Keep the rest of the application navigation immediate.
                    icon.animate().cancel();
                    icon.animate()
                            .translationX(-dp(icon,
                                    NativeHomeExpressiveProfile.CTA_ART_CLICK_X_DP))
                            .translationY(-dp(icon,
                                    NativeHomeExpressiveProfile.CTA_ART_Y_DP))
                            .rotation(-3f)
                            .scaleX(NativeHomeExpressiveProfile.CTA_ART_CLICK_SCALE)
                            .scaleY(NativeHomeExpressiveProfile.CTA_ART_CLICK_SCALE)
                            .setDuration(NativeHomeExpressiveProfile.CTA_ART_CLICK_MS)
                            .setInterpolator(NativeMotion.easing()).start();
                }
                if (chevron != null) {
                    chevron.animate().cancel();
                    chevron.animate().translationX(dp(chevron, 4))
                            .setDuration(120L)
                            .setInterpolator(NativeMotion.easing())
                            .withEndAction(() -> chevron.animate().translationX(0f)
                                    .setDuration(90L)
                                    .setInterpolator(NativeMotion.easing()).start())
                            .start();
                }
                // Show the accelerated assembly as the tap begins, then open
                // the Activity promptly instead of imposing an 800ms pause.
                v.postDelayed(() -> {
                    if (!v.isAttachedToWindow()) {
                        navigating[0] = false;
                        return;
                    }
                    action.run();
                    navigating[0] = false;
                }, NativeHomeExpressiveProfile.CTA_NAV_HANDOFF_MS);
                return;
            }
            if (icon != null) {
                icon.animate().cancel();
                icon.animate().translationY(scanner ? -dp(icon, 5) : 0)
                        .rotation(scanner ? 0f : -7f).scaleX(1.10f).scaleY(1.10f)
                        .setDuration(105).setInterpolator(NativeMotion.easing())
                        .withEndAction(() -> icon.animate().translationY(0f).rotation(0f)
                                .scaleX(1f).scaleY(1f)
                                .setDuration(NativeHomeExpressiveProfile.MICRO_MS)
                                .setInterpolator(OVERSHOOT).start()).start();
            }
            if (chevron != null) {
                chevron.animate().cancel();
                chevron.animate().translationX(dp(chevron, 5))
                        .setDuration(125).setInterpolator(NativeMotion.easing())
                        .withEndAction(() -> chevron.animate().translationX(0f)
                                .setDuration(145).setInterpolator(OVERSHOOT).start())
                        .start();
            }
            // Execute the destination in the same click event. Press/icon spring
            // is visual feedback, not a pre-navigation loading animation.
            action.run();
        });
    }

    /** Pressing the plus reshapes it into the dismiss glyph, without fake actions. */
    static void fabState(@NonNull TextView plus, boolean open) {
        if (!enabled(plus)) return;
        // Haptics happen only in the actual nav click, not when Back dismisses the menu.
        plus.animate().cancel();
        plus.animate().rotation(open ? 45f : 0f).scaleX(.88f).scaleY(1.13f)
                .setDuration(135).setInterpolator(NativeMotion.easing())
                .withEndAction(() -> plus.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(NativeHomeExpressiveProfile.MICRO_MS)
                        .setInterpolator(OVERSHOOT).start()).start();
    }

    /** Fast matte dock selection without delaying the destination callback. */
    static void moveIndicator(@NonNull View indicator, int index) {
        ViewGroup parent = (ViewGroup) indicator.getParent();
        if (parent == null) return;
        if (parent.getWidth() <= 0) {
            // A pre-layout tap must not silently lose its selection.
            parent.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override public void onLayoutChange(View view, int left, int top,
                        int right, int bottom, int oldLeft, int oldTop,
                        int oldRight, int oldBottom) {
                    if (right <= left) return;
                    view.removeOnLayoutChangeListener(this);
                    moveIndicator(indicator, index);
                }
            });
            return;
        }
        int indicatorWidth = indicator.getWidth();
        if (indicatorWidth <= 0 && indicator.getLayoutParams() != null) {
            indicatorWidth = indicator.getLayoutParams().width;
        }
        float center = parent.getWidth() * (index + .5f)
                / NativeHomeExpressiveProfile.DOCK_SLOTS;
        float x = center - indicatorWidth / 2f;
        indicator.animate().cancel();
        if (!NativeMotion.animationsEnabled()) {
            indicator.setTranslationX(x);
            indicator.setScaleX(1f);
            indicator.setScaleY(1f);
            return;
        }
        // Ordinary mode keeps the same blue selection, but uses a clean slide.
        if (!NativeTransformerMode.enabled(indicator.getContext())) {
            indicator.animate().translationX(x).scaleX(1f).scaleY(1f)
                    .setDuration(NativeHomeExpressiveProfile.INDICATOR_TRAVEL_MS)
                    .setInterpolator(NativeMotion.easing()).start();
            return;
        }
        indicator.animate().translationX(x).scaleX(1.08f).scaleY(.96f)
                .setDuration(NativeHomeExpressiveProfile.INDICATOR_TRAVEL_MS)
                .setInterpolator(NativeMotion.easing())
                .withEndAction(() -> indicator.animate().scaleX(1f).scaleY(1f)
                        .setDuration(NativeHomeExpressiveProfile.INDICATOR_SETTLE_MS)
                        .setInterpolator(NativeMotion.easing()).start())
                .start();
    }

    /**
     * Replay only the actual saved project cards. Reuses existing Views even
     * when saved project data did not change on return from Workspace.
     * No Transformer dependency, no layout animation or main-thread disk IO.
     */
    static void staggerRecentRows(@NonNull ViewGroup list) {
        int shown = 0;
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            Object tag = child.getTag();
            if (!(tag instanceof String)
                    || (!((String) tag).startsWith("native_home_recent_item_")
                    && !"native_home_recent_empty".equals(tag))) continue;
            child.animate().cancel();
            if (!NativeMotion.animationsEnabled()) {
                child.setAlpha(1f);
                child.setTranslationY(0f);
                continue;
            }
            child.setAlpha(.12f);
            child.setTranslationY(dp(child, 10));
            child.animate().alpha(1f).translationY(0f)
                    .setStartDelay(shown * NativeHomeExpressiveProfile.RECENT_STAGGER_MS)
                    .setDuration(NativeHomeExpressiveProfile.RECENT_ENTER_MS)
                    .setInterpolator(NativeMotion.easing()).start();
            shown++;
        }
    }

    static void staggerChildren(@NonNull ViewGroup list) {
        if (!enabled(list)) return;
        int limit = Math.min(list.getChildCount(), 5);
        for (int i = 0; i < limit; i++) {
            View child = list.getChildAt(i);
            child.animate().cancel();
            child.setAlpha(.15f);
            child.setTranslationY(dp(child, 26));
            child.setScaleX(.968f);
            child.setScaleY(.968f);
            long delay = (long) i * NativeHomeExpressiveProfile.STAGGER_STEP_MS;
            child.post(() -> {
                if (!child.isAttachedToWindow()) {
                    child.setAlpha(1f); child.setTranslationY(0f);
                    child.setScaleX(1f); child.setScaleY(1f);
                    return;
                }
                child.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay)
                        .setDuration(NativeHomeExpressiveProfile.MICRO_MS + 95L)
                        .setInterpolator(OVERSHOOT).start();
            });
        }
    }

    /** Only the CTA is a shared element: no screen bitmaps or canvas snapshots. */
    static void configureSharedElement(@NonNull Activity activity) {
        TransitionSet shared = new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new ChangeClipBounds())
                .addTransition(new ChangeTransform());
        shared.setOrdering(TransitionSet.ORDERING_TOGETHER);
        shared.setDuration(NativeHomeExpressiveProfile.MORPH_MS);
        shared.setInterpolator(NativeMotion.easing());
        activity.getWindow().setSharedElementEnterTransition(shared);
        activity.getWindow().setSharedElementReturnTransition(shared.clone());
    }

    static void shiftHeaderColor(@NonNull View header, int from, int to) {
        if (!enabled(header)) {
            header.setBackgroundColor(to);
            return;
        }
        header.setBackgroundColor(from);
        ValueAnimator animator = ValueAnimator.ofArgb(from, to);
        animator.setStartDelay(NativeHomeExpressiveProfile.MORPH_MS - 80);
        animator.setDuration(200);
        animator.setInterpolator(NativeMotion.easing());
        animator.addUpdateListener(value ->
                header.setBackgroundColor((Integer) value.getAnimatedValue()));
        animator.start();
    }

    static float dp(View v, float d) {
        return d * v.getResources().getDisplayMetrics().density;
    }
}
