package ru.kbr.cargoshare

import org.junit.Assert.*
import org.junit.Test

class PresentationV7Test {
    @Test fun bookingGroupsReduceThirteenInternalStatesToFiveVisibleSteps() {
        assertEquals(5, PresentationV7.groups.size)
        assertEquals(PresentationV7.Step.ACCOUNT, PresentationV7.stepFor(ShowcaseStage.PHONE))
        assertEquals(PresentationV7.Step.ACCOUNT, PresentationV7.stepFor(ShowcaseStage.DOCUMENTS))
        assertEquals(PresentationV7.Step.VEHICLE, PresentationV7.stepFor(ShowcaseStage.READY))
        assertEquals(PresentationV7.Step.PAYMENT, PresentationV7.stepFor(ShowcaseStage.CONTRACT))
        assertEquals(PresentationV7.Step.PAYMENT, PresentationV7.stepFor(ShowcaseStage.PAYMENT))
        assertEquals(PresentationV7.Step.HANDOVER, PresentationV7.stepFor(ShowcaseStage.PICKUP_INSPECTION))
        assertEquals(PresentationV7.Step.TRIP, PresentationV7.stepFor(ShowcaseStage.DRIVING))
        assertEquals(PresentationV7.Step.TRIP, PresentationV7.stepFor(ShowcaseStage.FINISHED))
    }
    @Test fun compactQuoteIncludesRefundableDepositSeparately() {
        val v=demoFleet.first()
        val quote=PresentationV7.quote(v,4)
        assertEquals(2600, quote.rental)
        assertEquals(5000, quote.refundableDeposit)
        assertEquals(7600, quote.totalToday)
    }
    @Test fun disablingTruckRemovesItFromCarousel() {
        val fleet=demoFleet.map { if(it.id=="g01") it.copy(enabled=false) else it }
        val selected=PresentationV7.pick(fleet,"g01")
        assertEquals("g02",selected?.id)
    }
    @Test fun onlyReadyStageRendersMapFirstScreen() {
        assertTrue(PresentationV7.mapFirst(ShowcaseStage.READY))
        assertFalse(PresentationV7.mapFirst(ShowcaseStage.PAYMENT))
        assertFalse(PresentationV7.mapFirst(ShowcaseStage.DRIVING))
    }
}
