package ru.kbr.cargoshare

/**
 * Completed demo rentals, with no real driver identity or card data.
 * Entries are immutable snapshots: starting another booking must not erase the ledger.
 */
data class RentalHistoryEntry(
    val id: String,
    val rideNumber: Int,
    val vehicleId: String,
    val vehicleTitle: String,
    val pickup: String,
    val start: String,
    val durationHours: Int,
    val elapsedMinutes: Int,
    val distanceKm: Int,
    val rentalPaid: Int,
    val extensionPaid: Int,
    val completedAtMillis: Long,
    val incidentReported: Boolean
) {
    val totalPaid: Int get() = rentalPaid + extensionPaid
}

object TripHistoryLedger {
    fun fromFinished(s: ShowcaseSession, atMillis: Long): RentalHistoryEntry {
        require(s.stage == ShowcaseStage.FINISHED && s.returnChecked)
        require(s.ridesCompleted >= 1)
        return RentalHistoryEntry(
            id = "DEMO-" + s.ridesCompleted + "-" + atMillis,
            rideNumber = s.ridesCompleted,
            vehicleId = s.vehicleId,
            vehicleTitle = s.vehicleTitle.ifBlank { "Грузовой автомобиль" },
            pickup = s.pickup,
            start = s.day + " · " + s.time,
            durationHours = s.hours + s.extraHours,
            elapsedMinutes = s.elapsedMinutes,
            distanceKm = s.distanceKm,
            rentalPaid = s.fare,
            extensionPaid = s.extensionCharge,
            completedAtMillis = atMillis,
            incidentReported = s.incidentReported
        )
    }

    fun append(
        previous: List<RentalHistoryEntry>,
        old: ShowcaseSession,
        new: ShowcaseSession,
        atMillis: Long = System.currentTimeMillis()
    ): List<RentalHistoryEntry> {
        if (old.stage == ShowcaseStage.FINISHED ||
            new.stage != ShowcaseStage.FINISHED ||
            !new.returnChecked ||
            new.ridesCompleted < 1 ||
            previous.any { it.rideNumber == new.ridesCompleted }
        ) return previous
        return (listOf(fromFinished(new, atMillis)) + previous).take(200)
    }
}
