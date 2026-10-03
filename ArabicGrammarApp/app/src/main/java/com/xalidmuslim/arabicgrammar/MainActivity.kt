package com.xalidmuslim.arabicgrammar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.xalidmuslim.arabicgrammar.data.BookRepository
import com.xalidmuslim.arabicgrammar.data.ProgressStore
import com.xalidmuslim.arabicgrammar.ui.ArabicGrammarApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = BookRepository(this)
        val progress = ProgressStore(this)

        setContent {
            ArabicGrammarApp(
                repository = repository,
                progress = progress,
            )
        }
    }
}
