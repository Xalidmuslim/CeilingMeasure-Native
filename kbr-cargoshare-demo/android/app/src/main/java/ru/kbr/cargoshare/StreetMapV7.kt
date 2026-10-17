package ru.kbr.cargoshare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

/** Street map is real OSM; ALL parked vehicle locations are illustrative demo points. */
@Composable
fun StreetMapV7(fleet:List<DemoVehicle>,selectedId:String,
                onSelect:(String)->Unit,modifier:Modifier=Modifier) {
    var realMap by remember { mutableStateOf(true) }
    Box(modifier) {
        if(realMap) {
            OSMNativeMapV7(fleet= fleet, selectedId=selectedId,
                onSelect=onSelect, modifier=Modifier.fillMaxSize())
        } else {
            MovableDemoTruckMap(fleet=fleet,selectedId=selectedId,
                onSelect=onSelect,modifier=Modifier.fillMaxSize(),fullWindow=true)
        }
        Row(Modifier.align(Alignment.TopCenter).padding(top=13.dp),
            horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Surface(onClick={realMap=true},
                shape=RoundedCornerShape(12.dp),shadowElevation=2.dp,
                color=if(realMap)Color(0xFF20523F) else Color.White) {
                Row(Modifier.padding(horizontal=12.dp,vertical=9.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Map,null,modifier=Modifier.size(16.dp),
                        tint=if(realMap)Color.White else Color(0xFF20523F))
                    Spacer(Modifier.width(5.dp))
                    Text("Улицы Нальчика",fontSize=11.sp,
                        color=if(realMap)Color.White else Color(0xFF20523F))
                }
            }
            Surface(onClick={realMap=false},
                shape=RoundedCornerShape(12.dp),shadowElevation=2.dp,
                color=if(!realMap)Color(0xFF20523F) else Color.White) {
                Row(Modifier.padding(horizontal=11.dp,vertical=9.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Layers,null,modifier=Modifier.size(16.dp),
                        tint=if(!realMap)Color.White else Color(0xFF20523F))
                    Spacer(Modifier.width(5.dp))
                    Text("Без сети",fontSize=11.sp,
                        color=if(!realMap)Color.White else Color(0xFF20523F))
                }
            }
        }
        Surface(color=Color.White.copy(alpha=.93f),
            shape=RoundedCornerShape(9.dp),
            modifier=Modifier.align(Alignment.TopStart).padding(top=58.dp,start=9.dp)) {
            Text("Машины и адреса — ДЕМО",
                modifier=Modifier.padding(horizontal=8.dp,vertical=6.dp),
                fontSize=10.sp,color=Color(0xFF3A554A))
        }
        if(realMap) {
            Surface(color=Color.White.copy(alpha=.9f),shape=RoundedCornerShape(7.dp),
                modifier=Modifier.align(Alignment.BottomStart).padding(bottom=12.dp,start=8.dp)) {
                Text("© OpenStreetMap contributors",fontSize=10.sp,
                    color=Color(0xFF58685E),modifier=Modifier.padding(horizontal=7.dp,vertical=4.dp))
            }
        }
    }
}

private val coordinates=mapOf(
    "g01" to GeoPoint(43.4857,43.6058),
    "g02" to GeoPoint(43.4801,43.6209),
    "g03" to GeoPoint(43.4931,43.5912)
)
private fun demoCoord(v:DemoVehicle):GeoPoint {
    coordinates[v.id]?.let{return it}
    val pt=DemoMapData.pointFor(v.id)
    return GeoPoint(43.476+(pt.second*.027),43.586+(pt.first*.040))
}

@Composable
private fun OSMNativeMapV7(fleet:List<DemoVehicle>,selectedId:String,
    onSelect:(String)->Unit,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName + "/0.7 (demo map)"
        onDispose{}
    }
    val map=remember(context) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setCenter(GeoPoint(43.485,43.608))
            controller.setZoom(13.7)
            minZoomLevel=9.0
            maxZoomLevel=19.0
        }
    }
    DisposableEffect(map) {
        map.onResume()
        onDispose { map.onPause(); map.onDetach() }
    }
    var lastSelected by remember { mutableStateOf(selectedId) }
    AndroidView(factory={map},modifier=modifier,update={view->
        view.overlays.clear()
        fleet.filter{it.enabled}.forEach { vehicle ->
            val pos=demoCoord(vehicle)
            val marker=Marker(view).apply {
                position=pos
                title=vehicle.title
                snippet=vehicle.pickup+" (условная точка)"
                icon=truckPin(context,selectedId==vehicle.id,vehicle.hourPrice)
                setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    onSelect(vehicle.id)
                    view.controller.animateTo(pos)
                    true
                }
            }
            view.overlays.add(marker)
        }
        if(lastSelected!=selectedId){
            lastSelected=selectedId
            fleet.firstOrNull{it.id==selectedId}?.let {
                view.controller.animateTo(demoCoord(it))
            }
        }
        view.invalidate()
    })
}

private fun truckPin(context:Context,active:Boolean,price:Int):BitmapDrawable {
    val density=context.resources.displayMetrics.density
    val w=(106*density).toInt()
    val h=(48*density).toInt()
    val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
    val c=Canvas(b)
    val p=Paint(Paint.ANTI_ALIAS_FLAG)
    p.color=if(active)android.graphics.Color.rgb(30,82,61) else android.graphics.Color.WHITE
    c.drawRoundRect(2f,2f,w-2f,h-8*density,12*density,12*density,p)
    val path=android.graphics.Path().apply {
        moveTo(w/2f-6*density,h-8*density)
        lineTo(w/2f+6*density,h-8*density)
        lineTo(w/2f,h-1*density)
        close()
    }
    c.drawPath(path,p)
    p.color=if(active)android.graphics.Color.WHITE else android.graphics.Color.rgb(30,82,61)
    p.textSize=12*density
    p.typeface=Typeface.create("sans-serif-medium",Typeface.BOLD)
    c.drawText("$price ₽/ч",11*density,25*density,p)
    return BitmapDrawable(context.resources,b)
}
