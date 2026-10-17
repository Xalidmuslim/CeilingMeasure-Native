package ru.kbr.cargoshare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/** Real device foreground location only; no background tracking or uploading. */
data class PhoneGpsFixV96(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val measuredAtMillis: Long
)

object PhoneGpsPolicyV96 {
    const val ENABLED_BY_DEFAULT=false
    fun valid(latitude:Double,longitude:Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
    fun validAccuracy(accuracy:Float):Boolean = accuracy.isFinite() && accuracy>0f
    fun fresh(measuredAtMillis:Long,nowMillis:Long):Boolean =
        measuredAtMillis<=nowMillis && nowMillis-measuredAtMillis<=120_000L
    fun permissionDescription(fine:Boolean,coarse:Boolean):String = when {
        fine -> "Точное местоположение разрешено"
        coarse -> "Только приблизительное. Разрешите точное в настройках Android."
        else -> "Разрешение не предоставлено"
    }
}

class PhoneGpsUiV96(
    val enabled:Boolean,
    val fix:PhoneGpsFixV96?,
    val status:String,
    val enable:()->Unit,
    val disable:()->Unit,
    val openSettings:()->Unit,
    val openAppSettings:()->Unit
)

@Composable
fun rememberPhoneGpsV96():PhoneGpsUiV96 {
    val context=LocalContext.current
    val appContext=remember(context) { context.applicationContext }
    val owner=LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(PhoneGpsPolicyV96.ENABLED_BY_DEFAULT) }
    var fix by remember { mutableStateOf<PhoneGpsFixV96?>(null) }
    var status by remember { mutableStateOf("GPS выключен") }
    var permissionRevision by remember { mutableIntStateOf(0) }
    var foreground by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_START || event==Lifecycle.Event.ON_RESUME) {
                foreground=true
                permissionRevision++
            }
            if(event==Lifecycle.Event.ON_STOP) foreground=false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    fun fineGranted():Boolean =
        ContextCompat.checkSelfPermission(appContext,Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    fun coarseGranted():Boolean =
        ContextCompat.checkSelfPermission(appContext,Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    fun hasPermission()=fineGranted() || coarseGranted()

    val launcher=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionRevision++
        val fine=results[Manifest.permission.ACCESS_FINE_LOCATION]==true || fineGranted()
        val coarse=results[Manifest.permission.ACCESS_COARSE_LOCATION]==true || coarseGranted()
        if(fine || coarse) {
            enabled=true
            fix=null
            status=if(fine) "Разрешение получено · поиск GPS…"
                else "Доступ приблизительный · поиск по сети…"
        } else {
            enabled=false
            fix=null
            status="Разрешение отклонено. Откройте «Разрешения»."
        }
    }

    DisposableEffect(enabled,foreground,permissionRevision) {
        if(!enabled || !foreground || !hasPermission()) {
            onDispose { }
        } else {
            val manager=appContext.getSystemService(Context.LOCATION_SERVICE)
                as LocationManager
            var active=true
            val fine=fineGranted()
            fun available(provider:String):Boolean = runCatching {
                manager.allProviders.contains(provider) && manager.isProviderEnabled(provider)
            }.getOrDefault(false)
            val providers=buildList {
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S &&
                   available(LocationManager.FUSED_PROVIDER))
                    add(LocationManager.FUSED_PROVIDER)
                if(fine && available(LocationManager.GPS_PROVIDER))
                    add(LocationManager.GPS_PROVIDER)
                if(available(LocationManager.NETWORK_PROVIDER))
                    add(LocationManager.NETWORK_PROVIDER)
            }.distinct()

            val listener=LocationListener { location:Location ->
                if(!active) return@LocationListener
                val now=System.currentTimeMillis()
                if(location.hasAccuracy() &&
                    PhoneGpsPolicyV96.valid(location.latitude,location.longitude) &&
                    PhoneGpsPolicyV96.validAccuracy(location.accuracy) &&
                    PhoneGpsPolicyV96.fresh(location.time,now)) {
                    if(fix==null || location.time>=fix!!.measuredAtMillis ||
                        location.accuracy<fix!!.accuracyMeters) {
                        fix=PhoneGpsFixV96(location.latitude,location.longitude,
                            location.accuracy,location.time)
                        status=if(fine) "GPS работает · точное местоположение"
                            else "Местоположение приблизительное"
                    }
                }
            }
            val registered=mutableListOf<String>()
            if(providers.isEmpty()) {
                status="Геолокация телефона выключена. Включите её в Android."
            } else {
                status=if(fine) "Поиск GPS-координат…"
                       else "Поиск по сети · точное местоположение отключено"
                providers.forEach { provider ->
                    try {
                        manager.requestLocationUpdates(
                            provider,2_000L,0f,listener,Looper.getMainLooper())
                        registered.add(provider)
                        manager.getLastKnownLocation(provider)?.let(listener::onLocationChanged)
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R) {
                            manager.getCurrentLocation(
                                provider,null,ContextCompat.getMainExecutor(appContext)
                            ) { location ->
                                if(active && location!=null)listener.onLocationChanged(location)
                            }
                        }
                    } catch (_:SecurityException) {
                        status="Нет разрешения Android. Откройте «Разрешения»."
                    } catch (_:RuntimeException) {
                        if(registered.isEmpty())
                            status="Провайдер GPS недоступен. Проверьте геолокацию Android."
                    }
                }
            }
            onDispose {
                active=false
                if(registered.isNotEmpty())runCatching { manager.removeUpdates(listener) }
            }
        }
    }

    // Explain when GPS has genuinely failed rather than silently remaining "off".
    LaunchedEffect(enabled,foreground,permissionRevision) {
        if(enabled && foreground) {
            val since=SystemClock.elapsedRealtime()
            while(enabled && foreground) {
                delay(4_000L)
                val current=fix
                if(current!=null && !PhoneGpsPolicyV96.fresh(
                        current.measuredAtMillis,System.currentTimeMillis())) {
                    fix=null
                    status="Координаты устарели · проверьте геолокацию Android"
                } else if(current==null && hasPermission() &&
                    SystemClock.elapsedRealtime()-since>20_000L &&
                    (status.startsWith("Поиск") ||
                     status.startsWith("Разрешение получено"))) {
                    status=if(fineGranted())
                        "Нет сигнала GPS. Попробуйте у окна или на улице."
                    else "Точное местоположение отключено. Включите его в «Разрешения»."
                }
            }
        }
    }

    return PhoneGpsUiV96(
        enabled=enabled,fix=fix,status=status,
        enable={
            // Immediate UI feedback proves the click was delivered (not intercepted).
            status="Нажатие принято · проверка разрешений…"
            if(fineGranted()) {
                enabled=true
                fix=null
                permissionRevision++
                status="Поиск GPS-координат…"
            } else {
                // Important: approximate-only is NOT treated as precise GPS.
                // Request precise even if coarse location was previously granted.
                launcher.launch(arrayOf(
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
            val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { appContext.startActivity(intent) }
        },
        openAppSettings={
            val intent=Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${appContext.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { appContext.startActivity(intent) }
        }
    )
}
