package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NativeRoomSwipePolicyTest {
    @Test public void deliberateHorizontalSwipeWinsOverVerticalScrolling() {
        assertTrue(NativeRoomSwipePolicy.horizontal(-220f, 27f, 2f,
                NativeRoomSwipePolicy.MIN_DISTANCE_DP));
        assertTrue(NativeRoomSwipePolicy.horizontal(225f, -42f, 2f,
                NativeRoomSwipePolicy.MIN_DISTANCE_DP));
        assertFalse(NativeRoomSwipePolicy.horizontal(38f, 2f, 2f,
                NativeRoomSwipePolicy.MIN_DISTANCE_DP));
        assertFalse(NativeRoomSwipePolicy.horizontal(100f, 150f, 1f,
                NativeRoomSwipePolicy.MIN_DISTANCE_DP));
        assertFalse(NativeRoomSwipePolicy.horizontal(70f, 60f, 1f,
                NativeRoomSwipePolicy.MIN_DISTANCE_DP));
    }

    @Test public void leftAdvancesAndRightReturnsWithoutWrapping() {
        assertEquals(1, NativeRoomSwipePolicy.direction(-140f));
        assertEquals(-1, NativeRoomSwipePolicy.direction(140f));
        assertEquals(1, NativeRoomSwipePolicy.target(0, 3, 1));
        assertEquals(2, NativeRoomSwipePolicy.target(1, 3, 1));
        assertEquals(0, NativeRoomSwipePolicy.target(1, 3, -1));
        assertEquals(0, NativeRoomSwipePolicy.target(0, 3, -1));
        assertEquals(2, NativeRoomSwipePolicy.target(2, 3, 1));
        assertEquals(0, NativeRoomSwipePolicy.target(0, 1, 1));
    }
}
