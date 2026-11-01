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

    private static void spring(@NonNull View view, float target, float damping) {
        if (!enabled(view)) return;
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
            @NonNull Runnable action, boolean mainCta, boolean scanner) {
        card.setOnTouchListener((v, event) -> {
            if (!enabled(v)) return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    spring(v, mainCta ? .96f : .975f, SpringForce.DAMPING_RATIO_NO_BOUNCY);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    spring(v, 1f, .59f);
                    break;
                default:
                    break;
            }
            return false;
        });
        card.setOnClickListener(v -> {
            if (!enabled(v)) {
                action.run();
                return;
            }
            if (mainCta) v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
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

    /** Matte indicator stretches between existing tab positions and recoils. */
    static void moveIndicator(@NonNull View indicator, int index) {
        if (!enabled(indicator)) return;
        ViewGroup parent = (ViewGroup) indicator.getParent();
        if (parent.getWidth() <= 0) return;
        float center = parent.getWidth() * (index + .5f)
                / NativeHomeExpressiveProfile.DOCK_SLOTS;
        float x = center - indicator.getWidth() / 2f;
        indicator.animate().cancel();
        indicator.animate().translationX(x).scaleX(1.34f).scaleY(.92f)
                .setDuration(NativeHomeExpressiveProfile.MICRO_MS)
                .setInterpolator(NativeMotion.easing())
                .withEndAction(() -> indicator.animate().scaleX(1f).scaleY(1f)
                        .setDuration(180).setInterpolator(OVERSHOOT).start())
                .start();
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
