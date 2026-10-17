package ru.kbr.cargoshare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

internal val SGreen = Color(0xFF20513F)
internal val SInk = Color(0xFF192722)
internal val SPale = Color(0xFFEAF1E9)
internal val SBack = Color(0xFFF5F7F3)
internal val SSecondary = Color(0xFF718077)
internal val SBorder = Color(0xFFE2E9E2)
internal val SLime = Color(0xFFD8EFA9)
internal fun sumRub(v: Int): String = NumberFormat.getNumberInstance(Locale("ru", "RU")).format(v) + " ₽"

@Composable
fun ShowcaseApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val showcaseStore = remember { ShowcaseStore(context.applicationContext) }
    val fleetStore = remember { DemoStore(context.applicationContext) }
    var state by remember { mutableStateOf(showcaseStore.read()) }
    var fleet by remember { mutableStateOf(fleetStore.fleet()) }
    var tab by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }
    var vehicleId by remember { mutableStateOf(fleet.firstOrNull{it.enabled}?.id ?: "") }
    var hours by remember { mutableIntStateOf(4) }
    var day by remember { mutableStateOf("Сегодня") }
    var time by remember { mutableStateOf("12:00") }
    val chosen = fleet.firstOrNull { it.id == vehicleId && it.enabled } ?: fleet.firstOrNull { it.enabled }

    fun update(next: ShowcaseSession) {
        state = next
        showcaseStore.save(next)
    }
    fun act(block: () -> ShowcaseSession) {
        runCatching(block).onSuccess { update(it) }
            .onFailure { notice = it.message ?: "Действие недоступно на этом шаге" }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = SGreen,
        onPrimary = Color.White, background = SBack, surface = Color.White,
        onSurface = SInk, secondary = SLime)) {
        Scaffold(
            containerColor = SBack,
            topBar = {
                Column(Modifier.background(Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(start = 17.dp, end = 17.dp, top = 11.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(39.dp).background(SPale, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.LocalShipping, null, tint = SGreen, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("ГРУЗШЕР КБР", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                                color = SInk, letterSpacing = .3.sp)
                            Text("Аренда грузовых автомобилей", color = SSecondary, fontSize = 10.sp)
                        }
                        Surface(color = SLime, shape = RoundedCornerShape(10.dp)) {
                            Text("ДЕМО 0.4", modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                color = SGreen, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth().background(SGreen).padding(horizontal = 15.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Без реальных оплат, СМС, проверки документов и открытия машины",
                            color = Color.White, fontSize = 10.sp, maxLines = 2,
                            lineHeight = 13.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 2.dp, modifier = Modifier.height(76.dp)) {
                    val names = listOf("Главная", "Аренда", "Профиль", "Управление")
                    val icons = listOf(Icons.Rounded.Home, Icons.Rounded.ReceiptLong,
                        Icons.Rounded.Person, Icons.Rounded.Settings)
                    names.forEachIndexed { i, name ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(icons[i], null, modifier = Modifier.size(22.dp)) },
                            label = { Text(name, fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SGreen, selectedTextColor = SGreen,
                                indicatorColor = SPale, unselectedIconColor = SSecondary,
                                unselectedTextColor = SSecondary)
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when(tab) {
                    0 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        DemoStreetMap(fleet.filter{it.enabled}, chosen?.id ?: "", onSelect = { vehicleId = it })
                        Column(Modifier.fillMaxWidth().padding(15.dp)) {
                            ProcessProgress(state.stage)
                            Spacer(Modifier.height(12.dp))
                            if (state.stage == ShowcaseStage.READY) {
                                FleetPicker(fleet.filter { it.enabled }, chosen?.id ?: "") { vehicleId = it }
                                Spacer(Modifier.height(12.dp))
                            }
                            FlowPanel(
                                state = state,
                                chosen = chosen,
                                hours = hours, onHours = { hours = it },
                                day = day, onDay = { day = it },
                                time = time, onTime = { time = it },
                                act = ::act,
                                onAdmin = { tab = 3 },
                                onNotice = { notice = it }
                            )
                            Spacer(Modifier.height(12.dp))
                            InfoPanel("Как работает демо", "Пройдите процесс от регистрации до возврата залога. Все документы, суммы и автомобили — примеры. Настоящие данные не вводите.")
                            Spacer(Modifier.height(18.dp))
                        }
                    }
                    1 -> DemoOrderScreen(state, onGo = { tab = 0 },
                        onReset = { update(ShowcaseEngine.reset()); tab = 0 })
                    2 -> DemoProfileScreen(state, onGo = { tab = 0 },
                        onAdmin = { tab = 3 },
                        onReset = { update(ShowcaseEngine.reset()); tab = 0 },
                        onNotice = { notice = it })
                    else -> DemoAdminScreen(
                        state = state, fleet = fleet,
                        onAction = ::act,
                        onChangeFleet = { updated ->
                            fleet = updated; fleetStore.saveFleet(updated)
                            if (updated.none { it.id == vehicleId && it.enabled }) {
                                vehicleId = updated.firstOrNull { it.enabled }?.id ?: ""
                            }
                        },
                        onClient = { tab = 0 },
                        onNotice = { notice = it }
                    )
                }
            }
        }
        if (notice != null) {
            AlertDialog(
                onDismissRequest = { notice = null },
                title = { Text("Демонстрационный режим") },
                text = { Text(notice ?: "") },
                confirmButton = { TextButton(onClick = { notice = null }) { Text("Закрыть") } },
                shape = RoundedCornerShape(19.dp))
        }
    }
}

@Composable
private fun ProcessProgress(stage: ShowcaseStage) {
    val index = ShowcaseStage.entries.indexOf(stage)
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TaskAlt, null, tint = SGreen, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(stage.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = SInk, modifier = Modifier.weight(1f))
                Text("${index+1}/${ShowcaseStage.entries.size}", color = SGreen,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { (index+1).toFloat()/ShowcaseStage.entries.size },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = SGreen, trackColor = SPale,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun DemoStreetMap(fleet: List<DemoVehicle>, selected: String, onSelect: (String)->Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(205.dp).background(Color(0xFFE6EEE7))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Color(0xFFE7EFE8))
            drawRect(Color(0xFFD5E6D6), Offset(w*.68f, h*.04f),
                androidx.compose.ui.geometry.Size(w*.26f, h*.25f))
            val lanes = listOf(
                Path().apply { moveTo(-w*.1f,h*.21f); cubicTo(w*.3f,h*.34f,w*.61f,h*.13f,w*1.1f,h*.29f) },
                Path().apply { moveTo(-w*.1f,h*.76f); cubicTo(w*.38f,h*.54f,w*.63f,h*.88f,w*1.1f,h*.60f) },
                Path().apply { moveTo(w*.16f,-h*.1f); cubicTo(w*.45f,h*.34f,w*.3f,h*.73f,w*.5f,h*1.1f) },
                Path().apply { moveTo(w*.83f,-h*.1f); cubicTo(w*.7f,h*.34f,w*.8f,h*.72f,w*.69f,h*1.1f) }
            )
            lanes.forEach {
                drawPath(it, Color(0xFFD4DDD3), style=Stroke(32.dp.toPx(), cap=StrokeCap.Round))
                drawPath(it, Color(0xFFFFFFFF), style=Stroke(26.dp.toPx(), cap=StrokeCap.Round))
            }
        }
        Text("НАЛЬЧИК", fontSize=11.sp, fontWeight=FontWeight.ExtraBold,
            letterSpacing=3.sp, color=Color(0xFF91A69A),
            modifier=Modifier.align(Alignment.Center))
        val xy = listOf(.11f to .28f, .58f to .12f, .43f to .60f, .05f to .65f)
        fleet.take(4).forEachIndexed { i, truck ->
            val (x,y) = xy[i]
            Surface(onClick = { onSelect(truck.id) },
                color = if(truck.id==selected) SGreen else Color.White,
                contentColor = if(truck.id==selected) Color.White else SGreen,
                shape=RoundedCornerShape(12.dp), shadowElevation=5.dp,
                modifier=Modifier.offset(x=maxWidth*x,y=maxHeight*y)) {
                Row(Modifier.padding(horizontal=9.dp,vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalShipping,null,modifier=Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(sumRub(truck.hourPrice),fontSize=10.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
        Surface(color=Color.White.copy(alpha=.94f), shape=RoundedCornerShape(10.dp),
            modifier=Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text("Карта-схема · точки условные", modifier=Modifier.padding(7.dp),
                fontSize=10.sp,color=SSecondary)
        }
    }
}

@Composable
private fun FleetPicker(fleet:List<DemoVehicle>,selected:String,onSelect:(String)->Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
            Text("Доступный транспорт",fontSize=20.sp,fontWeight=FontWeight.ExtraBold,
                color=SInk,modifier=Modifier.weight(1f))
            Text("${fleet.size} шт.",fontSize=12.sp,color=SGreen)
        }
        Spacer(Modifier.height(10.dp))
        if(fleet.isEmpty()) {
            InfoPanel("Нет доступных автомобилей","Администратор может включить демоавтомобили в разделе «Управление».")
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                fleet.forEach { v ->
                    Surface(onClick={onSelect(v.id)},color=if(v.id==selected) SPale else Color.White,
                        border=androidx.compose.foundation.BorderStroke(
                            if(v.id==selected) 1.5.dp else 1.dp,
                            if(v.id==selected) SGreen else SBorder),
                        shape=RoundedCornerShape(17.dp),modifier=Modifier.width(184.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Icon(Icons.Rounded.LocalShipping,null,tint=SGreen,
                                modifier=Modifier.size(29.dp))
                            Spacer(Modifier.height(7.dp))
                            Text(v.title,fontWeight=FontWeight.Bold,fontSize=14.sp,color=SInk,maxLines=1)
                            Text(v.kind+" · "+v.payload+" · "+v.volume,
                                fontSize=10.sp,color=SSecondary,maxLines=1)
                            Spacer(Modifier.height(6.dp))
                            Text(sumRub(v.hourPrice)+"/ч",fontWeight=FontWeight.ExtraBold,
                                color=SGreen,fontSize=16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun InfoPanel(title:String,subtitle:String) {
    Surface(color=SPale,shape=RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Text(title,color=SGreen,fontSize=13.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle,color=SGreen.copy(alpha=.8f),fontSize=12.sp,lineHeight=17.sp)
        }
    }
}

@Composable
internal fun DemoTitle(title:String,subtitle:String) {
    Column(Modifier.fillMaxWidth().padding(bottom=13.dp)) {
        Text(title,fontSize=23.sp,fontWeight=FontWeight.ExtraBold,color=SInk)
        Spacer(Modifier.height(3.dp))
        Text(subtitle,color=SSecondary,fontSize=12.sp,lineHeight=17.sp)
    }
}

@Composable
internal fun DemoPrimary(label:String, enabled:Boolean=true, onClick:()->Unit) {
    Button(onClick=onClick, enabled=enabled,
        shape=RoundedCornerShape(15.dp),
        modifier=Modifier.fillMaxWidth().height(50.dp),
        colors=ButtonDefaults.buttonColors(containerColor=SGreen)) {
        Text(label,fontSize=14.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.ArrowForward,null,modifier=Modifier.size(17.dp))
    }
}

@Composable
internal fun DemoLine(label:String,value:String) {
    Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.Top) {
        Text(label,modifier=Modifier.weight(.44f),fontSize=12.sp,color=SSecondary)
        Text(value,modifier=Modifier.weight(.56f),fontSize=13.sp,color=SInk,fontWeight=FontWeight.SemiBold)
    }
}

@Composable
internal fun DemoChip(label:String,chosen:Boolean,onClick:()->Unit) {
    Surface(onClick=onClick,color=if(chosen) SGreen else SBack,shape=RoundedCornerShape(11.dp)) {
        Box(Modifier.padding(horizontal=13.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
            Text(label,color=if(chosen)Color.White else SInk,
                fontSize=12.sp,fontWeight=if(chosen)FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun FlowPanel(
    state: ShowcaseSession,
    chosen: DemoVehicle?,
    hours:Int,onHours:(Int)->Unit,
    day:String,onDay:(String)->Unit,
    time:String,onTime:(String)->Unit,
    act: (() -> ShowcaseSession)->Unit,
    onAdmin:()->Unit,
    onNotice:(String)->Unit
) {
    var code by remember { mutableStateOf("0000") }
    var demoSms by remember { mutableStateOf(false) }
    var passport by remember { mutableStateOf(false) }
    var licence by remember { mutableStateOf(false) }
    var accept by remember { mutableStateOf(false) }
    var inspected by remember { mutableStateOf(false) }
    var inspectedReturn by remember { mutableStateOf(false) }
    var payMethod by remember { mutableStateOf("Карта") }
    Surface(color=Color.White,shape=RoundedCornerShape(20.dp),shadowElevation=1.dp) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            when(state.stage) {
                ShowcaseStage.PHONE -> {
                    DemoTitle("Добро пожаловать","Регистрация клиента: демонстрация по шагам")
                    InfoPanel("Тестовый номер", "+7 (900) 000-00-00. Настоящие данные не вводите. СМС не отправляется.")
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(value=code,onValueChange={code=it},label={Text("Демо-код подтверждения")},
                        singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick={demoSms=true},modifier=Modifier.fillMaxWidth()) {
                        Text("Имитировать отправку СМС")
                    }
                    if(demoSms) Text("Пример полученного кода: 0000",fontSize=12.sp,color=SGreen)
                    Spacer(Modifier.height(12.dp))
                    DemoPrimary("Подтвердить номер") { act { ShowcaseEngine.verifyPhone(state,code) } }
                }
                ShowcaseStage.DOCUMENTS -> {
                    DemoTitle("Проверка документов","Так клиент проходит идентификацию перед первым заказом")
                    InfoPanel("Только учебные образцы","Не загружайте настоящий паспорт и права. Ни одна фотография не сохраняется.")
                    Spacer(Modifier.height(12.dp))
                    DemoCheckRow("Паспорт РФ","Посмотреть образец и отметить загрузку",passport,
                        onToggle={passport=it}, onDetails={
                            onNotice("В рабочей версии: сканирование паспорта, извлечение реквизитов, проверка достоверности и защита данных. Здесь используются только условные образцы.")
                        })
                    Spacer(Modifier.height(9.dp))
                    DemoCheckRow("Водительское удостоверение","Категория B, срок и стаж",licence,
                        onToggle={licence=it}, onDetails={
                            onNotice("В рабочей версии: лицевая/оборотная стороны, категория допуска и проверка статуса. Сейчас подтверждение полностью условное.")
                        })
                    Spacer(Modifier.height(13.dp))
                    DemoPrimary("Отправить на проверку",enabled=passport && licence) {
                        act { ShowcaseEngine.submitDocuments(state,passport,licence) }
                    }
                }
                ShowcaseStage.REVIEW -> {
                    DemoTitle("Заявка на проверке","В реальном сервисе документы проверяет уполномоченный оператор")
                    InfoPanel("Статус: проверяется","Заявка поступила в демонстрационный кабинет администратора.")
                    Spacer(Modifier.height(15.dp))
                    DemoPrimary("Перейти к администратору",onClick=onAdmin)
                    Spacer(Modifier.height(8.dp))
                    Text("Там можно одобрить или отклонить проверку. После одобрения вернитесь в раздел «Главная».",
                        color=SSecondary,fontSize=12.sp,lineHeight=17.sp)
                }
                ShowcaseStage.READY -> {
                    DemoTitle("Забронировать грузовик","Выберите место получения, дату и тариф")
                    if(chosen==null) {
                        InfoPanel("Автомобили отключены","Включите хотя бы один грузовик в разделе управления.")
                    } else {
                        DemoLine("Автомобиль",chosen.title+" · "+chosen.kind)
                        DemoLine("Грузоподъёмность",chosen.payload+" · "+chosen.volume)
                        DemoLine("Место получения",chosen.pickup)
                        Spacer(Modifier.height(9.dp))
                        Text("Дата",fontSize=13.sp,fontWeight=FontWeight.Bold,color=SInk)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(7.dp),
                            modifier=Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("Сегодня","Завтра","Послезавтра").forEach {
                                DemoChip(it,day==it){onDay(it)}
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Время начала",fontSize=13.sp,fontWeight=FontWeight.Bold,color=SInk)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(7.dp),
                            modifier=Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("09:00","12:00","15:00","18:00").forEach {
                                DemoChip(it,time==it){onTime(it)}
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Продолжительность",fontSize=13.sp,fontWeight=FontWeight.Bold,color=SInk)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(7.dp),
                            modifier=Modifier.horizontalScroll(rememberScrollState())) {
                            RentalRules.allowedHours.forEach {
                                DemoChip(if(it==24)"1 сутки" else "${it} ч",hours==it){onHours(it)}
                            }
                        }
                        Spacer(Modifier.height(15.dp))
                        DemoLine("Аренда",sumRub(RentalRules.fare(chosen,hours)))
                        DemoLine("Залог",sumRub(RentalRules.DEMO_DEPOSIT))
                        Spacer(Modifier.height(8.dp))
                        DemoPrimary("Перейти к договору") {
                            act { ShowcaseEngine.reserve(state,chosen.id,hours,day,time,
                                chosen.title,chosen.pickup,RentalRules.fare(chosen,hours)) }
                        }
                    }
                }
                ShowcaseStage.CONTRACT -> {
                    DemoTitle("Электронный договор","Предварительный просмотр и имитация подписания")
                    InfoPanel("ДЕМО-ДОГОВОР № 0001","Учебный текст. Не является юридически действующим документом.")
                    Spacer(Modifier.height(12.dp))
                    DemoLine("Предмет","Аренда ${state.vehicleTitle} без водителя")
                    DemoLine("Место",state.pickup)
                    DemoLine("Срок","${state.day}, ${state.time} · ${state.hours} ч")
                    DemoLine("Арендная плата",sumRub(state.fare))
                    DemoLine("Возвратный залог",sumRub(RentalRules.DEMO_DEPOSIT))
                    Spacer(Modifier.height(5.dp))
                    Text("Арендатор обязан иметь действующее удостоверение нужной категории, соблюдать ПДД, правила загрузки и возврата. Передача управления третьим лицам без согласования запрещена.",
                        fontSize=12.sp,color=SSecondary,lineHeight=18.sp)
                    Spacer(Modifier.height(9.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=accept,onCheckedChange={accept=it})
                        Text("Я ознакомился с образцом договора",fontSize=13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    DemoPrimary("Подписать демодоговор",enabled=accept) {
                        act { ShowcaseEngine.signContract(state,accept) }
                    }
                }
                ShowcaseStage.PAYMENT -> {
                    DemoTitle("Оплата и залог","Полная имитация эквайринга без реального списания")
                    DemoLine("Стоимость аренды",sumRub(state.fare))
                    DemoLine("Обеспечительный платёж",sumRub(RentalRules.DEMO_DEPOSIT))
                    HorizontalDivider(color=SBorder,modifier=Modifier.padding(vertical=5.dp))
                    DemoLine("Итого в демо",sumRub(state.fare+RentalRules.DEMO_DEPOSIT))
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                        listOf("Карта","СБП").forEach {
                            DemoChip(it,payMethod==it){payMethod=it}
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    InfoPanel(if(payMethod=="Карта")"Тестовая банковская карта" else "Тестовый перевод СБП",
                        if(payMethod=="Карта")"•••• 4242 · банк и деньги не задействованы."
                        else "QR и банковское приложение не открываются. Платёж симулируется локально.")
                    Spacer(Modifier.height(15.dp))
                    OutlinedButton(
                        onClick={
                            act { ShowcaseEngine.simulatePayment(state,false) }
                            onNotice("Симуляция: банк отклонил операцию. Суммы не списаны, повторная попытка доступна.")
                        },modifier=Modifier.fillMaxWidth()) {
                        Text("Показать ошибку оплаты")
                    }
                    Spacer(Modifier.height(8.dp))
                    DemoPrimary("Оплатить (демо)") {
                        act { ShowcaseEngine.simulatePayment(state,true) }
                    }
                }
                ShowcaseStage.HANDOVER -> {
                    DemoTitle("Получение автомобиля","Проверка состояния перед передачей ключей")
                    InfoPanel("Оплата выполнена · Демонстрация","Аренда ${sumRub(state.fare)}, залог ${sumRub(RentalRules.DEMO_DEPOSIT)} — условно удержан.")
                    Spacer(Modifier.height(13.dp))
                    DemoLine("Грузовик",state.vehicleTitle)
                    DemoLine("Пункт выдачи",state.pickup)
                    DemoLine("Одометр до выезда","42 150 км (пример)")
                    DemoLine("Уровень топлива","¾ бака (пример)")
                    Spacer(Modifier.height(9.dp))
                    DemoCheckRow("Фотоосмотр до аренды","Кузов, салон, колёса, приборная панель",
                        inspected,onToggle={inspected=it},onDetails={
                            onNotice("Реальная версия предложит сделать минимум 4 снимка автомобиля и сохранить их в акте выдачи.")
                        })
                    Spacer(Modifier.height(15.dp))
                    DemoPrimary("Получить автомобиль",enabled=inspected) {
                        act { ShowcaseEngine.handover(state,inspected) }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text("Имитация выдачи — реальные двери и двигатель не управляются.",
                        color=SSecondary,fontSize=11.sp)
                }
                ShowcaseStage.DRIVING -> {
                    DemoTitle("Вы арендуете автомобиль","Карточка активной поездки в стиле каршеринга")
                    InfoPanel("В пути · Аренда активна","ГАЗель получена · ${state.vehicleTitle} · ${state.pickup}")
                    Spacer(Modifier.height(11.dp))
                    DemoLine("Дата старта",state.day+" · "+state.time)
                    DemoLine("Аренда",if(state.hours==24)"1 сутки" else "${state.hours} часа")
                    DemoLine("Продление",if(state.extraHours>0) "${state.extraHours} ч · ${sumRub(state.extensionCharge)}" else "Не оформлено")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                        OutlinedButton(onClick={act { ShowcaseEngine.toggleDoors(state) }},
                            modifier=Modifier.weight(1f)) {
                            Icon(if(state.doorsOpen)Icons.Rounded.Lock else Icons.Rounded.LockOpen,null,
                                modifier=Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if(state.doorsOpen)"Закрыть" else "Открыть",fontSize=12.sp)
                        }
                        OutlinedButton(onClick={
                            act { ShowcaseEngine.extend(state,2,
                                demoFleet.firstOrNull{it.id==state.vehicleId}?.hourPrice ?: 650) }
                            onNotice("Срок увеличен на 2 часа только в демонстрации. Дополнительная сумма не списывается.")
                        },modifier=Modifier.weight(1f)) {
                            Icon(Icons.Rounded.AddCircleOutline,null,modifier=Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ 2 часа",fontSize=12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick={
                        act { ShowcaseEngine.reportIncident(state) }
                        onNotice("Создано демо-обращение о ДТП или поломке. В рабочем сервисе здесь будут инструкции, связь с оператором и фотофиксация.")
                    },modifier=Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.WarningAmber,null,modifier=Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if(state.incidentReported)"Инцидент зарегистрирован" else "ДТП / поломка",fontSize=13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    DemoPrimary("Завершить поездку") {
                        act { ShowcaseEngine.requestReturn(state) }
                    }
                    Text("Кнопки управления замками работают только визуально — телематика не подключена.",
                        fontSize=11.sp,color=SSecondary,modifier=Modifier.padding(top=8.dp))
                }
                ShowcaseStage.RETURN -> {
                    DemoTitle("Возврат грузовика","Финальный осмотр, закрытие аренды и залога")
                    DemoLine("Место возврата",state.pickup)
                    DemoLine("Одометр после поездки","42 184 км (пример)")
                    DemoLine("Топливо после поездки","¾ бака (пример)")
                    DemoLine("Доп. время",sumRub(state.extensionCharge))
                    Spacer(Modifier.height(9.dp))
                    DemoCheckRow("Фотоосмотр после аренды","Кузов, салон, колёса и остаток топлива",
                        inspectedReturn,onToggle={inspectedReturn=it},
                        onDetails={onNotice("В реальной версии новые фотографии сравниваются с актом выдачи; спорные повреждения передаются оператору.")})
                    Spacer(Modifier.height(11.dp))
                    InfoPanel("Итог без повреждений","В этой демонстрации залог ${sumRub(RentalRules.DEMO_DEPOSIT)} будет полностью возвращён условной операцией.")
                    Spacer(Modifier.height(14.dp))
                    DemoPrimary("Сдать машину и вернуть залог",enabled=inspectedReturn) {
                        act { ShowcaseEngine.completeReturn(state,inspectedReturn) }
                    }
                }
                ShowcaseStage.FINISHED -> {
                    DemoTitle("Аренда завершена","Весь процесс от регистрации до возврата успешно пройден")
                    Surface(color=SPale,shape=RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(17.dp),
                            horizontalAlignment=Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.CheckCircle,null,tint=SGreen,modifier=Modifier.size(44.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Залог возвращён",fontSize=18.sp,fontWeight=FontWeight.ExtraBold,color=SGreen)
                            Text(sumRub(RentalRules.DEMO_DEPOSIT),fontSize=24.sp,
                                fontWeight=FontWeight.Bold,color=SGreen)
                            Text("Симуляция возврата, не банковский перевод",
                                color=SSecondary,fontSize=11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    DemoLine("Автомобиль",state.vehicleTitle)
                    DemoLine("Аренда",sumRub(state.fare))
                    DemoLine("Продление",sumRub(state.extensionCharge))
                    DemoLine("Итоговая стоимость",sumRub(state.fare+state.extensionCharge))
                    DemoLine("Чек","Демо-чек № 0001")
                    Spacer(Modifier.height(10.dp))
                    DemoPrimary("Посмотреть итоговый документ") {
                        onNotice("ДЕМО-ЧЕК № 0001\nАвтомобиль: ${state.vehicleTitle}\nАренда: ${sumRub(state.fare)}\nПродление: ${sumRub(state.extensionCharge)}\nЗалог: ${sumRub(RentalRules.DEMO_DEPOSIT)} — возвращён.\nДокумент не фискальный.")
                    }
                }
                else -> { Text("Продолжите оформление в новой версии главного экрана") }
            }
        }
    }
}

@Composable
private fun DemoCheckRow(title:String,subtitle:String,checked:Boolean,
    onToggle:(Boolean)->Unit,onDetails:()->Unit) {
    Surface(color=SBack,shape=RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
            Checkbox(checked=checked,onCheckedChange=onToggle)
            Column(Modifier.weight(1f)) {
                Text(title,fontSize=13.sp,fontWeight=FontWeight.Bold,color=SInk)
                Text(subtitle,color=SSecondary,fontSize=11.sp,lineHeight=15.sp)
            }
            IconButton(onClick=onDetails) {
                Icon(Icons.Rounded.Info,null,tint=SGreen,modifier=Modifier.size(19.dp))
            }
        }
    }
}
