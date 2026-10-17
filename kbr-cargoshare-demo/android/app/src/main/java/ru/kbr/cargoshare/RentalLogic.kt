package ru.kbr.cargoshare

enum class RentalStage(val title: String) {
    REQUESTED("Ожидает подтверждения"),
    CONFIRMED("Подтверждена · ожидает выдачи"),
    ACTIVE("Аренда активна"),
    COMPLETED("Завершена"),
    CANCELLED("Отменена")
}

data class DemoVehicle(
    val id: String,
    val title: String,
    val kind: String,
    val payload: String,
    val volume: String,
    val hourPrice: Int,
    val dayPrice: Int,
    val pickup: String,
    val enabled: Boolean = true
)

data class DemoOrder(
    val id: String,
    val vehicleId: String,
    val vehicleTitle: String,
    val pickup: String,
    val start: String,
    val hours: Int,
    val price: Int,
    val stage: RentalStage = RentalStage.REQUESTED
)

object RentalRules {
    val allowedHours = listOf(2, 4, 8, 24)
    const val DEMO_DEPOSIT = 5000

    fun fare(vehicle: DemoVehicle, hours: Int): Int {
        require(hours in allowedHours) { "Unsupported rental duration" }
        require(vehicle.hourPrice > 0 && vehicle.dayPrice > 0) { "Invalid rates" }
        return if (hours == 24) vehicle.dayPrice else vehicle.hourPrice * hours
    }

    fun nextStage(stage: RentalStage): RentalStage? = when (stage) {
        RentalStage.REQUESTED -> RentalStage.CONFIRMED
        RentalStage.CONFIRMED -> RentalStage.ACTIVE
        RentalStage.ACTIVE -> RentalStage.COMPLETED
        RentalStage.COMPLETED, RentalStage.CANCELLED -> null
    }

    fun mayCancel(stage: RentalStage): Boolean =
        stage == RentalStage.REQUESTED || stage == RentalStage.CONFIRMED
}

val demoFleet = listOf(
    DemoVehicle("g01", "ГАЗель NEXT", "Фургон", "1,5 т", "16 м³", 650, 4200, "Нальчик · Центр"),
    DemoVehicle("g02", "ГАЗель Бизнес", "Бортовая", "1,4 т", "12 м³", 590, 3900, "Нальчик · Стрелка"),
    DemoVehicle("g03", "ГАЗель NEXT XL", "Удлинённый", "1,3 т", "20 м³", 750, 4900, "Нальчик · Александровка")
)
