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
    primaryContainer = Color(0xFFE4ECE1),
    onPrimaryContainer = Color(0xFF263A2B),

    secondary = Color(0xFF8A684B),
    onSecondary = Color(0xFFFFFAF2),
    secondaryContainer = Color(0xFFE6EEDF),
    onSecondaryContainer = Color(0xFF354234),

    tertiary = Color(0xFF66775F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8EFE3),
    onTertiaryContainer = Color(0xFF32402F),

    background = Color(0xFFF5F1E8),
    onBackground = Color(0xFF20262D),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF20262D),
    surfaceVariant = Color(0xFFF0F4EC),
    onSurfaceVariant = Color(0xFF596159),

    surfaceDim = Color(0xFFE7ECE3),
    surfaceBright = Color(0xFFFFFCF7),
    surfaceContainerLowest = Color(0xFFFFFEFB),
    surfaceContainerLow = Color(0xFFF4F7F0),
    surfaceContainer = Color(0xFFEDF2E9),
    surfaceContainerHigh = Color(0xFFE8EEE4),
    surfaceContainerHighest = Color(0xFFE1E9DD),

    outline = Color(0xFFC4D0C0),
    outlineVariant = Color(0xFFD8E2D5),

    error = Color(0xFF9C463E),
    errorContainer = Color(0xFFF4DFDC),
    onErrorContainer = Color(0xFF4A201C),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFD5E2E9),
    onPrimary = Color(0xFF13222B),
    primaryContainer = Color(0xFF304136),
    onPrimaryContainer = Color(0xFFDDE9DE),

    secondary = Color(0xFFD0B397),
    onSecondary = Color(0xFF2B2119),
    secondaryContainer = Color(0xFF303A30),
    onSecondaryContainer = Color(0xFFE2E9DF),

    tertiary = Color(0xFFB8C8B2),
    onTertiary = Color(0xFF223021),
    tertiaryContainer = Color(0xFF2C382B),
    onTertiaryContainer = Color(0xFFDCE7D8),

    background = Color(0xFF11161A),
    onBackground = Color(0xFFEFE9E0),
    surface = Color(0xFF1B2220),
    onSurface = Color(0xFFF0EBE2),
    surfaceVariant = Color(0xFF252F28),
    onSurfaceVariant = Color(0xFFC7CEC6),

    surfaceDim = Color(0xFF111613),
    surfaceBright = Color(0xFF303834),
    surfaceContainerLowest = Color(0xFF0D120F),
    surfaceContainerLow = Color(0xFF18201A),
    surfaceContainer = Color(0xFF1D271F),
    surfaceContainerHigh = Color(0xFF243027),
    surfaceContainerHighest = Color(0xFF2B382E),

    outline = Color(0xFF526156),
    outlineVariant = Color(0xFF39463C),

    error = Color(0xFFD77A71),
    errorContainer = Color(0xFF4B2522),
    onErrorContainer = Color(0xFFFFDAD5),
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
    "naskh", "serif" -> FontFamily.Serif
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
