package com.xalid.meditsinaproroka.nativeapp

import android.app.Activity
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MedicinaApp(book: BookData, store: AppStore) {
    var current by remember { mutableStateOf<Route>(Route.Home) }
    val backStack = remember { mutableStateListOf<Route>() }
    var searchQuery by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf(SearchFilter.ALL) }
    var bookmarkFolder by remember { mutableStateOf("Все") }
    var lastExitAttemptAt by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val activity = context as? Activity

    fun navigate(route: Route, push: Boolean = true) {
        if (push && current != route) backStack.add(current)
        current = route
    }

    fun goBack() {
        if (backStack.isNotEmpty()) current = backStack.removeAt(backStack.lastIndex)
    }

    fun root(route: Route) {
        backStack.clear()
        current = route
    }

    BackHandler(enabled = true) {
        when {
            backStack.isNotEmpty() -> goBack()
            current != Route.Home -> root(Route.Home)
            else -> {
                val now = SystemClock.elapsedRealtime()
                if (now - lastExitAttemptAt <= 1800L) {
                    activity?.finish()
                } else {
                    lastExitAttemptAt = now
                    Toast.makeText(context, "Ещё раз назад — выйти", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNav(
                current = current,
                onHome = { root(Route.Home) },
                onTopics = { root(Route.Topics) },
                onSearch = { root(Route.Search) },
                onBookmarks = { root(Route.Bookmarks) },
                onMore = { root(Route.More) },
            )
        },
    ) { insets ->
        val modifier = Modifier.padding(insets)
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(160)) +
                        scaleIn(initialScale = 0.992f, animationSpec = tween(160))
                ).togetherWith(
                    fadeOut(animationSpec = tween(100)) +
                        scaleOut(targetScale = 0.996f, animationSpec = tween(100))
                ).using(SizeTransform(clip = false))
            },
            label = "sectionTransition",
        ) { route ->
            when (route) {
                Route.Home -> WebHomeScreen(book, store, modifier, ::navigate)
                Route.Book -> BookScreen(book, modifier, ::goBack) { navigate(Route.Reader(it)) }
                Route.Topics -> TopicsScreen(book, modifier) { navigate(Route.TopicDetail(it)) }
                Route.Search -> SearchScreen(
                    book = book,
                    query = searchQuery,
                    onQuery = { searchQuery = it },
                    filter = searchFilter,
                    onFilter = { searchFilter = it },
                    modifier = modifier,
                    onOpen = { chapterId, anchor -> navigate(Route.Reader(chapterId, anchor)) },
                )
                Route.Bookmarks -> BookmarksScreen(
                    book = book,
                    store = store,
                    folder = bookmarkFolder,
                    onFolder = { bookmarkFolder = it },
                    modifier = modifier,
                    onOpen = { id, anchor -> navigate(Route.Reader(id, anchor)) },
                )
                Route.More -> WebMoreScreen(modifier, ::navigate)
                Route.Remedies -> RemediesScreen(book, modifier, ::goBack) { navigate(Route.RemedyDetail(it)) }
                Route.Treatments -> TreatmentsScreen(book, modifier, ::goBack) { navigate(Route.Reader(it)) }
                Route.Notes -> NotesScreen(book, store, modifier, ::goBack) { id, anchor -> navigate(Route.Reader(id, anchor)) }
                Route.Settings -> WebSettingsScreen(store, modifier, ::goBack)
                Route.Hadiths -> HadithsScreen(book, modifier, ::goBack) { id, anchor -> navigate(Route.Reader(id, anchor)) }
                Route.History -> HistoryScreen(book, store, modifier, ::goBack) { id, anchor -> navigate(Route.Reader(id, anchor)) }
                Route.Offline -> OfflineScreen(book, modifier, ::goBack)
                Route.About -> AboutScreen(book, modifier, ::goBack)
                Route.Collections -> CollectionsScreen(book, modifier, ::goBack) { navigate(Route.CollectionDetail(it)) }
                Route.Glossary -> GlossaryScreen(book, modifier, ::goBack) { navigate(Route.GlossaryDetail(it)) }
                Route.Source -> SourceScreen(book, modifier, ::goBack)
                is Route.GlossaryDetail -> GlossaryDetailScreen(book, route.id, modifier, ::goBack) { id, anchor ->
                    navigate(Route.Reader(id, anchor))
                }
                is Route.TopicDetail -> TopicDetailScreen(book, route.id, modifier, ::goBack) {
                    navigate(Route.Reader(it))
                }
                is Route.RemedyDetail -> RemedyDetailScreen(book, route.id, modifier, ::goBack) { id, anchor ->
                    navigate(Route.Reader(id, anchor))
                }
                is Route.CollectionDetail -> CollectionDetailScreen(book, route.id, modifier, ::goBack) {
                    navigate(Route.Reader(it))
                }
                is Route.Reader -> ReaderScreen(book, store, route, modifier, ::goBack) { next ->
                    navigate(next)
                }
            }
        }
    }
}

@Composable
private fun BottomNav(
    current: Route,
    onHome: () -> Unit,
    onTopics: () -> Unit,
    onSearch: () -> Unit,
    onBookmarks: () -> Unit,
    onMore: () -> Unit,
) {
    val items = listOf(
        Triple("Главная", "⌂", onHome),
        Triple("Темы", "▦", onTopics),
        Triple("Поиск", "⌕", onSearch),
        Triple("Закладки", "★", onBookmarks),
        Triple("Ещё", "⋯", onMore),
    )

    val selectedIndex = when (current) {
        Route.Home -> 0
        Route.Topics, is Route.TopicDetail -> 1
        Route.Search -> 2
        Route.Bookmarks -> 3
        Route.More, Route.Remedies, Route.Treatments, Route.Notes, Route.Settings,
        Route.Hadiths, Route.History, Route.Offline, Route.About, Route.Collections,
        Route.Glossary, Route.Source, is Route.RemedyDetail, is Route.CollectionDetail,
        is Route.GlossaryDetail -> 4
        else -> -1
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        items.forEachIndexed { index, (label, glyph, action) ->
            val selected = selectedIndex == index
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.08f else 1f,
                animationSpec = tween(140),
                label = "bottomNavScale",
            )
            NavigationBarItem(
                selected = selected,
                onClick = action,
                icon = {
                    Text(
                        glyph,
                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
                        fontSize = 22.sp,
                    )
                },
                label = {
                    Text(
                        label,
                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
