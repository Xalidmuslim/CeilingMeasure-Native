package ru.kbr.cargoshare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Separate production-auth client. Does NOT modify the demo ShowcaseEngine,
 * does not accept identity photos, and does not persist session tokens.
 * Endpoints are enabled only when a real HTTPS server is configured at build time.
 */
private class SecureApiV100 {
    private val root=BuildConfig.REAL_AUTH_API_URL.trim().removeSuffix("/")
    val configured:Boolean get()=root.startsWith("https://") && root.length>12

    suspend fun call(path:String,payload:JSONObject?=null,token:String?=null):JSONObject =
        withContext(Dispatchers.IO) {
            check(configured) { "Сервер регистрации ещё не подключён" }
            val connection=(URL(root+path).openConnection() as HttpURLConnection)
            try {
                connection.requestMethod=if(payload==null)"GET" else "POST"
                connection.connectTimeout=8_000
                connection.readTimeout=12_000
                connection.setRequestProperty("Accept","application/json")
                connection.setRequestProperty("Cache-Control","no-store")
                if(!token.isNullOrEmpty())
                    connection.setRequestProperty("Authorization","Bearer "+token)
                if(payload!=null) {
                    connection.doOutput=true
                    connection.setRequestProperty("Content-Type","application/json; charset=UTF-8")
                    val encoded=payload.toString().toByteArray(Charsets.UTF_8)
                    connection.setFixedLengthStreamingMode(encoded.size)
                    connection.outputStream.use{it.write(encoded)}
                }
                val status=connection.responseCode
                val input=if(status in 200..299) connection.inputStream
                          else connection.errorStream
                val response=input?.bufferedReader(Charsets.UTF_8)?.use {it.readText().take(3000)}
                    ?: "{}"
                if(status !in 200..299) {
                    val message=runCatching{JSONObject(response).optString("detail")}.getOrNull()
                        ?.takeUnless{it.isBlank()} ?: "HTTP $status"
                    error(message)
                }
                JSONObject(response)
            } finally {
                connection.disconnect()
            }
        }
}

@Composable
fun ProductionEnrollmentV100(onBack:()->Unit) {
    val api=remember{SecureApiV100()}
    var phone by remember {mutableStateOf("+7")}
    var code by remember {mutableStateOf("")}
    var stage by remember {mutableIntStateOf(0)}
    var token by remember {mutableStateOf<String?>(null)}
    var status by remember {mutableStateOf<String?>(null)}
    var driverStatus by remember {mutableStateOf("NOT_STARTED")}
    var allowed by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val green=Color(0xFF20523E)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=17.dp,vertical=16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick=onBack){Text("← Назад в демопрофиль")}
        Text("Регистрация клиента",fontSize=24.sp,fontWeight=FontWeight.ExtraBold)
        Text("Рабочий контур · независимо от демонстрационной аренды",
            fontSize=12.sp,color=Color.Gray)
        if(!api.configured) {
            Surface(color=Color(0xFFF1E9D9),shape=RoundedCornerShape(15.dp)) {
                Text("Рабочий сервер ещё не подключён. Настоящие номера и документы пока не вводите. Для запуска нужен российский сервер HTTPS и договор с SMS-провайдером.",
                    modifier=Modifier.padding(16.dp),fontSize=13.sp,lineHeight=19.sp)
            }
        } else {
            when(stage) {
                0 -> {
                    Text("1. Подтверждение номера телефона",fontWeight=FontWeight.Bold)
                    OutlinedTextField(phone,{phone=it.take(12)},
                        label={Text("Телефон +7XXXXXXXXXX")},
                        singleLine=true,modifier=Modifier.fillMaxWidth())
                    Button(
                        enabled=!busy && Regex("^\\+7\\d{10}$").matches(phone),
                        onClick={
                            scope.launch {
                                busy=true
                                status="Отправляем одноразовый код…"
                                try {
                                    api.call("/v1/auth/otp/start",JSONObject().put("phone",phone))
                                    status="Код отправлен. Он действует пять минут."
                                    stage=1
                                } catch (e:Exception) {
                                    status=e.message ?: "Сервис временно недоступен"
                                } finally { busy=false }
                            }
                        },
                        modifier=Modifier.fillMaxWidth().height(49.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=green)
                    ){Text("Получить SMS")}
                }
                1 -> {
                    Text("2. Код из SMS",fontWeight=FontWeight.Bold)
                    Text("Код 0000 здесь не работает. Введите шестизначный код от SMS-провайдера.",
                        color=Color.Gray,fontSize=12.sp)
                    OutlinedTextField(code,{code=it.filter(Char::isDigit).take(6)},
                        label={Text("Одноразовый код")},singleLine=true,
                        modifier=Modifier.fillMaxWidth())
                    Button(
                        enabled=!busy && code.length==6,
                        onClick={
                            scope.launch{
                                busy=true
                                try {
                                    val reply=api.call("/v1/auth/otp/verify",JSONObject()
                                        .put("phone",phone).put("code",code))
                                    token=reply.getString("access_token")
                                    stage=2
                                    val user=api.call("/v1/me",token=token)
                                    driverStatus=user.optString("driver_status")
                                    allowed=user.optBoolean("can_rent_real_vehicle",false)
                                    status="Номер подтверждён сервером."
                                } catch(e:Exception) {
                                    status=e.message ?: "Неверный код"
                                } finally{busy=false}
                            }
                        },modifier=Modifier.fillMaxWidth(),
                        colors=ButtonDefaults.buttonColors(containerColor=green)
                    ){Text("Подтвердить номер")}
                }
                else -> {
                    Text("Телефон подтверждён",fontWeight=FontWeight.Bold,color=green)
                    Text("3. Проверка документов",fontSize=17.sp,fontWeight=FontWeight.Bold)
                    Text(when(driverStatus) {
                        "VERIFIED" -> "Сервер подтвердил статус водителя"
                        "REJECTED" -> "Проверка отклонена. Свяжитесь с поддержкой"
                        else -> "Документы пока не проверены. Допуск к настоящей машине закрыт"
                    },fontSize=13.sp,color=Color.Gray)
                    Text(if(allowed)"Доступ подтверждён сервером"
                        else "Аренда реального автомобиля недоступна",
                        color=if(allowed)green else Color(0xFF9B4934),fontSize=12.sp)
                    OutlinedButton(onClick={
                        scope.launch{
                            busy=true
                            try {
                                val result=api.call("/v1/driver-verifications/start",
                                    JSONObject(),token)
                                status=result.optString("status","Запрос принят")
                            } catch(e:Exception) {
                                status=if(e.message=="KYC_PROVIDER_NOT_CONFIGURED")
                                    "Проверка ещё не подключена. Нужен договор с KYC-провайдером."
                                    else e.message
                            } finally{busy=false}
                        }
                    },enabled=!busy,modifier=Modifier.fillMaxWidth()) {
                        Text("Запросить проверку документов")
                    }
                    Text("Не загружайте паспорт или водительское удостоверение в деморежим. После подключения лицензированного провайдера проверка будет проходить в защищённом контуре.",
                        fontSize=11.sp,lineHeight=16.sp,color=Color.Gray)
                }
            }
        }
        if(status!=null) {
            HorizontalDivider()
            Text(status ?: "",fontSize=12.sp,color=Color(0xFF4E6054))
        }
    }
}
