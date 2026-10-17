package ru.kbr.cargoshare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

@Composable
fun DemoOrderScreen(state:ShowcaseSession,onGo:()->Unit,onReset:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        DemoTitle("Моя аренда","Понятный статус заказа и финансовые операции")
        Surface(color=Color.White,shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(SPale,RoundedCornerShape(14.dp)),
                        contentAlignment=Alignment.Center) {
                        Icon(Icons.Rounded.LocalShipping,null,tint=SGreen,modifier=Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if(state.vehicleId.isEmpty())"Ещё нет бронирования" else state.vehicleTitle,
                            fontWeight=FontWeight.ExtraBold,fontSize=17.sp,color=SInk)
                        Text("Показ учебного процесса",fontSize=11.sp,color=SSecondary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                InfoPanel("Текущий этап",state.stage.title)
                if(state.vehicleId.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    DemoLine("Начало",state.day+" · "+state.time)
                    DemoLine("Получение",state.pickup)
                    DemoLine("Продолжительность",if(state.hours==24)"Сутки" else "${state.hours} ч")
                    DemoLine("Стоимость",sumRub(state.fare))
                    DemoLine("Продление",sumRub(state.extensionCharge))
                    DemoLine("Оплата",if(state.rentPaid)"Демо-оплата принята" else "Ожидается")
                    DemoLine("Залог",when {
                        state.depositReturned -> "Возвращён: ${sumRub(RentalRules.DEMO_DEPOSIT)}"
                        state.depositHeld -> "Условно удержан: ${sumRub(RentalRules.DEMO_DEPOSIT)}"
                        else -> "Не оплачен"
                    })
                    if(state.incidentReported) DemoLine("Обращение","ДТП / поломка · демо-заявка")
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(color=Color.White,shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Этапы аренды",fontSize=17.sp,fontWeight=FontWeight.ExtraBold,color=SInk)
                Spacer(Modifier.height(13.dp))
                val steps = listOf(
                    ShowcaseStage.PHONE to "Регистрация телефона",
                    ShowcaseStage.DOCUMENTS to "Паспорт и водительские права",
                    ShowcaseStage.REVIEW to "Решение администратора",
                    ShowcaseStage.CONTRACT to "Договор",
                    ShowcaseStage.PAYMENT to "Оплата и залог",
                    ShowcaseStage.HANDOVER to "Осмотр и получение",
                    ShowcaseStage.DRIVING to "Аренда грузовика",
                    ShowcaseStage.RETURN to "Возврат автомобиля",
                    ShowcaseStage.FINISHED to "Закрытие и возврат залога"
                )
                val current = ShowcaseStage.entries.indexOf(state.stage)
                steps.forEachIndexed { i, pair ->
                    val passed = ShowcaseStage.entries.indexOf(pair.first) < current
                    val active = state.stage == pair.first
                    Row(Modifier.fillMaxWidth().padding(vertical=6.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(26.dp).background(
                            if(passed || active) SGreen else SPale,CircleShape),
                            contentAlignment=Alignment.Center) {
                            if(passed) Icon(Icons.Rounded.Check,null,
                                tint=Color.White,modifier=Modifier.size(17.dp))
                            else Text((i+1).toString(),fontSize=11.sp,fontWeight=FontWeight.Bold,
                                color=if(active)Color.White else SSecondary)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(pair.second,modifier=Modifier.weight(1f),fontSize=13.sp,
                            color=if(passed||active)SInk else SSecondary,
                            fontWeight=if(active)FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        DemoPrimary(if(state.stage==ShowcaseStage.FINISHED)"Посмотреть завершённую аренду" else "Продолжить оформление",onClick=onGo)
        if(state.stage==ShowcaseStage.FINISHED) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick=onReset,modifier=Modifier.fillMaxWidth()) {
                Text("Начать новую демонстрацию")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun DemoProfileScreen(
    state:ShowcaseSession,onGo:()->Unit,onAdmin:()->Unit,
    onReset:()->Unit,onNotice:(String)->Unit
) {
    var confirmReset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)) {
        DemoTitle("Профиль","Аккаунт, документы, платежи и помощь")
        Surface(color=SGreen,shape=RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(17.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(Color.White.copy(alpha=.13f),CircleShape),
                    contentAlignment=Alignment.Center) {
                    Icon(Icons.Rounded.Person,null,tint=Color.White,modifier=Modifier.size(31.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Демо-клиент",fontWeight=FontWeight.Bold,color=Color.White,fontSize=17.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(if(state.demoApproved)"Документы условно одобрены"
                        else "Требуется демонстрационная проверка",
                        color=Color.White.copy(alpha=.8f),fontSize=12.sp)
                }
            }
        }
        Surface(color=Color.White,shape=RoundedCornerShape(19.dp)) {
            Column {
                ProfileLink(Icons.Rounded.Badge,"Личные документы",
                    if(state.demoApproved)"Одобрены (демо)" else "Статус: ${state.stage.title}") {
                    onNotice("В демонстрации нет настоящих персональных данных. Паспорт и права представлены учебными образцами. В рабочей версии потребуется отдельный защищённый сервер.")
                }
                HorizontalDivider(color=SBorder)
                ProfileLink(Icons.Rounded.CreditCard,"Оплата и залог",
                    if(state.depositReturned)"Залог возвращён (демо)"
                    else if(state.rentPaid)"Условная оплата выполнена" else "Тестовая карта / СБП") {
                    onNotice("В учебном режиме нет списания денег. История показывает имитацию оплаты аренды и возврата залога.")
                }
                HorizontalDivider(color=SBorder)
                ProfileLink(Icons.Rounded.Description,"Электронные документы",
                    if(state.signatureAccepted)"Демо-договор подписан" else "Нет подписанных документов") {
                    onNotice("Подписание только имитируется: настоящий юридически значимый договор не заключается.")
                }
                HorizontalDivider(color=SBorder)
                ProfileLink(Icons.Rounded.SupportAgent,"Помощь 24/7","Сценарии ДТП, поломки и поддержки") {
                    onNotice("Рабочий сервис покажет номер аварийной поддержки, передаст местоположение с согласия пользователя и оформит обращение. В этом APK сообщения никуда не отправляются.")
                }
            }
        }
        Surface(color=Color.White,shape=RoundedCornerShape(19.dp)) {
            Column {
                ProfileLink(Icons.Rounded.AdminPanelSettings,"Администратор",
                    "Показать обработку заявок и автопарк",onAdmin)
                HorizontalDivider(color=SBorder)
                ProfileLink(Icons.Rounded.Refresh,"Перезапустить демо",
                    "Сбросить учебную регистрацию и аренду") { confirmReset = true }
            }
        }
        InfoPanel("Безопасность демо","Не указывайте реальные паспортные данные, реквизиты карт или документы — здесь нет защищённого сервера и обработки настоящих заказов.")
        Text("ГрузШер КБР · интерактивная презентация v0.4",
            color=SSecondary,fontSize=11.sp,modifier=Modifier.padding(start=5.dp))
        if(state.stage!=ShowcaseStage.FINISHED) {
            OutlinedButton(onClick=onGo,modifier=Modifier.fillMaxWidth()) {
                Text("Продолжить сценарий клиента")
            }
        }
    }
    if(confirmReset) AlertDialog(
        onDismissRequest={confirmReset=false},
        title={Text("Начать заново?")},
        text={Text("Учебная регистрация, оплата и статус аренды будут сброшены. Демокарточки автомобилей и тарифы останутся.")},
        confirmButton={TextButton(onClick={confirmReset=false;onReset()}){Text("Сбросить")}},
        dismissButton={TextButton(onClick={confirmReset=false}){Text("Отмена")}}
    )
}

@Composable
private fun ProfileLink(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title:String, subtitle:String, onClick:()->Unit
) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(14.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(SPale,RoundedCornerShape(11.dp)),
            contentAlignment=Alignment.Center) {
            Icon(icon,null,tint=SGreen,modifier=Modifier.size(21.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title,color=SInk,fontWeight=FontWeight.Bold,fontSize=14.sp)
            Text(subtitle,color=SSecondary,fontSize=11.sp)
        }
        Icon(Icons.Rounded.ChevronRight,null,tint=SSecondary)
    }
}

@Composable
fun DemoAdminScreen(
    state:ShowcaseSession,
    fleet:List<DemoVehicle>,
    onAction:(()->ShowcaseSession)->Unit,
    onChangeFleet:(List<DemoVehicle>)->Unit,
    onClient:()->Unit,
    onNotice:(String)->Unit
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        DemoTitle("Панель владельца","Управление автопарком и проверкой клиентов")
        InfoPanel("Административное демо","Для презентации доступ открыт без авторизации. В настоящей системе доступ будет защищён ролями и двухфакторной проверкой.")
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            AdminMetric("Автомобилей",fleet.count{it.enabled}.toString(),
                modifier=Modifier.weight(1f))
            AdminMetric("Клиент",if(state.demoApproved)"Одобрен" else "Новый",
                modifier=Modifier.weight(1f))
        }
        Surface(color=Color.White,shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VerifiedUser,null,tint=SGreen,
                        modifier=Modifier.size(23.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Проверка документов",fontWeight=FontWeight.ExtraBold,
                        color=SInk,fontSize=17.sp)
                }
                Spacer(Modifier.height(12.dp))
                DemoLine("Заявитель","Демо-клиент № 0001")
                DemoLine("Образцы",if(state.documentsSubmitted)"Паспорт и права отмечены" else "Пока не отправлены")
                DemoLine("Статус",state.stage.title)
                if(state.stage==ShowcaseStage.REVIEW) {
                    Spacer(Modifier.height(11.dp))
                    DemoPrimary("Одобрить документы") { onAction { ShowcaseEngine.approve(state) } }
                    Spacer(Modifier.height(7.dp))
                    OutlinedButton(onClick={onAction { ShowcaseEngine.reject(state) }},
                        modifier=Modifier.fillMaxWidth()) {
                        Text("Отклонить · запросить повторную загрузку")
                    }
                } else {
                    InfoPanel("Решение администратора",if(state.demoApproved)
                        "Проверка пройдена в учебном режиме"
                        else "Новая заявка появится после демонстрационной отправки документов.")
                }
            }
        }
        Surface(color=Color.White,shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ReceiptLong,null,tint=SGreen,modifier=Modifier.size(23.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Заказ и платежи",fontWeight=FontWeight.ExtraBold,color=SInk,fontSize=17.sp)
                }
                Spacer(Modifier.height(10.dp))
                DemoLine("Этап",state.stage.title)
                DemoLine("Автомобиль",state.vehicleTitle.ifBlank{"Не выбран"})
                DemoLine("Аренда",sumRub(state.fare+state.extensionCharge))
                DemoLine("Оплачено",if(state.rentPaid)"Да · условно" else "Нет")
                DemoLine("Залог",when {
                    state.depositReturned -> "Условно возвращён"
                    state.depositHeld -> "Условно удержан"
                    else -> "Не поступил"
                })
                if(state.incidentReported) {
                    Spacer(Modifier.height(6.dp))
                    InfoPanel("Зарегистрирован инцидент","Учебная заявка о ДТП или поломке без реального обращения.")
                }
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Автопарк",fontWeight=FontWeight.ExtraBold,fontSize=19.sp,
                color=SInk,modifier=Modifier.weight(1f))
            TextButton(onClick={
                val added = DemoVehicle(
                    id=UUID.randomUUID().toString(),
                    title="Новый грузовик",kind="Фургон",payload="1,5 т",
                    volume="16 м³",hourPrice=600,dayPrice=4000,
                    pickup="Нальчик · Пункт выдачи")
                onChangeFleet(fleet+added)
                editingId=added.id
            }) {
                Icon(Icons.Rounded.Add,null,modifier=Modifier.size(18.dp))
                Text("Добавить")
            }
        }
        fleet.forEach { v ->
            Surface(color=Color.White,shape=RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalShipping,null,tint=SGreen,
                            modifier=Modifier.size(26.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.title,color=SInk,fontWeight=FontWeight.Bold,fontSize=14.sp)
                            Text(v.kind+" · "+v.pickup,color=SSecondary,fontSize=10.sp)
                        }
                        Switch(checked=v.enabled,
                            onCheckedChange={visible ->
                                onChangeFleet(fleet.map{
                                    if(it.id==v.id)it.copy(enabled=visible) else it
                                })
                            })
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(sumRub(v.hourPrice)+"/ч  ·  "+sumRub(v.dayPrice)+"/сут",
                            color=SGreen,fontWeight=FontWeight.Bold,fontSize=12.sp,
                            modifier=Modifier.weight(1f))
                        TextButton(onClick={editingId=if(editingId==v.id)null else v.id}) {
                            Icon(Icons.Rounded.Edit,null,modifier=Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Изменить",fontSize=12.sp)
                        }
                    }
                    if(editingId==v.id) FleetEditForm(v,onSave={updated ->
                        onChangeFleet(fleet.map{if(it.id==v.id)updated else it})
                        editingId=null
                    },onCancel={editingId=null},onNotice=onNotice)
                }
            }
        }
        OutlinedButton(onClick=onClient,modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.ArrowBack,null,modifier=Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("Вернуться к клиентскому сценарию")
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun AdminMetric(label:String,value:String,modifier:Modifier=Modifier) {
    Surface(modifier=modifier,shape=RoundedCornerShape(17.dp),color=SPale) {
        Column(Modifier.padding(14.dp)) {
            Text(label,color=SSecondary,fontSize=11.sp)
            Spacer(Modifier.height(5.dp))
            Text(value,color=SGreen,fontSize=18.sp,fontWeight=FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun FleetEditForm(
    vehicle:DemoVehicle,
    onSave:(DemoVehicle)->Unit,onCancel:()->Unit,onNotice:(String)->Unit
) {
    var title by remember(vehicle.id) { mutableStateOf(vehicle.title) }
    var kind by remember(vehicle.id) { mutableStateOf(vehicle.kind) }
    var pickup by remember(vehicle.id) { mutableStateOf(vehicle.pickup) }
    var payload by remember(vehicle.id) { mutableStateOf(vehicle.payload) }
    var volume by remember(vehicle.id) { mutableStateOf(vehicle.volume) }
    var hour by remember(vehicle.id) { mutableStateOf(vehicle.hourPrice.toString()) }
    var day by remember(vehicle.id) { mutableStateOf(vehicle.dayPrice.toString()) }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color=SBorder)
        OutlinedTextField(value=title,onValueChange={title=it},
            label={Text("Название")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=kind,onValueChange={kind=it},
            label={Text("Тип кузова")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=pickup,onValueChange={pickup=it},
            label={Text("Пункт выдачи")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value=payload,onValueChange={payload=it},
                label={Text("Грузоподъёмность")},singleLine=true,modifier=Modifier.weight(1f))
            OutlinedTextField(value=volume,onValueChange={volume=it},
                label={Text("Объём")},singleLine=true,modifier=Modifier.weight(1f))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value=hour,onValueChange={hour=it.filter(Char::isDigit)},
                label={Text("₽ за час")},singleLine=true,modifier=Modifier.weight(1f))
            OutlinedTextField(value=day,onValueChange={day=it.filter(Char::isDigit)},
                label={Text("₽ за сутки")},singleLine=true,modifier=Modifier.weight(1f))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=onCancel,modifier=Modifier.weight(1f)) {
                Text("Отмена")
            }
            Button(onClick={
                val h=hour.toIntOrNull()
                val d=day.toIntOrNull()
                if(title.isBlank()||pickup.isBlank()||kind.isBlank()||
                    h==null||d==null||h<=0||d<=0||h>100000||d>1000000) {
                    onNotice("Заполните название, тип, пункт выдачи и корректные цены.")
                } else onSave(vehicle.copy(title=title.trim(),kind=kind.trim(),
                    pickup=pickup.trim(),payload=payload.trim(),volume=volume.trim(),
                    hourPrice=h,dayPrice=d))
            },modifier=Modifier.weight(1f),
                colors=ButtonDefaults.buttonColors(containerColor=SGreen)) {
                Text("Сохранить")
            }
        }
    }
}
