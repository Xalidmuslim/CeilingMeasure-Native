package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Regression contract: staged architectural construction, not generic zoom. */
public final class CeilingInstallationProfileTest {
    @Test public void frameCompletesBeforeFabricAndLights() {
        assertTrue(CeilingInstallationProfile.profile(.28f) > .99f);
        assertTrue(CeilingInstallationProfile.membrane(.28f) < .3f);
        assertEquals(0f, CeilingInstallationProfile.light(.28f, 0), 0f);
        assertTrue(CeilingInstallationProfile.membrane(.68f) > .99f);
        assertTrue(CeilingInstallationProfile.wrinkles(.72f) < .01f);
    }

    @Test public void idlePacingIsReadablySlowerThanTapAndHasRest() {
        assertTrue(CeilingInstallationProfile.IDLE_DURATION_MS
                >= CeilingInstallationProfile.DURATION_MS * 4);
        assertTrue(CeilingInstallationProfile.IDLE_HOLD_MS >= 900);
        assertTrue(CeilingInstallationProfile.RESTART_AFTER_CANCEL_MS >= 0);
        assertTrue(CeilingInstallationProfile.IDLE_DURATION_MS
                + CeilingInstallationProfile.IDLE_HOLD_MS <= 5000);
    }

    @Test public void fourLampsIlluminateOneAfterTheOther() {
        float p = .73f;
        assertTrue(CeilingInstallationProfile.light(p, 0)
                > CeilingInstallationProfile.light(p, 1));
        assertTrue(CeilingInstallationProfile.light(p, 1)
                >= CeilingInstallationProfile.light(p, 2));
        assertEquals(0f, CeilingInstallationProfile.light(p, 3), 0f);
        for (int i = 0; i < 4; i++) {
            assertEquals(1f, CeilingInstallationProfile.light(1f, i), 0f);
        }
    }

    @Test public void phasesAreClampedSmoothAndMonotonic() {
        for (int i = 0; i <= 100; i++) {
            float p = i / 100f;
            assertTrue(CeilingInstallationProfile.profile(p) >= 0f);
            assertTrue(CeilingInstallationProfile.profile(p) <= 1f);
            assertTrue(CeilingInstallationProfile.membrane(p) >= 0f);
            assertTrue(CeilingInstallationProfile.membrane(p) <= 1f);
            if (i > 0) {
                float before = (i - 1) / 100f;
                assertTrue(CeilingInstallationProfile.profile(p)
                        >= CeilingInstallationProfile.profile(before));
                assertTrue(CeilingInstallationProfile.membrane(p)
                        >= CeilingInstallationProfile.membrane(before));
            }
        }
        assertEquals(0f, CeilingInstallationProfile.phase(-1f, .2f, .5f), 0f);
        assertEquals(1f, CeilingInstallationProfile.phase(2f, .2f, .5f), 0f);
        assertTrue(CeilingInstallationProfile.DURATION_MS <= 800);
    }
}
