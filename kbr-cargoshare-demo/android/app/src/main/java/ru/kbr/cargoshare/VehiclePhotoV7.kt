package ru.kbr.cargoshare

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Built-in CC0 example photos are embedded by the Android APK workflow. Missing photo = safe illustration. */
@Composable
fun VehiclePhotoV7(
    vehicle: DemoVehicle,
    modifier:Modifier=Modifier,
    showDisclaimer:Boolean=false
) {
    val context=LocalContext.current
    val asset=when (vehicle.id) {
        "g01"->"truck_g01.jpg"
        "g02"->"truck_g02.jpg"
        "g03"->"truck_g03.jpg"
        else->"truck_g01.jpg"
    }
    val bitmap=remember(asset) {
        runCatching {
            context.assets.open(asset).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }
    Box(modifier.clip(RoundedCornerShape(16.dp))
        .background(Color(0xFFE9EEE9))) {
        if (bitmap!=null) {
            Image(bitmap=bitmap,contentDescription="Пример фотографии грузовика",
                modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFFE8EEE9))
                drawRect(Color(0xFFD9E4D9),topLeft=Offset(0f,size.height*.67f),
                    size=androidx.compose.ui.geometry.Size(size.width,size.height*.33f))
                drawLine(Color.White,Offset(size.width*.10f,size.height*.78f),
                    Offset(size.width*.93f,size.height*.78f),strokeWidth=4.dp.toPx())
            }
            Icon(Icons.Rounded.LocalShipping,null,tint=Color(0xFF21533E),
                modifier=Modifier.align(Alignment.Center).fillMaxHeight(.59f).aspectRatio(1.2f))
        }
        if (showDisclaimer) {
            Surface(
                color=Color(0xDBFFFFFF),shape=RoundedCornerShape(7.dp),
                modifier=Modifier.align(Alignment.BottomStart).padding(7.dp)) {
                Text("Фото-пример",fontSize=10.sp,fontWeight=FontWeight.SemiBold,
                    modifier=Modifier.padding(horizontal=7.dp,vertical=4.dp),
                    color=Color(0xFF47564C))
            }
        }
    }
}
