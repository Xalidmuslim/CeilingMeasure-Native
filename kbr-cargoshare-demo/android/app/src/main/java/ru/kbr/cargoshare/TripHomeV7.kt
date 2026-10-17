package ru.kbr.cargoshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Green=Color(0xFF20523E)
private val Black=Color(0xFF182820)
private val Light=Color(0xFFF7F8F5)
private val Pale=Color(0xFFEAF1E9)
private val Soft=Color(0xFF758278)
private val Border=Color(0xFFE5EBE4)

@Composable
fun TripHomeV7(
    state:ShowcaseSession,
    fleet:List<DemoVehicle>,
    dispatch:(()->ShowcaseSession)->Unit,
    onInfo:(String)->Unit
) {
    val rate=fleet.firstOrNull{it.id==state.vehicleId}?.hourPrice
        ?: demoFleet.firstOrNull{it.id==state.vehicleId}?.hourPrice ?: 650
    val total=(state.hours+state.extraHours)*60
    var fakeSeconds by remember(state.stage) { mutableIntStateOf(0) }
    val remainingSeconds=TripClockV8.remainingSeconds(state,fakeSeconds)
    val remaining=(remainingSeconds+59)/60
    LaunchedEffect(state.stage) {
        while(state.stage==ShowcaseStage.DRIVING) {
            delay(1000)
            fakeSeconds=(fakeSeconds+1).coerceAtMost(86400)
        }
    }
    var dialog by remember { mutableStateOf(false) }
    var showFinance by remember { mutableStateOf(false) }
    val vehicle=fleet.firstOrNull{it.id==state.vehicleId}
        ?: demoFleet.firstOrNull{it.id==state.vehicleId}
    Column(Modifier.fillMaxSize().background(Light)
        .verticalScroll(rememberScrollState())
        .padding(horizontal=14.dp,vertical=12.dp),
        verticalArrangement=Arrangement.spacedBy(11.dp)) {

        Surface(color=Color.White,shape=RoundedCornerShape(19.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment=Alignment.CenterVertically) {
                if(vehicle!=null) VehiclePhotoV7(vehicle,Modifier.size(110.dp,83.dp),true)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.vehicleTitle,fontSize=17.sp,fontWeight=FontWeight.ExtraBold,
                        color=Black)
                    Text(state.pickup,fontSize=11.sp,color=Soft,maxLines=2)
                    Spacer(Modifier.height(6.dp))
                    Text("● Аренда активна",fontWeight=FontWeight.Bold,
                        fontSize=12.sp,color=Green)
                }
            }
        }

        Surface(shape=RoundedCornerShape(20.dp),color=Green) {
            Column(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=16.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule,null,tint=Color.White,
                        modifier=Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Осталось по демотарифу",
                        fontSize=12.sp,color=Color.White.copy(alpha=.83f),
                        modifier=Modifier.weight(1f))
                    Text("DEMO",fontSize=10.sp,color=Color.White.copy(alpha=.7f))
                }
                Spacer(Modifier.height(6.dp))
                val timer=TripClockV8.display(state,fakeSeconds)
                Text(timer,fontSize=38.sp,fontWeight=FontWeight.ExtraBold,color=Color.White)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress={((state.elapsedMinutes*60f+fakeSeconds)/(total.coerceAtLeast(1)*60f)).coerceIn(0f,1f)},
                    color=Color(0xFFD6EFA8),
                    trackColor=Color.White.copy(alpha=.20f),
                    modifier=Modifier.fillMaxWidth().height(6.dp),
                    strokeCap=androidx.compose.ui.graphics.StrokeCap.Round)
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("Начало: ${state.day}, ${state.time}",
                        color=Color.White.copy(alpha=.72f),fontSize=11.sp,
                        modifier=Modifier.weight(1f))
                    Text("Длительность ${state.hours+state.extraHours} ч",
                        color=Color.White.copy(alpha=.72f),fontSize=11.sp)
                }
            }
        }
        Surface(color=Color.White,shape=RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                MetricV7(Icons.Rounded.Route,"${state.distanceKm} км","Пробег",Modifier.weight(1f))
                MetricV7(Icons.Rounded.LocalGasStation,"${state.fuelPercent}%","Топливо",Modifier.weight(1f))
                MetricV7(Icons.Rounded.Payments,sumRub(state.fare+state.extensionCharge),
                    "Стоимость",Modifier.weight(1f))
            }
        }
        Box(Modifier.fillMaxWidth().height(225.dp)) {
            StreetMapV7(
                fleet=fleet.filter{it.enabled && it.id==state.vehicleId},
                selectedId=state.vehicleId, onSelect={},
                modifier=Modifier.fillMaxSize())
            Surface(color=Color.White.copy(alpha=.96f),
                modifier=Modifier.align(Alignment.BottomEnd).padding(10.dp),
                shape=RoundedCornerShape(10.dp)) {
                Text("Движение и точки — демонстрация",
                    modifier=Modifier.padding(8.dp),fontSize=10.sp,color=Soft)
            }
        }
        Surface(color=Color.White,shape=RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Быстрые действия",fontSize=15.sp,
                        fontWeight=FontWeight.ExtraBold,color=Black,
                        modifier=Modifier.weight(1f))
                    Icon(Icons.Rounded.Shield,null,tint=Green,modifier=Modifier.size(18.dp))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    QuickActionV7(
                        icon=if(state.doorsOpen)Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        label=if(state.doorsOpen)"Закрыть двери" else "Открыть двери",
                        modifier=Modifier.weight(1f)) {
                        dispatch{ShowcaseEngine.toggleDoors(state)}
                    }
                    QuickActionV7(
                        icon=Icons.Rounded.MoreTime,label="+ 2 часа",
                        modifier=Modifier.weight(1f)) {
                        dispatch{ShowcaseEngine.extend(state,2,rate)}
                        onInfo("Продление на 2 часа. Доплата ${sumRub(rate*2)} только в деморежиме, реальные деньги не списываются.")
                    }
                    QuickActionV7(
                        icon=Icons.Rounded.SupportAgent,label="Помощь",
                        modifier=Modifier.weight(1f)) { dialog=true }
                }
                Spacer(Modifier.height(13.dp))
                Text("Показать поездку в действии",fontSize=11.sp,color=Soft)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={dispatch{ShowcaseEngine.advanceTrip(state,15,4)}},
                        modifier=Modifier.weight(1f),shape=RoundedCornerShape(11.dp)) {
                        Text("+ 15 мин · 4 км",fontSize=11.sp)
                    }
                    OutlinedButton(onClick={dispatch{ShowcaseEngine.advanceTrip(state,60,18)}},
                        modifier=Modifier.weight(1f),shape=RoundedCornerShape(11.dp)) {
                        Text("+ 1 ч · 18 км",fontSize=11.sp)
                    }
                }
            }
        }
        Surface(color=Color.White,shape=RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Финансовая информация",fontSize=14.sp,
                        fontWeight=FontWeight.Bold,color=Black,
                        modifier=Modifier.weight(1f))
                    TextButton(onClick={showFinance=!showFinance}) {
                        Text(if(showFinance)"Скрыть" else "Подробнее",
                            fontSize=12.sp,color=Green)
                    }
                }
                V5Item("Аренда",sumRub(state.fare))
                if(state.extensionCharge>0)V5Item("Продление",sumRub(state.extensionCharge))
                V5Item("Залог",sumRub(RentalRules.DEMO_DEPOSIT))
                if(showFinance) {
                    HorizontalDivider(color=Border,modifier=Modifier.padding(vertical=8.dp))
                    V5Item("Условно оплачено",sumRub(state.fare+RentalRules.DEMO_DEPOSIT))
                    V5Item("Без залога",sumRub(state.fare+state.extensionCharge))
                    V5Note("Демоучёт. После возврата машины залог будет условно возвращён.")
                }
            }
        }
        OutlinedButton(onClick={dispatch{ShowcaseEngine.requestReturn(state)}},
            modifier=Modifier.fillMaxWidth().height(51.dp),
            shape=RoundedCornerShape(14.dp),
            border=androidx.compose.foundation.BorderStroke(1.3.dp,Color(0xFFB34A4A))) {
            Icon(Icons.Rounded.Flag,null,tint=Color(0xFFAA3B40),
                modifier=Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("Завершить аренду",color=Color(0xFFAA3B40),
                fontSize=14.sp,fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
    }
    if(dialog) AlertDialog(onDismissRequest={dialog=false},
        title={Text("Помощь на дороге")},
        text={Column {
            Text("Выберите демонстрационный сценарий. Заявка не отправляется реальному оператору.",
                color=Soft,fontSize=12.sp)
            TextButton(onClick={
                dialog=false;dispatch{ShowcaseEngine.reportIncident(state)}
                onInfo("Демо: обращение по ДТП зарегистрировано локально.")
            }){Text("ДТП")}
            TextButton(onClick={
                dialog=false;dispatch{ShowcaseEngine.reportIncident(state)}
                onInfo("Демо: обращение о поломке зарегистрировано локально.")
            }){Text("Поломка")}
        }},
        confirmButton={TextButton(onClick={dialog=false}){Text("Отмена")}})
}

@Composable
private fun MetricV7(icon:androidx.compose.ui.graphics.vector.ImageVector,
    value:String,label:String,modifier:Modifier=Modifier) {
    Column(modifier, horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(icon,null,tint=Green,modifier=Modifier.size(21.dp))
        Spacer(Modifier.height(8.dp))
        Text(value,fontSize=15.sp,fontWeight=FontWeight.Bold,color=Black,maxLines=1)
        Spacer(Modifier.height(4.dp))
        Text(label,fontSize=10.sp,color=Soft)
    }
}
@Composable
private fun QuickActionV7(icon:androidx.compose.ui.graphics.vector.ImageVector,
    label:String,modifier:Modifier=Modifier,onClick:()->Unit) {
    Surface(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(13.dp),
        color=Pale) {
        Column(Modifier.padding(horizontal=4.dp,vertical=12.dp),
            horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(icon,null,tint=Green,modifier=Modifier.size(24.dp))
            Spacer(Modifier.height(7.dp))
            Text(label,color=Black,fontWeight=FontWeight.Medium,
                fontSize=11.sp,maxLines=1)
        }
    }
}
