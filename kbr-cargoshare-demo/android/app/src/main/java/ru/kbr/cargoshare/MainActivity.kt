package ru.kbr.cargoshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private val Dark = Color(0xFF172523)
private val Green = Color(0xFF174B3B)
private val Accent = Color(0xFFD8F29E)
private val Back = Color(0xFFF5F7F3)
private val Muted = Color(0xFF77817B)
private val Pale = Color(0xFFEAF1E9)
private val Line = Color(0xFFE6ECE5)
private val PriceFormat = NumberFormat.getNumberInstance(Locale("ru", "RU"))
private fun rubles(value: Int): String = PriceFormat.format(value) + " ₽"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(245, 247, 243)
        window.navigationBarColor = android.graphics.Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Green, onPrimary = Color.White,
                secondary = Accent, background = Back,
                surface = Color.White, onSurface = Dark)) {
                ClientV5App()
            }
        }
    }
}

@Composable
private fun CargoApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { DemoStore(context.applicationContext) }
    var fleet by remember { mutableStateOf(store.fleet()) }
    val orders = remember { mutableStateListOf<DemoOrder>().apply { addAll(store.orders()) } }
    var section by remember { mutableIntStateOf(0) }
    var editFleet by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf(fleet.firstOrNull { it.enabled }?.id ?: "") }
    var showRequest by remember { mutableStateOf(false) }
    var hours by remember { mutableIntStateOf(4) }
    var selectedDay by remember { mutableIntStateOf(0) }
    var selectedTime by remember { mutableStateOf("12:00") }

    val selected = fleet.firstOrNull { it.id == selectedId && it.enabled }
        ?: fleet.firstOrNull { it.enabled }

    Scaffold(
        containerColor = Back,
        bottomBar = {
            if (!editFleet) NavigationBar(
                containerColor = Color.White,
                tonalElevation = 2.dp,
                modifier = Modifier.height(77.dp)
            ) {
                val names = listOf("Главная", "Мои аренды", "Профиль")
                val icons = listOf(Icons.Rounded.Home, Icons.Rounded.ReceiptLong, Icons.Rounded.Person)
                names.forEachIndexed { idx, label ->
                    NavigationBarItem(
                        selected = section == idx,
                        onClick = { section = idx },
                        icon = { Icon(icons[idx], contentDescription = label, modifier = Modifier.size(23.dp)) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (section == idx) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Green,
                            selectedTextColor = Green,
                            indicatorColor = Pale,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                editFleet -> FleetEditor(
                    fleet = fleet,
                    onBack = { editFleet = false },
                    onChange = { updated ->
                        fleet = updated
                        store.saveFleet(updated)
                        if (selected == null || updated.none { it.id == selectedId && it.enabled })
                            selectedId = updated.firstOrNull { it.enabled }?.id ?: ""
                    }
                )
                section == 0 -> HomeScreen(
                    fleet = fleet.filter { it.enabled },
                    selectedId = selected?.id ?: "",
                    onSelect = { selectedId = it },
                    selectedDay = selectedDay,
                    onDay = { selectedDay = it },
                    selectedTime = selectedTime,
                    onTime = { selectedTime = it },
                    hours = hours,
                    onHours = { hours = it },
                    onRequest = { showRequest = true }
                )
                section == 1 -> OrdersScreen(
                    orders = orders,
                    onChange = { updated ->
                        val index = orders.indexOfFirst { it.id == updated.id }
                        if (index >= 0) {
                            orders[index] = updated
                            store.saveOrders(orders)
                        }
                    }
                )
                else -> ProfileScreen(onEditFleet = { editFleet = true })
            }
        }
    }

    if (showRequest && selected != null) {
        RequestSheet(
            vehicle = selected,
            hours = hours,
            day = selectedDay,
            time = selectedTime,
            onDismiss = { showRequest = false },
            onConfirm = {
                val newOrder = DemoOrder(
                    id = UUID.randomUUID().toString(),
                    vehicleId = selected.id,
                    vehicleTitle = selected.title,
                    pickup = selected.pickup,
                    start = listOf("Сегодня", "Завтра", "Послезавтра")[selectedDay] + " · " + selectedTime,
                    hours = hours,
                    price = RentalRules.fare(selected, hours)
                )
                orders.add(0, newOrder)
                store.saveOrders(orders)
                showRequest = false
                section = 1
            }
        )
    }
}

@Composable
private fun HomeScreen(
    fleet: List<DemoVehicle>,
    selectedId: String,
    onSelect: (String) -> Unit,
    selectedDay: Int,
    onDay: (Int) -> Unit,
    selectedTime: String,
    onTime: (String) -> Unit,
    hours: Int,
    onHours: (Int) -> Unit,
    onRequest: () -> Unit
) {
    val vehicle = fleet.firstOrNull { it.id == selectedId } ?: fleet.firstOrNull()
    Box(Modifier.fillMaxSize()) {
        DemoMap(fleet, selectedId, onSelect)
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White, shadowElevation = 3.dp,
                    shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalShipping, null, tint = Green, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("ГРУЗШЕР", color = Dark, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                                letterSpacing = 0.5.sp)
                            Text("Грузовой каршеринг · КБР", color = Muted, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.width(9.dp))
                Surface(color = Accent, shape = RoundedCornerShape(15.dp), shadowElevation = 2.dp) {
                    Text("ДЕМО", modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        fontWeight = FontWeight.Bold, color = Green, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(color = Color.White.copy(alpha = .96f), shape = RoundedCornerShape(13.dp)) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, null, tint = Muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Схема карты · реальные GPS и маршруты не подключены",
                        fontSize = 10.sp, color = Muted)
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(.68f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            shadowElevation = 15.dp
        ) {
            Column {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(40.dp).height(4.dp).background(Line, CircleShape))
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Выберите грузовик", fontWeight = FontWeight.ExtraBold,
                            color = Dark, fontSize = 22.sp, modifier = Modifier.weight(1f))
                        Text(fleet.size.toString() + " авто", color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Аренда без водителя · Нальчик и КБР", fontSize = 12.sp, color = Muted)
                    Spacer(Modifier.height(14.dp))
                    if (fleet.isEmpty()) {
                        Text("В демокаталоге нет доступных машин. Включите их в редакторе автопарка.",
                            color = Muted, fontSize = 14.sp)
                    } else {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            fleet.forEach { truck ->
                                VehicleChoice(
                                    truck = truck,
                                    selected = truck.id == vehicle?.id,
                                    onClick = { onSelect(truck.id) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    if (vehicle != null) {
                        Surface(color = Back, shape = RoundedCornerShape(16.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(35.dp).background(Pale, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Place, null, tint = Green, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Пункт получения", color = Muted, fontSize = 11.sp)
                                    Text(vehicle.pickup, color = Dark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Icon(Icons.Rounded.CheckCircle, null, tint = Green, modifier = Modifier.size(19.dp))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Когда понадобится?", color = Dark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("Сегодня", "Завтра", "Послезавтра").forEachIndexed { idx, label ->
                                ChoicePill(label, selectedDay == idx) { onDay(idx) }
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("09:00", "12:00", "15:00", "18:00").forEach { time ->
                                ChoicePill(time, selectedTime == time) { onTime(time) }
                            }
                        }
                        Spacer(Modifier.height(13.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Срок аренды", color = Dark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.weight(1f))
                            Text("Демо-тариф", fontSize = 11.sp, color = Muted)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RentalRules.allowedHours.forEach { h ->
                                Box(Modifier.weight(1f)) {
                                    ChoicePill(
                                        if (h == 24) "Сутки" else h.toString() + " ч",
                                        selected = h == hours,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { onHours(h) }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
                HorizontalDivider(color = Line)
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Стоимость аренды", color = Muted, fontSize = 11.sp)
                        Text(if (vehicle == null) "—" else rubles(RentalRules.fare(vehicle, hours)),
                            color = Dark, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
                        Text("Залог " + rubles(RentalRules.DEMO_DEPOSIT) + " · не списывается",
                            color = Muted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = onRequest, enabled = vehicle != null,
                        shape = RoundedCornerShape(17.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        modifier = Modifier.height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                        Text("Продолжить", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleChoice(truck: DemoVehicle, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Pale else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 1.5.dp else 1.dp, if (selected) Green else Line
        ),
        modifier = Modifier.width(174.dp).height(120.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.LocalShipping, null, tint = Green, modifier = Modifier.size(31.dp))
                Spacer(Modifier.weight(1f))
                if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(truck.kind, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Dark, maxLines = 1)
            Text(truck.payload + " · " + truck.volume, fontSize = 11.sp, color = Muted)
            Spacer(Modifier.height(4.dp))
            Text(rubles(truck.hourPrice) + "/час", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Green)
        }
    }
}

@Composable
private fun ChoicePill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(11.dp),
        color = if (selected) Green else Back,
        modifier = modifier) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else Dark, fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1)
        }
    }
}

@Composable
private fun DemoMap(fleet: List<DemoVehicle>, selectedId: String, onSelect: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFE8EEE8))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Color(0xFFE7EEE8))
            drawRect(Color(0xFFD8E9D8), topLeft = Offset(w * .64f, h * .01f),
                size = androidx.compose.ui.geometry.Size(w * .3f, h * .31f))
            drawRect(Color(0xFFD6E8D3), topLeft = Offset(w * .04f, h * .20f),
                size = androidx.compose.ui.geometry.Size(w * .2f, h * .15f))
            val roads = listOf(
                Path().apply { moveTo(-w*.1f,h*.12f); cubicTo(w*.35f,h*.18f,w*.65f,h*.04f,w*1.1f,h*.11f) },
                Path().apply { moveTo(-w*.1f,h*.35f); cubicTo(w*.3f,h*.3f,w*.55f,h*.44f,w*1.1f,h*.38f) },
                Path().apply { moveTo(w*.23f,-h*.1f); cubicTo(w*.4f,h*.18f,w*.36f,h*.34f,w*.25f,h*.72f) },
                Path().apply { moveTo(w*.78f,-h*.1f); cubicTo(w*.71f,h*.22f,w*.76f,h*.39f,w*.6f,h*.64f) },
                Path().apply { moveTo(-w*.1f,h*.23f); lineTo(w*1.1f,h*.52f) }
            )
            roads.forEach { p ->
                drawPath(p, Color(0xFFCEDBD1), style = Stroke(width = 35.dp.toPx(), cap = StrokeCap.Round))
                drawPath(p, Color(0xFFFCFCF9), style = Stroke(width = 29.dp.toPx(), cap = StrokeCap.Round))
            }
            drawLine(Color(0xFFB7CCBC), Offset(-w*.1f,h*.41f), Offset(w*1.1f,h*.21f), strokeWidth=3.dp.toPx())
        }
        Text("НАЛЬЧИК", color = Color(0xFF8EA497), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp, modifier = Modifier.offset(x = maxWidth * .39f, y = maxHeight * .23f))
        val marks = listOf(Pair(.14f,.20f), Pair(.65f,.19f), Pair(.43f,.31f))
        fleet.take(3).forEachIndexed { i, vehicle ->
            val coord = marks[i]
            Surface(
                onClick = { onSelect(vehicle.id) },
                color = if (vehicle.id == selectedId) Green else Color.White,
                contentColor = if (vehicle.id == selectedId) Color.White else Green,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.offset(x = maxWidth * coord.first, y = maxHeight * coord.second)
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalShipping, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(rubles(vehicle.hourPrice), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestSheet(
    vehicle: DemoVehicle,
    hours: Int,
    day: Int,
    time: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val fare = RentalRules.fare(vehicle, hours)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().padding(start = 21.dp, end = 21.dp, bottom = 30.dp)) {
            Text("Подтверждение аренды", fontSize = 23.sp, color = Dark, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(7.dp))
            Text("Проверьте параметры тестовой заявки", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            SummaryLine("Автомобиль", vehicle.title + " · " + vehicle.kind)
            SummaryLine("Получение", vehicle.pickup)
            SummaryLine("Начало", listOf("Сегодня", "Завтра", "Послезавтра")[day] + ", " + time)
            SummaryLine("Длительность", if (hours == 24) "1 сутки" else hours.toString() + " часа")
            HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 10.dp))
            SummaryLine("Аренда", rubles(fare))
            SummaryLine("Обеспечительный платёж", rubles(RentalRules.DEMO_DEPOSIT))
            Spacer(Modifier.height(8.dp))
            Surface(color = Pale, shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Info, null, tint = Green, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Это демонстрация. Деньги не списываются, договор не заключается, автомобиль не резервируется на сервере.",
                        fontSize = 12.sp, color = Green, lineHeight = 17.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text("Создать демо-заявку", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(.42f))
        Text(value, color = Dark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(.58f))
    }
}

@Composable
private fun OrdersScreen(orders: List<DemoOrder>, onChange: (DemoOrder) -> Unit) {
    Column(Modifier.fillMaxSize().background(Back)) {
        StandardHeader("Мои аренды", "Тестовые заявки и история")
        if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(95.dp).background(Pale, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ReceiptLong, null, tint = Green, modifier = Modifier.size(43.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Здесь пока пусто", color = Dark, fontSize = 19.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("Выберите грузовик на главной и создайте демо-заявку.",
                        color = Muted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(orders, key = { it.id }) { o ->
                    OrderCard(o, onChange)
                }
                item {
                    Text("Статусы управляются только в демонстрационном режиме. Реальной выдачи автомобиля не происходит.",
                        fontSize = 11.sp, color = Muted, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun OrderCard(o: DemoOrder, onChange: (DemoOrder) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Pale, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocalShipping, null, tint = Green, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.vehicleTitle, color = Dark, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("№ " + o.id.take(8).uppercase(), color = Muted, fontSize = 10.sp)
                }
                Text(rubles(o.price), color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(12.dp),
                color = if (o.stage == RentalStage.CANCELLED) Color(0xFFF8ECEB) else Pale) {
                Row(Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (o.stage == RentalStage.CANCELLED) Icons.Rounded.Cancel
                        else Icons.Rounded.CheckCircle, null,
                        tint = if (o.stage == RentalStage.CANCELLED) Color(0xFFAC4E4B) else Green,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(o.stage.title, color = Dark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            SummaryLine("Получение", o.pickup)
            SummaryLine("Дата и время", o.start)
            SummaryLine("Срок", if (o.hours == 24) "Сутки" else o.hours.toString() + " ч")
            val next = RentalRules.nextStage(o.stage)
            if (next != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onChange(o.copy(stage = next)) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                    Text("Демо: " + when (next) {
                        RentalStage.CONFIRMED -> "подтвердить"
                        RentalStage.ACTIVE -> "начать аренду"
                        else -> "завершить аренду"
                    }, fontWeight = FontWeight.SemiBold)
                }
            }
            if (RentalRules.mayCancel(o.stage)) {
                TextButton(onClick = { onChange(o.copy(stage = RentalStage.CANCELLED)) },
                    modifier = Modifier.align(Alignment.End)) {
                    Text("Отменить демо-заявку", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StandardHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 19.dp)) {
        Text(title, fontWeight = FontWeight.ExtraBold, color = Dark, fontSize = 24.sp)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ProfileScreen(onEditFleet: () -> Unit) {
    var info by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Back)) {
        StandardHeader("Профиль", "Настройки и управление")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(color = Green, shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp).background(Color.White.copy(alpha = .13f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column {
                        Text("Демонстрационный профиль", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Нальчик · Кабардино-Балкария", color = Color.White.copy(alpha = .79f), fontSize = 12.sp)
                    }
                }
            }
            Surface(color = Color.White, shape = RoundedCornerShape(19.dp)) {
                Column {
                    SettingRow(Icons.Rounded.Badge, "Мои документы", "Проверка будет подключена") {
                        info = "Документы водителя"
                    }
                    HorizontalDivider(color = Line)
                    SettingRow(Icons.Rounded.CreditCard, "Способы оплаты", "Платежи пока отключены") {
                        info = "Способы оплаты"
                    }
                    HorizontalDivider(color = Line)
                    SettingRow(Icons.Rounded.HelpOutline, "Поддержка", "Информация о сервисе") {
                        info = "Поддержка"
                    }
                }
            }
            Text("ДЛЯ ВЛАДЕЛЬЦА АВТОПАРКА", fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold, color = Muted, letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 7.dp, top = 8.dp))
            Surface(color = Color.White, shape = RoundedCornerShape(19.dp)) {
                SettingRow(Icons.Rounded.Edit, "Редактор автомобилей", "Изменение цен и доступности на этом устройстве",
                    onClick = onEditFleet)
            }
            Surface(color = Pale, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Shield, null, tint = Green, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Приложение работает автономно. Не загружайте паспорт или права: защищённый сервер, идентификация и реальные заказы ещё не подключены.",
                        color = Green, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            Text("ГрузШер КБР · UX DEMO v0.3", color = Muted, fontSize = 11.sp,
                modifier = Modifier.padding(6.dp))
        }
    }
    if (info != null) {
        AlertDialog(onDismissRequest = { info = null },
            icon = { Icon(Icons.Rounded.Info, null) },
            title = { Text(info ?: "", fontWeight = FontWeight.Bold) },
            text = { Text(when (info) {
                "Документы водителя" -> "После подключения защищённого сервера здесь появятся проверка водительских прав, паспортных данных и договоры. Сейчас не передавайте персональные документы."
                "Способы оплаты" -> "В рабочем сервисе будут карты, СБП, обеспечительный платёж и чеки. В демо деньги не списываются."
                else -> "В рабочей версии здесь будут обращение в поддержку, правила аренды, аварийная помощь и контакты оператора."
            }, fontSize = 13.sp) },
            confirmButton = { TextButton(onClick = { info = null }) { Text("Понятно") } },
            shape = RoundedCornerShape(20.dp))
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Pale, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Dark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Muted, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun FleetEditor(
    fleet: List<DemoVehicle>,
    onBack: () -> Unit,
    onChange: (List<DemoVehicle>) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Back)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Назад") }
            Spacer(Modifier.width(6.dp))
            Text("Редактор автопарка", color = Dark, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Изменения сохраняются только на этом телефоне. Для полноценного управления автопарком потребуется сервер и защищённая админ-панель.",
                    color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            items(fleet, key = { it.id }) { vehicle ->
                Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalShipping, null, tint = Green, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(vehicle.title, fontWeight = FontWeight.Bold, color = Dark, fontSize = 16.sp)
                                Text(vehicle.kind + " · " + vehicle.pickup, fontSize = 11.sp, color = Muted)
                            }
                            Switch(checked = vehicle.enabled, onCheckedChange = { value ->
                                onChange(fleet.map { if (it.id == vehicle.id) it.copy(enabled = value) else it })
                            })
                        }
                        Spacer(Modifier.height(11.dp))
                        RateControl("Цена за час", vehicle.hourPrice, 50) { value ->
                            onChange(fleet.map { if (it.id == vehicle.id) it.copy(hourPrice = value) else it })
                        }
                        Spacer(Modifier.height(7.dp))
                        RateControl("Цена за сутки", vehicle.dayPrice, 100) { value ->
                            onChange(fleet.map { if (it.id == vehicle.id) it.copy(dayPrice = value) else it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RateControl(label: String, value: Int, step: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = Dark, fontSize = 13.sp)
        IconButton(onClick = { onChange((value-step).coerceAtLeast(step)) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.RemoveCircleOutline, "Уменьшить", tint = Muted, modifier = Modifier.size(21.dp))
        }
        Text(rubles(value), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Green)
        IconButton(onClick = { onChange(value+step) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.AddCircleOutline, "Увеличить", tint = Green, modifier = Modifier.size(21.dp))
        }
    }
}
