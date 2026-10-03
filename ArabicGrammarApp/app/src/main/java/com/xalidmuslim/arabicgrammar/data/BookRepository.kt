package com.xalidmuslim.arabicgrammar.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File

class BookRepository(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val database: SQLiteDatabase

    init {
        val dbFile = File(appContext.filesDir, "arabic_grammar_book_v2.db")
        if (!dbFile.exists() || dbFile.length() < 300_000L) {
            appContext.assets.open("book/book.db").use { input ->
                dbFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        database = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
    }

    fun meta(): BookMeta {
        val values = mutableMapOf<String, String>()
        database.rawQuery("SELECT key,value FROM meta", null).use { cursor ->
            while (cursor.moveToNext()) values[cursor.getString(0)] = cursor.getString(1)
        }
        return BookMeta(
            title = values["title"].orEmpty(),
            author = values["author"].orEmpty(),
            sourcePages = values["sourcePages"]?.toIntOrNull() ?: 0,
            blocks = values["blocks"]?.toIntOrNull() ?: 0,
            arabicChars = values["arabicChars"]?.toIntOrNull() ?: 0,
            coreSectionsFound = values["coreSectionsFound"]?.toIntOrNull() ?: 0,
            badEncodingBlocks = values["badEncodingBlocks"]?.toIntOrNull() ?: 0,
        )
    }

    fun sections(chapter: String? = null): List<BookSection> {
        val sql = if (chapter == null) {
            "SELECT id,chapter,number,title,found,exercise_count FROM sections ORDER BY CASE chapter WHEN 'syntax' THEN 1 WHEN 'morphology' THEN 2 ELSE 3 END, number"
        } else {
            "SELECT id,chapter,number,title,found,exercise_count FROM sections WHERE chapter=? ORDER BY number"
        }
        val args = chapter?.let { arrayOf(it) }
        return querySections(sql, args)
    }

    fun exerciseSections(): List<BookSection> =
        querySections(
            "SELECT id,chapter,number,title,found,exercise_count FROM sections WHERE exercise_count>0 AND found=1 ORDER BY CASE chapter WHEN 'syntax' THEN 1 WHEN 'morphology' THEN 2 ELSE 3 END, number",
            null,
        )

    fun section(id: String): BookSection? {
        database.rawQuery(
            "SELECT id,chapter,number,title,found,exercise_count FROM sections WHERE id=? LIMIT 1",
            arrayOf(id),
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toSection() else null
        }
    }

    fun adjacent(sectionId: String): Pair<BookSection?, BookSection?> {
        val current = section(sectionId) ?: return null to null
        val list = sections(current.chapter).filter { it.found }
        val index = list.indexOfFirst { it.id == sectionId }
        if (index < 0) return null to null
        return list.getOrNull(index - 1) to list.getOrNull(index + 1)
    }

    fun blocks(sectionId: String): List<BookBlock> {
        val out = mutableListOf<BookBlock>()
        database.rawQuery(
            "SELECT block_index,type,text FROM blocks WHERE section_id=? AND type!='section-heading' ORDER BY block_index",
            arrayOf(sectionId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                out += BookBlock(
                    index = cursor.getInt(0),
                    type = cursor.getString(1),
                    text = cursor.getString(2),
                )
            }
        }
        return out
    }

    fun search(rawQuery: String, limit: Int = 40): List<SearchResult> {
        val query = rawQuery.trim()
        if (query.length < 2) return emptyList()
        val like = "%$query%"
        val sql = """
            SELECT s.id,s.chapter,s.number,s.title,s.found,s.exercise_count,
                   MIN(b.text) AS snippet
            FROM sections s
            JOIN blocks b ON b.section_id=s.id
            WHERE s.found=1 AND (s.title LIKE ? OR b.text LIKE ?)
            GROUP BY s.id,s.chapter,s.number,s.title,s.found,s.exercise_count
            ORDER BY CASE s.chapter WHEN 'syntax' THEN 1 WHEN 'morphology' THEN 2 ELSE 3 END, s.number
            LIMIT ?
        """.trimIndent()
        val out = mutableListOf<SearchResult>()
        database.rawQuery(sql, arrayOf(like, like, limit.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val section = cursor.toSection(0)
                val snippet = cursor.getString(6).orEmpty()
                    .replace(Regex("\\s+"), " ")
                    .take(260)
                out += SearchResult(section, snippet)
            }
        }
        return out
    }

    fun quizQuestions(
        preferredSectionIds: Set<String> = emptySet(),
        limit: Int = 10,
    ): List<QuizQuestion> {
        val allCore = sections().filter {
            it.found && (it.chapter == "syntax" || it.chapter == "morphology")
        }
        val focus = if (preferredSectionIds.size >= 4) {
            allCore.filter { preferredSectionIds.contains(it.id) }
        } else {
            allCore
        }

        val questions = mutableListOf<QuizQuestion>()
        for (section in focus.shuffled()) {
            if (questions.size >= limit) break
            val candidate = blocks(section.id).firstOrNull { block ->
                block.type in setOf("text", "mixed", "arabic") &&
                    block.text.length in 70..380 &&
                    block.text.count { it == '\n' } <= 2 &&
                    !block.text.contains("упражнения", ignoreCase = true)
            } ?: continue

            val distractors = allCore.filter {
                it.chapter == section.chapter && it.id != section.id
            }.shuffled().take(3)
            if (distractors.size < 3) continue

            val optionTitles = (distractors.map { it.title } + section.title).shuffled()
            questions += QuizQuestion(
                id = section.id + ":" + candidate.index,
                sectionId = section.id,
                sectionTitle = section.title,
                chapter = section.chapter,
                prompt = candidate.text.trim(),
                options = optionTitles,
                correctIndex = optionTitles.indexOf(section.title),
                arabicHeavy = candidate.type == "arabic",
            )
        }
        return questions
    }

    fun flashCards(
        preferredSectionIds: Set<String> = emptySet(),
        limit: Int = 40,
    ): List<FlashCard> {
        val allCore = sections().filter {
            it.found && (it.chapter == "syntax" || it.chapter == "morphology")
        }
        val focus = if (preferredSectionIds.size >= 3) {
            allCore.filter { preferredSectionIds.contains(it.id) }
        } else {
            allCore
        }

        val cards = mutableListOf<FlashCard>()
        for (section in focus) {
            val sectionBlocks = blocks(section.id)
            val rule = sectionBlocks.firstOrNull { block ->
                block.type in setOf("text", "mixed") &&
                    block.text.length in 55..520 &&
                    !block.text.contains("упражнения", ignoreCase = true)
            }
            if (rule != null) {
                cards += FlashCard(
                    id = section.id + ":rule:" + rule.index,
                    sectionId = section.id,
                    sectionTitle = section.title,
                    chapter = section.chapter,
                    front = "Вспомни основное правило темы «" + section.title + "».",
                    back = rule.text.trim(),
                    arabicFront = false,
                )
            }

            val arabic = sectionBlocks.firstOrNull { block ->
                block.type == "arabic" && block.text.length in 2..220
            }
            if (arabic != null) {
                cards += FlashCard(
                    id = section.id + ":arabic:" + arabic.index,
                    sectionId = section.id,
                    sectionTitle = section.title,
                    chapter = section.chapter,
                    front = arabic.text.trim(),
                    back = "Этот пример относится к теме: " + section.title,
                    arabicFront = true,
                )
            }
        }
        return cards.shuffled().take(limit)
    }

    fun conjugationQuestions(limit: Int = 60): List<ConjugationQuestion> {
        val out = mutableListOf<ConjugationQuestion>()
        val morphology = sections("morphology").filter { it.found }

        for (section in morphology) {
            val groups = buildReaderGroups(blocks(section.id))
            for (group in groups) {
                if (group.kind != ReaderKind.TABLE) continue

                fun lines(text: String): List<String> =
                    text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

                val labels = group.blocks.firstOrNull()?.let { lines(it.text) }.orEmpty()
                val valueColumns = group.blocks.drop(1).take(3).map { block ->
                    lines(block.text).filterNot { it.lowercase().contains("число") }
                }
                val valid = labels.size in 4..6 &&
                    labels.any { it.lowercase().contains("лицо") } &&
                    valueColumns.size == 3 &&
                    valueColumns.all { it.size >= labels.size }
                if (!valid) continue

                val numberLabels = listOf(
                    "единственное число",
                    "двойственное число",
                    "множественное число",
                )

                labels.forEachIndexed { row, rawLabel ->
                    val label = rawLabel
                        .replace("жен.рода", "жен. рода")
                        .replace("муж.рода", "муж. рода")
                    valueColumns.forEachIndexed { column, values ->
                        val answer = values.getOrNull(row).orEmpty().trim()
                        if (answer.isBlank()) return@forEachIndexed
                        out += ConjugationQuestion(
                            id = section.id + ":table:" +
                                group.blocks.first().index + ":" + row + ":" + column,
                            sectionId = section.id,
                            sectionTitle = section.title,
                            personLabel = label,
                            numberLabel = numberLabels[column],
                            answer = answer,
                        )
                    }
                }
            }
        }
        return out.shuffled().take(limit)
    }

    private fun querySections(sql: String, args: Array<String>?): List<BookSection> {
        val out = mutableListOf<BookSection>()
        database.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) out += cursor.toSection()
        }
        return out
    }

    private fun Cursor.toSection(offset: Int = 0): BookSection = BookSection(
        id = getString(offset),
        chapter = getString(offset + 1),
        number = getInt(offset + 2),
        title = getString(offset + 3),
        found = getInt(offset + 4) == 1,
        exerciseCount = getInt(offset + 5),
    )

    override fun close() {
        database.close()
    }
}

