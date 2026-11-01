package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Existing button micro-motion remains; page transitions have their own budget. */
public final class NativeHomeExpressiveProfileTest {
    @Test public void existingButtonMotionStaysWithinInteractiveBudget() {
        assertTrue(NativeHomeExpressiveProfile.MICRO_MS >= 200);
        assertTrue(NativeHomeExpressiveProfile.MICRO_MS <= 300);
    }

    @Test public void dockHasOnlyRealExistingActions() {
        assertEquals(3, NativeHomeExpressiveProfile.QUICK_ACTIONS_COUNT);
        assertEquals(5, NativeHomeExpressiveProfile.DOCK_SLOTS);
        assertEquals(2, NativeHomeExpressiveProfile.CENTER_SLOT);
    }

    @Test public void interpolationIsClampedAndCanHideOnlyScrollableDock() {
        assertEquals(0f, NativeHomeExpressiveProfile.clamp(-5f), 0f);
        assertEquals(1f, NativeHomeExpressiveProfile.clamp(7f), 0f);
        assertEquals(.5f, NativeHomeExpressiveProfile.clamp(.5f), .0001f);
        assertTrue(NativeHomeExpressiveProfile.shouldHideDock(300, 250, 60, 20));
    }
}
