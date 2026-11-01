package ru.xalidmuslim.ceilingmeasure;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Page-only animation. Never animates navigation buttons, drawing geometry,
 * or the surface behind a dialog. No scale, rotation, stagger or shared element.
 */
final class NativePageMotion {
    private NativePageMotion() {}

    static void enter(@NonNull View content, boolean back) {
        content.animate().cancel();
        content.setTranslationX(0f);
        content.setTranslationY(0f);
        content.setScaleX(1f);
        content.setScaleY(1f);
        content.setRotationY(0f);
        content.setAlpha(1f);
        if (!NativeMotion.animationsEnabled()) return;
        if (!NativeTransformerMode.enabled(content.getContext())) {
            NativeMotion.enterCompact(content);
            return;
        }
        float offset = NativePageMotionProfile.direction(back)
                * NativePageMotionProfile.SLIDE_DP
                * content.getResources().getDisplayMetrics().density;
        content.setTranslationX(offset);
        content.setAlpha(NativePageMotionProfile.START_ALPHA);
        content.post(() -> {
            if (!content.isAttachedToWindow() || !NativeMotion.animationsEnabled()) {
                content.animate().cancel();
                content.setTranslationX(0f);
                content.setAlpha(1f);
                return;
            }
            content.animate().translationX(0f).alpha(1f)
                    .setDuration(NativePageMotionProfile.DURATION_MS)
                    .setInterpolator(NativeMotion.easing()).start();
        });
    }

    static void activityTransition(@NonNull Activity activity, boolean back) {
        if (!NativeMotion.animationsEnabled()
                || !NativeTransformerMode.enabled(activity)) {
            activity.overridePendingTransition(0, 0);
            return;
        }
        activity.overridePendingTransition(
                back ? R.anim.ceiling_axis_in_left : R.anim.ceiling_axis_in_right,
                back ? R.anim.ceiling_axis_out_right : R.anim.ceiling_axis_out_left);
    }
}
