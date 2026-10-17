package ru.kbr.cargoshare

import android.content.Context
import org.json.JSONObject

/** Synthetic flow state only; real identity documents or payment credentials are never stored. */
class ShowcaseStore(context: Context) {
    private val prefs = context.getSharedPreferences("kbr_full_showcase_v04", Context.MODE_PRIVATE)

    fun read(): ShowcaseSession {
        val json = prefs.getString("session", null) ?: return ShowcaseSession()
        return runCatching {
            val o = JSONObject(json)
            ShowcaseSession(
                stage = ShowcaseStage.valueOf(o.optString("stage", "PHONE")),
                documentsSubmitted = o.optBoolean("documentsSubmitted"),
                demoApproved = o.optBoolean("demoApproved"),
                vehicleId = o.optString("vehicleId"),
                vehicleTitle = o.optString("vehicleTitle"),
                pickup = o.optString("pickup"),
                day = o.optString("day", "Сегодня"),
                time = o.optString("time", "12:00"),
                hours = o.optInt("hours", 4),
                fare = o.optInt("fare"),
                signatureAccepted = o.optBoolean("signatureAccepted"),
                rentPaid = o.optBoolean("rentPaid"),
                depositHeld = o.optBoolean("depositHeld"),
                preTripChecked = o.optBoolean("preTripChecked"),
                doorsOpen = o.optBoolean("doorsOpen"),
                incidentReported = o.optBoolean("incidentReported"),
                extraHours = o.optInt("extraHours"),
                extensionCharge = o.optInt("extensionCharge"),
                returnChecked = o.optBoolean("returnChecked"),
                depositReturned = o.optBoolean("depositReturned"),
                damageCharge = o.optInt("damageCharge"),
                elapsedMinutes = o.optInt("elapsedMinutes"),
                distanceKm = o.optInt("distanceKm"),
                fuelPercent = o.optInt("fuelPercent",75),
                odometerStart = o.optInt("odometerStart",42150)
            )
        }.getOrElse { ShowcaseSession() }
    }

    fun save(s: ShowcaseSession) {
        val o = JSONObject()
            .put("stage", s.stage.name)
            .put("documentsSubmitted", s.documentsSubmitted)
            .put("demoApproved", s.demoApproved)
            .put("vehicleId", s.vehicleId)
            .put("vehicleTitle", s.vehicleTitle)
            .put("pickup", s.pickup)
            .put("day", s.day)
            .put("time", s.time)
            .put("hours", s.hours)
            .put("fare", s.fare)
            .put("signatureAccepted", s.signatureAccepted)
            .put("rentPaid", s.rentPaid)
            .put("depositHeld", s.depositHeld)
            .put("preTripChecked", s.preTripChecked)
            .put("doorsOpen", s.doorsOpen)
            .put("incidentReported", s.incidentReported)
            .put("extraHours", s.extraHours)
            .put("extensionCharge", s.extensionCharge)
            .put("returnChecked", s.returnChecked)
            .put("depositReturned", s.depositReturned)
            .put("damageCharge", s.damageCharge)
            .put("elapsedMinutes", s.elapsedMinutes)
            .put("distanceKm", s.distanceKm)
            .put("fuelPercent", s.fuelPercent)
            .put("odometerStart", s.odometerStart)
        prefs.edit().putString("session", o.toString()).apply()
    }
}
