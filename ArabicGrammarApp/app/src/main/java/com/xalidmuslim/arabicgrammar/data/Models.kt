package com.xalidmuslim.arabicgrammar.data

data class BookSection(
    val id: String,
    val chapter: String,
    val number: Int,
    val title: String,
    val found: Boolean,
    val exerciseCount: Int,
)

data class BookBlock(
    val index: Int,
    val type: String,
    val text: String,
)

data class SearchResult(
    val section: BookSection,
    val snippet: String,
)

data class BookMeta(
    val title: String,
    val author: String,
    val sourcePages: Int,
    val blocks: Int,
    val arabicChars: Int,
    val coreSectionsFound: Int,
    val badEncodingBlocks: Int,
)

data class ReaderGroup(
    val kind: ReaderKind,
    val blocks: List<BookBlock>,
    val exercise: Boolean = false,
)

enum class ReaderKind {
    PARAGRAPH,
    ARABIC,
    EXERCISE_TITLE,
}
