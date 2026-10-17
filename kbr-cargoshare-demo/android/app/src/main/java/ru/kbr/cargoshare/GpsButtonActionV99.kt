package ru.kbr.cargoshare

/** Dedicated action dispatcher to prevent silent non-invocation of GPS callbacks. */
object GpsButtonActionV99 {
    fun dispatch(
        currentlyEnabled:Boolean,
        enable:()->Unit,
        disable:()->Unit
    ) {
        if(currentlyEnabled) disable() else enable()
    }
}
