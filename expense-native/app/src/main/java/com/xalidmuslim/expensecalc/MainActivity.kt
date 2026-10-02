package com.xalidmuslim.expensecalc

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToInt

private data class Category(val id: Int, val name: String, val limitCents: Long = 0)
private data class MoneyTx(
    val id: Long,
    val type: String,
    val amountCents: Long,
    val categoryId: Int,
    val note: String,
    val epochDay: Long
)

private enum class Screen { HOME, HISTORY, STATS, SETTINGS }

private class ExpenseStore(context: Context) {
    private val prefs = context.getSharedPreferences("expense_native", Context.MODE_PRIVATE)

    fun loadCategories(): List<Category> {
        val raw = prefs.getString("categories", null)
        if (raw.isNullOrBlank()) {
            val defaults = listOf(
                Category(1, "Продукты", 0),
                Category(2, "Авто", 0),
                Category(3, "Дом", 0),
                Category(4, "Связь", 0),
                Category(5, "Здоровье", 0),
                Category(6, "Досуг", 0),
                Category(7, "Другое", 0)
            )
            saveCategories(defaults)
            return defaults
        }
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(Category(o.getInt("id"), o.getString("name"), o.optLong("limit", 0)))
                }
            }
        }.getOrElse { emptyList() }.ifEmpty {
            val defaults = listOf(Category(1, "Продукты"), Category(2, "Авто"), Category(3, "Дом"), Category(4, "Другое"))
            saveCategories(defaults)
            defaults
        }
    }

    fun saveCategories(items: List<Category>) {
        val array = JSONArray()
        items.forEach {
            array.put(JSONObject().put("id", it.id).put("name", it.name).put("limit", it.limitCents))
        }
        prefs.edit().putString("categories", array.toString()).apply()
    }

    fun loadTransactions(): List<MoneyTx> {
        val raw = prefs.getString("transactions", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        MoneyTx(
                            id = o.getLong("id"),
                            type = o.getString("type"),
                            amountCents = o.getLong("amount"),
                            categoryId = o.optInt("category", 0),
                            note = o.optString("note", ""),
                            epochDay = o.getLong("date")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveTransactions(items: List<MoneyTx>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("type", it.type)
                    .put("amount", it.amountCents)
                    .put("category", it.categoryId)
                    .put("note", it.note)
                    .put("date", it.epochDay)
            )
        }
        prefs.edit().putString("transactions", array.toString()).apply()
    }

    fun loadBudget(): Long = prefs.getLong("budget", 0)
    fun saveBudget(value: Long) = prefs.edit().putLong("budget", value).apply()
    fun loadDark(): Boolean = prefs.getBoolean("dark", false)
    fun saveDark(value: Boolean) = prefs.edit().putBoolean("dark", value).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ExpenseStore(this)
        setContent {
            var dark by remember { mutableStateOf(store.loadDark()) }
            ExpenseTheme(dark) {
                ExpenseApp(store = store, dark = dark, onDarkChanged = {
                    dark = it
                    store.saveDark(it)
                })
            }
        }
    }
}

@Composable
private fun ExpenseTheme(dark: Boolean, content: @Composable () -> Unit) {
    val light = lightColorScheme(
        primary = Color(0xFF4F6B5F),
        onPrimary = Color.White,
        secondary = Color(0xFF7A8F84),
        background = Color(0xFFF5F2EB),
        surface = Color(0xFFFFFCF7),
        surfaceVariant = Color(0xFFEAE6DE),
        error = Color(0xFFB3261E)
    )
    val darkScheme = darkColorScheme(
        primary = Color(0xFFAECDBD),
        secondary = Color(0xFF9FB5A9),
        background = Color(0xFF121714),
        surface = Color(0xFF1A211D),
        surfaceVariant = Color(0xFF27312C)
    )
    MaterialTheme(colorScheme = if (dark) darkScheme else light, content = content)
}