fun buildReaderGroups(blocks: List<BookBlock>): List<ReaderGroup> {
    val result = mutableListOf<ReaderGroup>()
    val paragraphBuffer = mutableListOf<BookBlock>()
    val tableBuffer = mutableListOf<BookBlock>()
    var exercise = false

    fun flushParagraphs() {
        if (paragraphBuffer.isEmpty()) return
        paragraphBuffer.chunked(2).forEach { chunk ->
            result += ReaderGroup(ReaderKind.PARAGRAPH, chunk.toList(), exercise)
        }
        paragraphBuffer.clear()
    }

    fun flushTable() {
        if (tableBuffer.isEmpty()) return
        result += ReaderGroup(ReaderKind.TABLE, tableBuffer.toList(), exercise)
        tableBuffer.clear()
    }

    for (block in blocks) {
        when (block.type) {
            "exercise-heading" -> {
                flushParagraphs()
                flushTable()
                exercise = true
                result += ReaderGroup(ReaderKind.EXERCISE_TITLE, listOf(block), true)
            }
            "table" -> {
                flushParagraphs()
                tableBuffer += block
            }
            "answer-line" -> {
                flushParagraphs()
                flushTable()
                result += ReaderGroup(ReaderKind.ANSWER_LINE, listOf(block), exercise)
            }
            "arabic" -> {
                flushParagraphs()
                flushTable()
                result += ReaderGroup(ReaderKind.ARABIC, listOf(block), exercise)
            }
            else -> {
                flushTable()
                paragraphBuffer += block
                if (paragraphBuffer.size >= 2) flushParagraphs()
            }
        }
    }
    flushParagraphs()
    flushTable()
    return result
}
