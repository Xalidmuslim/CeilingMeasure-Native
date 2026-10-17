package ru.kbr.cargoshare

import org.junit.Assert.*
import org.junit.Test

class RentalRulesTest {
    private val van = demoFleet.first()

    @Test fun hoursArePricedByHourlyRate() {
        assertEquals(1300, RentalRules.fare(van, 2))
        assertEquals(2600, RentalRules.fare(van, 4))
        assertEquals(5200, RentalRules.fare(van, 8))
    }

    @Test fun fullDayUsesDayPriceNot24HourRate() {
        assertEquals(4200, RentalRules.fare(van, 24))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedDurationIsRejected() {
        RentalRules.fare(van, 3)
    }

    @Test fun statusProgressionIsBounded() {
        assertEquals(RentalStage.CONFIRMED, RentalRules.nextStage(RentalStage.REQUESTED))
        assertEquals(RentalStage.ACTIVE, RentalRules.nextStage(RentalStage.CONFIRMED))
        assertEquals(RentalStage.COMPLETED, RentalRules.nextStage(RentalStage.ACTIVE))
        assertNull(RentalRules.nextStage(RentalStage.COMPLETED))
        assertNull(RentalRules.nextStage(RentalStage.CANCELLED))
    }

    @Test fun cancellationIsBlockedAfterVehicleHandover() {
        assertTrue(RentalRules.mayCancel(RentalStage.REQUESTED))
        assertTrue(RentalRules.mayCancel(RentalStage.CONFIRMED))
        assertFalse(RentalRules.mayCancel(RentalStage.ACTIVE))
        assertFalse(RentalRules.mayCancel(RentalStage.COMPLETED))
    }
}
