package com.xalid.meditsinaproroka.nativeapp

import org.json.JSONArray
import org.json.JSONObject

data class BookData(
    val title: String,
    val author: String,
    val chapters: List<Chapter>,
    val topics: List<Topic>,
    val remedies: List<Remedy>,
    val collections: List<BookCollection>,
)

data class Chapter(
    val id: String,
    val slug: String,
    val title: String,
    val order: Int,
    val section: String,
    val blocks: List<BookBlock>,
    val topics: List<String>,
    val remedies: List<String>,
    val previousId: String?,
    val nextId: String?,
)

data class BookBlock(
    val id: String,
    val anchor: String,
    val type: String,
    val text: String,
    val level: Int = 0,
    val hadithSources: List<String> = emptyList(),
)

data class Topic(val id: String, val title: String, val chapterIds: List<String>)
data class Remedy(
    val id: String,
    val title: String,
    val aliases: List<String>,
    val chapterIds: List<String>,
    val anchors: List<String>,
)
data class BookCollection(val id: String, val title: String, val chapterIds: List<String>)

data class SearchHit(
    val chapterId: String,
    val anchor: String?,
    val chapterTitle: String,
    val snippet: String,
    val type: String,
)

fun parseBook(json: String): BookData {
    val root = JSONObject(json)
    return BookData(
        title = root.optString("title", "Медицина Пророка ﷺ"),
        author = root.optString("author", "Ибн Каййим аль-Джаузия"),
        chapters = root.optJSONArray("chapters").toObjects { chapterFromJson(it) },
        topics = root.optJSONArray("topics").toObjects { topicFromJson(it) },
        remedies = root.optJSONArray("remedies").toObjects { remedyFromJson(it) },
        collections = root.optJSONArray("collections").toObjects { collectionFromJson(it) },
    )
}

private fun chapterFromJson(o: JSONObject) = Chapter(
    id = o.optString("id"),
    slug = o.optString("slug"),
    title = o.optString("title"),
    order = o.optInt("order"),
    section = o.optString("section"),
    blocks = o.optJSONArray("blocks").toObjects { blockFromJson(it) },
    topics = o.optJSONArray("topics").toStrings(),
    remedies = o.optJSONArray("remedies").toStrings(),
    previousId = o.optString("previousId").takeIf { it.isNotBlank() && it != "null" },
    nextId = o.optString("nextId").takeIf { it.isNotBlank() && it != "null" },
)

private fun blockFromJson(o: JSONObject): BookBlock {
    val sources = o.optJSONObject("hadith")?.optJSONArray("sources").toStrings()
    return BookBlock(
        id = o.optString("id"),
        anchor = o.optString("anchor"),
        type = o.optString("type", "text"),
        text = o.optString("text"),
        level = o.optInt("level"),
        hadithSources = sources,
    )
}

private fun topicFromJson(o: JSONObject) = Topic(
    id = o.optString("id"),
    title = o.optString("title"),
    chapterIds = o.optJSONArray("chapterIds").toStrings(),
)

private fun remedyFromJson(o: JSONObject) = Remedy(
    id = o.optString("id"),
    title = o.optString("title"),
    aliases = o.optJSONArray("aliases").toStrings(),
    chapterIds = o.optJSONArray("chapterIds").toStrings(),
    anchors = o.optJSONArray("anchors").toStrings(),
)

private fun collectionFromJson(o: JSONObject) = BookCollection(
    id = o.optString("id"),
    title = o.optString("title"),
    chapterIds = o.optJSONArray("chapterIds").toStrings(),
)

private inline fun <T> JSONArray?.toObjects(block: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) optJSONObject(i)?.let { add(block(it)) }
    }
}

private fun JSONArray?.toStrings(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) {
            val value = optString(i)
            if (value.isNotBlank()) add(value)
        }
    }
}
