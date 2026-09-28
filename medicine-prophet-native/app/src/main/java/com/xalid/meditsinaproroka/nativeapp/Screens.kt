package com.xalid.meditsinaproroka.nativeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageHeader(title: String, subtitle: String? = null, back: (() -> Unit)? = null) {
    TopAppBar(
        title = {
            Column {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        navigationIcon = {
            if (back != null) {
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад") }
            }
        }
    )
}

@Composable
private fun ChapterRow(chapter: Chapter, onClick: () -> Unit, trailing: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                chapter.order.toString().padStart(2, '0'),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(34.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(chapter.title, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
                if (chapter.section.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(chapter.section, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(18.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(title, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun HomeScreen(
    book: BookData,
    store: AppStore,
    modifier: Modifier,
    navigate: (Route) -> Unit,
    search: (String) -> Unit,
) {
    var quickSearch by remember { mutableStateOf("") }
    val chaptersById = remember(book) { book.chapters.associateBy { it.id } }
    val last = store.lastChapterId?.let(chaptersById::get)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Медицина Пророка ﷺ", fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
            Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text("Полный текст · чтение и изучение", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }

        if (last != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Продолжить чтение", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(5.dp))
                        Text(last.title, fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { navigate(Route.Reader(last.id, resume = true)) }, modifier = Modifier.fillMaxWidth()) { Text("Продолжить") }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = quickSearch,
                onValueChange = { quickSearch = it },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Поиск по всей книге") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { if (quickSearch.isNotBlank()) IconButton(onClick = { search(quickSearch) }) { Icon(Icons.Default.ArrowForward, "Искать") } }
            )
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { navigate(Route.Collections) }) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MedicalServices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Как лечили / что применялось", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("Состояние → средства → полный текст", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ArrowForward, null)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Читать книгу", "${book.chapters.size} глав", Modifier.weight(1f)) { navigate(Route.Book) }
                MetricCard("Темы", "${book.topics.size} разделов", Modifier.weight(1f)) { navigate(Route.Topics) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Средства", book.remedies.size.toString(), Modifier.weight(1f)) { navigate(Route.Remedies) }
                MetricCard("Закладки", store.bookmarks.size.toString(), Modifier.weight(1f)) { navigate(Route.Bookmarks) }
            }
        }

        item { Text("Быстрый доступ", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ListItem(
                    headlineContent = { Text("Хадисы и источники") },
                    supportingContent = { Text("Источники показываются только там, где они указаны в тексте") },
                    leadingContent = { Icon(Icons.Default.FormatQuote, null) },
                    modifier = Modifier.clickable { navigate(Route.Hadiths) }
                )
                ListItem(
                    headlineContent = { Text("Мои заметки") },
                    supportingContent = { Text("${store.notes.size} заметок · ${store.highlights.size} выделений") },
                    leadingContent = { Icon(Icons.Default.EditNote, null) },
                    modifier = Modifier.clickable { navigate(Route.Notes) }
                )
                ListItem(
                    headlineContent = { Text("Офлайн") },
                    supportingContent = { Text("Весь текст книги находится внутри приложения") },
                    leadingContent = { Icon(Icons.Default.OfflinePin, null) },
                    modifier = Modifier.clickable { navigate(Route.Offline) }
                )
            }
        }
    }
}

@Composable
fun BookScreen(book: BookData, modifier: Modifier, back: () -> Unit, open: (String) -> Unit) {
    Column(modifier.fillMaxSize()) {
        PageHeader("Содержание", "${book.chapters.size} глав", back)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(book.chapters, key = { it.id }) { chapter -> ChapterRow(chapter, { open(chapter.id) }) }
        }
    }
}

@Composable
fun TopicsScreen(book: BookData, modifier: Modifier, open: (String) -> Unit) {
    Column(modifier.fillMaxSize()) {
        PageHeader("Темы", "${book.topics.size} разделов")
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(book.topics, key = { it.id }) { topic ->
                Card(Modifier.fillMaxWidth().clickable { open(topic.id) }) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(topic.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            Text("${topic.chapterIds.size} глав", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

@Composable
fun TopicDetailScreen(book: BookData, id: String, modifier: Modifier, back: () -> Unit, open: (String) -> Unit) {
    val topic = book.topics.firstOrNull { it.id == id }
    val map = remember(book) { book.chapters.associateBy { it.id } }
    val chapters = topic?.chapterIds?.mapNotNull(map::get).orEmpty()
    Column(modifier.fillMaxSize()) {
        PageHeader(topic?.title ?: "Тема", "${chapters.size} глав", back)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(chapters, key = { it.id }) { ChapterRow(it, { open(it.id) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    book: BookData,
    query: String,
    onQuery: (String) -> Unit,
    filter: SearchFilter,
    onFilter: (SearchFilter) -> Unit,
    modifier: Modifier,
    onOpen: (String, String?) -> Unit,
) {
    val results = remember(book, query, filter) { searchBook(book, query, filter) }
    Column(modifier.fillMaxSize()) {
        PageHeader("Поиск", if (query.length >= 2) "${results.size} результатов" else null)
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            placeholder = { Text("Введите слово или фразу") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, "Очистить") } },
            singleLine = true
        )
        ScrollableTabRow(selectedTabIndex = SearchFilter.entries.indexOf(filter), edgePadding = 10.dp, divider = {}) {
            SearchFilter.entries.forEach { item -> Tab(selected = item == filter, onClick = { onFilter(item) }, text = { Text(item.label) }) }
        }
        when {
            query.length < 2 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Введите не менее двух символов", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else -> LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(results, key = { index, hit -> "${hit.chapterId}-${hit.anchor}-§index" }) { _, hit ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(hit.chapterId, hit.anchor) }) {
                        Column(Modifier.padding(15.dp)) {
                            Text(hit.chapterTitle, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(hit.snippet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    book: BookData,
    store: AppStore,
    folder: String,
    onFolder: (String) -> Unit,
    modifier: Modifier,
    onOpen: (String, String?) -> Unit,
) {
    var showFolderDialog by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf("") }
    val map = remember(book) { book.chapters.associateBy { it.id } }
    val visible = store.bookmarks.filter { folder == "Все" || it.folder == folder }

    Column(modifier.fillMaxSize()) {
        PageHeader("Закладки", store.bookmarks.size.toString())
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Папки", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { showFolderDialog = true }) { Icon(Icons.Default.CreateNewFolder, null); Spacer(Modifier.width(4.dp)); Text("Добавить") }
        }
        ScrollableTabRow(
            selectedTabIndex = (listOf("Все") + store.bookmarkFolders).indexOf(folder).coerceAtLeast(0),
            edgePadding = 10.dp,
            divider = {}
        ) {
            (listOf("Все") + store.bookmarkFolders).forEach { f -> Tab(selected = f == folder, onClick = { onFolder(f) }, text = { Text(f) }) }
        }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("В этой папке пока нет закладок", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(visible, key = { "${it.chapterId}-${it.anchor}-${it.folder}" }) { bm ->
                    val ch = map[bm.chapterId] ?: return@items
                    ChapterRow(ch, { onOpen(ch.id, bm.anchor) }, if (bm.anchor != null) "Фрагмент" else bm.folder)
                }
            }
        }
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Новая папка") },
            text = { OutlinedTextField(newFolder, { newFolder = it }, label = { Text("Название") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    store.addFolder(newFolder)
                    if (newFolder.trim().isNotEmpty()) onFolder(newFolder.trim())
                    newFolder = ""
                    showFolderDialog = false
                }) { Text("Создать") }
            },
            dismissButton = { TextButton(onClick = { showFolderDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
fun MoreScreen(modifier: Modifier, navigate: (Route) -> Unit) {
    val rows = listOf(
        Triple("Читать книгу", "Полное содержание", Route.Book),
        Triple("Средства", "Все упомянутые средства и связанные главы", Route.Remedies),
        Triple("Как лечили", "Тематические подборки", Route.Collections),
        Triple("Хадисы и источники", "Проверяемые пометки по источникам", Route.Hadiths),
        Triple("Мои заметки", "Выделения и личные записи", Route.Notes),
        Triple("История", "Недавно открытые главы", Route.History),
        Triple("Словарь", "Термины и названия средств", Route.Glossary),
        Triple("Настройки чтения", "Шрифт, размер, интервал, тема", Route.Settings),
        Triple("Офлайн", "Статус локальной копии", Route.Offline),
        Triple("О книге", "Источник текста и важная оговорка", Route.About),
    )
    Column(modifier.fillMaxSize()) {
        PageHeader("Ещё")
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(rows) { (title, subtitle, route) ->
                ListItem(
                    headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(subtitle) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.clickable { navigate(route) }
                )
            }
        }
    }
}

@Composable
fun RemediesScreen(book: BookData, modifier: Modifier, back: () -> Unit, open: (String) -> Unit) {
    Column(modifier.fillMaxSize()) {
        PageHeader("Средства", book.remedies.size.toString(), back)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(book.remedies.sortedBy { it.title }, key = { it.id }) { remedy ->
                Card(Modifier.fillMaxWidth().clickable { open(remedy.id) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(remedy.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            if (remedy.aliases.isNotEmpty()) Text(remedy.aliases.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${remedy.chapterIds.size} связанных глав", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

@Composable
fun RemedyDetailScreen(book: BookData, id: String, modifier: Modifier, back: () -> Unit, open: (String, String?) -> Unit) {
    val remedy = book.remedies.firstOrNull { it.id == id }
    val map = remember(book) { book.chapters.associateBy { it.id } }
    val chapters = remedy?.chapterIds?.mapNotNull(map::get).orEmpty()
    Column(modifier.fillMaxSize()) {
        PageHeader(remedy?.title ?: "Средство", "${chapters.size} глав", back)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (remedy != null && remedy.aliases.isNotEmpty()) item {
                Card { Column(Modifier.padding(16.dp)) { Text("Также встречается как", fontWeight = FontWeight.SemiBold); Text(remedy.aliases.joinToString(", "), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
            itemsIndexed(chapters, key = { _, ch -> ch.id }) { index, ch ->
                val anchor = remedy?.anchors?.getOrNull(index)
                ChapterRow(ch, { open(ch.id, anchor) })
            }
        }
    }
}

@Composable
fun NotesScreen(book: BookData, store: AppStore, modifier: Modifier, back: () -> Unit, open: (String, String?) -> Unit) {
    val chapters = remember(book) { book.chapters.associateBy { it.id } }
    Column(modifier.fillMaxSize()) {
        PageHeader("Мои заметки", store.notes.size.toString(), back)
        if (store.notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Выделите текст в главе и выберите «Заметка»", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(store.notes, key = { it.id }) { note ->
                    val ch = chapters[note.chapterId]
                    Card {
                        Column(Modifier.padding(15.dp)) {
                            Text(ch?.title ?: "Глава", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp))
                            Text("«${note.selectedText}»", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            Text(note.note)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { open(note.chapterId, ch?.blocks?.firstOrNull { it.id == note.blockId }?.anchor) }) { Text("Открыть") }
                                TextButton(onClick = { store.deleteNote(note.id) }) { Text("Удалить") }
                                Spacer(Modifier.weight(1f))
                                Text(formatDate(note.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HadithsScreen(book: BookData, modifier: Modifier, back: () -> Unit, open: (String, String?) -> Unit) {
    val hits = remember(book) {
        buildList { book.chapters.forEach { ch -> ch.blocks.filter { it.type == "hadith" }.forEach { b -> add(Triple(ch, b, b.hadithSources.isNotEmpty())) } } }
    }
    Column(modifier.fillMaxSize()) {
        PageHeader("Хадисы и источники", hits.size.toString(), back)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            itemsIndexed(hits, key = { index, item -> "${item.second.id}-§index" }) { _, (ch, block, hasSource) ->
                Card(Modifier.fillMaxWidth().clickable { open(ch.id, block.anchor) }) {
                    Column(Modifier.padding(15.dp)) {
                        Text(ch.title, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))
                        Text(block.text, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            if (hasSource) block.hadithSources.joinToString(" · ") else "Источник требует ручного тахриджа",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasSource) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(book: BookData, store: AppStore, modifier: Modifier, back: () -> Unit, open: (String, String?) -> Unit) {
    val chapters = remember(book) { book.chapters.associateBy { it.id } }
    Column(modifier.fillMaxSize()) {
        PageHeader("История", store.history.size.toString(), back)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(store.history, key = { "${it.chapterId}-${it.anchor}-${it.openedAt}" }) { entry ->
                val ch = chapters[entry.chapterId] ?: return@items
                Card(Modifier.fillMaxWidth().clickable { open(ch.id, entry.anchor) }) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ch.title, fontWeight = FontWeight.SemiBold)
                            Text(formatDate(entry.openedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

private fun effectiveCollections(book: BookData): List<BookCollection> =
    if (book.collections.isNotEmpty()) book.collections else listOf(
        BookCollection("treatment", "Лечение и средства", book.chapters.filter { it.title.contains("леч", true) }.map { it.id }),
        BookCollection("food", "Питание и продукты", book.chapters.filter { it.topics.any { t -> t.contains("nutrition", true) } }.map { it.id }),
        BookCollection("spiritual", "Рукъя и духовное лечение", book.chapters.filter { it.title.contains("рук", true) || it.title.contains("сглаз", true) }.map { it.id }),
    )

@Composable
fun CollectionsScreen(book: BookData, modifier: Modifier, back: () -> Unit, open: (String) -> Unit) {
    val cols = remember(book) { effectiveCollections(book) }
    Column(modifier.fillMaxSize()) {
        PageHeader("Как лечили", "${cols.size} подборок", back)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(cols, key = { it.id }) { col ->
                Card(Modifier.fillMaxWidth().clickable { open(col.id) }) {
                    Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MedicalServices, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(col.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            Text("${col.chapterIds.size} глав", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionDetailScreen(book: BookData, id: String, modifier: Modifier, back: () -> Unit, open: (String) -> Unit) {
    val col = remember(book, id) { effectiveCollections(book).firstOrNull { it.id == id } }
    val map = remember(book) { book.chapters.associateBy { it.id } }
    val chapters = col?.chapterIds?.mapNotNull(map::get).orEmpty()
    Column(modifier.fillMaxSize()) {
        PageHeader(col?.title ?: "Подборка", "${chapters.size} глав", back)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(chapters, key = { it.id }) { ChapterRow(it, { open(it.id) }) }
        }
    }
}

@Composable
fun GlossaryScreen(book: BookData, modifier: Modifier, back: () -> Unit, open: (String) -> Unit) {
    Column(modifier.fillMaxSize()) {
        PageHeader("Словарь", "${book.remedies.size} терминов", back)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(book.remedies.sortedBy { it.title }, key = { it.id }) { r ->
                ListItem(
                    headlineContent = { Text(r.title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(if (r.aliases.isEmpty()) "Упоминания в книге" else r.aliases.joinToString(", ")) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.clickable { open(r.id) }
                )
            }
        }
    }
}

@Composable
fun OfflineScreen(book: BookData, modifier: Modifier, back: () -> Unit) {
    Column(modifier.fillMaxSize()) {
        PageHeader("Офлайн", null, back)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.OfflinePin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
            Text("Полная книга доступна без интернета", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Текст ${book.chapters.size} глав встроен непосредственно в APK/AAB. Поиск, закладки, заметки, настройки и прогресс чтения также работают локально.")
            Text("Интернет для чтения книги не требуется.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AboutScreen(book: BookData, modifier: Modifier, back: () -> Unit) {
    Column(modifier.fillMaxSize()) {
        PageHeader("О книге", null, back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(book.title, fontFamily = FontFamily.Serif, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("В приложении сохранён полный текущий текст издания, используемый в веб-версии, с исходным порядком глав и дополнительной тематической навигацией.")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Важное примечание", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Это историко-религиозный текст, а не современное медицинское руководство. Описанные методы не заменяют диагностику и лечение врача; отдельные исторические средства могут быть устаревшими или небезопасными.")
                }
            }
            Text("Источники хадисов показываются только там, где они присутствуют в данных книги. Приложение не присваивает степень достоверности автоматически.")
        }
    }
}
