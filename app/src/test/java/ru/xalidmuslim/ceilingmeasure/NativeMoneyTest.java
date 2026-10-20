package ru.xalidmuslim.ceilingmeasure;

import org.junit.Test;
import static org.junit.Assert.*;

public final class NativeMoneyTest {
    @Test public void makesLargeEstimatesReadable() {
        assertEquals("20 029,03",NativeMoney.format(20029.03));
        assertEquals("1 000 000",NativeMoney.format(1000000));
        assertEquals("3 005,32",NativeMoney.format(3005.32));
    }

    @Test public void removesUnnecessaryFractionsWithoutLosingUsefulCents() {
        assertEquals("0",NativeMoney.format(0));
        assertEquals("500",NativeMoney.format(500));
        assertEquals("100,5",NativeMoney.format(100.5));
        assertEquals("1 000,01",NativeMoney.format(1000.01));
    }
}
