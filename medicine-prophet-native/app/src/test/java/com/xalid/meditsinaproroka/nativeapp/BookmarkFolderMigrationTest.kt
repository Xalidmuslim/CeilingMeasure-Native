package com.xalid.meditsinaproroka.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkFolderMigrationTest {
    @Test
    fun legacyDefaultFoldersMapToCurrentWebNames() {
        assertEquals("Без папки", canonicalBookmarkFolder(""))
        assertEquals("Без папки", canonicalBookmarkFolder("Общее"))
        assertEquals("Изучить", canonicalBookmarkFolder("Изучить"))
        assertEquals("Важное", canonicalBookmarkFolder("Вернуться"))
        assertEquals("Моя папка", canonicalBookmarkFolder("  Моя папка  "))
    }

    @Test
    fun defaultFoldersMatchWebOrder() {
        assertEquals(listOf("Без папки", "Изучить", "Важное"), DEFAULT_BOOKMARK_FOLDERS)
    }
}