@Composable
private fun ExpenseApp(store: ExpenseStore, dark: Boolean, onDarkChanged: (Boolean) -> Unit) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var transactions by remember { mutableStateOf(store.loadTransactions()) }
    var categories by remember { mutableStateOf(store.loadCategories()) }
    var budget by remember { mutableLongStateOf(store.loadBudget()) }
    var editor by remember { mutableStateOf<MoneyTx?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    fun persistTransactions(value: List<MoneyTx>) {
        transactions = value
        store.saveTransactions(value)
    }
    fun persistCategories(value: List<Category>) {
        categories = value
        store.saveCategories(value)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screen.HOME,
                    onClick = { screen = Screen.HOME },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Главная") }
                )
                NavigationBarItem(
                    selected = screen == Screen.HISTORY,
                    onClick = { screen = Screen.HISTORY },
                    icon = { Icon(Icons.Default.ReceiptLong, null) },
                    label = { Text("История") }
                )
                NavigationBarItem(
                    selected = screen == Screen.STATS,
                    onClick = { screen = Screen.STATS },
                    icon = { Icon(Icons.Default.PieChart, null) },
                    label = { Text("Статистика") }
                )
                NavigationBarItem(
                    selected = screen == Screen.SETTINGS,
                    onClick = { screen = Screen.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Настройки") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editor = null
                showEditor = true
            }) { Icon(Icons.Default.Add, "Добавить") }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomeScreen(transactions, categories, budget)
                Screen.HISTORY -> HistoryScreen(
                    transactions = transactions,
                    categories = categories,
                    onEdit = {
                        editor = it
                        showEditor = true
                    },
                    onDelete = { tx ->
                        persistTransactions(transactions.filterNot { it.id == tx.id })
                    }
                )
                Screen.STATS -> StatsScreen(transactions, categories)
                Screen.SETTINGS -> SettingsScreen(
                    budget = budget,
                    onBudget = {
                        budget = it
                        store.saveBudget(it)
                    },
                    categories = categories,
                    onCategories = { persistCategories(it) },
                    dark = dark,
                    onDarkChanged = onDarkChanged
                )
            }
        }
    }

    if (showEditor) {
        TransactionDialog(
            existing = editor,
            categories = categories,
            onDismiss = { showEditor = false },
            onSave = { tx ->
                val next = if (editor == null) {
                    listOf(tx) + transactions
                } else {
                    transactions.map { if (it.id == tx.id) tx else it }
                }
                persistTransactions(next)
                showEditor = false
            }
        )
    }
}

