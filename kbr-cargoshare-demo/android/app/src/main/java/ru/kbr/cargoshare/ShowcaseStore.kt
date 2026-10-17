package ru.kbr.cargoshare

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray

/** Synthetic flow state only; real identity documents or payment credentials are never stored. */
class ShowcaseStore(context: Context) {
    private val prefs = context.getSharedPreferences("kbr_full_showcase_v04", Context.MODE_PRIVATE)

    fun read(): ShowcaseSession {
        val json = prefs.getString("session", null) ?: return ShowcaseSession()
        val parsed = runCatching {
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
                odometerStart = o.optInt("odometerStart",42150),
                ridesCompleted = o.optInt("ridesCompleted", if (o.optString("stage") == "FINISHED") 1 else 0)
            )
        }.getOrElse { ShowcaseSession() }
        val updated = ShowcaseEngine.resume(parsed)
        // Older installed builds stored district names instead of illustrative addresses.
        val oldPickup = when (updated.vehicleId) {
            "g01" -> listOf("Нальчик · Центр", "Нальчик, центр")
            "g02" -> listOf("Нальчик · Стрелка", "Нальчик, район Стрелка")
            "g03" -> listOf("Нальчик · Александровка")
            else -> emptyList()
        }
        val newAddress = DemoMapData.addressFor(updated.vehicleId)
        val current = if (newAddress != null && updated.pickup in oldPickup)
            updated.copy(pickup = newAddress) else updated
        // Import a completed demo trip from an older APK once.
        if (current.stage == ShowcaseStage.FINISHED &&
            current.returnChecked && current.ridesCompleted > 0 &&
            history().none { it.rideNumber == current.ridesCompleted }) {
            writeHistory((listOf(TripHistoryLedger.fromFinished(current, System.currentTimeMillis())) +
                history()).take(200))
        }
        if (current != parsed) save(current)
        return current
    }

    fun history(): List<RentalHistoryEntry> {
        val raw = prefs.getString("completed_trips", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val o = array.getJSONObject(index)
                RentalHistoryEntry(
                    id = o.optString("id"),
                    rideNumber = o.optInt("rideNumber"),
                    vehicleId = o.optString("vehicleId"),
                    vehicleTitle = o.optString("vehicleTitle"),
                    pickup = o.optString("pickup"),
                    start = o.optString("start"),
                    durationHours = o.optInt("durationHours"),
                    elapsedMinutes = o.optInt("elapsedMinutes"),
                    distanceKm = o.optInt("distanceKm"),
                    rentalPaid = o.optInt("rentalPaid"),
                    extensionPaid = o.optInt("extensionPaid"),
                    completedAtMillis = o.optLong("completedAtMillis"),
                    incidentReported = o.optBoolean("incidentReported")
                )
            }
        }.getOrElse { emptyList() }
    }

    private fun writeHistory(entries: List<RentalHistoryEntry>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(JSONObject()
                .put("id", e.id)
                .put("rideNumber", e.rideNumber)
                .put("vehicleId", e.vehicleId)
                .put("vehicleTitle", e.vehicleTitle)
                .put("pickup", e.pickup)
                .put("start", e.start)
                .put("durationHours", e.durationHours)
                .put("elapsedMinutes", e.elapsedMinutes)
                .put("distanceKm", e.distanceKm)
                .put("rentalPaid", e.rentalPaid)
                .put("extensionPaid", e.extensionPaid)
                .put("completedAtMillis", e.completedAtMillis)
                .put("incidentReported", e.incidentReported))
        }
        prefs.edit().putString("completed_trips", array.toString()).commit()
    }

    fun save(s: ShowcaseSession) {
        // Archive only once upon actual completion, never on incidental UI saves.
        val before = prefs.getString("session", null)?.let { raw ->
            runCatching {
                val o = JSONObject(raw)
                ShowcaseSession(
                    stage = ShowcaseStage.valueOf(o.optString("stage", "PHONE")),
                    ridesCompleted = o.optInt("ridesCompleted")
                )
            }.getOrNull()
        } ?: ShowcaseSession()
        val updated = TripHistoryLedger.append(history(), before, s)
        if (updated.size != history().size) writeHistory(updated)
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
            .put("depositHeld", false)
            .put("preTripChecked", s.preTripChecked)
            .put("doorsOpen", s.doorsOpen)
            .put("incidentReported", s.incidentReported)
            .put("extraHours", s.extraHours)
            .put("extensionCharge", s.extensionCharge)
            .put("returnChecked", s.returnChecked)
            .put("depositReturned", false)
            .put("damageCharge", s.damageCharge)
            .put("elapsedMinutes", s.elapsedMinutes)
            .put("distanceKm", s.distanceKm)
            .put("fuelPercent", s.fuelPercent)
            .put("odometerStart", s.odometerStart)
            .put("ridesCompleted", s.ridesCompleted)
        prefs.edit().putString("session", o.toString()).apply()
    }
}
