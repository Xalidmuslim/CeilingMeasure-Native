package ru.kbr.cargoshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Only locally completed demonstration rentals are shown here, never real customer PII. */
@Composable
fun AdminHistoryV95(history: List<RentalHistoryEntry>) {
    val green = Color(0xFF215340)
    val ink = Color(0xFF192620)
    val muted = Color(0xFF737E74)
    val dateFormat = SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale("ru", "RU"))
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=13.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Rounded.History,null,tint=green,modifier=Modifier.size(25.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("История поездок",fontSize=21.sp,fontWeight=FontWeight.ExtraBold,color=ink)
                Text("Сохранённые завершённые демоаренды",fontSize=11.sp,color=muted)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),
            horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            Surface(color=Color.White,shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)) {
                Column(Modifier.padding(13.dp)) {
                    Text("Завершено",fontSize=11.sp,color=muted)
                    Spacer(Modifier.height(5.dp))
                    Text(history.size.toString(),fontSize=24.sp,fontWeight=FontWeight.Bold,color=green)
                }
            }
            Surface(color=Color.White,shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)) {
                Column(Modifier.padding(13.dp)) {
                    Text("Аренда · демо",fontSize=11.sp,color=muted)
                    Spacer(Modifier.height(5.dp))
                    Text(sumRub(history.sumOf { it.totalPaid }),fontSize=20.sp,
                        fontWeight=FontWeight.Bold,color=green)
                }
            }
        }
        Spacer(Modifier.height(11.dp))
        if(history.isEmpty()) {
            Surface(color=Color.White,shape=RoundedCornerShape(16.dp),
                modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp)) {
                Column(Modifier.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.LocalShipping,null,tint=green,modifier=Modifier.size(34.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Пока нет завершённых поездок",
                        fontSize=14.sp,fontWeight=FontWeight.Bold,color=ink)
                    Spacer(Modifier.height(5.dp))
                    Text("Проведите демоаренду до возврата автомобиля — запись появится здесь автоматически.",
                        color=muted,fontSize=12.sp,lineHeight=17.sp)
                }
            }
        } else {
            LazyColumn(
                modifier=Modifier.fillMaxSize(),
                contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=20.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)
            ) {
                items(history, key={it.id}) { trip ->
                    Surface(color=Color.White,shape=RoundedCornerShape(17.dp)) {
                        Column(Modifier.fillMaxWidth().padding(15.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Text("Поездка № ${trip.rideNumber}",
                                    fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink,
                                    modifier=Modifier.weight(1f))
                                Icon(Icons.Rounded.CheckCircle,null,tint=green,
                                    modifier=Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Завершена",color=green,fontSize=11.sp,
                                    fontWeight=FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(dateFormat.format(Date(trip.completedAtMillis)),
                                fontSize=11.sp,color=muted)
                            HorizontalDivider(color=Color(0xFFE6EBE5),
                                modifier=Modifier.padding(vertical=9.dp))
                            V5Item("Автомобиль",trip.vehicleTitle)
                            V5Item("Пункт выдачи",trip.pickup)
                            V5Item("Начало",trip.start)
                            V5Item("Длительность",formatMinutes(trip.elapsedMinutes))
                            V5Item("Пробег","${trip.distanceKm} км")
                            V5Item("Аренда",sumRub(trip.rentalPaid))
                            if(trip.extensionPaid>0) V5Item("Продление",sumRub(trip.extensionPaid))
                            V5Item("Всего",sumRub(trip.totalPaid),emphasis=true)
                            if(trip.incidentReported)
                                Text("Зафиксировано учебное обращение о поломке/ДТП",
                                    fontSize=11.sp,color=Color(0xFFA66322))
                        }
                    }
                }
            }
        }
    }
}
