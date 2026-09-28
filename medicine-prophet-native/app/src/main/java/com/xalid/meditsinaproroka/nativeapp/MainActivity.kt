package com.xalid.meditsinaproroka.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val book = runCatching {
            assets.open("book.json").bufferedReader(Charsets.UTF_8).use { parseBook(it.readText()) }
        }.getOrElse {
            BookData(
                title = "Медицина Пророка ﷺ",
                author = "Ибн Каййим аль-Джаузия",
                chapters = emptyList(),
                topics = emptyList(),
                remedies = emptyList(),
                collections = emptyList(),
            )
        }
        val store = AppStore(this)

        setContent {
            MedicinaTheme(store.settings.theme) {
                MedicinaApp(book = book, store = store)
            }
        }
    }
}
