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
    primaryContainer = Color(0xFFDDE6EB),
    onPrimaryContainer = Color(0xFF2B3942),

    secondary = Color(0xFF627889),
    onSecondary = Color(0xFFFFFAF2),
    secondaryContainer = Color(0xFFDDE5D7),
    onSecondaryContainer = Color(0xFF344236),

    tertiary = Color(0xFF74836F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD5E0D0),
    onTertiaryContainer = Color(0xFF344135),

    background = Color(0xFFF5F1E8),
    onBackground = Color(0xFF20262D),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF20262D),
    surfaceVariant = Color(0xFFE5EADF),
    onSurfaceVariant = Color(0xFF5A655A),

    surfaceDim = Color(0xFFE6EBEE),
    surfaceBright = Color(0xFFFFFCF7),
    surfaceContainerLowest = Color(0xFFFFFEFB),
    surfaceContainerLow = Color(0xFFF3F6F7),
    surfaceContainer = Color(0xFFEAF0F2),
    surfaceContainerHigh = Color(0xFFE3EAEE),
    surfaceContainerHighest = Color(0xFFDCE5E9),

    outline = Color(0xFFAAB9C2),
    outlineVariant = Color(0xFFC8D4DA),

    error = Color(0xFF9C463E),
    errorContainer = Color(0xFFF4DFDC),
    onErrorContainer = Color(0xFF4A201C),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFD5E2E9),
    onPrimary = Color(0xFF13222B),
    primaryContainer = Color(0xFF35434D),
    onPrimaryContainer = Color(0xFFE0E8ED),

    secondary = Color(0xFF9DB2C0),
    onSecondary = Color(0xFF2B2119),
    secondaryContainer = Color(0xFF344236),
    onSecondaryContainer = Color(0xFFE3E9DF),

    tertiary = Color(0xFFAFC0A9),
    onTertiary = Color(0xFF223021),
    tertiaryContainer = Color(0xFF3B493B),
    onTertiaryContainer = Color(0xFFE6ECE2),

    background = Color(0xFF11161A),
    onBackground = Color(0xFFEFE9E0),
    surface = Color(0xFF1D2428),
    onSurface = Color(0xFFF0EBE2),
    surfaceVariant = Color(0xFF2B372D),
    onSurfaceVariant = Color(0xFFC9D2C5),

    surfaceDim = Color(0xFF141A1D),
    surfaceBright = Color(0xFF303B41),
    surfaceContainerLowest = Color(0xFF0F1417),
    surfaceContainerLow = Color(0xFF192126),
    surfaceContainer = Color(0xFF202A30),
    surfaceContainerHigh = Color(0xFF26323A),
    surfaceContainerHighest = Color(0xFF2D3A43),

    outline = Color(0xFF5E7482),
    outlineVariant = Color(0xFF425662),

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
