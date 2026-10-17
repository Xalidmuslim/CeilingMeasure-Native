package ru.kbr.cargoshare

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline-only sample storage. No passport details or real payments are collected here.
 * Production data will be stored on the server with authenticated APIs.
 */
class DemoStore(context: Context) {
    private val prefs = context.getSharedPreferences("cargoshare_demo_03", Context.MODE_PRIVATE)

    fun fleet(): List<DemoVehicle> {
        val raw = prefs.getString("fleet", null) ?: return demoFleet
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { index ->
                val o = arr.getJSONObject(index)
                DemoVehicle(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    kind = o.getString("kind"),
                    payload = o.getString("payload"),
                    volume = o.getString("volume"),
                    hourPrice = o.getInt("hourPrice"),
                    dayPrice = o.getInt("dayPrice"),
                    pickup = o.getString("pickup"),
                    enabled = o.optBoolean("enabled", true)
                )
            }
        }.getOrDefault(demoFleet).map(DemoMapData::normalizePickup)
    }

    fun saveFleet(fleet: List<DemoVehicle>) {
        val arr = JSONArray()
        fleet.forEach { v ->
            arr.put(JSONObject()
                .put("id", v.id).put("title", v.title).put("kind", v.kind)
                .put("payload", v.payload).put("volume", v.volume)
                .put("hourPrice", v.hourPrice).put("dayPrice", v.dayPrice)
                .put("pickup", v.pickup).put("enabled", v.enabled))
        }
        prefs.edit().putString("fleet", arr.toString()).apply()
    }

    fun orders(): List<DemoOrder> {
        val raw = prefs.getString("orders", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { index ->
                val o = arr.getJSONObject(index)
                DemoOrder(
                    id = o.getString("id"),
                    vehicleId = o.getString("vehicleId"),
                    vehicleTitle = o.getString("vehicleTitle"),
                    pickup = o.getString("pickup"),
                    start = o.getString("start"),
                    hours = o.getInt("hours"),
                    price = o.getInt("price"),
                    stage = RentalStage.valueOf(o.getString("stage"))
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveOrders(orders: List<DemoOrder>) {
        val arr = JSONArray()
        orders.forEach { o ->
            arr.put(JSONObject()
                .put("id", o.id).put("vehicleId", o.vehicleId)
                .put("vehicleTitle", o.vehicleTitle)
                .put("pickup", o.pickup).put("start", o.start)
                .put("hours", o.hours).put("price", o.price)
                .put("stage", o.stage.name))
        }
        prefs.edit().putString("orders", arr.toString()).apply()
    }
}
