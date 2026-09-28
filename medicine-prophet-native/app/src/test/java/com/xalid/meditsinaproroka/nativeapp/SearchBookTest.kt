package com.xalid.meditsinaproroka.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchBookTest {
    @Test
    fun remedySearchUsesMatchingAnchorForEachChapter() {
        val first = Chapter(
            id = "ch-1",
            slug = "first",
            title = "Первая",
            order = 1,
            section = "Раздел",
            blocks = listOf(BookBlock("b-1", "a-1", "text", "Текст первой главы")),
            topics = emptyList(),
            remedies = listOf("r-1"),
            previousId = null,
            nextId = "ch-2",
        )
        val second = Chapter(
            id = "ch-2",
            slug = "second",
            title = "Вторая",
            order = 2,
            section = "Раздел",
            blocks = listOf(BookBlock("b-2", "a-2", "text", "Текст второй главы")),
            topics = emptyList(),
            remedies = listOf("r-1"),
            previousId = "ch-1",
            nextId = null,
        )
        val remedy = Remedy(
            id = "r-1",
            title = "Аджва",
            aliases = emptyList(),
            chapterIds = listOf(first.id, second.id),
            anchors = listOf("a-1", "a-2"),
        )
        val book = BookData(
            title = "Тест",
            author = "Автор",
            chapters = listOf(first, second),
            topics = emptyList(),
            remedies = listOf(remedy),
            collections = emptyList(),
        )

        val hits = searchBook(book, "адж", SearchFilter.REMEDIES)

        assertEquals(listOf(first.id, second.id), hits.map { it.chapterId })
        assertEquals(listOf("a-1", "a-2"), hits.map { it.anchor })
    }
}
