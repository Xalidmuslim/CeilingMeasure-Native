package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.xalidmuslim.arabicgrammar.data.ReaderSettings

private val Light = lightColorScheme(
    primary = Color(0xFF17384A),
    onPrimary = Color(0xFFFFFAF2),
    secondary = Color(0xFFA95F3B),
    onSecondary = Color(0xFFFFFAF2),
    background = Color(0xFFF4EFE6),
    onBackground = Color(0xFF20262D),
    surface = Color(0xFFFFFAF2),
    onSurface = Color(0xFF20262D),
    surfaceVariant = Color(0xFFEEE6D9),
    outline = Color(0xFFD8CDBD),
    error = Color(0xFF9C463E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFD5E2E9),
    onPrimary = Color(0xFF13222B),
    secondary = Color(0xFFD28A62),
    onSecondary = Color(0xFF1F1713),
    background = Color(0xFF11161A),
    onBackground = Color(0xFFEFE9E0),
    surface = Color(0xFF1B2227),
    onSurface = Color(0xFFF0EBE2),
    surfaceVariant = Color(0xFF222A30),
    outline = Color(0xFF30383E),
    error = Color(0xFFD77A71),
)

private val ArabicSegment = Regex(
    "[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF]+" +
        "(?:\\s+[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF]+)*"
)

fun russianFont(settings: ReaderSettings): FontFamily = when (settings.fontChoice) {
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

fun arabicFont(settings: ReaderSettings): FontFamily = when (settings.arabicFontChoice) {
    "sans" -> FontFamily.SansSerif
    "system" -> FontFamily.Default
    else -> FontFamily.Serif
}

fun styledMixedText(text: String, settings: ReaderSettings): AnnotatedString =
    buildAnnotatedString {
        var cursor = 0
        ArabicSegment.findAll(text).forEach { match ->
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }
            pushStyle(SpanStyle(fontFamily = arabicFont(settings)))
            append("\u2067")
            append(match.value)
            append("\u2069")
            pop()
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }

@Composable
fun ArabicGrammarTheme(
    settings: ReaderSettings,
    darkSystem: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = settings.theme == "dark" || (settings.theme == "system" && darkSystem)
    val base = russianFont(settings)
    val scale = settings.textScale
    val typography = MaterialTheme.typography.copy(
        bodyLarge = TextStyle(
            fontFamily = base,
            fontSize = (15.5f * scale).sp,
            lineHeight = (22.5f * scale).sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = base,
            fontSize = (14f * scale).sp,
            lineHeight = (20.5f * scale).sp,
        ),
        bodySmall = TextStyle(
            fontFamily = base,
            fontSize = (12.5f * scale).sp,
            lineHeight = (18f * scale).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = (24f * scale).sp,
            lineHeight = (28f * scale).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = (19.5f * scale).sp,
            lineHeight = (23f * scale).sp,
        ),
        titleSmall = TextStyle(
            fontFamily = base,
            fontSize = (15f * scale).sp,
            lineHeight = (19.5f * scale).sp,
        ),
        labelLarge = TextStyle(
            fontFamily = base,
            fontSize = (13f * scale).sp,
        ),
        labelMedium = TextStyle(
            fontFamily = base,
            fontSize = (11.5f * scale).sp,
        ),
    )

    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = typography,
        content = content,
    )
}
