package ru.kbr.cargoshare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MainGreen = Color(0xFF215340)
private val MainInk = Color(0xFF192620)
private val Paper = Color(0xFFF6F6F2)
private val Quiet = Color(0xFF737E74)
private val LightGreen = Color(0xFFE8F0E6)
private val Keyline = Color(0xFFE6E9E2)
private val Gold = Color(0xFFFFEBC5)

class AdminActivity : ComponentActivity() {
    private val store by lazy { ShowcaseStore(applicationContext) }
    private val adminFleetStore by lazy { DemoStore(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary=MainGreen, surface=Color.White)) {
                var state by remember { mutableStateOf(store.read()) }
                var vehicles by remember { mutableStateOf(adminFleetStore.fleet()) }
                var message by remember { mutableStateOf<String?>(null) }
                Column(Modifier.fillMaxSize().background(Paper).statusBarsPadding()
                    .navigationBarsPadding()) {
                    Row(Modifier.fillMaxWidth().background(MainInk).padding(horizontal=10.dp,vertical=5.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        IconButton(onClick={finish()}) {
                            Icon(Icons.Rounded.ArrowBack, "К клиенту",tint=Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Кабинет администратора",color=Color.White,
                                fontSize=17.sp,fontWeight=FontWeight.Bold)
                            Text("Отдельный интерфейс · учебный доступ",
                                color=Color.White.copy(alpha=.72f),fontSize=10.sp)
                        }
                        Icon(Icons.Rounded.AdminPanelSettings,null,tint=Color.White,
                            modifier=Modifier.size(24.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        DemoAdminScreen(state=state,fleet=vehicles,
                            onAction={block->
                                runCatching { block() }.onSuccess {
                                    state=it
                                    store.save(it)
                                }.onFailure { message=it.message }
                            },
                            onChangeFleet={updated->
                                vehicles=updated
                                adminFleetStore.saveFleet(updated)
                            },
                            onClient={finish()},
                            onNotice={message=it})
                    }
                }
                if(message!=null) AlertDialog(onDismissRequest={message=null},
                    title={Text("Демо-администратор")},
                    text={Text(message ?: "")},
                    confirmButton={TextButton(onClick={message=null}){Text("Понятно")}})
            }
        }
    }
}

@Composable
fun ClientV5App() {
    val context=androidx.compose.ui.platform.LocalContext.current
    val sessionStore=remember { ShowcaseStore(context.applicationContext) }
    val fleetStore=remember { DemoStore(context.applicationContext) }
    var state by remember { mutableStateOf(sessionStore.read()) }
    var fleet by remember { mutableStateOf(fleetStore.fleet()) }
    var tab by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var adminGate by remember { mutableStateOf(false) }
    var demoCode by remember { mutableStateOf("2468") }

    val lifecycleOwner=androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer=androidx.lifecycle.LifecycleEventObserver { _,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                state=sessionStore.read()
                fleet=fleetStore.fleet()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun commit(s:ShowcaseSession) {
        state=s
        sessionStore.save(s)
    }
    fun dispatch(next:()->ShowcaseSession) {
        runCatching(next).onSuccess(::commit)
            .onFailure { dialog=it.message ?: "Этот шаг ещё недоступен" }
    }

    // System back button and Android edge-swipe use the same safe navigation.
    // On the root booking screen Back is consumed; it must never terminate the app.
    BackHandler(enabled = true) {
        when {
            adminGate -> adminGate = false
            dialog != null -> dialog = null
            tab != 0 -> tab = 0
            else -> {
                val previous = ShowcaseEngine.backStep(state)
                if (previous != state) commit(previous)
                // READY / active rental / completed screens intentionally stay open.
            }
        }
    }

    MaterialTheme(colorScheme=lightColorScheme(primary=MainGreen,
        onPrimary=Color.White,background=Paper,surface=Color.White,
        onSurface=MainInk)) {
        Scaffold(
            containerColor=Paper,
            contentWindowInsets=WindowInsets(0,0,0,0),
            topBar={
                Column(Modifier.fillMaxWidth().background(Color.White).statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).background(LightGreen,RoundedCornerShape(11.dp)),
                            contentAlignment=Alignment.Center) {
                            Icon(Icons.Rounded.LocalShipping,null,tint=MainGreen,
                                modifier=Modifier.size(23.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("ГРУЗШЕР",fontSize=16.sp,fontWeight=FontWeight.ExtraBold,
                                color=MainInk,letterSpacing=.6.sp)
                            Text("Грузовой каршеринг · Нальчик и КБР",
                                fontSize=10.sp,color=Quiet)
                        }
                        Box(Modifier.background(LightGreen,RoundedCornerShape(8.dp))
                            .padding(horizontal=9.dp,vertical=6.dp)) {
                            Text("ДЕМО",fontSize=10.sp,color=MainGreen,fontWeight=FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color=Keyline)
                }
            },
            bottomBar={
                NavigationBar(containerColor=Color.White,tonalElevation=1.dp,
                    modifier=Modifier.navigationBarsPadding().height(67.dp)) {
                    val titles=listOf("Автомобили","Аренда","Профиль")
                    val icons=listOf(Icons.Rounded.DirectionsCar,Icons.Rounded.Route,Icons.Rounded.Person)
                    titles.forEachIndexed{i,name->
                        NavigationBarItem(selected=tab==i,onClick={tab=i},
                            icon={Icon(icons[i],null,modifier=Modifier.size(21.dp))},
                            label={Text(name,fontSize=10.sp)},
                            colors=NavigationBarItemDefaults.colors(
                                selectedIconColor=MainGreen,selectedTextColor=MainGreen,
                                indicatorColor=LightGreen,unselectedIconColor=Quiet,
                                unselectedTextColor=Quiet))
                    }
                }
            }
        ) { inner ->
            Box(Modifier.fillMaxSize().padding(inner)) {
                when(tab) {
                    0 -> when(state.stage) {
                        ShowcaseStage.READY -> MapHomeV7(
                            fleet=fleet,state=state,
                            onReserve={vehicle,hours,day,time ->
                                dispatch {
                                    ShowcaseEngine.reserve(state,vehicle.id,hours,day,time,
                                        vehicle.title,vehicle.pickup,RentalRules.fare(vehicle,hours))
                                }
                            }
                        )
                        ShowcaseStage.DRIVING -> TripHomeV7(
                            state=state,fleet=fleet,dispatch=::dispatch,onInfo={dialog=it}
                        )
                        else -> ClientJourneyV5(state=state,fleet=fleet,dispatch=::dispatch,
                            onAdmin={adminGate=true},onInfo={dialog=it},
                            onNewDemo={dispatch {
                                if (state.stage == ShowcaseStage.FINISHED)
                                    ShowcaseEngine.beginNextRental(state)
                                else ShowcaseEngine.resetRetainingVerification(state)
                            }})
                    }
                    1 -> ClientRentalV5(state=state,onJourney={tab=0},
                        dispatch=::dispatch,onInfo={dialog=it})
                    else -> ClientProfileV5(state=state,onJourney={tab=0},
                        onAdmin={adminGate=true},onInfo={dialog=it},
                        onNewDemo={
                            dispatch {
                                if (state.stage == ShowcaseStage.FINISHED)
                                    ShowcaseEngine.beginNextRental(state)
                                else ShowcaseEngine.resetRetainingVerification(state)
                            }
                            tab=0
                        })
                }
            }
        }
        if(dialog!=null) AlertDialog(onDismissRequest={dialog=null},
            title={Text("Демонстрационный режим")},
            text={Text(dialog ?: "",fontSize=13.sp,lineHeight=19.sp)},
            confirmButton={TextButton(onClick={dialog=null}){Text("Закрыть")}},
            shape=RoundedCornerShape(18.dp))
        if(adminGate) AlertDialog(onDismissRequest={adminGate=false},
            icon={Icon(Icons.Rounded.AdminPanelSettings,null)},
            title={Text("Отдельный кабинет администратора")},
            text={
                Column {
                    Text("Для демонстрации используйте код 2468. Это не настоящая защита входа.",
                        fontSize=12.sp,color=Quiet)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value=demoCode,onValueChange={demoCode=it},
                        singleLine=true,label={Text("Демо-PIN")},
                        modifier=Modifier.fillMaxWidth())
                }
            },
            confirmButton={TextButton(onClick={
                if(demoCode=="2468") {
                    adminGate=false
                    context.startActivity(Intent(context,AdminActivity::class.java))
                } else dialog="Неверный демо-PIN. Используйте 2468."
            }){Text("Войти")}},
            dismissButton={TextButton(onClick={adminGate=false}){Text("Отмена")}})
    }
}

@Composable
internal fun V5Card(content:@Composable ColumnScope.()->Unit) {
    Surface(color=Color.White,shape=RoundedCornerShape(18.dp),
        modifier=Modifier.fillMaxWidth(),shadowElevation=1.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp),content=content)
    }
}
@Composable
internal fun V5Header(title:String,subtitle:String) {
    Column(Modifier.padding(bottom=14.dp)) {
        Text(title,fontSize=23.sp,color=MainInk,fontWeight=FontWeight.ExtraBold)
        Spacer(Modifier.height(3.dp))
        Text(subtitle,fontSize=12.sp,color=Quiet,lineHeight=17.sp)
    }
}
@Composable
internal fun V5MainAction(label:String,enabled:Boolean=true,click:()->Unit) {
    Button(onClick=click,enabled=enabled,shape=RoundedCornerShape(13.dp),
        modifier=Modifier.fillMaxWidth().height(49.dp),
        colors=ButtonDefaults.buttonColors(containerColor=MainGreen)) {
        Text(label,fontWeight=FontWeight.Bold,fontSize=14.sp)
        Spacer(Modifier.width(7.dp))
        Icon(Icons.Rounded.ArrowForward,null,modifier=Modifier.size(17.dp))
    }
}
@Composable
internal fun V5Item(label:String,value:String,emphasis:Boolean=false) {
    Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.Top) {
        Text(label,modifier=Modifier.weight(.43f),fontSize=12.sp,color=Quiet)
        Text(value,modifier=Modifier.weight(.57f),fontSize=if(emphasis)15.sp else 12.sp,
            color=if(emphasis)MainGreen else MainInk,fontWeight=FontWeight.SemiBold)
    }
}
@Composable
internal fun V5Note(text:String) {
    Surface(color=LightGreen,shape=RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.Top) {
            Icon(Icons.Rounded.Info,null,tint=MainGreen,modifier=Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text(text,fontSize=11.sp,lineHeight=16.sp,color=MainGreen)
        }
    }
}
@Composable
internal fun V5SectionTitle(title:String) {
    Text(title,fontSize=15.sp,fontWeight=FontWeight.Bold,
        modifier=Modifier.padding(vertical=8.dp),color=MainInk)
}
@Composable
internal fun V5Toggle(title:String,detail:String,checked:Boolean,
    onChange:(Boolean)->Unit) {
    Surface(color=Paper,shape=RoundedCornerShape(11.dp)) {
        Row(Modifier.fillMaxWidth().clickable{onChange(!checked)}
            .padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
            Checkbox(checked=checked,onCheckedChange=onChange,
                colors=CheckboxDefaults.colors(checkedColor=MainGreen))
            Column(Modifier.weight(1f)) {
                Text(title,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=MainInk)
                Text(detail,fontSize=10.sp,color=Quiet)
            }
        }
    }
}
@Composable
internal fun V5Pill(title:String,selected:Boolean,onClick:()->Unit) {
    Surface(onClick=onClick,shape=RoundedCornerShape(10.dp),
        color=if(selected)MainGreen else Paper) {
        Box(Modifier.padding(horizontal=14.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
            Text(title,fontSize=12.sp,color=if(selected)Color.White else MainInk,
                fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium)
        }
    }
}
@Composable
internal fun V5StatusTag(label:String,done:Boolean) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Icon(if(done)Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
            null,tint=if(done)MainGreen else Quiet,modifier=Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label,fontSize=11.sp,color=if(done)MainGreen else Quiet,
            fontWeight=FontWeight.SemiBold)
    }
}
