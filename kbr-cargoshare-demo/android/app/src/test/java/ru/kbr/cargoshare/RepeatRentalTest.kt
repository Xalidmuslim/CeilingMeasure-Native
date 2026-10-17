package ru.kbr.cargoshare

import org.junit.Assert.*
import org.junit.Test

class RepeatRentalTest {
    private fun firstRentalFinished(): ShowcaseSession {
        var s=ShowcaseEngine.verifyPhone(ShowcaseSession(), "0000")
        s=ShowcaseEngine.submitDocuments(s, true, true)
        assertEquals(ShowcaseStage.READY, s.stage)
        assertTrue(s.demoApproved)
        s=ShowcaseEngine.reserve(s,"g01",4,"Сегодня","09:00",title="ГАЗель NEXT",pickup="Нальчик",fare=2600)
        s=ShowcaseEngine.signContract(s,true)
        s=ShowcaseEngine.simulatePayment(s,true)
        s=ShowcaseEngine.arriveAtVehicle(s)
        s=ShowcaseEngine.confirmPickupInspection(s,true,true,true,true)
        s=ShowcaseEngine.unlockForPickup(s)
        s=ShowcaseEngine.startDriving(s)
        s=ShowcaseEngine.requestReturn(s)
        return ShowcaseEngine.completeReturn(s,true)
    }

    @Test fun demoDocumentsAutoApproveWithoutAdmin() {
        val phone=ShowcaseEngine.verifyPhone(ShowcaseSession(),"0000")
        val ready=ShowcaseEngine.submitDocuments(phone,true,true)
        assertEquals(ShowcaseStage.READY,ready.stage)
        assertTrue(ready.documentsSubmitted)
        assertTrue(ready.demoApproved)
    }

    @Test fun secondBookingSkipsDocumentsAndPreservesVerification() {
        val finished=firstRentalFinished()
        assertEquals(1,finished.ridesCompleted)
        val again=ShowcaseEngine.beginNextRental(finished)
        assertEquals(ShowcaseStage.READY,again.stage)
        assertTrue(again.demoApproved)
        assertTrue(again.documentsSubmitted)
        assertFalse(again.rentPaid)
        assertFalse(again.depositHeld)
        assertEquals("",again.vehicleId)
        val reserved=ShowcaseEngine.reserve(again,"g02",2,"Завтра","12:00")
        assertEquals(ShowcaseStage.CONTRACT,reserved.stage)
    }

    @Test(expected=IllegalArgumentException::class)
    fun cannotRestartWhileDriving() {
        val finished=firstRentalFinished()
        val resumed=ShowcaseEngine.beginNextRental(finished)
        val booked=ShowcaseEngine.reserve(resumed,"g01",2,"Сегодня","12:00")
        ShowcaseEngine.beginNextRental(booked)
    }

    @Test fun oldPendingReviewIsUpgradedToAutoApproved() {
        val old=ShowcaseSession(stage=ShowcaseStage.REVIEW,documentsSubmitted=true)
        val updated=ShowcaseEngine.resume(old)
        assertEquals(ShowcaseStage.READY,updated.stage)
        assertTrue(updated.demoApproved)
    }

    @Test fun backReturnsToVehicleSelectionBeforePayment() {
        val approved=ShowcaseEngine.submitDocuments(
            ShowcaseEngine.verifyPhone(ShowcaseSession(),"0000"),true,true)
        val contract=ShowcaseEngine.reserve(approved,"g01",4,"Сегодня","12:00")
        val backed=ShowcaseEngine.backStep(contract)
        assertEquals(ShowcaseStage.READY,backed.stage)
        assertTrue(backed.demoApproved)
    }

    @Test fun backCannotReverseActiveDriveOrRefund() {
        val finished=firstRentalFinished()
        val unchanged=ShowcaseEngine.backStep(finished)
        assertEquals(ShowcaseStage.FINISHED,unchanged.stage)
        assertEquals(finished.ridesCompleted,unchanged.ridesCompleted)
    }

    @Test fun newRentalKeepsApprovalWhenDemoResetIsChosen() {
        val completed=firstRentalFinished()
        val again=ShowcaseEngine.resetRetainingVerification(completed)
        assertEquals(ShowcaseStage.READY,again.stage)
        assertTrue(again.demoApproved)
    }
}
