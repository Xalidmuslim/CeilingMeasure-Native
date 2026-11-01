package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Checks that a transformer button still invokes its original destination exactly once. */
@RunWith(AndroidJUnit4.class)
public final class NativeTransformerMotionTest {
    @Test public void rapidRepeatedNavigationDoesNotOpenDestinationTwice() throws Exception {
        AtomicInteger navigationCount = new AtomicInteger();
        CountDownLatch committed = new CountDownLatch(1);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final View[] navigation = new View[1];

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            LinearLayout item = new LinearLayout(context);
            ImageView glyph = new ImageView(context);
            item.addView(glyph);
            NativeTransformerMotion.bindNavigation(item, glyph, -1,
                    () -> {
                        navigationCount.incrementAndGet();
                        committed.countDown();
                    });
            navigation[0] = item;
            item.performClick();
            if (NativeMotion.animationsEnabled()) item.performClick();
        });

        assertTrue("Navigation did not complete", committed.await(3, TimeUnit.SECONDS));
        Thread.sleep(140L);
        assertEquals("A single navigation gesture must not open multiple screens",
                1, navigationCount.get());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> assertTrue(navigation[0].isEnabled()));
    }
    @Test public void acceptedBottomNavigationTapEmitsOneSoftTick() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            class TrackingNav extends LinearLayout {
                int count;
                int feedback;
                TrackingNav(Context c) { super(c); }
                @Override public boolean performHapticFeedback(int kind) {
                    count++;
                    feedback = kind;
                    return true;
                }
            }
            TrackingNav item = new TrackingNav(context);
            ImageView glyph = new ImageView(context);
            item.addView(glyph);
            NativeTransformerMotion.bindNavigation(item, glyph, 1, () -> {});
            item.performClick();
            assertEquals("Exactly one tactile tick per accepted press", 1, item.count);
            assertEquals(Build.VERSION.SDK_INT >= 34
                    ? HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                    : HapticFeedbackConstants.CLOCK_TICK, item.feedback);
            item.setEnabled(false);
            item.performClick();
            assertEquals("Disabled navigation must not vibrate", 1, item.count);
        });
    }

}
