package com.xalidmuslim.arabicgrammar.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

data class ReaderSettings(
    val theme: String = "light",
    val textScale: Float = 1f,
    val arabicScale: Float = 1.12f,
    val lineHeight: Float = 1.55f,
    val paragraphGap: Float = 12f,
    val fontChoice: String = "sans",
    val arabicFontChoice: String = "naskh",
    val animations: Boolean = true,
)

class ProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "arabic_grammar_progress_v4",
        Context.MODE_PRIVATE,
    )

    var completed by mutableStateOf(prefs.getStringSet("completed", emptySet())?.toSet() ?: emptySet())
        private set
    var bookmarks by mutableStateOf(prefs.getStringSet("bookmarks", emptySet())?.toSet() ?: emptySet())
        private set
    var exerciseDone by mutableStateOf(prefs.getStringSet("exercise_done", emptySet())?.toSet() ?: emptySet())
        private set
    var quizWrongSections by mutableStateOf(
        prefs.getStringSet("quiz_wrong_sections", emptySet())?.toSet() ?: emptySet()
    )
        private set
    var studyDates by mutableStateOf(
        prefs.getStringSet("study_dates", emptySet())?.toSet() ?: emptySet()
    )
        private set
    var flashKnown by mutableStateOf(
        prefs.getStringSet("flash_known", emptySet())?.toSet() ?: emptySet()
    )
        private set
    var flashRepeat by mutableStateOf(
        prefs.getStringSet("flash_repeat", emptySet())?.toSet() ?: emptySet()
    )
        private set
    var conjugationWrong by mutableStateOf(
        prefs.getStringSet("conjugation_wrong", emptySet())?.toSet() ?: emptySet()
    )
        private set
    var conjugationAttempts by mutableStateOf(prefs.getInt("conjugation_attempts", 0))
        private set
    var conjugationCorrect by mutableStateOf(prefs.getInt("conjugation_correct", 0))
        private set
    var recentSections by mutableStateOf(loadRecent())
        private set

    var lastSection by mutableStateOf(prefs.getString("last_section", "s1") ?: "s1")
        private set
    var quizAttempts by mutableStateOf(prefs.getInt("quiz_attempts", 0))
        private set
    var quizCorrect by mutableStateOf(prefs.getInt("quiz_correct", 0))
        private set
    var searchQuery by mutableStateOf("")
        private set
    var settings by mutableStateOf(loadSettings())
        private set

    fun toggleCompleted(id: String) {
        completed = completed.toggle(id)
        prefs.edit().putStringSet("completed", completed).apply()
        touchStudy()
    }

    fun toggleBookmark(id: String) {
        bookmarks = bookmarks.toggle(id)
        prefs.edit().putStringSet("bookmarks", bookmarks).apply()
    }

    fun toggleExerciseDone(id: String) {
        exerciseDone = exerciseDone.toggle(id)
        prefs.edit().putStringSet("exercise_done", exerciseDone).apply()
        touchStudy()
    }

    fun markExerciseDone(id: String) {
        if (!exerciseDone.contains(id)) {
            exerciseDone = exerciseDone + id
            prefs.edit().putStringSet("exercise_done", exerciseDone).apply()
        }
        touchStudy()
    }

    fun updateLastSection(id: String) {
        lastSection = id
        recentSections = (listOf(id) + recentSections.filterNot { it == id }).take(8)
        prefs.edit()
            .putString("last_section", id)
            .putString("recent_sections", recentSections.joinToString("|"))
            .apply()
        touchStudy()
    }

    fun saveReaderPosition(sectionId: String, itemIndex: Int, scrollOffset: Int) {
        prefs.edit()
            .putInt("reader_index_$sectionId", itemIndex.coerceAtLeast(0))
            .putInt("reader_offset_$sectionId", scrollOffset.coerceAtLeast(0))
            .apply()
    }

    fun readerIndex(sectionId: String): Int = prefs.getInt("reader_index_$sectionId", 0)
    fun readerOffset(sectionId: String): Int = prefs.getInt("reader_offset_$sectionId", 0)

    fun recordQuiz(sectionId: String, correct: Boolean) {
        quizAttempts += 1
        if (correct) {
            quizCorrect += 1
            quizWrongSections = quizWrongSections - sectionId
        } else {
            quizWrongSections = quizWrongSections + sectionId
        }
        prefs.edit()
            .putInt("quiz_attempts", quizAttempts)
            .putInt("quiz_correct", quizCorrect)
            .putStringSet("quiz_wrong_sections", quizWrongSections)
            .apply()
        touchStudy()
    }

    fun quizAccuracy(): Int =
        if (quizAttempts == 0) 0 else ((quizCorrect * 100f) / quizAttempts).toInt()

    fun currentStreak(): Int {
        if (studyDates.isEmpty()) return 0
        var date = LocalDate.now()
        if (!studyDates.contains(date.toString())) date = date.minusDays(1)
        var streak = 0
        while (studyDates.contains(date.toString())) {
            streak += 1
            date = date.minusDays(1)
        }
        return streak
    }

    fun rateFlashCard(cardId: String, known: Boolean) {
        if (known) {
            flashKnown = flashKnown + cardId
            flashRepeat = flashRepeat - cardId
        } else {
            flashKnown = flashKnown - cardId
            flashRepeat = flashRepeat + cardId
        }
        prefs.edit()
            .putStringSet("flash_known", flashKnown)
            .putStringSet("flash_repeat", flashRepeat)
            .apply()
        touchStudy()
    }

    fun recordConjugation(questionId: String, correct: Boolean) {
        conjugationAttempts += 1
        if (correct) {
            conjugationCorrect += 1
            conjugationWrong = conjugationWrong - questionId
        } else {
            conjugationWrong = conjugationWrong + questionId
        }
        prefs.edit()
            .putInt("conjugation_attempts", conjugationAttempts)
            .putInt("conjugation_correct", conjugationCorrect)
            .putStringSet("conjugation_wrong", conjugationWrong)
            .apply()
        touchStudy()
    }

    fun conjugationAccuracy(): Int =
        if (conjugationAttempts == 0) 0
        else ((conjugationCorrect * 100f) / conjugationAttempts).toInt()

    fun updateSearchQuery(value: String) {
        searchQuery = value
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

    private fun touchStudy() {
        val today = LocalDate.now().toString()
        if (!studyDates.contains(today)) {
            studyDates = studyDates + today
            prefs.edit().putStringSet("study_dates", studyDates).apply()
        }
    }

    private fun loadRecent(): List<String> =
        prefs.getString("recent_sections", "")
            .orEmpty()
            .split("|")
            .filter { it.isNotBlank() }
            .take(8)

    private fun loadSettings() = ReaderSettings(
        theme = prefs.getString("theme", "light") ?: "light",
        textScale = prefs.getFloat("text_scale", 1f),
        arabicScale = prefs.getFloat("arabic_scale", 1.12f),
        lineHeight = prefs.getFloat("line_height", 1.55f),
        paragraphGap = prefs.getFloat("paragraph_gap", 12f),
        fontChoice = prefs.getString("font_choice", "sans") ?: "sans",
        arabicFontChoice = prefs.getString("arabic_font_choice", "naskh") ?: "naskh",
        animations = prefs.getBoolean("animations", true),
    )

    private fun Set<String>.toggle(id: String): Set<String> =
        if (contains(id)) this - id else this + id
}
