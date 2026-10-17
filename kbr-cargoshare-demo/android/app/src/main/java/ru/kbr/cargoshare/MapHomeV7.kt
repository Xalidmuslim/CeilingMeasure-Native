package ru.kbr.cargoshare

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Ink=Color(0xFF182A22)
private val Deep=Color(0xFF20523E)
private val Secondary=Color(0xFF718178)
private val Pale=Color(0xFFEAF1E9)
private val Paper=Color(0xFFF7F8F5)
private val Divider=Color(0xFFE9ECE7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapHomeV7(
    fleet:List<DemoVehicle>,
    state:ShowcaseSession,
    onReserve:(DemoVehicle,Int,String,String)->Unit
) {
    val all=fleet.filter{it.enabled}
    var filter by remember { mutableStateOf("Все") }
    val filtered=all.filter { v -> when(filter) {
        "Фургоны" -> "фургон" in v.kind.lowercase() || "удлин" in v.kind.lowercase()
        "Бортовые" -> "борт" in v.kind.lowercase()
        else -> true
    } }
    var selectedId by remember { mutableStateOf(all.firstOrNull()?.id ?: "") }
    val chosen=PresentationV7.pick(filtered,selectedId)
    var day by remember { mutableStateOf("Сегодня") }
    var startTime by remember { mutableStateOf("12:00") }
    var hours by remember { mutableIntStateOf(4) }
    val sheetState=rememberStandardBottomSheetState(
        initialValue=SheetValue.PartiallyExpanded,skipHiddenState=true)
    val scaffold=rememberBottomSheetScaffoldState(bottomSheetState=sheetState)
    val scope=rememberCoroutineScope()
    val expanded=sheetState.currentValue==SheetValue.Expanded
    BackHandler(enabled=expanded) { scope.launch { sheetState.partialExpand() } }
    BottomSheetScaffold(
        scaffoldState=scaffold,
        sheetPeekHeight=252.dp,
        sheetContainerColor=Color.White,
        sheetTonalElevation=0.dp,
        sheetShadowElevation=13.dp,
        sheetShape=RoundedCornerShape(topStart=26.dp,topEnd=26.dp),
        sheetDragHandle={
            Box(Modifier.fillMaxWidth().padding(top=10.dp,bottom=7.dp),
                contentAlignment=Alignment.Center) {
                Box(Modifier.size(38.dp,4.dp).background(Divider,CircleShape))
            }
        },
        sheetContent={
            Column(
                Modifier.fillMaxWidth().heightIn(max=620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal=16.dp)
            ) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Грузовики рядом",fontSize=19.sp,
                            fontWeight=FontWeight.ExtraBold,color=Ink)
                        Spacer(Modifier.height(1.dp))
                        Text("На карте и в каталоге · демо",
                            color=Secondary,fontSize=11.sp)
                    }
                    Surface(color=Pale,shape=RoundedCornerShape(10.dp)) {
                        Text("${filtered.size} авто",modifier=Modifier.padding(horizontal=9.dp,vertical=6.dp),
                            color=Deep,fontSize=12.sp,fontWeight=FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                if(chosen!=null) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        VehiclePhotoV7(chosen,modifier=Modifier.size(125.dp,93.dp),showDisclaimer=true)
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chosen.title,fontWeight=FontWeight.ExtraBold,
                                color=Ink,fontSize=16.sp,maxLines=1)
                            Text(chosen.kind+" · "+chosen.payload+" · "+chosen.volume,
                                color=Secondary,fontSize=11.sp,maxLines=2)
                            Spacer(Modifier.height(5.dp))
                            Text(chosen.pickup,color=Secondary,fontSize=10.sp,maxLines=2)
                            Spacer(Modifier.height(5.dp))
                            Text("от "+sumRub(chosen.hourPrice)+"/ч",
                                color=Deep,fontSize=15.sp,fontWeight=FontWeight.ExtraBold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if(!expanded) {
                        Button(
                            onClick={scope.launch { sheetState.expand() }},
                            modifier=Modifier.fillMaxWidth().height(46.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=Deep),
                            shape=RoundedCornerShape(13.dp)
                        ) { Text("Выбрать автомобиль",fontWeight=FontWeight.Bold) }
                    }
                } else {
                    Text("Нет доступных автомобилей",color=Secondary,
                        modifier=Modifier.padding(vertical=14.dp))
                }
                if(expanded && chosen!=null) {
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp),
                        modifier=Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("Все","Фургоны","Бортовые").forEach{type->
                            MapChipV7(type,type==filter){filter=type}
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                        filtered.forEach {car->
                            Surface(onClick={selectedId=car.id},
                                shape=RoundedCornerShape(15.dp),
                                color=if(car.id==chosen.id)Pale else Color.White,
                                border=androidx.compose.foundation.BorderStroke(
                                    if(car.id==chosen.id)1.3.dp else .7.dp,
                                    if(car.id==chosen.id)Deep else Divider
                                ),modifier=Modifier.width(143.dp)) {
                                Column(Modifier.padding(7.dp)) {
                                    VehiclePhotoV7(car,Modifier.fillMaxWidth().height(65.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Text(car.title,fontSize=12.sp,color=Ink,
                                        fontWeight=FontWeight.Bold,maxLines=1)
                                    Text("● Свободен · "+sumRub(car.hourPrice)+"/ч",fontSize=11.sp,
                                        color=Deep,fontWeight=FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Когда заберёте автомобиль?",fontSize=14.sp,
                        color=Ink,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp),
                        modifier=Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("Сегодня","Завтра","Послезавтра").forEach { d->
                            MapChipV7(d,day==d){day=d}
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp),
                        modifier=Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("09:00","12:00","15:00","18:00").forEach {t->
                            MapChipV7(t,startTime==t){startTime=t}
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    Text("Срок аренды",fontSize=14.sp,
                        color=Ink,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
                        modifier=Modifier.horizontalScroll(rememberScrollState())) {
                        RentalRules.allowedHours.forEach {h->
                            MapChipV7(if(h==24)"1 сутки" else "${h} часа",
                                hours==h){hours=h}
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    val quote=PresentationV7.quote(chosen,hours)
                    Surface(color=Paper,shape=RoundedCornerShape(15.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            QuoteLineV7("Аренда",sumRub(quote.rental),true)
                            QuoteLineV7("Залог (возвращается)",sumRub(quote.refundableDeposit),false)
                            HorizontalDivider(color=Divider,modifier=Modifier.padding(vertical=8.dp))
                            QuoteLineV7("Итого условно",sumRub(quote.totalToday),true)
                        }
                    }
                    Spacer(Modifier.height(11.dp))
                    Button(onClick={onReserve(chosen,hours,day,startTime)},
                        modifier=Modifier.fillMaxWidth().height(51.dp),
                        shape=RoundedCornerShape(14.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=Deep)) {
                        Text("Продолжить оформление",fontWeight=FontWeight.Bold,fontSize=14.sp)
                        Spacer(Modifier.width(9.dp))
                        Icon(Icons.Rounded.ArrowForward,null,modifier=Modifier.size(17.dp))
                    }
                    Spacer(Modifier.height(17.dp))
                }
            }
        },
    ) { contentPadding ->
        Box(Modifier.fillMaxSize().padding(contentPadding)) {
            StreetMapV7(fleet=filtered,selectedId=chosen?.id ?: "",onSelect={selectedId=it},
                modifier=Modifier.fillMaxSize())
            Surface(color=Color.White,shape=RoundedCornerShape(14.dp),
                shadowElevation=3.dp,
                modifier=Modifier.align(Alignment.TopStart).padding(start=12.dp,top=61.dp)) {
                Row(Modifier.padding(horizontal=12.dp,vertical=10.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Place,null,modifier=Modifier.size(16.dp),tint=Deep)
                    Spacer(Modifier.width(6.dp))
                    Text("Нальчик • КБР",fontWeight=FontWeight.Bold,
                        color=Ink,fontSize=12.sp)
                }
            }
        }
    }
}

@Composable
private fun MapChipV7(label:String,chosen:Boolean,onClick:()->Unit) {
    Surface(onClick=onClick,color=if(chosen)Deep else Paper,
        shape=RoundedCornerShape(11.dp)) {
        Text(label,modifier=Modifier.padding(horizontal=13.dp,vertical=10.dp),
            fontSize=12.sp,fontWeight=if(chosen)FontWeight.Bold else FontWeight.Medium,
            color=if(chosen)Color.White else Ink)
    }
}
@Composable
private fun QuoteLineV7(left:String,right:String,strong:Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(left,modifier=Modifier.weight(1f),color=Secondary,fontSize=12.sp)
        Text(right,color=if(strong)Deep else Ink,fontWeight=FontWeight.Bold,fontSize=13.sp)
    }
}
