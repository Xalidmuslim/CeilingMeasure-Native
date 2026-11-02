package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.animation.ValueAnimator;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

/** The idle vector animation may run only on the visible Home screen. */
@RunWith(AndroidJUnit4.class)
public final class NativeHomeIdleInstallationUiTest {
    @Test public void autoStartsPausesOnStopAndRestartsOnHomeResume() {
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            AtomicReference<View> artwork = new AtomicReference<>();
            scenario.onActivity(activity -> artwork.set(
                    activity.findViewById(android.R.id.content)
                            .findViewWithTag("native_home_ceiling_art")));
            assertNotNull(artwork.get());
            SystemClock.sleep(330);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                if (NativeMotion.animationsEnabled()) {
                    assertTrue("Home artwork should auto-run with no tap",
                            boolField(artwork.get(), "idleLoopRequested"));
                    assertTrue("First installation cycle should be drawing",
                            boolField(artwork.get(), "installing"));
                    assertTrue("Animation must advance frames",
                            floatField(artwork.get(), "installationProgress") > 0f);
                    ValueAnimator slow = (ValueAnimator) objectField(artwork.get(), "scanAnimator");
                    assertNotNull("First cycle must still be animating", slow);
                    assertEquals("Initial Home assembly should use the slow 6-second profile",
                            CeilingInstallationProfile.IDLE_DURATION_MS, slow.getDuration());
                }
            });

            scenario.moveToState(Lifecycle.State.CREATED);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertFalse(boolField(artwork.get(), "idleLoopRequested"));
                assertFalse(boolField(artwork.get(), "installing"));
                assertEquals(0f, floatField(artwork.get(), "installationProgress"), 0f);
                assertTrue("Stopped Home must not keep a frame animator",
                        objectField(artwork.get(), "scanAnimator") == null);
            });
            scenario.moveToState(Lifecycle.State.RESUMED);
            SystemClock.sleep(250);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                if (NativeMotion.animationsEnabled()) {
                    assertTrue("Returning Home must restart idle installation",
                            boolField(artwork.get(), "idleLoopRequested"));
                    assertTrue(boolField(artwork.get(), "installing"));
                }
            });
        }
    }

    @Test public void tapInterruptsSlowLoopWithOriginalFastAssembly() {
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            SystemClock.sleep(230);
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                View card = root.findViewWithTag("native_home_new");
                View art = root.findViewWithTag("native_home_ceiling_art");
                assertNotNull(card);
                assertNotNull(art);
                long time = SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(time, time,
                        MotionEvent.ACTION_DOWN,
                        card.getWidth() * .5f, card.getHeight() * .5f, 0);
                try { card.dispatchTouchEvent(down); }
                finally { down.recycle(); }
                if (NativeMotion.animationsEnabled()) {
                    ValueAnimator running = (ValueAnimator) objectField(art, "scanAnimator");
                    assertNotNull(running);
                    assertEquals(CeilingInstallationProfile.DURATION_MS,
                            running.getDuration());
                    assertTrue(boolField(art, "installing"));
                }
                MotionEvent cancel = MotionEvent.obtain(time, time + 40,
                        MotionEvent.ACTION_CANCEL,
                        card.getWidth() * .5f, card.getHeight() * .5f, 0);
                try { card.dispatchTouchEvent(cancel); }
                finally { cancel.recycle(); }
                assertFalse(boolField(art, "installing"));
            });
            SystemClock.sleep(CeilingInstallationProfile.RESTART_AFTER_CANCEL_MS + 240);
            scenario.onActivity(activity -> {
                if (!NativeMotion.animationsEnabled()) return;
                View art = activity.findViewById(android.R.id.content)
                        .findViewWithTag("native_home_ceiling_art");
                assertTrue("Canceled touch returns to slow idle playback",
                        boolField(art, "installing"));
                ValueAnimator running = (ValueAnimator) objectField(art, "scanAnimator");
                if (running != null) {
                    assertEquals(CeilingInstallationProfile.IDLE_DURATION_MS,
                            running.getDuration());
                } else {
                    // Busy emulators may defer the assertion until the one-second
                    // completed-ceiling hold. This is still a healthy idle cycle.
                    assertEquals("Finished idle frame stays visible during hold",
                            1f, floatField(art, "installationProgress"), 0f);
                }
            });
        }
    }

    private static Object objectField(View view, String name) {
        try {
            Field field = view.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(view);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Ceiling motion field missing: " + name, e);
        }
    }

    private static boolean boolField(View view, String name) {
        return (Boolean) objectField(view, name);
    }

    private static float floatField(View view, String name) {
        return (Float) objectField(view, name);
    }
}
