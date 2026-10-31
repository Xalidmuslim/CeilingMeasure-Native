package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Exact stagger order is deterministic, distinct, and intentionally non-linear. */
public final class TransformerMotionScatterTest {
    @Test public void fiveItemsAppearFromDifferentPositionsInSequence() {
        int[] ranks = new int[5];
        for (int index = 0; index < 5; index++) {
            ranks[index] = TransformerMotionProfile.scatterRank(index, 5);
        }
        assertArrayEquals(new int[]{1, 3, 0, 4, 2}, ranks);
        assertTrue(TransformerMotionProfile.SCATTER_MS >= 360L);
        assertTrue(TransformerMotionProfile.SCATTER_STEP_MS >= 60L);
    }

    @Test public void evenNumberOfButtonsGetsUniqueRanks() {
        boolean[] used = new boolean[4];
        for (int index = 0; index < 4; index++) {
            int rank = TransformerMotionProfile.scatterRank(index, 4);
            assertTrue(rank >= 0 && rank < 4);
            assertTrue(!used[rank]);
            used[rank] = true;
        }
        assertEquals(0, TransformerMotionProfile.scatterRank(2, 4));
    }
}
