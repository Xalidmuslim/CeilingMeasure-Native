package ru.xalidmuslim.ceilingmeasure;

import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/** Explicit responsive long press for Android buttons/cards, preserving short taps and scrolling. */
final class NativeFastLongPress {
    static final long HOLD_MS = NativeTouchTargetPolicy.EDIT_HOLD_MS;

    private NativeFastLongPress() {}

    static void attach(View view, Runnable action) {
        Handler handler = new Handler(Looper.getMainLooper());
        int slop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        final float[] down = new float[2];
        final boolean[] triggered = {false};
        final boolean[] moved = {false};
        Runnable fire = () -> {
            if (moved[0] || !view.isPressed() || !view.isAttachedToWindow()) return;
            triggered[0] = true;
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            action.run();
        };
        view.setClickable(true);
        // Disable Android's later, platform-timeout long press to avoid double actions.
        view.setLongClickable(false);
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    triggered[0] = false;
                    moved[0] = false;
                    down[0] = event.getX();
                    down[1] = event.getY();
                    handler.removeCallbacks(fire);
                    handler.postDelayed(fire, HOLD_MS);
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (Math.hypot(event.getX() - down[0], event.getY() - down[1]) > slop) {
                        moved[0] = true;
                        handler.removeCallbacks(fire);
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(fire);
                    if (triggered[0]) {
                        v.setPressed(false);
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(fire);
                    return false;
                default:
                    return false;
            }
        });
    }
}
