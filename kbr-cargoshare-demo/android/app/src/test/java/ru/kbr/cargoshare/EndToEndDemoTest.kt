package ru.kbr.cargoshare
import org.junit.Assert.*
import org.junit.Test
class EndToEndDemoTest {
    @Test fun fullJourneyWithoutDeposit() {
        var s = ShowcaseSession()
        assertEquals(ShowcaseStage.PHONE, s.stage)
        s = ShowcaseEngine.verifyPhone(s, "0000")
        s = ShowcaseEngine.submitDocuments(s, true, true)
        s = ShowcaseEngine.approve(s)
        s = ShowcaseEngine.reserve(s, "g01", 4, "Сегодня", "12:00")
        s = ShowcaseEngine.signContract(s, true)
        s = ShowcaseEngine.simulatePayment(s, true)
        assertFalse(s.depositHeld)
        s = ShowcaseEngine.handover(s, true)
        s = ShowcaseEngine.requestReturn(s)
        s = ShowcaseEngine.completeReturn(s, true)
        assertEquals(ShowcaseStage.FINISHED, s.stage)
        assertFalse(s.depositReturned)
        assertFalse(s.depositHeld)
    }
    @Test(expected=IllegalArgumentException::class)
    fun bookingRequiresDocumentApproval() {
        ShowcaseEngine.reserve(ShowcaseSession(), "g01", 4, "Сегодня", "12:00")
    }
    @Test fun unsuccessfulPaymentDoesNotAdvance() {
        var s = ShowcaseEngine.verifyPhone(ShowcaseSession(), "0000")
        s = ShowcaseEngine.submitDocuments(s, true, true)
        s = ShowcaseEngine.approve(s)
        s = ShowcaseEngine.reserve(s, "g01", 4, "Сегодня", "12:00")
        s = ShowcaseEngine.signContract(s, true)
        s = ShowcaseEngine.simulatePayment(s, false)
        assertEquals(ShowcaseStage.PAYMENT, s.stage)
        assertFalse(s.rentPaid)
    }
    @Test(expected=IllegalArgumentException::class)
    fun handoverRequiresInspection() {
        var s = ShowcaseEngine.verifyPhone(ShowcaseSession(), "0000")
        s = ShowcaseEngine.submitDocuments(s, true, true)
        s = ShowcaseEngine.approve(s)
        s = ShowcaseEngine.reserve(s, "g01", 4, "Сегодня", "12:00")
        s = ShowcaseEngine.signContract(s, true)
        s = ShowcaseEngine.simulatePayment(s, true)
        ShowcaseEngine.handover(s, false)
    }
}