package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalidmuslim.arabicgrammar.data.BookRepository
import com.xalidmuslim.arabicgrammar.data.ProgressStore
import com.xalidmuslim.arabicgrammar.data.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(
    repository: BookRepository,
    navigate: (Screen) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val text = query.trim()
        if (text.length < 2) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(220)
        results = withContext(Dispatchers.IO) { repository.search(text) }
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Kicker("Вся книга")
            Text(
                "Поиск",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Термин или арабское слово") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        if (!loading && query.trim().length >= 2 && results.isEmpty()) {
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Совпадений не найдено.",
                        modifier = Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(results, key = { it.section.id }) { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navigate(Screen.Reader(result.section.id)) },
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Kicker(chapterName(result.section.chapter))
                    Text(result.section.title, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        result.snippet,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDirection = TextDirection.ContentOrRtl,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarksScreen(
    repository: BookRepository,
    progress: ProgressStore,
    navigate: (Screen) -> Unit,
    back: () -> Unit,
) {
    val all = remember { repository.sections() }
    val saved = all.filter { progress.bookmarks.contains(it.id) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                }
                Column {
                    Kicker("Сохранённое")
                    Text("Закладки", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        if (saved.isEmpty()) {
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Пока нет закладок. Добавляй важные темы из верхней панели урока.",
                        modifier = Modifier.padding(22.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(saved, key = { it.id }) { section ->
            TopicRow(
                section,
                progress.completed.contains(section.id),
            ) { navigate(Screen.Reader(section.id)) }
        }
    }
}

@Composable
fun SettingsScreen(progress: ProgressStore) {
    val settings = progress.settings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Kicker("Под себя")
            Text(
                "Настройки чтения",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp),
            )
        }

        item {
            SettingsCard("Тема") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "light" to "День",
                        "dark" to "Ночь",
                        "system" to "Система",
                    ).forEach { pair ->
                        FilterChip(
                            selected = settings.theme == pair.first,
                            onClick = {
                                progress.updateSettings(settings.copy(theme = pair.first))
                            },
                            label = { Text(pair.second) },
                        )
                    }
                }
            }
        }

        item {
            SettingsCard("Русский шрифт") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "sans" to "Обычный",
                        "serif" to "Книжный",
                        "mono" to "Моно",
                    ).forEach { pair ->
                        FilterChip(
                            selected = settings.fontChoice == pair.first,
                            onClick = {
                                progress.updateSettings(settings.copy(fontChoice = pair.first))
                            },
                            label = { Text(pair.second) },
                        )
                    }
                }
            }
        }

        item {
            SettingsCard("Арабский шрифт") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "serif" to "Книжный",
                        "sans" to "Sans",
                        "mono" to "Моно",
                    ).forEach { pair ->
                        FilterChip(
                            selected = settings.arabicFontChoice == pair.first,
                            onClick = {
                                progress.updateSettings(settings.copy(arabicFontChoice = pair.first))
                            },
                            label = { Text(pair.second) },
                        )
                    }
                }
                Text(
                    "النَّحْوُ وَالصَّرْفُ",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = arabicFont(settings),
                        fontSize = (28f * settings.arabicScale).sp,
                    ),
                )
            }
        }

        item {
            SliderCard(
                "Размер русского текста · " + (settings.textScale * 100).toInt() + "%",
                settings.textScale,
                0.85f..1.35f,
            ) {
                progress.updateSettings(settings.copy(textScale = it))
            }
        }

        item {
            SliderCard(
                "Размер арабского текста · " + (settings.arabicScale * 100).toInt() + "%",
                settings.arabicScale,
                0.9f..1.5f,
            ) {
                progress.updateSettings(settings.copy(arabicScale = it))
            }
        }

        item {
            SliderCard(
                "Межстрочный интервал",
                settings.lineHeight,
                1.25f..2f,
            ) {
                progress.updateSettings(settings.copy(lineHeight = it))
            }
        }

        item {
            SliderCard(
                "Отступы между карточками",
                settings.paragraphGap,
                6f..24f,
            ) {
                progress.updateSettings(settings.copy(paragraphGap = it))
            }
        }

        item {
            SettingsCard("Плавные анимации") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Переходы экранов и раскрывающихся функций",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Switch(
                        checked = settings.animations,
                        onCheckedChange = {
                            progress.updateSettings(settings.copy(animations = it))
                        },
                    )
                }
            }
        }
    }
}
