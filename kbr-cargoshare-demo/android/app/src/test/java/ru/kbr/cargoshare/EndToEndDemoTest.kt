package ru.kbr.cargoshare
import org.junit.Assert.*
import org.junit.Test
class EndToEndDemoTest {
 @Test fun startsWithRegistration() { assertEquals(ShowcaseStage.PHONE, ShowcaseSession().stage) }
}