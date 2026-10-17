package ru.kbr.cargoshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Camera preview and checklist placeholders do not store or upload real personal photographs. */
@Composable
fun InspectionVisualV8(vehicle: DemoVehicle?, title: String) {
    var demoShots by remember(title) { mutableIntStateOf(0) }
    var remarks by remember(title) { mutableStateOf("") }
    if(vehicle!=null) {
        VehiclePhotoV7(vehicle,Modifier.fillMaxWidth().height(182.dp),true)
        Spacer(Modifier.height(9.dp))
    }
    Surface(shape=RoundedCornerShape(15.dp),color=Color(0xFFF6F8F5)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Rounded.PhotoCamera,null,tint=Color(0xFF20523E),
                    modifier=Modifier.size(23.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Фотографии осмотра",fontWeight=FontWeight.Bold,
                        fontSize=13.sp,color=Color(0xFF1C2B24))
                    Text("Условные снимки, без доступа к камере",
                        fontSize=11.sp,color=Color(0xFF79837A))
                }
                Text("$demoShots/4",fontWeight=FontWeight.Bold,
                    color=Color(0xFF20523E),fontSize=13.sp)
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress={demoShots/4f},modifier=Modifier.fillMaxWidth().height(5.dp),
                color=Color(0xFF20523E),trackColor=Color(0xFFE5ECE5))
            Spacer(Modifier.height(9.dp))
            OutlinedButton(
                onClick={ demoShots=(demoShots+1).coerceAtMost(4) },
                enabled=demoShots<4,
                modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.AddAPhoto,null,modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(if(demoShots==4)"Все демо-фото добавлены" else "Добавить демо-фото",fontSize=12.sp)
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value=remarks,onValueChange={remarks=it.take(250)},
                label={Text("Замечания к состоянию автомобиля")},
                placeholder={Text("Например: царапина на бампере")},
                minLines=2,maxLines=3,
                shape=RoundedCornerShape(11.dp),
                modifier=Modifier.fillMaxWidth()
            )
            Text("Это предварительный просмотр: фотографии и заметки не передаются серверу.",
                fontSize=10.sp,color=Color(0xFF859087),
                modifier=Modifier.padding(top=7.dp))
        }
    }
}
