package com.xalidmuslim.arabicgrammar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.xalidmuslim.arabicgrammar.data.BookRepository
import com.xalidmuslim.arabicgrammar.data.ProgressStore

sealed interface Screen {
    data object Home : Screen
    data class Library(val chapter: String? = null) : Screen
    data class Reader(val sectionId: String) : Screen
    data object Practice : Screen
    data class Quiz(val reviewErrors: Boolean = false) : Screen
    data class Flashcards(val reviewOnly: Boolean = false) : Screen
    data class ConjugationTrainer(val reviewOnly: Boolean = false) : Screen
    data object Search : Screen
    data object Settings : Screen
    data object Bookmarks : Screen
}

private fun screenStateKey(screen: Screen): String = when (screen) {
    Screen.Home -> "home"
    is Screen.Library -> "library:" + (screen.chapter ?: "root")
    is Screen.Reader -> "reader:" + screen.sectionId
    Screen.Practice -> "practice"
    is Screen.Quiz -> "quiz:" + screen.reviewErrors
    is Screen.Flashcards -> "flashcards:" + screen.reviewOnly
    is Screen.ConjugationTrainer -> "conjugation:" + screen.reviewOnly
    Screen.Search -> "search"
    Screen.Settings -> "settings"
    Screen.Bookmarks -> "bookmarks"
}

@Composable
fun ArabicGrammarApp(repository: BookRepository, progress: ProgressStore) {
    val settings = progress.settings
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val stateHolder = rememberSaveableStateHolder()
    val current = stack.last()

    DisposableEffect(repository) { onDispose { repository.close() } }

    fun navigate(screen: Screen) {
        if (stack.lastOrNull() != screen) stack.add(screen)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun topLevel(screen: Screen) {
        if (screen == Screen.Home) {
            stack.clear()
            stack.add(Screen.Home)
            return
        }
        if (stack.firstOrNull() != Screen.Home) {
            stack.clear()
            stack.add(Screen.Home)
        }
        if (stack.lastOrNull() != screen) stack.add(screen)
    }

    BackHandler(enabled = true) {
        if (stack.size > 1) back()
    }

    ArabicGrammarTheme(settings, isSystemInDarkTheme()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { AppBottomBar(current, ::topLevel) },
        ) { padding ->
            AnimatedContent(
                targetState = current,
                modifier = Modifier.fillMaxSize().padding(padding),
                transitionSpec = {
                    if (settings.animations) {
                        (slideInHorizontally(tween(300)) { it / 12 } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(240)) { -it / 14 } + fadeOut(tween(220)))
                    } else {
                        fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                    }
                },
                label = "screen",
            ) { screen ->
                stateHolder.SaveableStateProvider(screenStateKey(screen)) {
                    when (screen) {
                        Screen.Home -> HomeScreen(repository, progress, ::navigate)
                        is Screen.Library -> LibraryScreen(repository, progress, screen.chapter, ::navigate, ::back)
                        is Screen.Reader -> ReaderScreen(repository, progress, screen.sectionId, ::navigate, ::back)
                        Screen.Practice -> PracticeScreen(repository, progress, ::navigate)
                        is Screen.Quiz -> QuizScreen(repository, progress, screen.reviewErrors, ::back)
                        is Screen.Flashcards -> FlashcardsScreen(repository, progress, screen.reviewOnly, ::back)
                        is Screen.ConjugationTrainer -> ConjugationTrainerScreen(repository, progress, screen.reviewOnly, ::back)
                        Screen.Search -> SearchScreen(repository, progress, ::navigate)
                        Screen.Settings -> SettingsScreen(progress)
                        Screen.Bookmarks -> BookmarksScreen(repository, progress, ::navigate, ::back)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(current: Screen, navigate: (Screen) -> Unit) {
    val items = listOf(
        Triple(Screen.Home as Screen, "Главная", Icons.Default.Home),
        Triple(Screen.Library() as Screen, "Книга", Icons.Default.MenuBook),
        Triple(Screen.Practice as Screen, "Практика", Icons.Default.School),
        Triple(Screen.Search as Screen, "Поиск", Icons.Default.Search),
        Triple(Screen.Settings as Screen, "Настройки", Icons.Default.Settings),
    )

    NavigationBar {
        items.forEach { (screen, label, icon) ->
            val selected = when (screen) {
                Screen.Home -> current is Screen.Home
                is Screen.Library ->
                    current is Screen.Library ||
                        current is Screen.Reader ||
                        current is Screen.Bookmarks
                Screen.Practice ->
                    current is Screen.Practice ||
                        current is Screen.Quiz ||
                        current is Screen.Flashcards ||
                        current is Screen.ConjugationTrainer
                Screen.Search -> current is Screen.Search
                Screen.Settings -> current is Screen.Settings
                else -> false
            }

            NavigationBarItem(
                selected = selected,
                onClick = { navigate(screen) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}
