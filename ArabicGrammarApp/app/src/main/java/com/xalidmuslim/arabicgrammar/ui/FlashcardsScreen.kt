package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import com.xalidmuslim.arabicgrammar.data.FlashCard
import com.xalidmuslim.arabicgrammar.data.ProgressStore

@Composable
fun FlashcardsScreen(
    repository: BookRepository,
    progress: ProgressStore,
    reviewOnly: Boolean,
    back: () -> Unit,
) {
    var deckKey by remember { mutableIntStateOf(0) }
    val allCards = remember(deckKey, progress.completed) {
        repository.flashCards(progress.completed, 200)
    }
    val cards = remember(allCards, reviewOnly, progress.flashRepeat) {
        if (reviewOnly) {
            allCards.filter { progress.flashRepeat.contains(it.id) }
        } else {
            allCards.take(30)
        }
    }

    if (cards.isEmpty()) {
        EmptyStudyState(
            title = if (reviewOnly) "Нет карточек на повторение" else "Карточки пока не готовы",
            description = if (reviewOnly)
                "Все отмеченные карточки уже разобраны."
            else
                "Пройди несколько тем — после этого здесь появятся карточки для активного повторения.",
            back = back,
        )
        return
    }

    var index by remember(deckKey) { mutableIntStateOf(0) }
    var revealed by remember(deckKey, index) { mutableStateOf(false) }
    var knownInSession by remember(deckKey) { mutableIntStateOf(0) }
    var repeatInSession by remember(deckKey) { mutableIntStateOf(0) }

    if (index >= cards.size) {
        FlashFinish(
            known = knownInSession,
            repeat = repeatInSession,
            onAgain = {
                deckKey += 1
                index = 0
                revealed = false
                knownInSession = 0
                repeatInSession = 0
            },
            back = back,
        )
        return
    }

    val card = cards[index]

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
                Kicker(if (reviewOnly) "Повторение" else "Карточки")
                Text(
                    (index + 1).toString() + " из " + cards.size,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                progress.flashKnown.size.toString() + " знаю",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        LinearProgressIndicator(
            progress = (index + 1f) / cards.size,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { revealed = !revealed },
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Kicker(if (revealed) "Ответ" else chapterName(card.chapter))
                    Spacer(Modifier.height(18.dp))

                    AnimatedContent(
                        targetState = revealed,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "flash_reveal",
                    ) { showAnswer ->
                        if (showAnswer) {
                            Text(
                                card.back,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.ContentOrLtr,
                                ),
                            )
                        } else if (card.arabicFront) {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides LayoutDirection.Rtl,
                            ) {
                                Text(
                                    card.front,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        fontFamily = arabicFont(progress.settings),
                                        fontSize = (28f * progress.settings.arabicScale).sp,
                                        lineHeight = (42f * progress.settings.arabicScale).sp,
                                        textDirection = TextDirection.Rtl,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                            }
                        } else {
                            Text(
                                card.front,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    Text(
                        if (revealed) card.sectionTitle else "Нажми на карточку, чтобы открыть ответ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (!revealed) {
            Button(
                onClick = { revealed = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Показать ответ")
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        progress.rateFlashCard(card.id, false)
                        repeatInSession += 1
                        index += 1
                        revealed = false
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Повторить")
                }
                Button(
                    onClick = {
                        progress.rateFlashCard(card.id, true)
                        knownInSession += 1
                        index += 1
                        revealed = false
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Знаю")
                }
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun FlashFinish(
    known: Int,
    repeat: Int,
    onAgain: () -> Unit,
    back: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Kicker("Сессия завершена")
        Text(
            "Карточки разобраны",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(known.toString(), "знаю", Modifier.weight(1f))
            StatCard(repeat.toString(), "повторить", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = back, modifier = Modifier.weight(1f)) {
                Text("Назад")
            }
            Button(onClick = onAgain, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Ещё раз")
            }
        }
    }
}

@Composable
fun EmptyStudyState(
    title: String,
    description: String,
    back: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = back) { Text("Назад") }
    }
}
