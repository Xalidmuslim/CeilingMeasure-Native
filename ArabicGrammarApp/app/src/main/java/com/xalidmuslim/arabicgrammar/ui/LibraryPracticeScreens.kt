package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalidmuslim.arabicgrammar.data.BookRepository
import com.xalidmuslim.arabicgrammar.data.ProgressStore

@Composable
fun LibraryScreen(
    repository: BookRepository,
    progress: ProgressStore,
    chapter: String?,
    navigate: (Screen) -> Unit,
    back: () -> Unit,
) {
    if (chapter == null) {
        val syntax = remember { repository.sections("syntax") }
        val morphology = remember { repository.sections("morphology") }
        val extras = remember { repository.sections("extras").filter { it.found } }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Kicker("Вся книга")
                Text(
                    "Содержание",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Каждая тема открывается как отдельный урок, а не как страница документа.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                ChapterCard(
                    "Синтаксис",
                    "Глава 1 · " + syntax.size + " тем",
                    "1",
                    syntax.count { progress.completed.contains(it.id) }.toString() + " тем отмечено",
                ) { navigate(Screen.Library("syntax")) }
            }

            item {
                ChapterCard(
                    "Морфология",
                    "Глава 2 · " + morphology.size + " тем",
                    "2",
                    morphology.count { progress.completed.contains(it.id) }.toString() + " тем отмечено",
                ) { navigate(Screen.Library("morphology")) }
            }

            if (extras.isNotEmpty()) {
                item { SectionTitle("Дополнительно", "Словари и тексты") }
                items(extras, key = { it.id }) { section ->
                    ActionCard(
                        Icons.Default.MenuBook,
                        section.title,
                        "Дополнительный раздел",
                    ) { navigate(Screen.Reader(section.id)) }
                }
            }
        }
    } else {
        val sections = remember(chapter) { repository.sections(chapter) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = back) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                    Column {
                        Kicker(if (chapter == "syntax") "Глава 1" else "Глава 2")
                        Text(chapterName(chapter), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            items(sections, key = { it.id }) { section ->
                TopicRow(
                    section,
                    progress.completed.contains(section.id),
                ) { navigate(Screen.Reader(section.id)) }
            }
        }
    }
}

@Composable
fun PracticeScreen(
    repository: BookRepository,
    progress: ProgressStore,
    navigate: (Screen) -> Unit,
) {
    val sections = remember { repository.exerciseSections() }
    val done = sections.count { progress.exerciseDone.contains(it.id) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Kicker("Закрепление")
            Text(
                "Практика",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Карточки, спряжение, быстрые проверки и упражнения из книги.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { SectionTitle("Активное повторение", "Вспомнить без подсказки") }

        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigate(Screen.Flashcards(false)) },
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Quiz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("Карточки", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Правило → ответ и арабский пример → тема. Ответ сначала скрыт.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text(progress.flashKnown.size.toString() + " знаю") },
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(progress.flashRepeat.size.toString() + " повторить") },
                        )
                    }
                }
            }
        }

        if (progress.flashRepeat.isNotEmpty()) {
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navigate(Screen.Flashcards(true)) },
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Повторить карточки", style = MaterialTheme.typography.titleSmall)
                            Text(
                                progress.flashRepeat.size.toString() + " карточек отмечено как сложные",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigate(Screen.ConjugationTrainer(false)) },
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("Тренажёр спряжения", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Приложение показывает лицо и число. Арабскую форму вводишь сам.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                progress.conjugationAccuracy().toString() +
                                    "% точность"
                            )
                        },
                    )
                }
            }
        }

        if (progress.conjugationWrong.isNotEmpty()) {
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navigate(Screen.ConjugationTrainer(true)) },
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Повторить формы", style = MaterialTheme.typography.titleSmall)
                            Text(
                                progress.conjugationWrong.size.toString() +
                                    " форм требуют повторения",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle("Проверка понимания", "Узнать тему по фрагменту") }

        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigate(Screen.Quiz(false)) },
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Quiz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("Быстрая проверка", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "10 вопросов по реальным фрагментам книги.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    progress.quizCorrect.toString() + "/" +
                                        progress.quizAttempts + " верно"
                                )
                            },
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(progress.quizAccuracy().toString() + "% точность") },
                        )
                    }
                }
            }
        }

        if (progress.quizWrongSections.isNotEmpty()) {
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navigate(Screen.Quiz(true)) },
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Повторить ошибки теста", style = MaterialTheme.typography.titleSmall)
                            Text(
                                progress.quizWrongSections.size.toString() +
                                    " тем требуют повторения",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle("Из книги", "Авторские упражнения") }

        item {
            LinearProgressIndicator(
                progress = if (sections.isEmpty()) 0f else done.toFloat() / sections.size,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                done.toString() + " из " + sections.size + " тем выполнено",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        items(sections, key = { it.id }) { section ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigate(Screen.Reader(section.id)) },
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = progress.exerciseDone.contains(section.id),
                        onCheckedChange = { progress.toggleExerciseDone(section.id) },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(section.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            chapterName(section.chapter) + " · " +
                                section.exerciseCount + " блок(а) упражнений",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
