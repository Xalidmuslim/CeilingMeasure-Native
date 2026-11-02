package ru.xalidmuslim.ceilingmeasure;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.WeakHashMap;

/**
 * Identical height + opacity transition for all Workspace sections.
 * Unlike a fade followed by GONE, the section's height changes through
 * the whole animation, so cards below it never jump abruptly.
 */
final class NativeSectionMotion {
    static final long DURATION_MS = 245L;
    private static final WeakHashMap<View, ValueAnimator> RUNNING = new WeakHashMap<>();

    private NativeSectionMotion() {}

    static void setExpanded(@NonNull View body, @Nullable TextView chevron,
            boolean collapsed) {
        ValueAnimator old = RUNNING.remove(body);
        if (old != null) {
            old.removeAllListeners(); // Interruption must not apply stale GONE.
            old.cancel();
        }
        if (chevron != null) chevron.setText(collapsed ? "⌄" : "⌃");
        if (!NativeMotion.animationsEnabled()) {
            ViewGroup.LayoutParams lp = body.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            body.setLayoutParams(lp);
            body.setAlpha(1f);
            body.setTranslationY(0f);
            body.setVisibility(collapsed ? View.GONE : View.VISIBLE);
            return;
        }
        View parent = (View) body.getParent();
        int width = body.getWidth() > 0 ? body.getWidth()
                : parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        if (width <= 0) {
            body.setVisibility(collapsed ? View.GONE : View.VISIBLE);
            return;
        }
        int from = body.getVisibility() == View.VISIBLE
                ? (body.getLayoutParams().height >= 0
                   ? body.getLayoutParams().height : body.getHeight()) : 0;
        body.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int natural = body.getMeasuredHeight();
        int to = collapsed ? 0 : natural;
        if (from == to) {
            body.setVisibility(collapsed ? View.GONE : View.VISIBLE);
            ViewGroup.LayoutParams lp = body.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            body.setLayoutParams(lp);
            body.setAlpha(1f);
            body.setTranslationY(0f);
            return;
        }
        body.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams lp = body.getLayoutParams();
        lp.height = from;
        body.setLayoutParams(lp);
        float startAlpha = Math.max(.15f, body.getAlpha());
        ValueAnimator height = ValueAnimator.ofInt(from, to);
        RUNNING.put(body, height);
        height.setDuration(DURATION_MS);
        height.setInterpolator(NativeMotion.easing());
        height.addUpdateListener(value -> {
            ViewGroup.LayoutParams params = body.getLayoutParams();
            params.height = (Integer) value.getAnimatedValue();
            body.setLayoutParams(params);
            float progress = value.getAnimatedFraction();
            body.setAlpha(startAlpha + ((collapsed ? .3f : 1f) - startAlpha) * progress);
        });
        height.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (RUNNING.get(body) != height) return;
                RUNNING.remove(body);
                ViewGroup.LayoutParams params = body.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                body.setLayoutParams(params);
                body.setAlpha(1f);
                body.setTranslationY(0f);
                body.setVisibility(collapsed ? View.GONE : View.VISIBLE);
            }
        });
        height.start();
    }
}
