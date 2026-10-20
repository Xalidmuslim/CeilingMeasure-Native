package ru.xalidmuslim.ceilingmeasure;

import org.junit.Test;

import static org.junit.Assert.*;

/** Short wall press competes with a corner hitbox: wall wins except at the actual dot. */
public final class NativeTouchTargetPolicyTest {
    @Test public void aPressOnShortWallOutsidePreciseCornerPrefersWall() {
        assertTrue(NativeTouchTargetPolicy.preferWall(18f,2f,48f));
        assertTrue(NativeTouchTargetPolicy.preferWall(15f,1f,65f));
    }

    @Test public void deliberatePreciseCornerPressStillOpensCornerMenu() {
        assertFalse(NativeTouchTargetPolicy.preferWall(7f,1f,40f));
        assertFalse(NativeTouchTargetPolicy.preferWall(9f,0f,140f));
    }

    @Test public void wallPressMayWinWithoutNearbyVertex() {
        assertTrue(NativeTouchTargetPolicy.preferWall(Float.POSITIVE_INFINITY,6f,140f));
        assertFalse(NativeTouchTargetPolicy.preferWall(22f,28f,45f));
    }
}
