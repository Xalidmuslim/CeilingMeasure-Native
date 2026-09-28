package com.xalid.meditsinaproroka.nativeapp

import android.content.Context
import android.graphics.Typeface
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs

val WebSansFont = FontFamily(Font(R.font.manrope_variable))
val WebModernFont = FontFamily(Font(R.font.inter_variable))
val WebLiterataFont = FontFamily(Font(R.font.literata_variable))
val WebSerifFont = FontFamily(
    Font(R.font.pt_serif_regular, FontWeight.Normal),
    Font(R.font.pt_serif_bold, FontWeight.Bold),
)
val WebClassicFont = WebSerifFont

fun readerComposeFontFamily(value: String): FontFamily = when (value) {
    "classic" -> WebLiterataFont
    "modern" -> WebModernFont
    else -> WebSerifFont
}

fun readerTypeface(context: Context, value: String): Typeface {
    val font = when (value) {
        "classic" -> R.font.literata_variable
        "modern" -> R.font.inter_variable
        else -> R.font.pt_serif_regular
    }
    return ResourcesCompat.getFont(context, font) ?: Typeface.DEFAULT
}

@Composable
fun WebHeader(
    title: String,
    subtitle: String? = null,
    back: (() -> Unit)? = null,
    settings: (() -> Unit)? = null,
) {
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    if (back != null) {
                        IconButton(onClick = back) {
                            Text("←", fontFamily = WebSansFont, fontSize = 24.sp)
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        title,
                        fontFamily = WebSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 21.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            subtitle,
                            fontFamily = WebSansFont,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    if (settings != null) {
                        IconButton(onClick = settings) {
                            Text("⚙", fontSize = 22.sp)
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun WebHomeHeader(
    title: String,
    author: String,
    onSettings: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.medicine_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontFamily = WebSansFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 23.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        author,
                        fontFamily = WebSansFont,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Настройки",
                        modifier = Modifier.size(27.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f))
        }
    }
}

@Composable
fun WebHomeScreen(
    book: BookData,
    store: AppStore,
    modifier: Modifier,
    navigate: (Route) -> Unit,
) {
    val chaptersById = book.chapters.associateBy { it.id }
    val last = store.lastChapterId?.let(chaptersById::get)
    val completed = book.chapters.count { chapter ->
        val index = store.progress[chapter.id]
        index != null && chapter.blocks.isNotEmpty() &&
            index.toFloat() >= ((chapter.blocks.size - 1).toFloat() * 0.96f)
    }
    val pct = if (book.chapters.isEmpty()) 0 else
        ((completed * 100f) / book.chapters.size).toInt().coerceIn(0, 100)
    val quickCollections = book.collections.take(4)

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        WebHomeHeader(book.title, book.author) { navigate(Route.Settings) }

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 1.dp, vertical = 2.dp)) {
                    Text(
                        "Книга, разбитая на главы,\nтемы и средства",
                        fontFamily = WebLiterataFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 29.sp,
                        lineHeight = 33.sp,
                        letterSpacing = (-0.25).sp,
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "Полный русский текст с поиском, заметками, источниками и офлайн-доступом.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = WebSansFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp,
                    )
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 1.dp,
                ) {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(9.dp),
                            ) {
                                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(9.dp))
                            Text(
                                "Продолжить чтение",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = WebSansFont,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                shape = CircleShape,
                            ) {
                                Text(
                                    "$pct%",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = WebSansFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }

                        Text(
                            if (last != null) "Глава: ${last.title}" else "Начать с первой главы",
                            fontFamily = WebSansFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                        )

                        Box(
                            Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.32f))
                        ) {
                            if (pct > 0) {
                                Box(
                                    Modifier.fillMaxHeight().fillMaxWidth(pct / 100f)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val chapter = last ?: book.chapters.firstOrNull()
                                if (chapter != null) navigate(Route.Reader(chapter.id, resume = last != null))
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(11.dp),
                        ) {
                            Text(
                                if (last != null) "Продолжить →" else "Начать чтение →",
                                fontFamily = WebSansFont,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    onClick = { navigate(Route.Search) },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(25.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Поиск по всей книге",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = WebSansFont,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            item {
                Surface(
                    onClick = { navigate(Route.Treatments) },
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(11.dp)) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "✚",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Как лечили / что применялось",
                                fontFamily = WebSansFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Состояние → средства → полный текст",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = WebSansFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WebQuickCard("▤", "Читать книгу", "${book.chapters.size} глав", Modifier.weight(1f)) {
                        navigate(Route.Book)
                    }
                    WebQuickCard("▦", "Темы", "${book.topics.size} разделов", Modifier.weight(1f)) {
                        navigate(Route.Topics)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WebQuickCard("⚗", "Средства", "${book.remedies.size} позиций", Modifier.weight(1f)) {
                        navigate(Route.Remedies)
                    }
                    WebQuickCard("★", "Закладки", "${store.bookmarks.size} сохранено", Modifier.weight(1f)) {
                        navigate(Route.Bookmarks)
                    }
                }
            }

            if (quickCollections.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Быстрые подборки",
                            modifier = Modifier.weight(1f),
                            fontFamily = WebSansFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                        )
                        TextButton(onClick = { navigate(Route.Collections) }) {
                            Text("Все", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                for (rowIndex in 0 until ((quickCollections.size + 1) / 2)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val first = quickCollections.getOrNull(rowIndex * 2)
                            val second = quickCollections.getOrNull(rowIndex * 2 + 1)
                            if (first != null) {
                                WebCollectionCard(first, Modifier.weight(1f)) {
                                    navigate(Route.CollectionDetail(first.id))
                                }
                            }
                            if (second != null) {
                                WebCollectionCard(second, Modifier.weight(1f)) {
                                    navigate(Route.CollectionDetail(second.id))
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Продолжить изучение",
                        fontFamily = WebSansFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WebChip("Словарь терминов") { navigate(Route.Glossary) }
                        WebChip("Хадисы и источники") { navigate(Route.Hadiths) }
                        WebChip("Мои заметки") { navigate(Route.Notes) }
                        WebChip("Офлайн-копия") { navigate(Route.Offline) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebQuickCard(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 116.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(9.dp),
                ) {
                    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                        Text(
                            icon,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = WebSansFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.height(11.dp))
            Text(
                title,
                fontFamily = WebSansFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = WebSansFont,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun WebCollectionCard(
    collection: BookCollection,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 152.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                collection.title,
                fontFamily = WebSerifFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                collection.description,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${collection.chapterIds.size} глав →",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun WebChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp)
    }
}

private data class MoreItem(
    val icon: String,
    val title: String,
    val subtitle: String,
    val route: Route,
)

@Composable
fun WebMoreScreen(modifier: Modifier, navigate: (Route) -> Unit) {
    val rows = listOf(
        MoreItem("▤", "Оглавление книги", "111 глав в оригинальном порядке", Route.Book),
        MoreItem("✚", "Как лечили / что применялось", "Состояния и методы из лечебных глав", Route.Treatments),
        MoreItem("▦", "Быстрые подборки", "Головная боль, сон, тревога, рукъя и другое", Route.Collections),
        MoreItem("⚗", "Справочник средств", "Переходы к местам полного текста", Route.Remedies),
        MoreItem("Aa", "Словарь терминов", "Рукъя, кыст, тальбина и другое", Route.Glossary),
        MoreItem("≡", "Хадисы и источники", "Источники, указанные в тексте", Route.Hadiths),
        MoreItem("✎", "Мои заметки", "Выделения и личные записи", Route.Notes),
        MoreItem("◷", "История чтения", "Недавно открытые главы", Route.History),
        MoreItem("↓", "Офлайн-копия", "Книга уже сохранена внутри приложения", Route.Offline),
        MoreItem("⚙", "Настройки чтения", "Шрифт, интервал и оформление", Route.Settings),
        MoreItem("i", "О приложении", "Назначение и границы использования", Route.About),
        MoreItem("§", "Об источнике текста", "Структура и полнота локального издания", Route.Source),
    )

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        WebHeader("Ещё", settings = { navigate(Route.Settings) })
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 26.dp),
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column {
                        rows.forEachIndexed { index, row ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { navigate(row.route) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(11.dp),
                                ) {
                                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            row.icon,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontFamily = if (row.icon == "Aa") WebSerifFont else WebSansFont,
                                            fontSize = if (row.icon == "Aa") 20.sp else 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(row.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        row.subtitle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text("→", fontSize = 18.sp)
                            }
                            if (index != rows.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val readerSizeOptions = listOf(
    16f to "Малый",
    17f to "Обычный",
    19f to "Крупный",
    21f to "Очень крупный",
)

private val readerLineOptions = listOf(
    1.35f to "Плотный",
    1.48f to "Обычный",
    1.60f to "Свободный",
)

private fun closestSizeIndex(value: Float): Int =
    readerSizeOptions.indices.minByOrNull { abs(readerSizeOptions[it].first - value) } ?: 1

private fun closestLine(value: Float): Float =
    readerLineOptions.minByOrNull { abs(it.first - value) }?.first ?: 1.48f

@Composable
fun WebSettingsScreen(store: AppStore, modifier: Modifier, back: () -> Unit) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        WebHeader("Настройки чтения", back = back)
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Все изменения ниже сразу видны в этом образце.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = WebSansFont,
                    fontSize = 12.sp,
                )
            }
            item { WebSettingsPreview(store.settings) }

            item {
                WebSettingCard("Тема") {
                    WebChoiceRow(
                        options = listOf(
                            "system" to "Система",
                            "light" to "Светлая",
                            "dark" to "Тёмная",
                        ),
                        selected = store.settings.theme,
                    ) { value ->
                        store.updateSettings { it.copy(theme = value) }
                    }
                }
            }

            item {
                WebSettingCard("Шрифт книги") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WebFontOption("Книжный", "PT Serif", "book", store.settings.fontFamily) {
                            store.updateSettings { it.copy(fontFamily = "book") }
                        }
                        WebFontOption("Literata", "Literata", "classic", store.settings.fontFamily) {
                            store.updateSettings { it.copy(fontFamily = "classic") }
                        }
                        WebFontOption("Современный", "Inter", "modern", store.settings.fontFamily) {
                            store.updateSettings { it.copy(fontFamily = "modern") }
                        }
                    }
                }
            }

            item {
                WebSettingCard("Размер текста") {
                    val selected = readerSizeOptions[closestSizeIndex(store.settings.fontSizeSp)].first.toString()
                    WebChoiceRow(
                        options = readerSizeOptions.map { it.first.toString() to it.second },
                        selected = selected,
                    ) { value ->
                        store.updateSettings { it.copy(fontSizeSp = value.toFloat()) }
                    }
                }
            }

            item {
                WebSettingCard("Межстрочный интервал") {
                    WebChoiceRow(
                        options = readerLineOptions.map { it.first.toString() to it.second },
                        selected = closestLine(store.settings.lineSpacing).toString(),
                    ) { value ->
                        store.updateSettings { it.copy(lineSpacing = value.toFloat()) }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Метки медицины эпохи", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Показывать редакционную маркировку",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = {
                                store.updateSettings {
                                    it.copy(showHistoricalLabels = !it.showHistoricalLabels)
                                }
                            },
                            shape = RoundedCornerShape(9.dp),
                        ) {
                            Text(if (store.settings.showHistoricalLabels) "Вкл." else "Выкл.")
                        }
                    }
                }
            }

            item {
                WebSettingCard("Данные чтения") {
                    OutlinedButton(
                        onClick = { store.clearProgress() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Сбросить прогресс и историю")
                    }
                }
            }
        }
    }
}

@Composable
fun WebReaderSettingsSheet(store: AppStore, onDone: () -> Unit) {
    val settings = store.settings
    Column(
        Modifier.fillMaxWidth()
            .heightIn(max = 720.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    "НАСТРОЙКИ ЧТЕНИЯ",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    "Текст прямо в этой главе",
                    fontFamily = WebSerifFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                )
            }
            IconButton(onClick = onDone) { Text("×", fontSize = 24.sp) }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(
                "Так будет выглядеть основной текст книги после изменения настроек.",
                modifier = Modifier.padding(14.dp),
                fontFamily = readerComposeFontFamily(settings.fontFamily),
                fontSize = settings.fontSizeSp.sp,
                lineHeight = (settings.fontSizeSp * settings.lineSpacing).sp,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text("Шрифт", fontWeight = FontWeight.SemiBold)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WebFontOption("Книжный", "PT Serif", "book", settings.fontFamily) {
                store.updateSettings { it.copy(fontFamily = "book") }
            }
            WebFontOption("Literata", "Literata", "classic", settings.fontFamily) {
                store.updateSettings { it.copy(fontFamily = "classic") }
            }
            WebFontOption("Современный", "Inter", "modern", settings.fontFamily) {
                store.updateSettings { it.copy(fontFamily = "modern") }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text("Размер", fontWeight = FontWeight.SemiBold)

        val sizeIndex = closestSizeIndex(settings.fontSizeSp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    val next = readerSizeOptions[(sizeIndex - 1).coerceAtLeast(0)].first
                    store.updateSettings { it.copy(fontSizeSp = next) }
                },
                modifier = Modifier.width(58.dp),
            ) { Text("A−") }

            Text(
                readerSizeOptions[sizeIndex].second,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedButton(
                onClick = {
                    val next = readerSizeOptions[(sizeIndex + 1).coerceAtMost(readerSizeOptions.lastIndex)].first
                    store.updateSettings { it.copy(fontSizeSp = next) }
                },
                modifier = Modifier.width(58.dp),
            ) { Text("A+") }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text("Интервал", fontWeight = FontWeight.SemiBold)

        WebChoiceRow(
            options = readerLineOptions.map { it.first.toString() to it.second },
            selected = closestLine(settings.lineSpacing).toString(),
        ) { value ->
            store.updateSettings { it.copy(lineSpacing = value.toFloat()) }
        }

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("Готово")
        }
    }
}

@Composable
private fun WebSettingsPreview(settings: ReaderSettings) {
    Surface(
        modifier = Modifier.animateContentSize(animationSpec = tween(160)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                "ПРЕДПРОСМОТР",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Руководство Пророка ﷺ в лечении",
                fontFamily = readerComposeFontFamily(settings.fontFamily),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "Полезное знание требует спокойного и внимательного чтения. Здесь сразу видно выбранный шрифт, размер текста и межстрочный интервал.",
                fontFamily = readerComposeFontFamily(settings.fontFamily),
                fontSize = settings.fontSizeSp.sp,
                lineHeight = (settings.fontSizeSp * settings.lineSpacing).sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ) {
                    Text(
                        when (settings.fontFamily) {
                            "classic" -> "Literata"
                            "modern" -> "Inter"
                            else -> "PT Serif"
                        },
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                ) {
                    Text(
                        "${settings.fontSizeSp.toInt()} sp · ×${"%.2f".format(settings.lineSpacing)}",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun WebSettingCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.animateContentSize(animationSpec = tween(160)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                fontFamily = WebSerifFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            content()
        }
    }
}

@Composable
private fun WebChoiceRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            val containerColor by animateColorAsState(
                targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                animationSpec = tween(140),
                label = "choiceContainer",
            )
            val contentColor by animateColorAsState(
                targetValue = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(140),
                label = "choiceContent",
            )
            val borderColor by animateColorAsState(
                targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                animationSpec = tween(140),
                label = "choiceBorder",
            )
            Surface(
                onClick = { onSelect(value) },
                color = containerColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, borderColor),
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun WebFontOption(
    label: String,
    fontName: String,
    value: String,
    selected: String,
    onClick: () -> Unit,
) {
    val active = value == selected
    val containerColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surface,
        animationSpec = tween(150),
        label = "fontContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(150),
        label = "fontBorder",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier.animateContentSize(animationSpec = tween(160)),
        color = containerColor,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(if (active) 2.dp else 1.dp, borderColor),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp)) {
            Text(
                label,
                fontFamily = readerComposeFontFamily(value),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Text(
                fontName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Пример текста книги",
                fontFamily = readerComposeFontFamily(value),
                fontSize = 16.sp,
            )
        }
    }
}
