package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;

/** Validate that the approved blue illustration really animates on press. */
@RunWith(AndroidJUnit4.class)
public final class NativeHomeInstallationUiTest {
    @Test public void assemblyProgressesOnTouchAndCancelsCleanly() throws Exception {
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                View card = root.findViewWithTag("native_home_new");
                View art = root.findViewWithTag("native_home_ceiling_art");
                assertNotNull(card);
                assertNotNull(art);
                long time = SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN,
                        card.getWidth() / 2f, card.getHeight() / 2f, 0);
                try {
                    card.dispatchTouchEvent(down);
                } finally {
                    down.recycle();
                }
                if (NativeMotion.animationsEnabled()) {
                    assertTrue("Construction must start before opening the next page",
                            boolField(art, "installing"));
                }
            });
            SystemClock.sleep(340L);
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                View art = root.findViewWithTag("native_home_ceiling_art");
                View card = root.findViewWithTag("native_home_new");
                if (NativeMotion.animationsEnabled()) {
                    float progress = floatField(art, "installationProgress");
                    // Instrumentation can be descheduled for >780ms on a
                    // busy emulator. Its delayed callback must not mistake a
                    // successfully finished animation for a graphics defect.
                    assertTrue("Touch must advance construction", progress > .12f);
                    assertTrue("Construction progress must remain clamped", progress <= 1f);
                }
                long time = SystemClock.uptimeMillis();
                MotionEvent cancel = MotionEvent.obtain(time, time,
                        MotionEvent.ACTION_CANCEL,
                        card.getWidth() / 2f, card.getHeight() / 2f, 0);
                try {
                    card.dispatchTouchEvent(cancel);
                } finally {
                    cancel.recycle();
                }
                assertFalse("Canceled press must reset the mounting scene",
                        boolField(art, "installing"));
                assertEquals(0f, floatField(art, "installationProgress"), 0f);
            });
        }
    }

    private static boolean boolField(View view, String name) {
        try {
            Field field = view.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getBoolean(view);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Ceiling motion field missing: " + name, e);
        }
    }

    private static float floatField(View view, String name) {
        try {
            Field field = view.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getFloat(view);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Ceiling motion field missing: " + name, e);
        }
    }
}
