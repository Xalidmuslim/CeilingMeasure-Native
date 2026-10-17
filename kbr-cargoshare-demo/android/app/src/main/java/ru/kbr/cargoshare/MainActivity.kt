package ru.kbr.cargoshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

private val Green = Color(0xFF26594C)
private val Cream = Color(0xFFF6F5F0)
private val Ink = Color(0xFF1B2A27)
private val Soft = Color(0xFFE2EBE5)

data class Truck(val id: String, val title: String, val body: String,
    val payload: String, val volume: String, val hourly: Int, val daily: Int,
    val pickup: String)
data class DemoBooking(val id: String, val truck: String, val hours: Int, val total: Int)

private val trucks = listOf(
    Truck("001", "ГАЗель NEXT", "Промтоварный фургон", "1 500 кг", "16 м³", 650, 4200, "Нальчик, центр"),
    Truck("002", "ГАЗель Бизнес", "Бортовая с тентом", "1 400 кг", "12 м³", 590, 3900, "Нальчик, район Стрелка"),
    Truck("003", "ГАЗель NEXT", "Удлинённый фургон", "1 350 кг", "20 м³", 750, 4900, "Нальчик, Александровка")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Green, background = Cream, surface = Color.White, onSurface = Ink)) {
                CargoApp()
            }
        }
    }
}

@Composable
private fun CargoApp() {
    var tab by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Truck?>(null) }
    val bookings = remember { mutableStateListOf<DemoBooking>() }
    Scaffold(
        containerColor = Cream,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Автомобили", "Мои заявки", "Профиль").forEachIndexed { i, name ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Text(listOf("🚚", "📋", "👤")[i], fontSize = 19.sp) },
                        label = { Text(name) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("ДЕМО • Автомобили и цены условные • Без реальных бронирований",
                color = Green, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().background(Soft).padding(12.dp))
            when (tab) {
                0 -> CatalogScreen(onSelect = { picked = it })
                1 -> BookingScreen(bookings) { bookings.remove(it) }
                else -> ProfileScreen()
            }
        }
    }
    val truck = picked
    if (truck != null) {
        BookingSheet(truck, onDismiss = { picked = null }) { hours, price ->
            bookings.add(0, DemoBooking(UUID.randomUUID().toString(), truck.title, hours, price))
            picked = null
            tab = 1
        }
    }
}

@Composable
private fun CatalogScreen(onSelect: (Truck) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
            Text("📍 Нальчик и КБР", fontSize = 13.sp, color = Green)
            Spacer(Modifier.height(12.dp))
            Text("Грузовой транспорт\nрядом с вами", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, lineHeight = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text("Аренда без водителя • от 2 часов", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Surface(color = Soft, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Безопасное бронирование", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Проверка документов и оплата будут подключены позже",
                        fontSize = 12.sp, color = Green)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Доступный транспорт", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(trucks, key = { it.id }) { truck ->
                TruckCard(truck, onClick = { onSelect(truck) })
            }
        }
    }
}

@Composable
private fun TruckCard(truck: Truck, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Box(Modifier.fillMaxWidth().height(94.dp).background(Soft, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center) {
                Text("🚚", fontSize = 58.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(truck.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(truck.body, fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text(truck.payload + "  ·  " + truck.volume + "  ·  МКПП",
                fontSize = 13.sp, color = Color.DarkGray)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("от " + truck.hourly + " ₽", color = Green, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                Text(" / час", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(truck.daily.toString() + " ₽ / сутки", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingSheet(truck: Truck, onDismiss: () -> Unit, onBook: (Int, Int) -> Unit) {
    var hours by remember(truck.id) { mutableIntStateOf(4) }
    val price = if (hours == 24) truck.daily else hours * truck.hourly
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 38.dp)) {
            Text(truck.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(truck.body, color = Color.Gray, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text("Грузоподъёмность: " + truck.payload)
            Text("Объём кузова: " + truck.volume)
            Text("Получение: " + truck.pickup)
            Spacer(Modifier.height(20.dp))
            Text("Длительность аренды", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 4, 8, 24).forEach { h ->
                    FilterChip(selected = hours == h, onClick = { hours = h },
                        label = { Text(if (h == 24) "Сутки" else h.toString() + " ч") })
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Условная стоимость: " + price + " ₽", fontWeight = FontWeight.Bold,
                color = Green, fontSize = 21.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onBook(hours, price) }, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                Text("Создать тестовую заявку")
            }
            Spacer(Modifier.height(8.dp))
            Text("Только демонстрация. Заявка не отправляется владельцу, деньги не списываются.",
                fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun BookingScreen(bookings: List<DemoBooking>, onCancel: (DemoBooking) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Мои заявки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (bookings.isEmpty()) {
            Text("Пока нет тестовых заявок. Выберите автомобиль в каталоге.",
                color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(bookings, key = { it.id }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(item.truck, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("Тестовая заявка · " + item.hours + " ч", color = Green, fontSize = 13.sp)
                            Text(item.total.toString() + " ₽", fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { onCancel(item) }) { Text("Удалить заявку") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Профиль", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Демонстрационный режим", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Нальчик • Кабардино-Балкария", color = Green)
                Spacer(Modifier.height(8.dp))
                Text("Вход по SMS, проверка водительских документов, электронные договоры и оплата появятся после подключения защищённого сервера.",
                    color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Text("Все заявки хранятся только до закрытия приложения. Автомобили — вымышленные.",
                    color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
