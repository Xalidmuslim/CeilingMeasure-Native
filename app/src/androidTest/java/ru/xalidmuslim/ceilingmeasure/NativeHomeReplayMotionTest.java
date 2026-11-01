package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** The two home animations do not depend on the old full-screen Transformer. */
@RunWith(AndroidJUnit4.class)
public final class NativeHomeReplayMotionTest {
    @Test public void blueNewMeasurementArtMovesOnTouchDownInOrdinaryMode() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean before = NativeTransformerMode.enabled(context);
        NativeTransformerMode.setEnabled(context, false);
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                View button = root.findViewWithTag("native_home_new");
                View art = root.findViewWithTag("native_home_ceiling_art");
                assertNotNull(button);
                assertNotNull(art);
                long now = SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_DOWN, button.getWidth() / 2f,
                        button.getHeight() / 2f, 0);
                try {
                    assertTrue(button.dispatchTouchEvent(down));
                } finally {
                    down.recycle();
                }
            });
            SystemClock.sleep(180L);
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                View button = root.findViewWithTag("native_home_new");
                View art = root.findViewWithTag("native_home_ceiling_art");
                if (NativeMotion.animationsEnabled()) {
                    assertTrue("Artwork must move before navigating, even in Ordinary mode",
                            art.getTranslationX() < -0.5f);
                }
                long now = SystemClock.uptimeMillis();
                MotionEvent cancel = MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_CANCEL, button.getWidth() / 2f,
                        button.getHeight() / 2f, 0);
                try {
                    button.dispatchTouchEvent(cancel);
                } finally {
                    cancel.recycle();
                }
            });
        } finally {
            NativeTransformerMode.setEnabled(context, before);
        }
    }

    @Test public void unchangedSavedCardsCanRepeatStaggerWithoutReinflation() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean before = NativeTransformerMode.enabled(context);
        NativeTransformerMode.setEnabled(context, false);
        try (ActivityScenario<NativeHomeActivity> scenario =
                     ActivityScenario.launch(NativeHomeActivity.class)) {
            scenario.onActivity(activity -> {
                LinearLayout list = new LinearLayout(activity);
                View first = new View(activity);
                first.setTag("native_home_recent_item_0");
                View divider = new View(activity);
                View second = new View(activity);
                second.setTag("native_home_recent_item_1");
                list.addView(first);
                list.addView(divider);
                list.addView(second);
                NativeHomeExpressiveMotion.staggerRecentRows(list);
                assertEquals("Separators must never fade", 1f, divider.getAlpha(), 0.001f);
                if (NativeMotion.animationsEnabled()) {
                    assertEquals(.12f, first.getAlpha(), .001f);
                    assertEquals(.12f, second.getAlpha(), .001f);
                }
                // No replacement or snapshot change; the exact same cards replay.
                NativeHomeExpressiveMotion.staggerRecentRows(list);
                assertTrue(list.getChildAt(0) == first);
                assertTrue(list.getChildAt(2) == second);
                if (NativeMotion.animationsEnabled()) {
                    assertEquals(.12f, first.getAlpha(), .001f);
                    assertTrue(first.getTranslationY() > 0f);
                }
            });
        } finally {
            NativeTransformerMode.setEnabled(context, before);
        }
    }
}
