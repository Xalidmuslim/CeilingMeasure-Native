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

private val TGreen=Color(0xFF215340)
private val TInk=Color(0xFF192620)
private val TMuted=Color(0xFF737E74)
private val TBack=Color(0xFFF6F6F2)
private val TSoft=Color(0xFFE8F0E6)
private val TLine=Color(0xFFE6E9E2)
private val TGold=Color(0xFFFFEAC5)

@Composable
fun TripDashboardV5(
    state:ShowcaseSession,
    fleet:List<DemoVehicle>,
    dispatch:(()->ShowcaseSession)->Unit,
    onInfo:(String)->Unit
) {
    val total=(state.hours+state.extraHours)*60
    val remaining=ShowcaseEngine.remainingMinutes(state)
    val used=state.elapsedMinutes
    val progress=(used.toFloat()/total.coerceAtLeast(1)).coerceIn(0f,1f)
    val hourRate=fleet.firstOrNull{it.id==state.vehicleId}?.hourPrice
        ?: demoFleet.firstOrNull{it.id==state.vehicleId}?.hourPrice ?: 650
    var incidentMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(11.dp)) {
        Surface(color=TGreen,shape=RoundedCornerShape(19.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Surface(color=Color.White.copy(alpha=.15f),shape=RoundedCornerShape(7.dp)) {
                        Text("АРЕНДА АКТИВНА",modifier=Modifier.padding(horizontal=8.dp,vertical=5.dp),
                            color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Wifi,null,tint=Color.White,modifier=Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("GPS: демо",color=Color.White.copy(alpha=.8f),fontSize=10.sp)
                }
                Spacer(Modifier.height(15.dp))
                Text(state.vehicleTitle,fontSize=24.sp,color=Color.White,
                    fontWeight=FontWeight.ExtraBold)
                Text("Получение: "+state.pickup,color=Color.White.copy(alpha=.8f),fontSize=11.sp)
                Spacer(Modifier.height(18.dp))
                Text("Осталось времени",color=Color.White.copy(alpha=.8f),fontSize=12.sp)
                Spacer(Modifier.height(2.dp))
                Text(formatMinutes(remaining),color=Color.White,fontWeight=FontWeight.ExtraBold,
                    fontSize=34.sp)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress={progress},color=Color(0xFFD6EEA7),
                    trackColor=Color.White.copy(alpha=.23f),
                    modifier=Modifier.fillMaxWidth().height(6.dp),
                    strokeCap=androidx.compose.ui.graphics.StrokeCap.Round)
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Прошло: "+formatMinutes(used),color=Color.White.copy(alpha=.83f),fontSize=11.sp,
                        modifier=Modifier.weight(1f))
                    Text("Всего: "+formatMinutes(total),color=Color.White.copy(alpha=.83f),fontSize=11.sp)
                }
                if(remaining==0) {
                    Spacer(Modifier.height(11.dp))
                    Text("Демонстрационное время истекло. Продлите аренду или завершите возврат.",
                        color=TGold,fontSize=12.sp,lineHeight=17.sp)
                }
            }
        }

        V5Card {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Показатели поездки",color=TInk,fontWeight=FontWeight.ExtraBold,fontSize=16.sp,
                    modifier=Modifier.weight(1f))
                Text("ДЕМО",color=TGreen,fontSize=10.sp,fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TripMetric(Icons.Rounded.Route,"Пробег","${state.distanceKm} км",Modifier.weight(1f))
                TripMetric(Icons.Rounded.LocalGasStation,"Топливо","${state.fuelPercent}%",Modifier.weight(1f))
                TripMetric(Icons.Rounded.Speed,"Одометр",
                    formatKm(state.odometerStart+state.distanceKm),Modifier.weight(1f))
            }
            Spacer(Modifier.height(11.dp))
            Surface(color=TSoft,shape=RoundedCornerShape(11.dp)) {
                Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn,null,tint=TGreen,modifier=Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Текущее положение",fontWeight=FontWeight.Bold,color=TInk,fontSize=12.sp)
                        Text("Нальчик · перемещение моделируется",fontSize=11.sp,color=TMuted)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Показать движение в демо:",color=TMuted,fontSize=11.sp)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={
                    dispatch{ShowcaseEngine.advanceTrip(state,15,4)}
                },modifier=Modifier.weight(1f),shape=RoundedCornerShape(11.dp)) {
                    Text("+ 15 мин · 4 км",fontSize=11.sp)
                }
                OutlinedButton(onClick={
                    dispatch{ShowcaseEngine.advanceTrip(state,60,18)}
                },modifier=Modifier.weight(1f),shape=RoundedCornerShape(11.dp)) {
                    Text("+ 1 ч · 18 км",fontSize=11.sp)
                }
            }
        }
        V5Card {
            V5SectionTitle("Автомобиль")
            Surface(color=TBack,shape=RoundedCornerShape(13.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(if(state.doorsOpen)Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        null,tint=TGreen,modifier=Modifier.size(25.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if(state.doorsOpen)"Двери открыты" else "Двери закрыты",
                            color=TInk,fontWeight=FontWeight.Bold,fontSize=13.sp)
                        Text("Состояние замков только на экране",
                            color=TMuted,fontSize=10.sp)
                    }
                    TextButton(onClick={dispatch{ShowcaseEngine.toggleDoors(state)}}) {
                        Text(if(state.doorsOpen)"Закрыть" else "Открыть",fontSize=12.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={
                    dispatch{ShowcaseEngine.extend(state,2,hourRate)}
                    onInfo("Срок продлён условно на 2 часа. Пример дополнительной оплаты: ${sumRub(hourRate*2)}. Деньги не списываются.")
                },modifier=Modifier.weight(1f),shape=RoundedCornerShape(11.dp)) {
                    Icon(Icons.Rounded.AddCircleOutline,null,modifier=Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Продлить +2 ч",fontSize=11.sp)
                }
                OutlinedButton(onClick={incidentMenu=true},modifier=Modifier.weight(1f),
                    shape=RoundedCornerShape(11.dp)) {
                    Icon(Icons.Rounded.SupportAgent,null,modifier=Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Помощь",fontSize=11.sp)
                }
            }
        }

        V5Card {
            V5SectionTitle("Финансовая информация")
            V5Item("Тариф",sumRub(hourRate)+"/час")
            V5Item("Аренда оплачена",sumRub(state.fare))
            V5Item("Дополнительные часы",sumRub(state.extensionCharge))
            V5Item("Итого за аренду",sumRub(state.fare+state.extensionCharge),emphasis=true)
            Spacer(Modifier.height(7.dp))
            V5Note("Все суммы условные. Реальных платежей и банковских операций не происходит.")
        }
        V5Card {
            V5SectionTitle("Перед завершением")
            Row(verticalAlignment=Alignment.Top) {
                Icon(Icons.Rounded.AssignmentTurnedIn,null,tint=TGreen,modifier=Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text("Верните автомобиль на согласованную стоянку, проверьте топливо, закройте двери и пройдите итоговый осмотр.",
                    color=TMuted,fontSize=12.sp,lineHeight=17.sp)
            }
            Spacer(Modifier.height(13.dp))
            V5MainAction("Перейти к возврату") {
                dispatch{ShowcaseEngine.requestReturn(state)}
            }
        }

        if(incidentMenu) AlertDialog(onDismissRequest={incidentMenu=false},
            icon={Icon(Icons.Rounded.ReportProblem,null,tint=TGreen)},
            title={Text("Экстренная помощь")},
            text={Column {
                Text("Выберите учебную ситуацию. Никакой вызов в службы не производится.",
                    fontSize=12.sp,color=TMuted)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick={
                    incidentMenu=false
                    dispatch{ShowcaseEngine.reportIncident(state)}
                    onInfo("Создана учебная заявка о ДТП. В рабочем сервисе будут экстренные номера, инструкции и передача данных оператору.")
                }){Text("ДТП")}
                TextButton(onClick={
                    incidentMenu=false
                    dispatch{ShowcaseEngine.reportIncident(state)}
                    onInfo("Создана учебная заявка о поломке. В рабочем сервисе клиент сможет приложить фотографии и связаться с диспетчером.")
                }){Text("Поломка автомобиля")}
                TextButton(onClick={
                    incidentMenu=false
                    onInfo("Поддержка демоверсии: оператор не подключён. В коммерческой версии будет чат и линия помощи.")
                }){Text("Написать в поддержку")}
            }},
            confirmButton={TextButton(onClick={incidentMenu=false}){Text("Закрыть")}})
    }
}

@Composable
private fun TripMetric(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,
    value:String,modifier:Modifier=Modifier) {
    Surface(color=TBack,modifier=modifier,shape=RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(10.dp)) {
            Icon(icon,null,tint=TGreen,modifier=Modifier.size(18.dp))
            Spacer(Modifier.height(9.dp))
            Text(label,fontSize=10.sp,color=TMuted)
            Text(value,fontWeight=FontWeight.Bold,fontSize=12.sp,color=TInk,maxLines=1)
        }
    }
}

@Composable
fun ClientRentalV5(state:ShowcaseSession,onJourney:()->Unit,
    dispatch:(()->ShowcaseSession)->Unit,onInfo:(String)->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(14.dp),verticalArrangement=Arrangement.spacedBy(11.dp)) {
        V5Header("Моя аренда","Текущий статус и история действий")
        V5Card {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(43.dp).background(TSoft,RoundedCornerShape(12.dp)),
                    contentAlignment=Alignment.Center) {
                    Icon(Icons.Rounded.LocalShipping,null,tint=TGreen,modifier=Modifier.size(26.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.vehicleTitle.ifBlank{"Автомобиль ещё не выбран"},
                        color=TInk,fontSize=15.sp,fontWeight=FontWeight.ExtraBold)
                    Text(state.stage.title,fontSize=11.sp,color=TMuted)
                }
                Icon(Icons.Rounded.ChevronRight,null,tint=TMuted)
            }
            if(state.vehicleId.isNotEmpty()) {
                Spacer(Modifier.height(13.dp))
                V5Item("Пункт",state.pickup)
                V5Item("Начало",state.day+" · "+state.time)
                V5Item("Тариф",sumRub(state.fare))
                if(state.extraHours>0)V5Item("Продление",sumRub(state.extensionCharge))
                if(state.elapsedMinutes>0)V5Item("В пути",formatMinutes(state.elapsedMinutes))
                V5Item("Дистанция","${state.distanceKm} км")
            }
            Spacer(Modifier.height(11.dp))
            V5MainAction(if(state.stage==ShowcaseStage.DRIVING)"Открыть панель поездки"
                else "Продолжить сценарий",click=onJourney)
        }
        V5Card {
            V5SectionTitle("Этапы заказа")
            val checkpoints=listOf(
                ShowcaseStage.PHONE to "Регистрация и телефон",
                ShowcaseStage.DOCUMENTS to "Паспорт и права",
                ShowcaseStage.REVIEW to "Одобрение администратором",
                ShowcaseStage.READY to "Подбор грузовика",
                ShowcaseStage.CONTRACT to "Договор аренды",
                ShowcaseStage.PAYMENT to "Оплата аренды",
                ShowcaseStage.HANDOVER to "Поиск автомобиля",
                ShowcaseStage.PICKUP_INSPECTION to "Осмотр кузова и салона",
                ShowcaseStage.UNLOCK to "Открытие дверей",
                ShowcaseStage.READY_TO_DRIVE to "Подготовка к выезду",
                ShowcaseStage.DRIVING to "Поездка",
                ShowcaseStage.RETURN to "Возврат и осмотр",
                ShowcaseStage.FINISHED to "Завершение и квитанция"
            )
            val current=ShowcaseStage.entries.indexOf(state.stage)
            checkpoints.forEachIndexed { idx,item->
                val stageIdx=ShowcaseStage.entries.indexOf(item.first)
                Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).background(
                        if(current>=stageIdx)TGreen else TSoft,CircleShape),
                        contentAlignment=Alignment.Center) {
                        if(current>stageIdx) Icon(Icons.Rounded.Check,null,tint=Color.White,
                            modifier=Modifier.size(15.dp))
                        else Text((idx+1).toString(),
                            color=if(current==stageIdx)Color.White else TMuted,
                            fontSize=10.sp,fontWeight=FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(item.second,color=if(current>=stageIdx)TInk else TMuted,
                        fontSize=12.sp,fontWeight=if(current==stageIdx)FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        if(state.incidentReported) V5Note("В журнале зафиксировано демонстрационное обращение о ДТП или поломке.")
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun ClientProfileV5(
    state:ShowcaseSession,onJourney:()->Unit,onAdmin:()->Unit,
    onInfo:(String)->Unit,onNewDemo:()->Unit,
    onProductionEnrollment:()->Unit
) {
    var restart by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(14.dp),verticalArrangement=Arrangement.spacedBy(11.dp)) {
        V5Header("Профиль клиента","Документы, платежи и поддержка")
        Surface(color=TGreen,shape=RoundedCornerShape(19.dp)) {
            Row(Modifier.fillMaxWidth().padding(17.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(Color.White.copy(alpha=.16f),CircleShape),
                    contentAlignment=Alignment.Center) {
                    Icon(Icons.Rounded.Person,null,tint=Color.White,modifier=Modifier.size(30.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("Демо-клиент",color=Color.White,
                        fontWeight=FontWeight.ExtraBold,fontSize=17.sp)
                    Text("+7 (900) 000-00-00 · пример",fontSize=11.sp,
                        color=Color.White.copy(alpha=.7f))
                    Spacer(Modifier.height(5.dp))
                    Text(if(state.demoApproved)"Документы проверены (демо)"
                        else "Документы ожидают проверки",color=Color.White,fontSize=11.sp)
                }
            }
        }
        V5Card {
            V5ProfileRow(Icons.Rounded.Badge,"Документы",
                if(state.demoApproved)"Демо-проверка пройдена" else "Ожидание / образцы") {
                onInfo("В рабочей версии будет проверка личности, паспорта, водительского удостоверения и стажа. Сейчас личные данные не принимаются.")
            }
            HorizontalDivider(color=TLine)
            V5ProfileRow(Icons.Rounded.CreditCard,"Платёжные методы","Тестовая карта и СБП") {
                onInfo("Подключения к банку нет. Демо-оплата аренды учитывается только локально.")
            }
            HorizontalDivider(color=TLine)
            V5ProfileRow(Icons.Rounded.Description,"Договоры и акты",
                if(state.signatureAccepted)"Есть демодоговор" else "Нет оформленных договоров") {
                onInfo("Рабочий сервис будет формировать договор, акт выдачи, акт возврата и чеки. Сейчас это учебные экраны.")
            }
            HorizontalDivider(color=TLine)
            V5ProfileRow(Icons.Rounded.HelpOutline,"Поддержка","ДТП, поломка, вопросы") {
                onInfo("Диспетчерская служба в демоверсии не подключена. При реальной аварии используйте установленные экстренные службы.")
            }
        }
        V5Card {
            V5SectionTitle("Рабочая регистрация")
            V5ProfileRow(Icons.Rounded.VerifiedUser,"Настоящая авторизация",
                "SMS от сервера · документы через проверенного провайдера",
                onProductionEnrollment)
            V5Note("Рабочая регистрация отделена от демо. Пока сервер и провайдер не подключены, настоящие документы не принимаются.")
        }
        V5Card {
            V5SectionTitle("Для демонстрации бизнеса")
            V5ProfileRow(Icons.Rounded.AdminPanelSettings,"Кабинет администратора",
                "Отдельный вход · демо-PIN 2468",onAdmin)
            HorizontalDivider(color=TLine)
            V5ProfileRow(Icons.Rounded.RestartAlt,"Новая демонстрационная аренда",
                "Подтверждённые документы сохраняются") { restart=true }
        }
        V5Note("Режим без настоящих документов, SMS, GPS, доступа к замкам или финансовых операций.")
        if(state.stage!=ShowcaseStage.FINISHED) V5MainAction("Вернуться к оформлению",click=onJourney)
        Spacer(Modifier.height(15.dp))
    }
    if(restart) AlertDialog(onDismissRequest={restart=false},
        title={Text("Начать новую аренду?")},
        text={Text("Данные подтверждённых учебных документов сохранятся. Активную поездку нельзя сбросить до возврата машины.")},
        confirmButton={TextButton(onClick={restart=false;onNewDemo()}){Text("Сбросить")}},
        dismissButton={TextButton(onClick={restart=false}){Text("Отмена")}})
}

@Composable
private fun V5ProfileRow(icon:androidx.compose.ui.graphics.vector.ImageVector,
    title:String,subtitle:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=10.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=TGreen,modifier=Modifier.size(23.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title,fontWeight=FontWeight.Bold,color=TInk,fontSize=13.sp)
            Text(subtitle,color=TMuted,fontSize=11.sp)
        }
        Icon(Icons.Rounded.ChevronRight,null,tint=TMuted,modifier=Modifier.size(19.dp))
    }
}
