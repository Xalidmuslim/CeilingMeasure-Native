package ru.xalidmuslim.ceilingmeasure;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class NativeFastCeilingHandoffTest {
    @Test public void shortHandoffMatchesQuickInstallation() {
        assertTrue("CTA navigation must not wait 800 ms",
                NativeHomeExpressiveProfile.CTA_NAV_HANDOFF_MS <= 320);
        assertTrue("Ceiling installation should complete within a short tap",
                CeilingInstallationProfile.DURATION_MS <= 380);
        assertTrue("Finishing CTA animation should not lag behind navigation",
                NativeHomeExpressiveProfile.CTA_ART_CLICK_MS
                        <= NativeHomeExpressiveProfile.CTA_NAV_HANDOFF_MS + 50);
    }

    @Test public void finishedIllustrationHasAllFourFixtures() {
        for (int i = 0; i < 4; i++) {
            assertEquals(1f, CeilingInstallationProfile.light(1f, i), 0f);
        }
    }
}
