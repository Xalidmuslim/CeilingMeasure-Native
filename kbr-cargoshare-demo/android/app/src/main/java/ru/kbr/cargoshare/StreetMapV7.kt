package ru.kbr.cargoshare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Real Nalchik OpenStreetMap basemap; ALL vehicle pins and tracks are fictitious.
 * Reuses MapView, caches rendered pin icons, and does not rebuild overlays on recomposition.
 */
@Composable
fun StreetMapV7(
    fleet:List<DemoVehicle>,
    selectedId:String,
    onSelect:(String)->Unit,
    modifier:Modifier=Modifier,
    tripKm:Int?=null
) {
    var realMap by rememberSaveable { mutableStateOf(true) }
    Box(modifier) {
        if (realMap) {
            OSMNativeMapV7(
                fleet=fleet,selectedId=selectedId,onSelect=onSelect,
                tripKm=tripKm,modifier=Modifier.fillMaxSize())
        } else {
            MovableDemoTruckMap(
                fleet=fleet,selectedId=selectedId,onSelect=onSelect,
                modifier=Modifier.fillMaxSize(),fullWindow=true)
        }
        Row(Modifier.align(Alignment.TopCenter).padding(top=11.dp),
            horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Surface(onClick={realMap=true},
                shape=RoundedCornerShape(12.dp),
                color=if(realMap)Color(0xFF20523F) else Color.White) {
                Row(Modifier.padding(horizontal=10.dp,vertical=8.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Map,null,modifier=Modifier.size(16.dp),
                        tint=if(realMap)Color.White else Color(0xFF20523F))
                    Spacer(Modifier.width(5.dp))
                    Text("Нальчик",fontSize=11.sp,
                        color=if(realMap)Color.White else Color(0xFF20523F))
                }
            }
            Surface(onClick={realMap=false},
                shape=RoundedCornerShape(12.dp),
                color=if(!realMap)Color(0xFF20523F) else Color.White) {
                Row(Modifier.padding(horizontal=10.dp,vertical=8.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Layers,null,modifier=Modifier.size(16.dp),
                        tint=if(!realMap)Color.White else Color(0xFF20523F))
                    Spacer(Modifier.width(5.dp))
                    Text("Без сети",fontSize=11.sp,
                        color=if(!realMap)Color.White else Color(0xFF20523F))
                }
            }
        }
        Surface(color=Color.White.copy(alpha=.95f),
            shape=RoundedCornerShape(9.dp),
            modifier=Modifier.align(Alignment.TopStart)
                .padding(top=52.dp,start=9.dp)) {
            Text(if(tripKm!=null)NalchikTripMap.disclaimer
                else "Демо-точки грузовиков · не GPS",
                modifier=Modifier.padding(horizontal=8.dp,vertical=6.dp),
                fontSize=10.sp,color=Color(0xFF3A554A))
        }
        if (realMap) {
            Surface(color=Color.White.copy(alpha=.9f),
                shape=RoundedCornerShape(7.dp),
                modifier=Modifier.align(Alignment.BottomStart)
                    .padding(bottom=9.dp,start=8.dp)) {
                Text("© OpenStreetMap contributors",
                    fontSize=10.sp,color=Color(0xFF58685E),
                    modifier=Modifier.padding(horizontal=7.dp,vertical=4.dp))
            }
        }
    }
}

private val demoCoordinates=mapOf(
    "g01" to GeoPoint(43.4857,43.6058),
    "g02" to GeoPoint(43.4801,43.6209),
    "g03" to GeoPoint(43.4931,43.5912)
)
private fun demoCoord(v:DemoVehicle):GeoPoint {
    demoCoordinates[v.id]?.let{return it}
    val pt=DemoMapData.pointFor(v.id)
    return GeoPoint(43.476+(pt.second*.027),43.586+(pt.first*.040))
}

