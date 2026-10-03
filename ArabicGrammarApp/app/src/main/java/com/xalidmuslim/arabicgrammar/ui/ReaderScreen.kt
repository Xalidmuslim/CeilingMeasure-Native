package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

private enum class ReaderSheet {
    EXERCISES,
    NOTE,
    SOURCE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    repository: BookRepository,
    progress: ProgressStore,
    sectionId: String,
    back: () -> Unit,
) {
    val section = remember(sectionId) { repository.section(sectionId) }
    val groups = remember(sectionId) { buildReaderGroups(repository.blocks(sectionId)) }
    val settings = progress.settings
    var sheet by remember { mutableStateOf<ReaderSheet?>(null) }
    var note by remember(sectionId) { mutableStateOf(progress.note(sectionId)) }

    if (section == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Раздел не найден")
        }
        return
    }

    LaunchedEffect(section.id) {
        progress.updateLastSection(section.id)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            chapterName(section.chapter) + " · § " + section.number,
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
                    IconButton(onClick = { progress.toggleBookmark(section.id) }) {
                        Icon(
                            if (progress.bookmarks.contains(section.id)) {
                                Icons.Default.Bookmark
                            } else {
                                Icons.Outlined.BookmarkBorder
                            },
                            contentDescription = "Закладка",
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                ReaderAction(
                    "Упражнения",
                    Icons.Outlined.Quiz,
                    Modifier.weight(1f),
                ) { sheet = ReaderSheet.EXERCISES }
                ReaderAction(
                    "Заметка",
                    Icons.Outlined.NoteAlt,
                    Modifier.weight(1f),
                ) { sheet = ReaderSheet.NOTE }
                ReaderAction(
                    "О разделе",
                    Icons.Outlined.Info,
                    Modifier.weight(1f),
                ) { sheet = ReaderSheet.SOURCE }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(settings.paragraphGap.dp),
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Kicker("Урок")
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Объяснения, арабские примеры и упражнения показаны отдельными блоками.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { progress.toggleCompleted(section.id) },
                            shape = RoundedCornerShape(99.dp),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (progress.completed.contains(section.id)) {
                                    "Освоено"
                                } else {
                                    "Отметить тему"
                                }
                            )
                        }
                    }
                }
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
                    ReaderKind.PARAGRAPH -> ParagraphCard(
                        group = group,
                        settings = settings,
                        first = index == 0,
                    )
                }
            }
        }
    }

    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp),
            ) {
                when (sheet) {
                    ReaderSheet.EXERCISES -> {
                        Text("Упражнения", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (section.exerciseCount > 0) {
                                "В уроке найдено " + section.exerciseCount +
                                    " блока упражнений. Они выделены внутри чтения."
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
                                if (progress.exerciseDone.contains(section.id)) {
                                    "Отмечено выполненным"
                                } else {
                                    "Отметить выполненным"
                                }
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
                            minLines = 5,
                            label = { Text("Правило своими словами, пример или вопрос") },
                            shape = RoundedCornerShape(16.dp),
                        )
                        Spacer(Modifier.height(12.dp))
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
                                "Экран урока нативно собирается из тем, абзацев, арабских примеров " +
                                "и упражнений. PDF, HTML и WebView при чтении не используются.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    null -> Unit
                }
                Spacer(Modifier.height(14.dp))
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
    val background = if (group.exercise) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = background),
    ) {
        Column(Modifier.padding(17.dp)) {
            if (first && !group.exercise) {
                Text(
                    "Объяснение",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
            }

            group.blocks.forEachIndexed { index, block ->
                Text(
                    block.text,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.ContentOrRtl,
                        lineHeight = (
                            25f *
                                settings.textScale *
                                settings.lineHeight /
                                1.55f
                            ).sp,
                    ),
                )
                if (index != group.blocks.lastIndex) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                    Spacer(Modifier.height(10.dp))
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (group.exercise) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    if (group.exercise) "Задание" else "Арабский пример",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                Text(
                    group.blocks.first().text,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = TextStyle(
                        fontFamily = arabicFont(settings),
                        fontSize = (27f * settings.arabicScale).sp,
                        lineHeight = (45f * settings.arabicScale).sp,
                        textDirection = TextDirection.Rtl,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ExerciseTitleCard(group: ReaderGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Quiz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(10.dp))
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
    TextButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}
