package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalidmuslim.arabicgrammar.data.BookRepository
import com.xalidmuslim.arabicgrammar.data.ProgressStore

@Composable
fun QuizScreen(
    repository: BookRepository,
    progress: ProgressStore,
    reviewErrors: Boolean,
    back: () -> Unit,
) {
    var sessionKey by remember { mutableIntStateOf(0) }
    val preferred = if (reviewErrors) progress.quizWrongSections else progress.completed
    val questions = remember(sessionKey, reviewErrors, preferred) {
        repository.quizQuestions(preferred, 10)
    }

    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Пока недостаточно материала для проверки.")
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = back) { Text("Назад") }
            }
        }
        return
    }

    var index by remember(sessionKey) { mutableIntStateOf(0) }
    var selected by remember(sessionKey, index) { mutableStateOf<Int?>(null) }
    var sessionCorrect by remember(sessionKey) { mutableIntStateOf(0) }
    val missed = remember(sessionKey) { mutableStateListOf<String>() }

    if (index >= questions.size) {
        QuizFinish(
            correct = sessionCorrect,
            total = questions.size,
            missedTitles = missed.toList(),
            onAgain = { sessionKey += 1 },
            onBack = back,
        )
        return
    }

    val question = questions[index]
    val answered = selected != null
    val isCorrect = selected == question.correctIndex

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = back) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
            }
            Column(Modifier.weight(1f)) {
                Kicker(if (reviewErrors) "Повторение ошибок" else "Быстрая проверка")
                Text(
                    "Вопрос " + (index + 1) + " из " + questions.size,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                sessionCorrect.toString() + "/" + (index + if (answered) 1 else 0),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        LinearProgressIndicator(
            progress = (index + if (answered) 1 else 0).toFloat() / questions.size,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Kicker("Фрагмент из книги")
                        Spacer(Modifier.height(8.dp))
                        if (question.arabicHeavy) {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides LayoutDirection.Rtl,
                            ) {
                                Text(
                                    question.prompt,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    style = TextStyle(
                                        fontFamily = arabicFont(progress.settings),
                                        fontSize = (23f * progress.settings.arabicScale).sp,
                                        lineHeight = (36f * progress.settings.arabicScale).sp,
                                        textDirection = TextDirection.Rtl,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                            }
                        } else {
                            Text(
                                styledMixedText(question.prompt, progress.settings),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Ltr,
                                ),
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "К какой теме относится этот фрагмент?",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            items(question.options.size) { optionIndex ->
                val correctOption = optionIndex == question.correctIndex
                val selectedOption = optionIndex == selected

                val containerColor = when {
                    answered && correctOption ->
                        MaterialTheme.colorScheme.primaryContainer
                    answered && selectedOption && !correctOption ->
                        MaterialTheme.colorScheme.errorContainer
                    else ->
                        MaterialTheme.colorScheme.surface
                }

                val borderColor = when {
                    answered && correctOption ->
                        MaterialTheme.colorScheme.primary
                    answered && selectedOption && !correctOption ->
                        MaterialTheme.colorScheme.error
                    else ->
                        MaterialTheme.colorScheme.outline
                }

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!answered) {
                            selected = optionIndex
                            val hit = optionIndex == question.correctIndex
                            if (hit) {
                                sessionCorrect += 1
                            } else if (!missed.contains(question.sectionTitle)) {
                                missed += question.sectionTitle
                            }
                            progress.recordQuiz(question.sectionId, hit)
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = containerColor,
                    ),
                    border = BorderStroke(1.dp, borderColor),
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(30.dp),
                            shape = RoundedCornerShape(99.dp),
                            color = if (answered && correctOption)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    ('A'.code + optionIndex).toChar().toString(),
                                    color = if (answered && correctOption)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            question.options[optionIndex],
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (answered) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            else
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        ),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                if (isCorrect) "Верно" else "Нужно повторить",
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Правильная тема: " + question.sectionTitle,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            index += 1
                            selected = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(if (index == questions.lastIndex) "Завершить" else "Следующий вопрос")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizFinish(
    correct: Int,
    total: Int,
    missedTitles: List<String>,
    onAgain: () -> Unit,
    onBack: () -> Unit,
) {
    val percent = if (total == 0) 0 else ((correct * 100f) / total).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Kicker("Результат")
            Text(
                correct.toString() + " из " + total,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 38.sp),
            )
            Text(
                percent.toString() + "% правильных ответов",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            percent >= 90 -> "Материал удерживается хорошо."
                            percent >= 70 -> "Основа усвоена, но есть что повторить."
                            else -> "Лучше вернуться к темам с ошибками и пройти ещё раз."
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        if (missedTitles.isNotEmpty()) {
            item { SectionTitle("Повторить", "Темы с ошибками") }
            items(missedTitles.size) { i ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        missedTitles[i],
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Назад")
                }
                Button(
                    onClick = onAgain,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Ещё раз")
                }
            }
        }
    }
}
