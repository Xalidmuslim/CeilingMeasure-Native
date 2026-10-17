package ru.kbr.cargoshare

/** Maps thirteen guarded business states onto five comprehensible customer milestones. */
object PresentationV7 {
    enum class Step(val title:String) {
        ACCOUNT("Профиль"), VEHICLE("Автомобиль"), PAYMENT("Договор и оплата"),
        HANDOVER("Получение"), TRIP("Поездка")
    }
    val groups = Step.entries.toList()
    data class Quote(val rental:Int) {
        val totalToday:Int get() = rental
    }
    fun stepFor(s:ShowcaseStage):Step = when(s) {
        ShowcaseStage.PHONE, ShowcaseStage.DOCUMENTS, ShowcaseStage.REVIEW -> Step.ACCOUNT
        ShowcaseStage.READY -> Step.VEHICLE
        ShowcaseStage.CONTRACT, ShowcaseStage.PAYMENT -> Step.PAYMENT
        ShowcaseStage.HANDOVER, ShowcaseStage.PICKUP_INSPECTION, ShowcaseStage.UNLOCK,
        ShowcaseStage.READY_TO_DRIVE -> Step.HANDOVER
        ShowcaseStage.DRIVING, ShowcaseStage.RETURN, ShowcaseStage.FINISHED -> Step.TRIP
    }
    fun quote(vehicle: DemoVehicle, hours:Int): Quote =
        Quote(RentalRules.fare(vehicle,hours))
    fun pick(fleet: List<DemoVehicle>, id:String):DemoVehicle? =
        DemoMapData.selectedVehicle(fleet,id)
    fun mapFirst(s:ShowcaseStage)=s==ShowcaseStage.READY
}
