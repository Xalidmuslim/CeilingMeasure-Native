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
    var exercise = false

    fun flushParagraphs() {
        if (paragraphBuffer.isEmpty()) return
        paragraphBuffer.chunked(3).forEach { chunk ->
            result += ReaderGroup(ReaderKind.PARAGRAPH, chunk.toList(), exercise)
        }
        paragraphBuffer.clear()
    }

    for (block in blocks) {
        when (block.type) {
            "exercise-heading" -> {
                flushParagraphs()
                exercise = true
                result += ReaderGroup(ReaderKind.EXERCISE_TITLE, listOf(block), true)
            }
            "arabic" -> {
                flushParagraphs()
                result += ReaderGroup(ReaderKind.ARABIC, listOf(block), exercise)
            }
            else -> {
                paragraphBuffer += block
                if (paragraphBuffer.size >= 3) flushParagraphs()
            }
        }
    }
    flushParagraphs()
    return result
}
