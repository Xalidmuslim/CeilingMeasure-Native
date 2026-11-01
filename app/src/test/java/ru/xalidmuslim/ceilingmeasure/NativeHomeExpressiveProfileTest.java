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

    @Test public void dockIndicatorFinishesItsTravelBeforeOldBounceDid() {
        assertTrue(NativeHomeExpressiveProfile.INDICATOR_TRAVEL_MS >= 110);
        assertTrue(NativeHomeExpressiveProfile.INDICATOR_TRAVEL_MS <= 170);
        assertTrue(NativeHomeExpressiveProfile.INDICATOR_SETTLE_MS <= 110);
        assertTrue(NativeHomeExpressiveProfile.INDICATOR_TRAVEL_MS
                + NativeHomeExpressiveProfile.INDICATOR_SETTLE_MS < 300);
    }

    @Test public void recentMeasurementsCanReplayWithinFastMotionBudget() {
        assertTrue(NativeHomeExpressiveProfile.RECENT_STAGGER_MS >= 45);
        assertTrue(NativeHomeExpressiveProfile.RECENT_STAGGER_MS <= 85);
        assertTrue(NativeHomeExpressiveProfile.RECENT_ENTER_MS >= 185);
        assertTrue(NativeHomeExpressiveProfile.RECENT_ENTER_MS <= 280);
        assertTrue("Three recent projects finish in under half a second",
                2 * NativeHomeExpressiveProfile.RECENT_STAGGER_MS
                        + NativeHomeExpressiveProfile.RECENT_ENTER_MS < 500);
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
