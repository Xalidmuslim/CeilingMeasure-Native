package ru.kbr.cargoshare

import org.junit.Assert.*
import org.junit.Test

class PhoneGpsPolicyV96Test {
    @Test fun validLatitudeAndLongitude() {
        assertTrue(PhoneGpsPolicyV96.valid(43.485,43.608))
        assertTrue(PhoneGpsPolicyV96.valid(52.0,4.0))
        assertFalse(PhoneGpsPolicyV96.valid(94.0,43.0))
        assertFalse(PhoneGpsPolicyV96.valid(43.0,181.0))
        assertFalse(PhoneGpsPolicyV96.valid(Double.NaN,5.0))
    }

    @Test fun oldCoordinatesAreNotShownAsLive() {
        val now=1_782_000_000_000L
        assertTrue(PhoneGpsPolicyV96.fresh(now-6_000,now))
        assertFalse(PhoneGpsPolicyV96.fresh(now-130_000,now))
        assertFalse(PhoneGpsPolicyV96.fresh(now+10_000,now))
    }

    @Test fun zeroAndNegativeAccuracyAreRejected() {
        assertFalse(PhoneGpsPolicyV96.validAccuracy(0f))
        assertFalse(PhoneGpsPolicyV96.validAccuracy(-5f))
        assertFalse(PhoneGpsPolicyV96.validAccuracy(Float.NaN))
        assertTrue(PhoneGpsPolicyV96.validAccuracy(15.5f))
    }

    @Test fun noDeviceTrackingStartsByDefault() {
        assertFalse(PhoneGpsPolicyV96.ENABLED_BY_DEFAULT)
    }
}
