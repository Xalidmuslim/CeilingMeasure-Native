package ru.kbr.cargoshare

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat

/** Phone's real foreground position only. No server, history, or car tracker integration. */
data class PhoneGpsFixV96(
    val latitude:Double,
    val longitude:Double,
    val accuracyMeters:Float,
    val measuredAtMillis:Long
)
object PhoneGpsPolicyV96 {
    const val ENABLED_BY_DEFAULT=false
    fun valid(latitude:Double,longitude:Double):Boolean =
        latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
    fun validAccuracy(accuracy:Float):Boolean = accuracy.isFinite() && accuracy>0f
    fun fresh(measuredAtMillis:Long,nowMillis:Long):Boolean =
        measuredAtMillis<=nowMillis && nowMillis-measuredAtMillis<=120_000L
}
class PhoneGpsUiV96(
    val enabled:Boolean,
    val fix:PhoneGpsFixV96?,
    val status:String,
    val enable:()->Unit,
    val disable:()->Unit,
    val openSettings:()->Unit
)

@Composable
fun rememberPhoneGpsV96(): PhoneGpsUiV96 {
    val context=LocalContext.current
    val appContext=remember(context) { context.applicationContext }
    var enabled by remember { mutableStateOf(PhoneGpsPolicyV96.ENABLED_BY_DEFAULT) }
    var fix by remember { mutableStateOf<PhoneGpsFixV96?>(null) }
    var status by remember { mutableStateOf("GPS выключен") }
    val lifecycleOwner=LocalLifecycleOwner.current
    var foreground by remember { mutableStateOf(
        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    ) }
    DisposableEffect(lifecycleOwner) {
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_START)foreground=true
            if(event==Lifecycle.Event.ON_STOP)foreground=false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun granted():Boolean {
        val fine=ContextCompat.checkSelfPermission(appContext,
            Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        val coarse=ContextCompat.checkSelfPermission(appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
        return fine||coarse
    }
    val permission=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if(result.values.any { it }) {
            enabled=true
            status="Поиск местоположения…"
        } else {
            enabled=false
            fix=null
            status="Доступ к местоположению запрещён"
        }
    }
    DisposableEffect(enabled,foreground) {
        if (!enabled || !foreground || !granted()) {
            onDispose { }
        } else {
            val manager=appContext.getSystemService(Context.LOCATION_SERVICE)
                as LocationManager
            val fine=ContextCompat.checkSelfPermission(appContext,
                Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
            val providers=buildList {
                if(fine&&manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    add(LocationManager.GPS_PROVIDER)
                if(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    add(LocationManager.NETWORK_PROVIDER)
            }
            val listener=LocationListener { location:Location ->
                val now=System.currentTimeMillis()
                if(PhoneGpsPolicyV96.valid(location.latitude,location.longitude) &&
                    location.hasAccuracy() &&
                    PhoneGpsPolicyV96.validAccuracy(location.accuracy) &&
                    PhoneGpsPolicyV96.fresh(location.time,now)) {
                    fix=PhoneGpsFixV96(location.latitude,location.longitude,
                        location.accuracy,location.time)
                    status="Местоположение определено"
                }
            }
            if(providers.isEmpty()) status="Включите геолокацию в настройках телефона"
            else status="Поиск местоположения…"
            var registered=false
            try {
                providers.forEach { provider ->
                    manager.requestLocationUpdates(provider,3_000L,4f,listener,
                        Looper.getMainLooper())
                    registered=true
                    manager.getLastKnownLocation(provider)?.let(listener::onLocationChanged)
                }
            } catch (_:SecurityException) {
                status="Нет разрешения на геолокацию"
                fix=null
            } catch (_:IllegalArgumentException) {
                status="Геолокация сейчас недоступна"
                fix=null
            }
            onDispose {
                if(registered)runCatching { manager.removeUpdates(listener) }
            }
        }
    }
    LaunchedEffect(enabled,fix?.measuredAtMillis,foreground) {
        if(enabled && foreground && fix==null &&
           status=="Поиск местоположения…") {
            delay(25_000L)
            if(fix==null && status=="Поиск местоположения…") {
                status="Нет сигнала GPS. Проверьте геолокацию телефона."
            }
        }
    }
    LaunchedEffect(enabled,fix?.measuredAtMillis) {
        val current=fix
        if(enabled && current!=null) {
            delay(121_000L)
            if(fix==current && !PhoneGpsPolicyV96.fresh(
                current.measuredAtMillis,System.currentTimeMillis())) {
                fix=null
                status="GPS: нет свежих координат"
            }
        }
    }
    return PhoneGpsUiV96(enabled,fix,status,
        enable={
            if(granted()) {
                enabled=true
                status="Поиск местоположения…"
            } else {
                permission.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        },
        disable={
            enabled=false
            fix=null
            status="GPS выключен"
        },
        openSettings={
            val settings=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(settings)
        }
    )
}
