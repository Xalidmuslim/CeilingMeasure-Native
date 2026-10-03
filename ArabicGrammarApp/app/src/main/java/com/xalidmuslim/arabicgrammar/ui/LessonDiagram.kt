package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xalidmuslim.arabicgrammar.data.BookSection
import com.xalidmuslim.arabicgrammar.data.ReaderSettings

@Composable
fun LessonDiagram(section: BookSection, settings: ReaderSettings) {
    val title = section.title.lowercase()

    when {
        title.contains("части речи") -> PartsOfSpeechDiagram()
        title.contains("склонение им") || title.contains("падеж") -> CasesDiagram(settings)
        section.chapter == "morphology" &&
            (title.contains("спряж") || title.contains("глагол")) -> VerbMapDiagram()
    }
}

@Composable
private fun DiagramShell(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Kicker("Схема для запоминания")
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PartsOfSpeechDiagram() {
    DiagramShell("Слово и три части речи") {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text("Слово", modifier = Modifier.padding(horizontal = 24.dp, vertical = 9.dp))
        }
        Text("↓", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Имя", "Глагол", "Частица").forEach { label ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CasesDiagram(settings: ReaderSettings) {
    DiagramShell("Три падежа") {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(
                "الرَّفْعُ\nим.",
                "الجَرُّ\nрод.",
                "النَّصْبُ\nвин.",
            ).forEach { label ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    Text(
                        styledMixedText(label, settings),
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun VerbMapDiagram() {
    DiagramShell("Как читать форму глагола") {
        val steps = listOf("Корень", "Порода", "Время", "Лицо и число")
        steps.forEachIndexed { index, step ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                color = if (index == 0)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    step,
                    modifier = Modifier.padding(vertical = 9.dp),
                    textAlign = TextAlign.Center,
                )
            }
            if (index != steps.lastIndex) {
                Text("↓", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
