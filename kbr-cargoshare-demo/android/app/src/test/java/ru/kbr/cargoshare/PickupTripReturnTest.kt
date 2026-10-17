package ru.kbr.cargoshare

import org.junit.Assert.*
import org.junit.Test

class PickupTripReturnTest {
    private fun paid(): ShowcaseSession {
        var s = ShowcaseEngine.verifyPhone(ShowcaseSession(), "0000")
        s = ShowcaseEngine.submitDocuments(s, true, true)
        s = ShowcaseEngine.approve(s)
        s = ShowcaseEngine.reserve(s, "g01", 4, "Сегодня", "12:00",
            title="ГАЗель NEXT", pickup="Нальчик", fare=2600)
        s = ShowcaseEngine.signContract(s, true)
        return ShowcaseEngine.simulatePayment(s, true)
    }

    @Test fun inspectionUnlockDriveAndReturn() {
        var s = ShowcaseEngine.arriveAtVehicle(paid())
        assertEquals(ShowcaseStage.PICKUP_INSPECTION, s.stage)
        s = ShowcaseEngine.confirmPickupInspection(s, true, true, true, true)
        assertEquals(ShowcaseStage.UNLOCK, s.stage)
        s = ShowcaseEngine.unlockForPickup(s)
        assertTrue(s.doorsOpen)
        s = ShowcaseEngine.startDriving(s)
        assertEquals(ShowcaseStage.DRIVING, s.stage)
        assertFalse(s.doorsOpen)
        s = ShowcaseEngine.advanceTrip(s, 45, 18)
        assertEquals(45, s.elapsedMinutes)
        assertEquals(18, s.distanceKm)
        assertEquals(195, ShowcaseEngine.remainingMinutes(s))
        s = ShowcaseEngine.requestReturn(s)
        assertEquals(ShowcaseStage.RETURN, s.stage)
        s = ShowcaseEngine.completeReturn(s, true)
        assertEquals(ShowcaseStage.FINISHED, s.stage)
        assertFalse(s.depositReturned)
    }

    @Test(expected=IllegalArgumentException::class)
    fun pickupInspectionRequiresAllChecks() {
        ShowcaseEngine.confirmPickupInspection(ShowcaseEngine.arriveAtVehicle(paid()),
            true, true, false, true)
    }

    @Test(expected=IllegalArgumentException::class)
    fun cannotOpenUninspectedVehicle() {
        ShowcaseEngine.unlockForPickup(ShowcaseEngine.arriveAtVehicle(paid()))
    }

    @Test(expected=IllegalArgumentException::class)
    fun cannotDriveBeforeUnlocking() {
        val s = ShowcaseEngine.confirmPickupInspection(
            ShowcaseEngine.arriveAtVehicle(paid()), true, true, true, true)
        ShowcaseEngine.startDriving(s)
    }

    @Test(expected=IllegalArgumentException::class)
    fun timeCannotJumpBackward() {
        var s = ShowcaseEngine.arriveAtVehicle(paid())
        s = ShowcaseEngine.confirmPickupInspection(s, true, true, true, true)
        s = ShowcaseEngine.unlockForPickup(s)
        s = ShowcaseEngine.startDriving(s)
        ShowcaseEngine.advanceTrip(s, -5, 12)
    }

    @Test fun extraTimeChangesRemainingTimeAndQuote() {
        var s = ShowcaseEngine.arriveAtVehicle(paid())
        s = ShowcaseEngine.confirmPickupInspection(s,true,true,true,true)
        s = ShowcaseEngine.unlockForPickup(s)
        s = ShowcaseEngine.startDriving(s)
        s = ShowcaseEngine.advanceTrip(s, 210, 27)
        assertEquals(30, ShowcaseEngine.remainingMinutes(s))
        s = ShowcaseEngine.extend(s, 2, 650)
        assertEquals(150, ShowcaseEngine.remainingMinutes(s))
        assertEquals(1300, s.extensionCharge)
    }
}