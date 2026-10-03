package com.xalidmuslim.arabicgrammar.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class ReaderSettings(
    val theme: String = "light",
    val textScale: Float = 1f,
    val arabicScale: Float = 1.12f,
    val lineHeight: Float = 1.55f,
    val paragraphGap: Float = 12f,
    val fontChoice: String = "sans",
    val arabicFontChoice: String = "serif",
    val animations: Boolean = true,
)

class ProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("arabic_grammar_progress_v3", Context.MODE_PRIVATE)

    var completed by mutableStateOf(prefs.getStringSet("completed", emptySet())?.toSet() ?: emptySet())
        private set
    var bookmarks by mutableStateOf(prefs.getStringSet("bookmarks", emptySet())?.toSet() ?: emptySet())
        private set
    var exerciseDone by mutableStateOf(prefs.getStringSet("exercise_done", emptySet())?.toSet() ?: emptySet())
        private set
    var lastSection by mutableStateOf(prefs.getString("last_section", "s1") ?: "s1")
        private set
    var settings by mutableStateOf(loadSettings())
        private set

    fun toggleCompleted(id: String) {
        completed = completed.toggle(id)
        prefs.edit().putStringSet("completed", completed).apply()
    }

    fun toggleBookmark(id: String) {
        bookmarks = bookmarks.toggle(id)
        prefs.edit().putStringSet("bookmarks", bookmarks).apply()
    }

    fun markExerciseDone(id: String) {
        exerciseDone = exerciseDone + id
        prefs.edit().putStringSet("exercise_done", exerciseDone).apply()
    }

    fun updateLastSection(id: String) {
        lastSection = id
        prefs.edit().putString("last_section", id).apply()
    }

    fun note(sectionId: String): String = prefs.getString("note_$sectionId", "").orEmpty()

    fun saveNote(sectionId: String, text: String) {
        prefs.edit().putString("note_$sectionId", text).apply()
    }

    fun updateSettings(next: ReaderSettings) {
        settings = next
        prefs.edit()
            .putString("theme", next.theme)
            .putFloat("text_scale", next.textScale)
            .putFloat("arabic_scale", next.arabicScale)
            .putFloat("line_height", next.lineHeight)
            .putFloat("paragraph_gap", next.paragraphGap)
            .putString("font_choice", next.fontChoice)
            .putString("arabic_font_choice", next.arabicFontChoice)
            .putBoolean("animations", next.animations)
            .apply()
    }

    private fun loadSettings() = ReaderSettings(
        theme = prefs.getString("theme", "light") ?: "light",
        textScale = prefs.getFloat("text_scale", 1f),
        arabicScale = prefs.getFloat("arabic_scale", 1.12f),
        lineHeight = prefs.getFloat("line_height", 1.55f),
        paragraphGap = prefs.getFloat("paragraph_gap", 12f),
        fontChoice = prefs.getString("font_choice", "sans") ?: "sans",
        arabicFontChoice = prefs.getString("arabic_font_choice", "serif") ?: "serif",
        animations = prefs.getBoolean("animations", true),
    )

    private fun Set<String>.toggle(id: String): Set<String> =
        if (contains(id)) this - id else this + id
}
