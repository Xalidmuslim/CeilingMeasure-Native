package com.xalid.meditsinaproroka.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier

@Composable
fun MedicinaApp(book: BookData, store: AppStore) {
    var current by remember { mutableStateOf<Route>(Route.Home) }
    val backStack = remember { mutableStateListOf<Route>() }
    var searchQuery by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf(SearchFilter.ALL) }
    var bookmarkFolder by remember { mutableStateOf("Все") }

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

    BackHandler(enabled = backStack.isNotEmpty()) { goBack() }

    Scaffold(
        bottomBar = {
            BottomNav(
                current = current,
                onHome = { root(Route.Home) },
                onTopics = { root(Route.Topics) },
                onSearch = { root(Route.Search) },
                onBookmarks = { root(Route.Bookmarks) },
                onMore = { root(Route.More) },
            )
        }
    ) { insets ->
        val modifier = Modifier.padding(insets)
        when (val route = current) {
            Route.Home -> HomeScreen(book, store, modifier, ::navigate) {
                searchQuery = it
                navigate(Route.Search)
            }
            Route.Book -> BookScreen(book, modifier, ::goBack) { navigate(Route.Reader(it)) }
            Route.Topics -> TopicsScreen(book, modifier) { navigate(Route.TopicDetail(it)) }
            Route.Search -> SearchScreen(
                book = book,
                query = searchQuery,
                onQuery = { searchQuery = it },
                filter = searchFilter,
                onFilter = { searchFilter = it },
                modifier = modifier,
                onOpen = { chapterId, anchor -> navigate(Route.Reader(chapterId, anchor)) }
            )
            Route.Bookmarks -> BookmarksScreen(
                book, store, bookmarkFolder, { bookmarkFolder = it }, modifier,
                onOpen = { id, anchor -> navigate(Route.Reader(id, anchor)) },
            )
            Route.More -> MoreScreen(modifier, ::navigate)
            Route.Remedies -> RemediesScreen(book, modifier, ::goBack) { navigate(Route.RemedyDetail(it)) }
            Route.Notes -> NotesScreen(book, store, modifier, ::goBack) { id, anchor -> navigate(Route.Reader(id, anchor)) }
            Route.Settings -> SettingsScreen(store, modifier, ::goBack)
            Route.Hadiths -> HadithsScreen(book, modifier, ::goBack) { id, anchor -> navigate(Route.Reader(id, anchor)) }
            Route.History -> HistoryScreen(book, store, modifier, ::goBack) { id, anchor -> navigate(Route.Reader(id, anchor)) }
            Route.Offline -> OfflineScreen(book, modifier, ::goBack)
            Route.About -> AboutScreen(book, modifier, ::goBack)
            Route.Collections -> CollectionsScreen(book, modifier, ::goBack) { navigate(Route.CollectionDetail(it)) }
            Route.Glossary -> GlossaryScreen(book, modifier, ::goBack) { navigate(Route.RemedyDetail(it)) }
            is Route.TopicDetail -> TopicDetailScreen(book, route.id, modifier, ::goBack) { navigate(Route.Reader(it)) }
            is Route.RemedyDetail -> RemedyDetailScreen(book, route.id, modifier, ::goBack) { id, anchor -> navigate(Route.Reader(id, anchor)) }
            is Route.CollectionDetail -> CollectionDetailScreen(book, route.id, modifier, ::goBack) { navigate(Route.Reader(it)) }
            is Route.Reader -> ReaderScreen(book, store, route, modifier, ::goBack) { next -> navigate(next) }
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
        Triple("Главная", Icons.Default.Home, onHome),
        Triple("Темы", Icons.Default.Book, onTopics),
        Triple("Поиск", Icons.Default.Search, onSearch),
        Triple("Закладки", Icons.Default.Bookmark, onBookmarks),
        Triple("Ещё", Icons.Default.MoreHoriz, onMore),
    )
    val selectedIndex = when (current) {
        Route.Home -> 0
        Route.Topics, is Route.TopicDetail -> 1
        Route.Search -> 2
        Route.Bookmarks -> 3
        Route.More, Route.Remedies, Route.Notes, Route.Settings, Route.Hadiths, Route.History,
        Route.Offline, Route.About, Route.Collections, Route.Glossary, is Route.RemedyDetail,
        is Route.CollectionDetail -> 4
        else -> -1
    }
    NavigationBar {
        items.forEachIndexed { index, (label, icon, action) ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = action,
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(),
            )
        }
    }
}
