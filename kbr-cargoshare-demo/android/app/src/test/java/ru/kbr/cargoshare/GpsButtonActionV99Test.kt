package ru.kbr.cargoshare

import org.junit.Assert.*
import org.junit.Test

class GpsButtonActionV99Test {
    @Test fun enableButtonExecutesEnableCallback() {
        var enabled=0
        var disabled=0
        GpsButtonActionV99.dispatch(false,{enabled++},{disabled++})
        assertEquals(1,enabled)
        assertEquals(0,disabled)
    }

    @Test fun disableButtonExecutesDisableCallback() {
        var enabled=0
        var disabled=0
        GpsButtonActionV99.dispatch(true,{enabled++},{disabled++})
        assertEquals(0,enabled)
        assertEquals(1,disabled)
    }
}