@Composable
private fun HomeScreen(transactions: List<MoneyTx>, categories: List<Category>, budget: Long) {
    val month = LocalDate.now().withDayOfMonth(1)
    val monthItems = remember(transactions) {
        transactions.filter {
            val d = LocalDate.ofEpochDay(it.epochDay)
            d.year == month.year && d.month == month.month
        }
    }
    val expenses = monthItems.filter { it.type == "expense" }.sumOf { it.amountCents }
    val income = monthItems.filter { it.type == "income" }.sumOf { it.amountCents }
    val balance = income - expenses
    val spentFraction = if (budget > 0) (expenses.toFloat() / budget).coerceIn(0f, 1f) else 0f
    val categoryTotals = categories.map { c ->
        c to monthItems.filter { it.type == "expense" && it.categoryId == c.id }.sumOf { it.amountCents }
    }.filter { it.second > 0 }.sortedByDescending { it.second }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium)
        Text("Расходы", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(money(expenses), fontSize = 36.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Доходы", money(income), Modifier.weight(1f))
            MetricCard("Осталось", money(balance), Modifier.weight(1f))
        }

        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                BudgetRing(spentFraction, if (budget > 0) percent(expenses, budget) else null, Modifier.size(128.dp))
                Spacer(Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Месячный бюджет", style = MaterialTheme.typography.titleMedium)
                    Text(if (budget > 0) money(budget) else "Лимит не задан", style = MaterialTheme.typography.bodyLarge)
                    if (budget > 0) {
                        Text("Потрачено " + money(expenses), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (expenses <= budget) "Осталось " + money(budget - expenses)
                            else "Перерасход " + money(expenses - budget),
                            color = if (expenses <= budget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Text("Категории", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (categoryTotals.isEmpty()) {
            EmptyCard("Добавь первый расход — здесь появятся проценты по категориям.")
        } else {
            categoryTotals.forEach { (category, total) ->
                val share = if (expenses > 0) total.toFloat() / expenses else 0f
                CategoryShareCard(category, total, share)
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BudgetRing(fraction: Float, label: Int?, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = 18.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                style = Stroke(width, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (label == null) "—" else label.toString() + "%", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("бюджета", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryShareCard(category: Category, total: Long, share: Float) {
    val usedLimit = if (category.limitCents > 0) total.toFloat() / category.limitCents else 0f
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp))
                    Text(category.name, fontWeight = FontWeight.Medium)
                }
                Text(money(total), fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Доля всех расходов", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text((share * 100).roundToInt().toString() + "%", fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { share.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            if (category.limitCents > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Лимит: " + money(category.limitCents), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(percent(total, category.limitCents).toString() + "%", color = if (usedLimit > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { usedLimit.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (usedLimit > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    transactions: List<MoneyTx>,
    categories: List<Category>,
    onEdit: (MoneyTx) -> Unit,
    onDelete: (MoneyTx) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    val map = categories.associateBy { it.id }
    val filtered = transactions.filter { tx ->
        val categoryName = map[tx.categoryId]?.name.orEmpty()
        val typeOk = filter == "all" || tx.type == filter
        val qOk = query.isBlank() || tx.note.contains(query, true) || categoryName.contains(query, true)
        typeOk && qOk
    }.sortedWith(compareByDescending<MoneyTx> { it.epochDay }.thenByDescending { it.id })

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("История", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("Все") })
            FilterChip(selected = filter == "expense", onClick = { filter = "expense" }, label = { Text("Расходы") })
            FilterChip(selected = filter == "income", onClick = { filter = "income" }, label = { Text("Доходы") })
        }
        Spacer(Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            EmptyCard("Операций пока нет.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
                items(filtered, key = { it.id }) { tx ->
                    val category = map[tx.categoryId]?.name ?: if (tx.type == "income") "Доход" else "Без категории"
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onEdit(tx) }
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (tx.type == "income") Icons.Default.SouthWest else Icons.Default.NorthEast,
                                null,
                                tint = if (tx.type == "income") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(category, fontWeight = FontWeight.SemiBold)
                                Text(
                                    LocalDate.ofEpochDay(tx.epochDay).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("ru"))) +
                                            if (tx.note.isBlank()) "" else " · " + tx.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                (if (tx.type == "income") "+ " else "− ") + money(tx.amountCents),
                                fontWeight = FontWeight.Bold,
                                color = if (tx.type == "income") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { onDelete(tx) }) { Icon(Icons.Default.DeleteOutline, "Удалить") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(transactions: List<MoneyTx>, categories: List<Category>) {
    val today = LocalDate.now()
    val current = transactions.filter {
        val d = LocalDate.ofEpochDay(it.epochDay)
        it.type == "expense" && d.year == today.year && d.month == today.month
    }
    val total = current.sumOf { it.amountCents }
    val totals = categories.map { c ->
        c to current.filter { it.categoryId == c.id }.sumOf { it.amountCents }
    }.filter { it.second > 0 }.sortedByDescending { it.second }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Статистика", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Расходы за месяц", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(money(total), fontSize = 34.sp, fontWeight = FontWeight.Bold)
        if (totals.isEmpty()) {
            EmptyCard("Недостаточно данных для статистики.")
        } else {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CategoryDonut(totals, total, Modifier.size(190.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Распределение расходов", style = MaterialTheme.typography.titleMedium)
                }
            }
            totals.forEachIndexed { index, (category, amount) ->
                val share = amount.toFloat() / total
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.Medium)
                        LinearProgressIndicator(progress = { share }, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(money(amount), fontWeight = FontWeight.SemiBold)
                        Text((share * 100).roundToInt().toString() + "%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            DailyBars(current)
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun CategoryDonut(totals: List<Pair<Category, Long>>, total: Long, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            var start = -90f
            totals.forEachIndexed { index, pair ->
                val sweep = 360f * pair.second.toFloat() / total
                drawArc(
                    color = palette(index),
                    startAngle = start,
                    sweepAngle = (sweep - 2f).coerceAtLeast(0f),
                    useCenter = false,
                    style = Stroke(30.dp.toPx(), cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(totals.size.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("категорий", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DailyBars(items: List<MoneyTx>) {
    val sums = items.groupBy { LocalDate.ofEpochDay(it.epochDay).dayOfMonth }.mapValues { e -> e.value.sumOf { it.amountCents } }
    val max = sums.values.maxOrNull() ?: 1L
    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("По дням", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(130.dp), verticalAlignment = Alignment.Bottom) {
                val days = LocalDate.now().lengthOfMonth()
                for (day in 1..days) {
                    val amount = sums[day] ?: 0L
                    val fraction = amount.toFloat() / max
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (amount > 0) 0.9f else 0.12f),
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                            modifier = Modifier.fillMaxWidth(0.58f).fillMaxHeight(if (amount > 0) fraction.coerceAtLeast(0.04f) else 0.02f)
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    budget: Long,
    onBudget: (Long) -> Unit,
    categories: List<Category>,
    onCategories: (List<Category>) -> Unit,
    dark: Boolean,
    onDarkChanged: (Boolean) -> Unit
) {
    var budgetText by remember(budget) { mutableStateOf(if (budget > 0) plainMoney(budget) else "") }
    var newCategory by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Бюджет на месяц", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Сумма, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Button(onClick = { parseMoney(budgetText)?.let(onBudget) }) { Text("Сохранить бюджет") }
            }
        }
        Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Тёмная тема", fontWeight = FontWeight.SemiBold)
                    Text("Переключается сразу", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = dark, onCheckedChange = onDarkChanged)
            }
        }

        Text("Категории и лимиты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        categories.forEach { category ->
            var limitText by remember(category.id, category.limitCents) {
                mutableStateOf(if (category.limitCents > 0) plainMoney(category.limitCents) else "")
            }
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(category.name, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Лимит, ₽") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = {
                        val limit = if (limitText.isBlank()) 0 else parseMoney(limitText) ?: return@TextButton
                        onCategories(categories.map { if (it.id == category.id) it.copy(limitCents = limit) else it })
                    }) { Text("Сохранить лимит") }
                }
            }
        }

        Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Новая категория", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val name = newCategory.trim()
                        if (name.isNotEmpty()) {
                            val id = (categories.maxOfOrNull { it.id } ?: 0) + 1
                            onCategories(categories + Category(id, name))
                            newCategory = ""
                        }
                    },
                    enabled = newCategory.isNotBlank()
                ) { Text("Добавить") }
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun TransactionDialog(
    existing: MoneyTx?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (MoneyTx) -> Unit
) {
    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: "expense") }
    var amount by remember(existing?.id) { mutableStateOf(existing?.let { plainMoney(it.amountCents) } ?: "") }
    var categoryId by remember(existing?.id) { mutableIntStateOf(existing?.categoryId ?: (categories.firstOrNull()?.id ?: 0)) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var date by remember(existing?.id) { mutableStateOf(LocalDate.ofEpochDay(existing?.epochDay ?: LocalDate.now().toEpochDay()).toString()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Новая операция" else "Редактирование") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "expense", onClick = { type = "expense" }, label = { Text("Расход") })
                    FilterChip(selected = type == "income", onClick = { type = "income" }, label = { Text("Доход") })
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; error = "" },
                    label = { Text("Сумма, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (type == "expense") {
                    Box {
                        OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(categories.firstOrNull { it.id == categoryId }?.name ?: "Выбрать категорию")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        categoryId = c.id
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it; error = "" },
                    label = { Text("Дата ГГГГ-ММ-ДД") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val cents = parseMoney(amount)
                val parsedDate = try { LocalDate.parse(date) } catch (_: DateTimeParseException) { null }
                when {
                    cents == null || cents <= 0 -> error = "Укажи корректную сумму"
                    parsedDate == null -> error = "Дата должна быть в формате ГГГГ-ММ-ДД"
                    type == "expense" && categories.none { it.id == categoryId } -> error = "Выбери категорию"
                    else -> onSave(
                        MoneyTx(
                            id = existing?.id ?: System.currentTimeMillis(),
                            type = type,
                            amountCents = cents,
                            categoryId = if (type == "expense") categoryId else 0,
                            note = note.trim(),
                            epochDay = parsedDate.toEpochDay()
                        )
                    )
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun EmptyCard(text: String) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun palette(index: Int): Color {
    val colors = listOf(
        Color(0xFF587A69), Color(0xFF9B735A), Color(0xFF6D7894), Color(0xFFA48756),
        Color(0xFF7C6B8E), Color(0xFF5E8790), Color(0xFF8A7265), Color(0xFF72825D)
    )
    return colors[index % colors.size]
}

private fun money(cents: Long): String {
    val nf = NumberFormat.getNumberInstance(Locale("ru", "RU"))
    nf.maximumFractionDigits = if (cents % 100L == 0L) 0 else 2
    nf.minimumFractionDigits = 0
    return nf.format(cents / 100.0) + " ₽"
}

private fun plainMoney(cents: Long): String {
    val value = BigDecimal(cents).divide(BigDecimal(100))
    return value.stripTrailingZeros().toPlainString()
}

private fun parseMoney(text: String): Long? {
    val clean = text.trim().replace(" ", "").replace(",", ".")
    if (clean.isBlank()) return null
    return runCatching {
        BigDecimal(clean).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()
}

private fun percent(part: Long, whole: Long): Int =
    if (whole <= 0) 0 else ((part.toDouble() / whole.toDouble()) * 100.0).roundToInt()
