package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Contract for the native Transformer Motion navigation choreography. */
public final class TransformerMotionProfileTest {
    @Test public void fiveDestinationDockMovesOutwardFromTheCenter() {
        assertEquals(-1, TransformerMotionProfile.directionForIndex(0, 5));
        assertEquals(-1, TransformerMotionProfile.directionForIndex(1, 5));
        assertEquals(0, TransformerMotionProfile.directionForIndex(2, 5));
        assertEquals(1, TransformerMotionProfile.directionForIndex(3, 5));
        assertEquals(1, TransformerMotionProfile.directionForIndex(4, 5));
    }

    @Test public void evenDestinationDockSplitsLeftAndRight() {
        assertEquals(-1, TransformerMotionProfile.directionForIndex(0, 4));
        assertEquals(-1, TransformerMotionProfile.directionForIndex(1, 4));
        assertEquals(1, TransformerMotionProfile.directionForIndex(2, 4));
        assertEquals(1, TransformerMotionProfile.directionForIndex(3, 4));
    }

    @Test public void transformsAreSubtleAndNavigationRespondsQuickly() {
        assertEquals(0, TransformerMotionProfile.directionForIndex(0, 1));
        assertTrue(TransformerMotionProfile.START_SCALE >= .97f);
        assertTrue(TransformerMotionProfile.ENTRY_X_DP <= 20f);
        assertTrue(TransformerMotionProfile.NAV_TAP_LOCK_MS <= 150L);
        assertTrue(TransformerMotionProfile.ENTRY_MS > TransformerMotionProfile.NAV_TAP_LOCK_MS);
    }
}
