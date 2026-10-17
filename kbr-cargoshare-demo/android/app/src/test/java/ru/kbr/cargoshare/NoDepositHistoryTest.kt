package ru.kbr.cargoshare

import org.junit.Assert.*
import org.junit.Test

class NoDepositHistoryTest {
    private fun paid(): ShowcaseSession {
        var s=ShowcaseEngine.verifyPhone(ShowcaseSession(),"0000")
        s=ShowcaseEngine.submitDocuments(s,true,true)
        s=ShowcaseEngine.reserve(s,"g01",4,"Сегодня","12:00",
            title="ГАЗель NEXT",pickup="Нальчик, проспект Ленина, 27",fare=2600)
        s=ShowcaseEngine.signContract(s,true)
        return ShowcaseEngine.simulatePayment(s,true)
    }

    @Test fun paymentRequiresOnlyRentalPriceNoDeposit() {
        val s=paid()
        assertTrue(s.rentPaid)
        assertFalse(s.depositHeld)
        assertFalse(s.depositReturned)
        assertEquals(0,RentalRules.DEMO_DEPOSIT)
        val quote=PresentationV7.quote(demoFleet[0],4)
        assertEquals(2600,quote.rental)
        assertEquals(2600,quote.totalToday)
    }

    @Test fun pickupAndFinishWorkWithoutDeposit() {
        var s=ShowcaseEngine.arriveAtVehicle(paid())
        s=ShowcaseEngine.confirmPickupInspection(s,true,true,true,true)
        s=ShowcaseEngine.unlockForPickup(s)
        s=ShowcaseEngine.startDriving(s)
        s=ShowcaseEngine.advanceTrip(s,30,10)
        s=ShowcaseEngine.requestReturn(s)
        s=ShowcaseEngine.completeReturn(s,true)
        assertEquals(ShowcaseStage.FINISHED,s.stage)
        assertFalse(s.depositReturned)
        assertEquals(1,s.ridesCompleted)
    }

    @Test fun finishedTripsAreArchivedOnceNotOnRepeatSave() {
        var s=paid()
        s=ShowcaseEngine.handover(s,true)
        s=ShowcaseEngine.requestReturn(s)
        val finished=ShowcaseEngine.completeReturn(s,true)
        val at=1781724000000L
        val history=TripHistoryLedger.append(emptyList(),s,finished,at)
        assertEquals(1,history.size)
        assertEquals(2600,history[0].totalPaid)
        assertEquals("ГАЗель NEXT",history[0].vehicleTitle)
        val duplicate=TripHistoryLedger.append(history,finished,finished,at)
        assertEquals(1,duplicate.size)
    }

    @Test fun secondCompletedRentalAppendsSecondArchivedEntry() {
        var a=paid()
        a=ShowcaseEngine.handover(a,true)
        a=ShowcaseEngine.requestReturn(a)
        val first=ShowcaseEngine.completeReturn(a,true)
        val archive=TripHistoryLedger.append(emptyList(),a,first,111L)
        var next=ShowcaseEngine.beginNextRental(first)
        next=ShowcaseEngine.reserve(next,"g02",2,"Завтра","15:00",
            title="ГАЗель Бизнес",fare=1180)
        next=ShowcaseEngine.signContract(next,true)
        next=ShowcaseEngine.simulatePayment(next,true)
        next=ShowcaseEngine.handover(next,true)
        next=ShowcaseEngine.requestReturn(next)
        val second=ShowcaseEngine.completeReturn(next,true)
        val two=TripHistoryLedger.append(archive,next,second,222L)
        assertEquals(2,two.size)
        assertEquals("ГАЗель Бизнес",two.first().vehicleTitle)
        assertEquals(2,two.first().rideNumber)
        assertEquals(1,two.last().rideNumber)
        assertEquals(3780,two.sumOf { it.totalPaid })
    }

    @Test fun incompleteTripsNeverEnterHistory() {
        val paid=paid()
        val result=TripHistoryLedger.append(emptyList(),paid,paid,333L)
        assertTrue(result.isEmpty())
    }

    @Test fun oldDepositFlagsClearedWhenResumingInstalledSession() {
        val old=ShowcaseSession(stage=ShowcaseStage.DRIVING,rentPaid=true,
            depositHeld=true,depositReturned=true)
        val updated=ShowcaseEngine.resume(old)
        assertFalse(updated.depositHeld)
        assertFalse(updated.depositReturned)
    }
}