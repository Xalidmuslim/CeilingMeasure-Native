package ru.xalidmuslim.ceilingmeasure;

import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.NonNull;

/**
 * Non-displacing entrance animation. Never hide an entire editor frame:
 * bottom navigation and drawing remain visible during re-attachment.
 */
final class NativeMotion {
    private static final DecelerateInterpolator EASE = new DecelerateInterpolator(1.2f);
    private NativeMotion() {}

    static void enter(@NonNull View view) {
        fadeIn(view,135L);
    }

    static void enterCompact(@NonNull View view) {
        fadeIn(view,105L);
    }

    private static void fadeIn(@NonNull View view,long duration) {
        view.animate().cancel();
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        // The root (including navigation) is always visible on the first frame.
        // Fading from 0f on a detached/reused view caused apparent missing UI.
        view.setAlpha(.94f);
        // Window/shell drawing must run on the UI looper. Detached preview
        // views may be instantiated by Android tests on a worker thread.
        view.post(()->{
            if(!view.isAttachedToWindow()){
                view.setAlpha(1f);
                return;
            }
            view.animate()
                    .alpha(1f)
                    .setDuration(duration)
                    .setInterpolator(EASE)
                    .start();
        });
    }
}
