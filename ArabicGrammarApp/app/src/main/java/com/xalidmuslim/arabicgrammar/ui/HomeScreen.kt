package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalidmuslim.arabicgrammar.data.BookRepository
import com.xalidmuslim.arabicgrammar.data.ProgressStore
import com.xalidmuslim.arabicgrammar.data.buildReaderGroups

@Composable
fun HomeScreen(
    repository: BookRepository,
    progress: ProgressStore,
    navigate: (Screen) -> Unit,
) {
    val meta = remember { repository.meta() }
    val syntax = remember { repository.sections("syntax") }
    val morphology = remember { repository.sections("morphology") }
    val proverbCount = remember { repository.proverbs().size }
    val current = remember(progress.lastSection) {
        repository.section(progress.lastSection) ?: syntax.firstOrNull()
    }
    val currentRead = remember(current?.id, progress.lastSection) {
        current?.let {
            val totalItems = buildReaderGroups(repository.blocks(it.id)).size + 1
            if (totalItems <= 1) 0
            else ((progress.readerIndex(it.id) * 100f) / totalItems).toInt().coerceIn(0, 99)
        } ?: 0
    }
    val recent = progress.recentSections
        .filterNot { it == current?.id }
        .mapNotNull { repository.section(it) }
        .take(3)

    val done = progress.completed.count { it.startsWith("s") || it.startsWith("m") }
    val total = syntax.size + morphology.size
    val ratio = if (total == 0) 0f else done.toFloat() / total.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Kicker(meta.author.ifBlank { "И. Н. Хайбуллин" })
                    Text(
                        "Арабская грамматика",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp),
                    )
                }
                IconButton(onClick = { navigate(Screen.Bookmarks) }) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Закладки")
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Kicker("Продолжить обучение")
                        Text(
                            current?.title ?: "Части речи",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            current?.let {
                                chapterName(it.chapter) + " · Тема " + it.number +
                                    if (currentRead > 0) " · " + currentRead + "%" else ""
                            } ?: "Глава 1",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { navigate(Screen.Reader(current?.id ?: "s1")) },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (currentRead > 0) "Продолжить с места" else "Начать")
                        }
                    }
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = ratio,
                            modifier = Modifier.size(72.dp),
                            strokeWidth = 7.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(
                            ((ratio * 100).toInt()).toString() + "%",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(done.toString() + "/" + total, "тем", Modifier.weight(1f))
                StatCard(progress.exerciseDone.size.toString(), "упражнений", Modifier.weight(1f))
                StatCard(progress.currentStreak().toString(), "дней подряд", Modifier.weight(1f))
            }
        }

        if (recent.isNotEmpty()) {
            item { SectionTitle("История", "Недавние темы") }
            recent.forEach { section ->
                item(key = "recent_" + section.id) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigate(Screen.Reader(section.id)) },
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(section.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    chapterName(section.chapter) + " · Тема " + section.number,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle("Основной маршрут", "Две главы — один курс") }

        item {
            ChapterCard(
                title = "Синтаксис",
                subtitle = "Глава 1 · " + syntax.size + " тем",
                badge = "نحو",
                description = "Правила предложения, падежи, частицы и конструкции.",
                onClick = { navigate(Screen.Library("syntax")) },
            )
        }

        item {
            ChapterCard(
                title = "Морфология",
                subtitle = "Глава 2 · " + morphology.size + " тем",
                badge = "صرف",
                description = "Породы, спряжения, масдары и производные формы.",
                onClick = { navigate(Screen.Library("morphology")) },
            )
        }

        item { SectionTitle("Не только чтение", "Закрепить материал") }

        item {
            ActionCard(
                Icons.Default.School,
                "Практика",
                "Упражнения и быстрые проверки",
            ) { navigate(Screen.Practice) }
        }
        item {
            ActionCard(
                Icons.Default.Search,
                "Поиск по книге",
                "Русский и арабский текст",
            ) { navigate(Screen.Search) }
        }
        item {
            ActionCard(
                Icons.Outlined.FormatQuote,
                "Пословицы",
                proverbCount.toString() + " пословиц из всей книги",
            ) { navigate(Screen.Proverbs) }
        }
        item {
            ActionCard(
                Icons.Default.MenuBook,
                "Содержание",
                "Все темы и дополнительные разделы",
            ) { navigate(Screen.Library()) }
        }
    }
}
