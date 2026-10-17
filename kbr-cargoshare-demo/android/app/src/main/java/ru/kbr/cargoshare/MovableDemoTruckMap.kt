package ru.kbr.cargoshare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val CanvasBG=Color(0xFFEAF0E9)
private val Street=Color(0xFFFFFFFF)
private val RoadOutline=Color(0xFFD6E0D7)
private val Tree=Color(0xFFD6E7D3)
private val Selected=Color(0xFF20523D)
private val BodyText=Color(0xFF26382B)
private val MutedText=Color(0xFF728077)

/**
 * Offline schematic: move with one finger, pinch with two, tap a truck pin.
 * Locations and addresses are representative examples, not GPS telemetry.
 */
@Composable
fun MovableDemoTruckMap(
    fleet: List<DemoVehicle>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    val selected=DemoMapData.selectedVehicle(fleet,selectedId)
    Column(modifier) {
        BoxWithConstraints(
            modifier=Modifier.fillMaxWidth().height(300.dp)
                .clip(RoundedCornerShape(19.dp)).background(CanvasBG)
        ) {
            val maxPanX = with(androidx.compose.ui.platform.LocalDensity.current) {
                (maxWidth * 1.15f).toPx()
            }
            val maxPanY = with(androidx.compose.ui.platform.LocalDensity.current) {
                (maxHeight * 1.15f).toPx()
            }
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        scaleX=zoom
                        scaleY=zoom
                        translationX=drag.x
                        translationY=drag.y
                    }
                    .pointerInput(maxPanX,maxPanY) {
                        detectTransformGestures(panZoomLock = false) { _, pan, scaleChange, _ ->
                            val updated=(zoom*scaleChange).coerceIn(1f,3.4f)
                            zoom=updated
                            drag=Offset(
                                (drag.x+pan.x).coerceIn(-maxPanX,maxPanX),
                                (drag.y+pan.y).coerceIn(-maxPanY,maxPanY)
                            )
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w=size.width
                    val h=size.height
                    drawRect(CanvasBG)
                    val greens=listOf(
                        Offset(0.03f,0.09f) to (0.30f to 0.27f),
                        Offset(0.65f,0.04f) to (0.28f to 0.23f),
                        Offset(0.70f,0.61f) to (0.27f to 0.34f),
                        Offset(0.05f,0.72f) to (0.31f to 0.21f)
                    )
                    greens.forEach { (o,sz) ->
                        drawRoundRect(Tree,topLeft=Offset(w*o.x,h*o.y),
                            size=androidx.compose.ui.geometry.Size(w*sz.first,h*sz.second),
                            cornerRadius=androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
                    }
                    val paths=listOf(
                        Path().apply { moveTo(-w*.12f,h*.14f);cubicTo(w*.26f,h*.28f,w*.62f,h*.04f,w*1.13f,h*.23f) },
                        Path().apply { moveTo(-w*.13f,h*.57f);cubicTo(w*.27f,h*.47f,w*.70f,h*.75f,w*1.12f,h*.56f) },
                        Path().apply { moveTo(w*.18f,-h*.08f);cubicTo(w*.40f,h*.22f,w*.19f,h*.56f,w*.41f,h*1.11f) },
                        Path().apply { moveTo(w*.70f,-h*.10f);cubicTo(w*.52f,h*.25f,w*.90f,h*.44f,w*.69f,h*1.13f) },
                        Path().apply { moveTo(-w*.07f,h*.86f);lineTo(w*1.10f,h*.05f) },
                        Path().apply { moveTo(-w*.03f,h*.36f);lineTo(w*1.11f,h*.85f) }
                    )
                    paths.forEachIndexed { index, p ->
                        val wide=if(index<=3) 29.dp.toPx() else 14.dp.toPx()
                        drawPath(p,RoadOutline,style=Stroke(width=wide+7.dp.toPx(),cap=StrokeCap.Round))
                        drawPath(p,Street,style=Stroke(width=wide,cap=StrokeCap.Round))
                    }
                }
                TextMarker("ЦЕНТР",x=maxWidth*.44f,y=maxHeight*.16f)
                TextMarker("СТРЕЛКА",x=maxWidth*.70f,y=maxHeight*.73f)
                TextMarker("НАЛЬЧИК",x=maxWidth*.12f,y=maxHeight*.52f)
                fleet.filter{it.enabled}.forEachIndexed { i,car ->
                    val pt=DemoMapData.pointFor(car.id,i)
                    val isSelected=car.id==selected?.id
                    Surface(
                        onClick={onSelect(car.id)},
                        color=if(isSelected)Selected else Color.White,
                        shadowElevation=if(isSelected)9.dp else 5.dp,
                        shape=RoundedCornerShape(13.dp),
                        modifier=Modifier.offset(x=maxWidth*pt.first-45.dp,
                            y=maxHeight*pt.second-18.dp)
                    ) {
                        Row(Modifier.padding(horizontal=11.dp,vertical=9.dp),
                            verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalShipping,null,
                                tint=if(isSelected)Color.White else Selected,
                                modifier=Modifier.size(21.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(sumRub(car.hourPrice)+"/ч",
                                fontSize=12.sp,
                                color=if(isSelected)Color.White else Selected,
                                fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
            Column(Modifier.align(Alignment.TopEnd).padding(12.dp),
                verticalArrangement=Arrangement.spacedBy(6.dp)) {
                MapControl(Icons.Rounded.Add,"Приблизить") {
                    zoom=(zoom+.4f).coerceAtMost(3.4f)
                }
                MapControl(Icons.Rounded.Remove,"Отдалить") {
                    zoom=(zoom-.4f).coerceAtLeast(1f)
                }
                MapControl(Icons.Rounded.CenterFocusStrong,"Вернуть карту") {
                    zoom=1f
                    drag=Offset.Zero
                }
            }
            Surface(color=Color.White.copy(alpha=.94f),
                shape=RoundedCornerShape(10.dp),
                modifier=Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text("Карта-схема · точки и адреса условные",
                    fontSize=10.sp,color=MutedText,
                    modifier=Modifier.padding(horizontal=9.dp,vertical=7.dp))
            }
        }
        Spacer(Modifier.height(9.dp))
        if(selected!=null) {
            Surface(shape=RoundedCornerShape(15.dp),color=Color.White,
                border=BorderStroke(1.dp,Color(0xFFE4EBE3))) {
                Row(Modifier.fillMaxWidth().clickable { onSelect(selected.id) }.padding(12.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(39.dp).background(Color(0xFFE8F2E8),
                        RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center) {
                        Icon(Icons.Rounded.Place,null,tint=Selected,modifier=Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selected.title,fontWeight=FontWeight.Bold,
                            fontSize=13.sp,color=BodyText)
                        Spacer(Modifier.height(3.dp))
                        Text(selected.pickup,color=MutedText,
                            fontSize=11.sp,lineHeight=15.sp)
                    }
                    Icon(Icons.Rounded.CheckCircle,null,tint=Selected,modifier=Modifier.size(21.dp))
                }
            }
        }
    }
}

@Composable
private fun TextMarker(text:String,x:androidx.compose.ui.unit.Dp,y:androidx.compose.ui.unit.Dp) {
    Text(text,modifier=Modifier.offset(x=x,y=y),
        fontSize=10.sp,letterSpacing=1.4.sp,fontWeight=FontWeight.Bold,
        color=Color(0xFF9CAA9E))
}
@Composable
private fun MapControl(icon:androidx.compose.ui.graphics.vector.ImageVector,
    description:String,onClick:()->Unit) {
    Surface(onClick=onClick,shape=RoundedCornerShape(11.dp),
        color=Color.White,shadowElevation=3.dp) {
        Box(Modifier.size(38.dp),contentAlignment=Alignment.Center) {
            Icon(icon,description,tint=Selected,modifier=Modifier.size(20.dp))
        }
    }
}
