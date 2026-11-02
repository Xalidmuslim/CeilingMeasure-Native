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

    @Test public void newMeasurementArtworkHasSmoothVisibleMovementAndBoundedHandoff() {
        assertTrue("Press lasts long enough to see movement",
                NativeHomeExpressiveProfile.CTA_ART_PRESS_MS >= 230);
        assertTrue("Return must be gentle",
                NativeHomeExpressiveProfile.CTA_ART_RETURN_MS >= 290);
        assertTrue("Press movement is bigger than before",
                NativeHomeExpressiveProfile.CTA_ART_PRESS_X_DP >= 12);
        assertTrue("Click finishes at a visibly different position",
                NativeHomeExpressiveProfile.CTA_ART_CLICK_X_DP
                        > NativeHomeExpressiveProfile.CTA_ART_PRESS_X_DP);
        assertTrue("Enlargement should be subtle but noticeable",
                NativeHomeExpressiveProfile.CTA_ART_CLICK_SCALE >= 1.10f
                        && NativeHomeExpressiveProfile.CTA_ART_CLICK_SCALE <= 1.16f);
        assertTrue("Actual assembly should complete before navigation",
                NativeHomeExpressiveProfile.CTA_NAV_HANDOFF_MS
                        >= CeilingInstallationProfile.DURATION_MS);
        assertTrue("Single CTA must remain under one second",
                NativeHomeExpressiveProfile.CTA_NAV_HANDOFF_MS <= 900);
        assertEquals(NativeHomeExpressiveProfile.CTA_ART_CLICK_MS,
                NativeHomeExpressiveProfile.CTA_NAV_HANDOFF_MS);
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