@Composable
private fun OSMNativeMapV7(
    fleet:List<DemoVehicle>,
    selectedId:String,
    onSelect:(String)->Unit,
    tripKm:Int?,
    modifier:Modifier
) {
    val context=LocalContext.current
    val onSelectLatest by rememberUpdatedState(onSelect)
    DisposableEffect(context) {
        Configuration.getInstance().userAgentValue=context.packageName + "/0.9 (Nalchik demo map)"
        onDispose {}
    }
    val map=remember(context) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setCenter(GeoPoint(43.485,43.608))
            controller.setZoom(14.0)
            minZoomLevel=10.0
            maxZoomLevel=19.0
        }
    }
    DisposableEffect(map) {
        map.onResume()
        onDispose {
            map.onPause()
            map.onDetach()
        }
    }
    // These caches are plain mutable collections, NOT observable UI state.
    // AndroidView update must not regenerate overlays every second of the trip clock.
    val cache=remember(map) { mutableMapOf<String,BitmapDrawable>() }
    val applied=remember(map) { mutableMapOf<String,String>() }
    val fleetKey=fleet.filter{it.enabled}
        .joinToString("|") { "${it.id}:${it.title}:${it.hourPrice}:${it.pickup}" }
    val signature="$selectedId/$tripKm/$fleetKey"
    AndroidView(factory={map},modifier=modifier,update={view->
        if(applied["signature"] != signature) {
            val previousSelected=applied["selected"]
            val previousTrip=applied["tripKm"]
            applied["signature"]=signature
            applied["selected"]=selectedId
            applied["tripKm"]=tripKm.toString()
            view.overlays.clear()
            if(tripKm!=null) {
                val positions=NalchikTripMap.routeFor(selectedId)
                    .map {GeoPoint(it.lat,it.lon)}
                val route=Polyline().apply {
                    setPoints(positions)
                    outlinePaint.color=android.graphics.Color.rgb(42,116,85)
                    outlinePaint.strokeWidth=5.0f * context.resources.displayMetrics.density
                    outlinePaint.isAntiAlias=true
                    title="Условный трек по Нальчику"
                }
                view.overlays.add(route)
                val pickup=Marker(view).apply {
                    position=positions.first()
                    title="Пункт получения (демо)"
                    setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
                }
                view.overlays.add(pickup)
                val now=NalchikTripMap.current(selectedId,tripKm)
                val marker=Marker(view).apply {
                    position=GeoPoint(now.lat,now.lon)
                    title="Автомобиль: демонстрационное положение"
                    snippet="Не реальные координаты GPS"
                    icon=cachedTruckPin(context,cache,true,0)
                    setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ -> true }
                }
                view.overlays.add(marker)
                if(previousTrip != tripKm.toString()) {
                    view.controller.animateTo(marker.position)
                }
            } else {
                fleet.filter{it.enabled}.forEach { vehicle ->
                    val pos=demoCoord(vehicle)
                    val isSelected=selectedId==vehicle.id
                    val marker=Marker(view).apply {
                        position=pos
                        title=vehicle.title
                        snippet=vehicle.pickup+" · адрес условный"
                        icon=cachedTruckPin(context,cache,isSelected,vehicle.hourPrice)
                        setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            onSelectLatest(vehicle.id)
                            true
                        }
                    }
                    view.overlays.add(marker)
                }
                if(previousSelected != null && previousSelected != selectedId) {
                    fleet.firstOrNull{it.id==selectedId}?.let {
                        view.controller.animateTo(demoCoord(it))
                    }
                }
            }
            view.invalidate()
        }
    })
}

private fun cachedTruckPin(
    context:Context,cache:MutableMap<String,BitmapDrawable>,
    active:Boolean,price:Int
):BitmapDrawable {
    val key="$active/$price"
    return cache.getOrPut(key) {
        val density=context.resources.displayMetrics.density
        val w=(if(price==0)106 else 116)*density
        val h=47*density
        val bitmap=Bitmap.createBitmap(w.toInt(),h.toInt(),Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color=if(active)android.graphics.Color.rgb(30,82,61)
                    else android.graphics.Color.WHITE
        canvas.drawRoundRect(2f,2f,w-2f,h-9*density,11*density,11*density,paint)
        val tip=android.graphics.Path().apply {
            moveTo(w/2f-6*density,h-9*density)
            lineTo(w/2f+6*density,h-9*density)
            lineTo(w/2f,h-1*density)
            close()
        }
        canvas.drawPath(tip,paint)
        paint.color=if(active)android.graphics.Color.WHITE
                    else android.graphics.Color.rgb(30,82,61)
        paint.textSize=12*density
        paint.typeface=Typeface.create("sans-serif-medium",Typeface.BOLD)
        canvas.drawText(if(price==0)"● Машина" else "$price ₽/ч",
            9*density,25*density,paint)
        BitmapDrawable(context.resources,bitmap)
    }
}
