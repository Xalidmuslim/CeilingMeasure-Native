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
    primaryContainer = Color(0xFFE3DFCF),
    onPrimaryContainer = Color(0xFF373428),

    secondary = Color(0xFF8A684B),
    onSecondary = Color(0xFFFFFAF2),
    secondaryContainer = Color(0xFFDED8C3),
    onSecondaryContainer = Color(0xFF403B2E),

    tertiary = Color(0xFF756F59),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE7E1D0),
    onTertiaryContainer = Color(0xFF403B2E),

    background = Color(0xFFF5F1E8),
    onBackground = Color(0xFF20262D),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF20262D),
    surfaceVariant = Color(0xFFF0ECDF),
    onSurfaceVariant = Color(0xFF625E50),

    surfaceDim = Color(0xFFE8E2D4),
    surfaceBright = Color(0xFFFFFCF7),
    surfaceContainerLowest = Color(0xFFFFFEFB),
    surfaceContainerLow = Color(0xFFF5F1E7),
    surfaceContainer = Color(0xFFEDE7D8),
    surfaceContainerHigh = Color(0xFFE6DFCE),
    surfaceContainerHighest = Color(0xFFDDD5C2),

    outline = Color(0xFFB7AE91),
    outlineVariant = Color(0xFFD1C8AF),

    error = Color(0xFF9C463E),
    errorContainer = Color(0xFFF4DFDC),
    onErrorContainer = Color(0xFF4A201C),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFD5E2E9),
    onPrimary = Color(0xFF13222B),
    primaryContainer = Color(0xFF464437),
    onPrimaryContainer = Color(0xFFE8E2D5),

    secondary = Color(0xFFD0B397),
    onSecondary = Color(0xFF2B2119),
    secondaryContainer = Color(0xFF403D31),
    onSecondaryContainer = Color(0xFFE8E2D5),

    tertiary = Color(0xFFC3BA9E),
    onTertiary = Color(0xFF223021),
    tertiaryContainer = Color(0xFF3C392E),
    onTertiaryContainer = Color(0xFFE9E2D2),

    background = Color(0xFF11161A),
    onBackground = Color(0xFFEFE9E0),
    surface = Color(0xFF201F1A),
    onSurface = Color(0xFFF0EBE2),
    surfaceVariant = Color(0xFF2E2C25),
    onSurfaceVariant = Color(0xFFCFC7B6),

    surfaceDim = Color(0xFF151410),
    surfaceBright = Color(0xFF38352C),
    surfaceContainerLowest = Color(0xFF100F0C),
    surfaceContainerLow = Color(0xFF1C1B16),
    surfaceContainer = Color(0xFF242219),
    surfaceContainerHigh = Color(0xFF2C291F),
    surfaceContainerHighest = Color(0xFF343024),

    outline = Color(0xFF665F4A),
    outlineVariant = Color(0xFF494432),

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
