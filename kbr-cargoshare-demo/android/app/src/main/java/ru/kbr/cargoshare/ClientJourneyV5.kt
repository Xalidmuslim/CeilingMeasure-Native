package ru.kbr.cargoshare

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Pine = Color(0xFF215340)
private val Ink = Color(0xFF192620)
private val Grey = Color(0xFF737E74)
private val Mist = Color(0xFFE8F0E6)
private val CreamBg = Color(0xFFF6F6F2)
private val DividerBg = Color(0xFFE6E9E2)
private val Sky = Color(0xFFDBE5DD)

@Composable
fun ClientJourneyV5(
    state:ShowcaseSession,
    fleet:List<DemoVehicle>,
    dispatch:(()->ShowcaseSession)->Unit,
    onAdmin:()->Unit,
    onInfo:(String)->Unit,
    onNewDemo:()->Unit
) {
    var code by remember { mutableStateOf("0000") }
    var passport by remember { mutableStateOf(false) }
    var license by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf(fleet.firstOrNull{it.enabled}?.id ?: "") }
    var day by remember { mutableStateOf("Сегодня") }
    var time by remember { mutableStateOf("12:00") }
    var hours by remember { mutableIntStateOf(4) }
    var agreed by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("Банковская карта") }
    var photosDone by remember { mutableStateOf(false) }
    var body by remember { mutableStateOf(false) }
    var wheels by remember { mutableStateOf(false) }
    var cabin by remember { mutableStateOf(false) }
    var fuel by remember { mutableStateOf(false) }
    var returnBody by remember { mutableStateOf(false) }
    var returnFuel by remember { mutableStateOf(false) }
    var returnBelongings by remember { mutableStateOf(false) }
    var returnLocked by remember { mutableStateOf(false) }
    val available=fleet.filter{it.enabled}
    val vehicle=available.firstOrNull{it.id==selectedId} ?: available.firstOrNull()
    val bookedVehicle=fleet.firstOrNull{it.id==state.vehicleId} ?: demoFleet.firstOrNull{it.id==state.vehicleId}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=14.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(11.dp)) {

        if(state.stage != ShowcaseStage.DRIVING && state.stage != ShowcaseStage.FINISHED) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(when(state.stage) {
                        ShowcaseStage.PHONE -> "Начало аренды"
                        ShowcaseStage.DOCUMENTS -> "Ваши документы"
                        ShowcaseStage.REVIEW -> "Проверка водителя"
                        ShowcaseStage.READY -> "Выберите автомобиль"
                        ShowcaseStage.CONTRACT -> "Оформление аренды"
                        ShowcaseStage.PAYMENT -> "Оплата"
                        ShowcaseStage.HANDOVER -> "Получение машины"
                        ShowcaseStage.PICKUP_INSPECTION -> "Осмотр автомобиля"
                        ShowcaseStage.UNLOCK -> "Открытие автомобиля"
                        ShowcaseStage.READY_TO_DRIVE -> "Начало поездки"
                        ShowcaseStage.RETURN -> "Возврат автомобиля"
                        else -> "Ваша аренда"
                    },fontSize=20.sp,fontWeight=FontWeight.ExtraBold,color=Ink)
                    Text("Грузовой каршеринг · Нальчик и КБР",color=Grey,fontSize=11.sp)
                }
                Surface(color=Mist,shape=RoundedCornerShape(11.dp)) {
                    Row(Modifier.padding(horizontal=9.dp,vertical=7.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Verified,null,tint=Pine,modifier=Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${PresentationV7.groups.indexOf(PresentationV7.stepFor(state.stage))+1}/${PresentationV7.groups.size}",
                            color=Pine,fontSize=12.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }

        when(state.stage) {
            ShowcaseStage.PHONE -> {
                V5Card {
                    V5StepHead(Icons.Rounded.PhoneAndroid,"Регистрация","СМС и профиль клиента")
                    Spacer(Modifier.height(12.dp))
                    V5Note("Учебный телефон: +7 (900) 000-00-00. Код 0000. Настоящие СМС не отправляются.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value=code,onValueChange={code=it.take(4)},
                        label={Text("Код подтверждения")},
                        singleLine=true,modifier=Modifier.fillMaxWidth())
                    Spacer(Modifier.height(13.dp))
                    V5MainAction("Продолжить",enabled=code.length==4) {
                        dispatch { ShowcaseEngine.verifyPhone(state,code) }
                    }
                }
                V5Guide("Зачем проверять водителя?",
                    "После регистрации клиент подтверждает документы один раз. В рабочем сервисе доступ к аренде появится только после настоящей проверки.")
            }
            ShowcaseStage.DOCUMENTS -> {
                V5Card {
                    V5StepHead(Icons.Rounded.Badge,"Документы водителя","Проверка до первой аренды")
                    Spacer(Modifier.height(12.dp))
                    V5Note("Демо-проверка происходит автоматически сразу после подтверждения обоих образцов. Настоящие документы не загружайте.")
                    Spacer(Modifier.height(10.dp))
                    V5Toggle("Паспорт РФ","Условная загрузка разворота и регистрации",passport){passport=it}
                    Spacer(Modifier.height(7.dp))
                    V5Toggle("Водительское удостоверение","Лицевая и обратная стороны, категория B",license){license=it}
                    Spacer(Modifier.height(14.dp))
                    V5MainAction("Подтвердить документы автоматически",enabled=passport&&license) {
                        dispatch { ShowcaseEngine.submitDocuments(state,passport,license) }
                    }
                }
                V5Guide("Что делает система?",
                    "В реальной версии оператор проверяет личность, право управления, категорию и ограничения, а клиент видит результат в профиле.")
            }
            ShowcaseStage.REVIEW -> {
                V5Card {
                    V5StepHead(Icons.Rounded.CheckCircle,"Проверка завершена","Документы подтверждены автоматически в демо")
                    Spacer(Modifier.height(12.dp))
                    V5Note("Сведения подтверждены в демонстрационном режиме. Дополнительного одобрения администратора не требуется.")
                    Spacer(Modifier.height(15.dp))
                    V5MainAction("Продолжить к выбору машины") {
                        dispatch { ShowcaseEngine.resume(state) }
                    }
                    Spacer(Modifier.height(9.dp))
                    Text("Проверка не повторяется при следующей аренде.",
                        color=Grey,fontSize=11.sp,lineHeight=17.sp)
                }
            }
            ShowcaseStage.READY -> {
                V5Card {
                    V5StepHead(Icons.Rounded.Place,"Место получения","Нальчик · автомобили на демонстрационной карте")
                    Spacer(Modifier.height(10.dp))
                    MovableDemoTruckMap(
                        fleet = available, selectedId = vehicle?.id ?: "",
                        onSelect = { selectedId = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    V5SectionTitle("Свободные грузовики")
                    if(available.isEmpty()) V5Note("Нет машин в демокаталоге. Включите их в кабинете владельца.")
                    available.forEach { v ->
                        VehicleOfferV5(v,chosen=vehicle?.id==v.id) { selectedId=v.id }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if(vehicle!=null) V5Card {
                    V5SectionTitle("Когда заберёте автомобиль?")
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        listOf("Сегодня","Завтра","Послезавтра").forEach { d ->
                            V5Pill(d,d==day){day=d}
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        listOf("09:00","12:00","15:00","18:00").forEach { t ->
                            V5Pill(t,t==time){time=t}
                        }
                    }
                    V5SectionTitle("Срок аренды")
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        RentalRules.allowedHours.forEach { h ->
                            V5Pill(if(h==24)"Сутки" else "${h} ч",h==hours){hours=h}
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    V5Item("Аренда",sumRub(RentalRules.fare(vehicle,hours)),emphasis=true)
                    V5Item("Залог (пример)",sumRub(RentalRules.DEMO_DEPOSIT))
                    V5Item("Получение",vehicle.pickup)
                    Spacer(Modifier.height(10.dp))
                    V5MainAction("Перейти к договору") {
                        dispatch {
                            ShowcaseEngine.reserve(state,vehicle.id,hours,day,time,
                                vehicle.title,vehicle.pickup,RentalRules.fare(vehicle,hours))
                        }
                    }
                }
            }
            ShowcaseStage.CONTRACT -> {
                V5Card {
                    V5StepHead(Icons.Rounded.Description,"Договор аренды","Электронное подписание · имитация")
                    if(bookedVehicle!=null) {
                        Spacer(Modifier.height(9.dp))
                        VehiclePhotoV7(bookedVehicle,Modifier.fillMaxWidth().height(135.dp),true)
                    }
                    Spacer(Modifier.height(10.dp))
                    V5Note("ОБРАЗЕЦ. Никакого юридически значимого договора сейчас не заключается.")
                    Spacer(Modifier.height(12.dp))
                    V5Item("Автомобиль",state.vehicleTitle)
                    V5Item("Выдача",state.pickup)
                    V5Item("Начало",state.day+" · "+state.time)
                    V5Item("Продолжительность",if(state.hours==24)"Сутки" else "${state.hours} ч")
                    V5Item("Аренда",sumRub(state.fare))
                    V5Item("Залог",sumRub(RentalRules.DEMO_DEPOSIT))
                    HorizontalDivider(color=DividerBg,modifier=Modifier.padding(vertical=10.dp))
                    Text("Клиент обязан соблюдать ПДД, не передавать руль посторонним, соблюдать ограничения по грузу, зафиксировать состояние кузова до поездки и вернуть машину в согласованное место.",
                        fontSize=12.sp,color=Grey,lineHeight=18.sp)
                    Spacer(Modifier.height(9.dp))
                    V5Toggle("Условия просмотрены","Имитация согласия с демодоговором",agreed){agreed=it}
                    Spacer(Modifier.height(13.dp))
                    V5MainAction("Подписать и продолжить",enabled=agreed) {
                        dispatch { ShowcaseEngine.signContract(state,agreed) }
                    }
                }
            }
            ShowcaseStage.PAYMENT -> {
                V5Card {
                    V5StepHead(Icons.Rounded.CreditCard,"Оплата аренды","Без банковского подключения")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        V5Pill("Банковская карта",paymentMethod=="Банковская карта"){paymentMethod="Банковская карта"}
                        V5Pill("СБП",paymentMethod=="СБП"){paymentMethod="СБП"}
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(color=Ink,shape=RoundedCornerShape(17.dp)) {
                        Column(Modifier.fillMaxWidth().padding(17.dp)) {
                            Icon(Icons.Rounded.Contactless,null,tint=Color.White,
                                modifier=Modifier.size(26.dp))
                            Spacer(Modifier.height(18.dp))
                            Text(if(paymentMethod=="СБП")"Тестовое СБП" else "DEMO •••• 4242",
                                color=Color.White,fontWeight=FontWeight.Bold)
                            Text("Реальной оплаты нет",color=Color.White.copy(alpha=.7f),fontSize=11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    V5Item("Аренда",sumRub(state.fare))
                    V5Item("Возвратный залог",sumRub(RentalRules.DEMO_DEPOSIT))
                    HorizontalDivider(color=DividerBg)
                    V5Item("Итого условно",sumRub(state.fare+RentalRules.DEMO_DEPOSIT),emphasis=true)
                    Spacer(Modifier.height(9.dp))
                    OutlinedButton(onClick={
                        dispatch { ShowcaseEngine.simulatePayment(state,false) }
                        onInfo("Демо: банк отклонил оплату. Заказ остаётся на этапе оплаты, повторить можно сразу.")
                    },modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)) {
                        Text("Проверить сценарий отказа",fontSize=12.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                    V5MainAction("Подтвердить демооплату") {
                        dispatch { ShowcaseEngine.simulatePayment(state,true) }
                    }
                }
            }
            ShowcaseStage.HANDOVER -> {
                V5Card {
                    V5StepHead(Icons.Rounded.Navigation,"Подойти к автомобилю","Передача грузовика без сотрудника — демонстрация")
                    if(bookedVehicle!=null) {
                        Spacer(Modifier.height(9.dp))
                        VehiclePhotoV7(bookedVehicle,Modifier.fillMaxWidth().height(125.dp),true)
                    }
                    Spacer(Modifier.height(11.dp))
                    V5Item("Автомобиль",state.vehicleTitle)
                    V5Item("Точка выдачи",state.pickup)
                    V5Item("Начало",state.day+" · "+state.time)
                    Spacer(Modifier.height(9.dp))
                    MovableDemoTruckMap(
                        fleet = fleet.filter { it.enabled && it.id == state.vehicleId },
                        selectedId = state.vehicleId,
                        onSelect = { }
                    )
                    Spacer(Modifier.height(10.dp))
                    V5Note("Оплата и залог условно подтверждены. На следующем экране проверьте номер машины, кузов и салон.")
                    Spacer(Modifier.height(13.dp))
                    V5MainAction("Я возле автомобиля") {
                        dispatch { ShowcaseEngine.arriveAtVehicle(state) }
                    }
                }
            }
            ShowcaseStage.PICKUP_INSPECTION -> {
                V5Card {
                    V5StepHead(Icons.Rounded.FactCheck,"Проверка перед выездом","Чек-лист осмотра автомобиля")
                    Spacer(Modifier.height(10.dp))
                    V5Note("Реальное приложение потребует фотографии. Здесь отмечайте только условный фотоосмотр.")
                    Spacer(Modifier.height(10.dp))
                    V5Toggle("Кузов и тент","Осмотр всех сторон, дверей и повреждений",body){body=it}
                    Spacer(Modifier.height(6.dp))
                    V5Toggle("Шины и колёса","Давление визуально, диски, повреждения",wheels){wheels=it}
                    Spacer(Modifier.height(6.dp))
                    V5Toggle("Кабина и документы","Салон, приборы, ключи, аптечка",cabin){cabin=it}
                    Spacer(Modifier.height(6.dp))
                    V5Toggle("Пробег и топливо","42 150 км · 75% топлива (пример)",fuel){fuel=it}
                    Spacer(Modifier.height(13.dp))
                    V5MainAction("Осмотр завершён",enabled=body&&wheels&&cabin&&fuel) {
                        dispatch { ShowcaseEngine.confirmPickupInspection(state,body,wheels,cabin,fuel) }
                    }
                }
            }
            ShowcaseStage.UNLOCK -> {
                V5Card {
                    V5StepHead(Icons.Rounded.LockOpen,"Открыть автомобиль","Имитация дистанционного доступа")
                    Spacer(Modifier.height(11.dp))
                    V5Note("Удержание кнопки в рабочем сервисе отправит защищённую команду телематическому устройству. Сейчас это только изменение статуса на экране.")
                    Spacer(Modifier.height(12.dp))
                    DemoVehicleBadge(state.vehicleTitle,"Осмотр пройден · двери закрыты")
                    Spacer(Modifier.height(15.dp))
                    V5MainAction("Открыть двери (демо)") {
                        dispatch { ShowcaseEngine.unlockForPickup(state) }
                    }
                }
            }
            ShowcaseStage.READY_TO_DRIVE -> {
                V5Card {
                    V5StepHead(Icons.Rounded.CheckCircle,"Двери открыты","Автомобиль готов к поездке")
                    Spacer(Modifier.height(11.dp))
                    DemoVehicleBadge(state.vehicleTitle,"Доступ открыт в деморежиме")
                    Spacer(Modifier.height(11.dp))
                    V5Item("Одометр на старте","42 150 км")
                    V5Item("Топливо на старте","75%")
                    V5Item("Длительность",if(state.hours==24)"1 сутки" else "${state.hours} часа")
                    Spacer(Modifier.height(10.dp))
                    V5Note("Никакой реальный двигатель не запускается и двери не открываются. Следующий шаг включит виртуальную поездку.")
                    Spacer(Modifier.height(13.dp))
                    V5MainAction("Начать поездку") {
                        dispatch { ShowcaseEngine.startDriving(state) }
                    }
                }
            }
            ShowcaseStage.DRIVING -> TripDashboardV5(state,fleet,dispatch,onInfo)
            ShowcaseStage.RETURN -> {
                V5Card {
                    V5StepHead(Icons.Rounded.AssignmentTurnedIn,"Вернуть грузовик","Проверка машины и закрытие аренды")
                    Spacer(Modifier.height(11.dp))
                    V5Item("Пункт возврата",state.pickup)
                    V5Item("Пробег при выдаче",formatKm(state.odometerStart))
                    V5Item("Пробег при возврате",formatKm(state.odometerStart+state.distanceKm))
                    V5Item("Топливо",state.fuelPercent.toString()+"%")
                    V5Item("Время использования",formatMinutes(state.elapsedMinutes))
                    Spacer(Modifier.height(9.dp))
                    V5Toggle("Машина осмотрена","Фото всех сторон автомобиля (имитация)",returnBody){returnBody=it}
                    Spacer(Modifier.height(6.dp))
                    V5Toggle("Топливо и пробег сверены","Показания указаны в акте возврата",returnFuel){returnFuel=it}
                    Spacer(Modifier.height(6.dp))
                    V5Toggle("Личные вещи забраны","Кабина и грузовой отсек пустые",returnBelongings){returnBelongings=it}
                    Spacer(Modifier.height(6.dp))
                    V5Toggle("Двери закрыты","Имитация блокировки замков",returnLocked){returnLocked=it}
                    Spacer(Modifier.height(12.dp))
                    V5Item("Стоимость аренды",sumRub(state.fare+state.extensionCharge),emphasis=true)
                    V5Item("Залог к возврату",sumRub(RentalRules.DEMO_DEPOSIT))
                    Spacer(Modifier.height(10.dp))
                    V5MainAction("Подтвердить возврат",enabled=returnBody&&returnFuel&&returnBelongings&&returnLocked) {
                        dispatch { ShowcaseEngine.completeReturn(state,true) }
                    }
                }
            }
            ShowcaseStage.FINISHED -> {
                V5Card {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle,null,tint=Pine,modifier=Modifier.size(32.dp))
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("Аренда завершена",fontWeight=FontWeight.ExtraBold,color=Ink,fontSize=20.sp)
                            Text("Автомобиль возвращён · чек сформирован в демо",
                                color=Grey,fontSize=11.sp)
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    V5Item("Автомобиль",state.vehicleTitle)
                    V5Item("Время поездки",formatMinutes(state.elapsedMinutes))
                    V5Item("Дистанция",state.distanceKm.toString()+" км")
                    V5Item("Аренда",sumRub(state.fare))
                    V5Item("Продление",sumRub(state.extensionCharge))
                    HorizontalDivider(color=DividerBg,modifier=Modifier.padding(vertical=8.dp))
                    V5Item("Всего за аренду",sumRub(state.fare+state.extensionCharge),emphasis=true)
                    V5Item("Возвращённый залог",sumRub(RentalRules.DEMO_DEPOSIT))
                    Spacer(Modifier.height(11.dp))
                    V5Note("Залог возвращён только в демонстрационном учёте. Фискального чека и банковского возврата нет.")
                    Spacer(Modifier.height(13.dp))
                    V5MainAction("Взять следующий автомобиль",click=onNewDemo)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun V5StepHead(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,caption:String) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(39.dp).background(Mist,RoundedCornerShape(12.dp)),
            contentAlignment=Alignment.Center) {
            Icon(icon,null,tint=Pine,modifier=Modifier.size(22.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title,color=Ink,fontSize=17.sp,fontWeight=FontWeight.ExtraBold)
            Text(caption,color=Grey,fontSize=11.sp)
        }
    }
}

@Composable
private fun VehicleOfferV5(v:DemoVehicle,chosen:Boolean,onClick:()->Unit) {
    Surface(onClick=onClick,shape=RoundedCornerShape(14.dp),
        color=if(chosen)Mist else CreamBg,
        border=androidx.compose.foundation.BorderStroke(
            if(chosen)1.3.dp else .7.dp,if(chosen)Pine else DividerBg)) {
        Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).background(Color.White,RoundedCornerShape(11.dp)),
                contentAlignment=Alignment.Center) {
                Icon(Icons.Rounded.LocalShipping,null,tint=Pine,modifier=Modifier.size(37.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(v.title,fontWeight=FontWeight.ExtraBold,fontSize=14.sp,color=Ink)
                Text(v.kind+" · "+v.payload+" · "+v.volume,fontSize=11.sp,color=Grey)
                Text(v.pickup,fontSize=10.sp,color=Grey)
                Spacer(Modifier.height(3.dp))
                Text("от "+sumRub(v.hourPrice)+"/ч · "+sumRub(v.dayPrice)+"/сут",
                    fontSize=11.sp,fontWeight=FontWeight.Bold,color=Pine)
            }
            Icon(if(chosen)Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                null,tint=if(chosen)Pine else Grey,modifier=Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DemoVehicleBadge(name:String,status:String) {
    Surface(color=Mist,shape=RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalShipping,null,tint=Pine,modifier=Modifier.size(39.dp))
            Spacer(Modifier.width(13.dp))
            Column {
                Text(name,fontWeight=FontWeight.Bold,color=Ink,fontSize=15.sp)
                Text(status,color=Grey,fontSize=11.sp)
            }
        }
    }
}

@Composable
private fun V5Guide(title:String,details:String) {
    V5Card {
        Row(verticalAlignment=Alignment.Top) {
            Icon(Icons.Rounded.Security,null,tint=Pine,modifier=Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Column {
                Text(title,fontSize=13.sp,fontWeight=FontWeight.Bold,color=Ink)
                Spacer(Modifier.height(4.dp))
                Text(details,fontSize=11.sp,lineHeight=17.sp,color=Grey)
            }
        }
    }
}

internal fun formatMinutes(mins:Int):String {
    val h=mins / 60
    val m=mins % 60
    return if(h>0) "${h} ч ${m} мин" else "${m} мин"
}
internal fun formatKm(km:Int):String = "%,d км".format(java.util.Locale("ru","RU"),km)
