package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalidmuslim.arabicgrammar.data.BookRepository
import com.xalidmuslim.arabicgrammar.data.ProgressStore

private fun normalizeArabicAnswer(value: String): String =
    value
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
        .replace(Regex("[\\s،؛,.!?؟]"), "")
        .trim()

@Composable
fun ConjugationTrainerScreen(
    repository: BookRepository,
    progress: ProgressStore,
    reviewOnly: Boolean,
    back: () -> Unit,
) {
    var sessionKey by remember { mutableIntStateOf(0) }
    val all = remember(sessionKey) { repository.conjugationQuestions(240) }
    val questions = remember(all, reviewOnly, progress.conjugationWrong) {
        if (reviewOnly) {
            all.filter { progress.conjugationWrong.contains(it.id) }
        } else {
            all.take(20)
        }
    }

    if (questions.isEmpty()) {
        EmptyStudyState(
            title = if (reviewOnly) "Ошибок для повторения нет" else "Таблицы спряжения не найдены",
            description = if (reviewOnly)
                "Все отмеченные формы сейчас закрыты."
            else
                "Тренажёр использует только распознанные таблицы из раздела морфологии.",
            back = back,
        )
        return
    }

    var index by remember(sessionKey) { mutableIntStateOf(0) }
    var answer by remember(sessionKey, index) { mutableStateOf("") }
    var checked by remember(sessionKey, index) { mutableStateOf(false) }
    var correctInSession by remember(sessionKey) { mutableIntStateOf(0) }

    if (index >= questions.size) {
        val percent = ((correctInSession * 100f) / questions.size).toInt()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(38.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("Тренировка завершена", style = MaterialTheme.typography.titleLarge)
            Text(
                correctInSession.toString() + " из " + questions.size +
                    " · " + percent + "%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = back, modifier = Modifier.weight(1f)) {
                    Text("Назад")
                }
                Button(
                    onClick = { sessionKey += 1 },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Ещё раз")
                }
            }
        }
        return
    }

    val question = questions[index]
    val normalizedExpected = normalizeArabicAnswer(question.answer)
    val normalizedGiven = normalizeArabicAnswer(answer)
    val correct = checked && normalizedGiven == normalizedExpected

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = back) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
            }
            Column(Modifier.weight(1f)) {
                Kicker(if (reviewOnly) "Работа над ошибками" else "Спряжение")
                Text(
                    (index + 1).toString() + " из " + questions.size,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                progress.conjugationAccuracy().toString() + "%",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        LinearProgressIndicator(
            progress = (index + 1f) / questions.size,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Kicker(question.sectionTitle)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        question.personLabel,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        question.numberLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Введи арабскую форму по памяти.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                OutlinedTextField(
                    value = answer,
                    onValueChange = { if (!checked) answer = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontFamily = arabicFont(progress.settings),
                        fontSize = (24f * progress.settings.arabicScale).sp,
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.End,
                    ),
                    placeholder = { Text("اكتب الجواب") },
                    singleLine = true,
                    enabled = !checked,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            }

            if (!checked) {
                Button(
                    onClick = {
                        checked = true
                        val hit = normalizedGiven.isNotBlank() && normalizedGiven == normalizedExpected
                        if (hit) correctInSession += 1
                        progress.recordConjugation(question.id, hit)
                    },
                    enabled = answer.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Проверить")
                }

                TextButton(
                    onClick = {
                        checked = true
                        progress.recordConjugation(question.id, false)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Не помню — показать ответ")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (correct)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (correct) "Верно" else "Правильная форма",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                question.answer,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                                style = TextStyle(
                                    fontFamily = arabicFont(progress.settings),
                                    fontSize = (26f * progress.settings.arabicScale).sp,
                                    lineHeight = (38f * progress.settings.arabicScale).sp,
                                    textDirection = TextDirection.Rtl,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        index += 1
                        answer = ""
                        checked = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (index == questions.lastIndex) "Завершить" else "Следующая форма")
                }
            }
        }
    }
}
