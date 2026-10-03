package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalidmuslim.arabicgrammar.data.BookRepository
import com.xalidmuslim.arabicgrammar.data.ProgressStore
import com.xalidmuslim.arabicgrammar.data.ReaderGroup
import com.xalidmuslim.arabicgrammar.data.ReaderKind
import com.xalidmuslim.arabicgrammar.data.ReaderSettings
import com.xalidmuslim.arabicgrammar.data.buildReaderGroups
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class ReaderSheet {
    EXERCISES,
    NOTE,
    SOURCE,
}

private val arabicRun = Regex(
    "[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]+" +
        "(?:\\s+[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]+)*"
)

private fun bidiSafe(text: String): String =
    arabicRun.replace(text) { match -> "\u2067" + match.value + "\u2069" }

private fun tableLines(text: String): List<String> =
    text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    repository: BookRepository,
    progress: ProgressStore,
    sectionId: String,
    navigate: (Screen) -> Unit,
    back: () -> Unit,
) {
    val section = remember(sectionId) { repository.section(sectionId) }
    val groups = remember(sectionId) { buildReaderGroups(repository.blocks(sectionId)) }
    val settings = progress.settings
    var sheet by remember { mutableStateOf<ReaderSheet?>(null) }
    var note by remember(sectionId) { mutableStateOf(progress.note(sectionId)) }

    if (section == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Раздел не найден")
        }
        return
    }

    val adjacent = remember(sectionId) { repository.adjacent(sectionId) }
    val initialIndex = progress.readerIndex(sectionId).coerceIn(0, groups.size + 1)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex,
        initialFirstVisibleItemScrollOffset = progress.readerOffset(sectionId),
    )
    val readingProgress by remember {
        derivedStateOf {
            val totalItems = (groups.size + 1).coerceAtLeast(1)
            (listState.firstVisibleItemIndex.toFloat() / totalItems).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(section.id) {
        progress.updateLastSection(section.id)
    }

    LaunchedEffect(sectionId, listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { pair ->
                progress.saveReaderPosition(sectionId, pair.first, pair.second)
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            chapterName(section.chapter) + " · Тема " + section.number,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            section.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Text(
                        ((readingProgress * 100).toInt()).toString() + "%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = { progress.toggleBookmark(section.id) }) {
                        Icon(
                            if (progress.bookmarks.contains(section.id)) Icons.Default.Bookmark
                            else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Закладка",
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(62.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReaderAction("Упражнения", Icons.Outlined.Quiz, Modifier.weight(1f)) {
                        sheet = ReaderSheet.EXERCISES
                    }
                    ReaderAction("Заметка", Icons.Outlined.NoteAlt, Modifier.weight(1f)) {
                        sheet = ReaderSheet.NOTE
                    }
                    ReaderAction("О разделе", Icons.Outlined.Info, Modifier.weight(1f)) {
                        sheet = ReaderSheet.SOURCE
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(settings.paragraphGap.coerceIn(7f, 16f).dp),
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Kicker("Урок")
                        Text(section.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "Правила, примеры и упражнения собраны в отдельные компактные блоки.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { progress.toggleCompleted(section.id) },
                            shape = RoundedCornerShape(99.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (progress.completed.contains(section.id)) "Освоено" else "Отметить тему")
                        }
                    }
                }
            }

            item {
                LessonDiagram(section)
            }

            item {
                LinearProgressIndicator(
                    progress = readingProgress,
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            itemsIndexed(
                groups,
                key = { index, group ->
                    group.kind.name + "_" + (group.blocks.firstOrNull()?.index ?: index)
                },
            ) { index, group ->
                when (group.kind) {
                    ReaderKind.EXERCISE_TITLE -> ExerciseTitleCard(group)
                    ReaderKind.ARABIC -> ArabicCard(group, settings)
                    ReaderKind.TABLE -> TableCard(group, settings)
                    ReaderKind.ANSWER_LINE -> AnswerField(group)
                    ReaderKind.PARAGRAPH -> ParagraphCard(group, settings, index == 0)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!progress.completed.contains(section.id)) {
                        Button(
                            onClick = { progress.toggleCompleted(section.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Тема пройдена")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                adjacent.first?.let { navigate(Screen.Reader(it.id)) }
                            },
                            enabled = adjacent.first != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("← Предыдущая")
                        }
                        OutlinedButton(
                            onClick = {
                                adjacent.second?.let { navigate(Screen.Reader(it.id)) }
                            },
                            enabled = adjacent.second != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Следующая →")
                        }
                    }
                }
            }
        }
    }

    if (sheet != null) {
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
            ) {
                when (sheet) {
                    ReaderSheet.EXERCISES -> {
                        Text("Упражнения", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (section.exerciseCount > 0) {
                                "В уроке найдено " + section.exerciseCount +
                                    " блока упражнений. Они выделены внутри темы."
                            } else {
                                "В этом разделе отдельный блок упражнений не найден."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                progress.markExerciseDone(section.id)
                                sheet = null
                            },
                            enabled = section.exerciseCount > 0,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (progress.exerciseDone.contains(section.id))
                                    "Отмечено выполненным"
                                else
                                    "Отметить выполненным"
                            )
                        }
                    }

                    ReaderSheet.NOTE -> {
                        Text("Моя заметка", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            label = { Text("Правило своими словами, пример или вопрос") },
                            shape = RoundedCornerShape(14.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                progress.saveNote(section.id, note)
                                sheet = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Сохранить")
                        }
                    }

                    ReaderSheet.SOURCE -> {
                        Text("О формате", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Книга хранится в структурированной SQLite-базе внутри APK. " +
                                "Русские пояснения, арабские примеры, таблицы и упражнения " +
                                "рендерятся отдельными нативными компонентами.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    null -> Unit
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ParagraphCard(
    group: ReaderGroup,
    settings: ReaderSettings,
    first: Boolean,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (group.exercise)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
            else
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (first && !group.exercise) {
                Text(
                    "Объяснение",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(7.dp))
            }

            group.blocks.forEachIndexed { index, block ->
                Text(
                    bidiSafe(block.text),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.Ltr,
                        lineHeight = (22f * settings.textScale * settings.lineHeight / 1.55f).sp,
                    ),
                )
                if (index != group.blocks.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ArabicCard(
    group: ReaderGroup,
    settings: ReaderSettings,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (group.exercise)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (group.exercise) "Задание" else "Арабский пример",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(7.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    group.blocks.first().text,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = TextStyle(
                        fontFamily = arabicFont(settings),
                        fontSize = (21.5f * settings.arabicScale).sp,
                        lineHeight = (34f * settings.arabicScale).sp,
                        textDirection = TextDirection.Rtl,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TableCard(
    group: ReaderGroup,
    settings: ReaderSettings,
) {
    val blocks = group.blocks
    val labels = blocks.firstOrNull()?.let { tableLines(it.text) }.orEmpty()
    val valueColumns = blocks.drop(1).take(3).map { block ->
        tableLines(block.text).filterNot { it.lowercase().contains("число") }
    }
    val conjugation = labels.size in 4..6 &&
        labels.any { it.lowercase().contains("лицо") } &&
        valueColumns.size == 3 &&
        valueColumns.all { it.size >= labels.size }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            if (conjugation) {
                Text(
                    "Спряжение",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Лицо",
                        modifier = Modifier.width(88.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    listOf("Ед.", "Дв.", "Мн.").forEach { header ->
                        Text(
                            header,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                labels.forEachIndexed { row, label ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label.replace("жен.рода", "жен. рода").replace("муж.рода", "муж. рода"),
                            modifier = Modifier.width(88.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        valueColumns.forEach { column ->
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    column.getOrNull(row).orEmpty(),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        fontFamily = arabicFont(settings),
                                        fontSize = (18f * settings.arabicScale).sp,
                                        lineHeight = (26f * settings.arabicScale).sp,
                                        textDirection = TextDirection.Rtl,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                            }
                        }
                    }
                    if (row != labels.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    }
                }
            } else {
                Text(
                    "Таблица",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(7.dp))
                blocks.forEachIndexed { blockIndex, block ->
                    tableLines(block.text).forEach { line ->
                        Text(
                            bidiSafe(line),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                        )
                    }
                    if (blockIndex != blocks.lastIndex) {
                        Spacer(Modifier.height(5.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        Spacer(Modifier.height(5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerField(group: ReaderGroup) {
    var answer by rememberSaveable(group.blocks.firstOrNull()?.index) { mutableStateOf("") }
    OutlinedTextField(
        value = answer,
        onValueChange = { answer = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Ваш ответ") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ExerciseTitleCard(group: ReaderGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Quiz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Kicker("Практика")
                Text(
                    group.blocks.firstOrNull()?.text ?: "Упражнения",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun ReaderAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
