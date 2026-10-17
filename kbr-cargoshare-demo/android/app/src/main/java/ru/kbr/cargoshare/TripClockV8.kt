package ru.kbr.cargoshare

import java.util.Locale

/** Demo seconds are display-only; business rental accounting is server-owned in production. */
object TripClockV8 {
    fun remainingSeconds(s: ShowcaseSession, elapsedDemoSeconds: Int): Int {
        val allowed = (s.hours.toLong() + s.extraHours.toLong()) * 3600L
        val used = s.elapsedMinutes.toLong() * 60L + elapsedDemoSeconds.coerceAtLeast(0)
        return (allowed - used).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }
    fun display(s: ShowcaseSession, elapsedDemoSeconds: Int): String {
        val secs = remainingSeconds(s,elapsedDemoSeconds)
        return String.format(Locale.US,"%02d:%02d:%02d",
            secs/3600,(secs%3600)/60,secs%60)
    }
}
