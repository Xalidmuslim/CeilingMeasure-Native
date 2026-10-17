package ru.kbr.cargoshare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import kotlinx.coroutines.delay

/**
 * 1Hz clock is isolated from MapView and the parent trip dashboard.
 * Real rental time in production must be based on backend timestamps.
 */
@Composable
fun TripTimerV9(state:ShowcaseSession) {
    var elapsedSeconds by remember(state.stage,state.elapsedMinutes,state.extraHours) {
        mutableIntStateOf(0)
    }
    LaunchedEffect(state.stage,state.elapsedMinutes,state.extraHours) {
        while (state.stage == ShowcaseStage.DRIVING) {
            delay(1000L)
            elapsedSeconds = (elapsedSeconds + 1).coerceAtMost(86400)
        }
    }
    val total=(state.hours+state.extraHours)*3600
    val elapsed=(state.elapsedMinutes*60+elapsedSeconds).coerceAtMost(total)
    val progress=(elapsed.toFloat()/total.coerceAtLeast(1)).coerceIn(0f,1f)
    Surface(shape=RoundedCornerShape(20.dp),color=Color(0xFF20523E)) {
        Column(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=16.dp)) {
            Row {
                Icon(Icons.Rounded.Schedule,null,tint=Color.White,
                    modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Осталось времени",fontSize=12.sp,
                    color=Color.White.copy(alpha=.84f),modifier=Modifier.weight(1f))
                Text("ДЕМО",fontSize=10.sp,color=Color.White.copy(alpha=.76f))
            }
            Spacer(Modifier.height(7.dp))
            Text(TripClockV8.display(state,elapsedSeconds),
                fontSize=36.sp,fontWeight=FontWeight.ExtraBold,color=Color.White)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress={progress},
                color=Color(0xFFD6EFA8),
                trackColor=Color.White.copy(alpha=.20f),
                modifier=Modifier.fillMaxWidth().height(5.dp)
            )
            Spacer(Modifier.height(9.dp))
            Row {
                Text("Начало: ${state.day}, ${state.time}",
                    color=Color.White.copy(alpha=.72f),fontSize=11.sp,
                    modifier=Modifier.weight(1f))
                Text("Срок: ${state.hours+state.extraHours} ч",
                    color=Color.White.copy(alpha=.72f),fontSize=11.sp)
            }
        }
    }
}
