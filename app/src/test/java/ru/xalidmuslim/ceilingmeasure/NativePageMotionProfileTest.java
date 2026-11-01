package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Regression contract: simple page switches, expressive button presses unchanged. */
public final class NativePageMotionProfileTest {
    @Test public void pagesMoveOnlySlightlyAlongTheHorizontalAxis() {
        assertTrue(NativePageMotionProfile.SLIDE_DP > 0f);
        assertTrue(NativePageMotionProfile.SLIDE_DP <= 16f);
        assertEquals(1, NativePageMotionProfile.direction(false));
        assertEquals(-1, NativePageMotionProfile.direction(true));
    }

    @Test public void pagesAreFastAndNearlyOpaque() {
        assertTrue(NativePageMotionProfile.DURATION_MS >= 150L);
        assertTrue(NativePageMotionProfile.DURATION_MS <= 220L);
        assertTrue(NativePageMotionProfile.START_ALPHA >= .95f);
        assertTrue(NativePageMotionProfile.START_ALPHA <= 1f);
    }
}
