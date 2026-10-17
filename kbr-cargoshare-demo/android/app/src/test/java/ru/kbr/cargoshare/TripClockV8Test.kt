package ru.kbr.cargoshare

import org.junit.Assert.*
import org.junit.Test

class TripClockV8Test {
    @Test fun timerStartsFromExactRentalDuration() {
        assertEquals("04:00:00", TripClockV8.display(ShowcaseSession(hours=4),0))
        assertEquals("03:59:59", TripClockV8.display(ShowcaseSession(hours=4),1))
    }
    @Test fun timerTracksDemoMovementAndExtension() {
        val s=ShowcaseSession(hours=4,extraHours=2,elapsedMinutes=61)
        assertEquals("04:58:59",TripClockV8.display(s,61))
    }
    @Test fun exhaustedRentalStopsAtZero() {
        val s=ShowcaseSession(hours=2,elapsedMinutes=120)
        assertEquals("00:00:00",TripClockV8.display(s,43))
    }
    @Test fun zeroDisplayNeverGoesNegative() {
        assertEquals("00:00:00",TripClockV8.display(ShowcaseSession(hours=2),100000))
    }
}
