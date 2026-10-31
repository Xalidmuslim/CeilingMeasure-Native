package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
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
}
