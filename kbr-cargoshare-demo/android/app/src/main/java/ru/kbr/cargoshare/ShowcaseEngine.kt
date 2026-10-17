package ru.kbr.cargoshare

enum class ShowcaseStage(val title: String) {
    PHONE("Регистрация"),
    DOCUMENTS("Проверка документов"),
    REVIEW("Проверка заявки"),
    READY("Выбор автомобиля"),
    CONTRACT("Договор аренды"),
    PAYMENT("Оплата аренды"),
    HANDOVER("Получение автомобиля"),
    PICKUP_INSPECTION("Осмотр перед выдачей"),
    UNLOCK("Открытие автомобиля"),
    READY_TO_DRIVE("Готов к поездке"),
    DRIVING("Аренда активна"),
    RETURN("Возврат автомобиля"),
    FINISHED("Аренда завершена")
}

data class ShowcaseSession(
    val stage: ShowcaseStage = ShowcaseStage.PHONE,
    val documentsSubmitted: Boolean = false,
    val demoApproved: Boolean = false,
    val vehicleId: String = "",
    val vehicleTitle: String = "",
    val pickup: String = "",
    val day: String = "Сегодня",
    val time: String = "12:00",
    val hours: Int = 4,
    val fare: Int = 0,
    val signatureAccepted: Boolean = false,
    val rentPaid: Boolean = false,
    val depositHeld: Boolean = false,
    val preTripChecked: Boolean = false,
    val doorsOpen: Boolean = false,
    val incidentReported: Boolean = false,
    val extraHours: Int = 0,
    val extensionCharge: Int = 0,
    val returnChecked: Boolean = false,
    val depositReturned: Boolean = false,
    val damageCharge: Int = 0,
    val elapsedMinutes: Int = 0,
    val distanceKm: Int = 0,
    val fuelPercent: Int = 75,
    val odometerStart: Int = 42150
)

object ShowcaseEngine {
    private fun requireStage(s: ShowcaseSession, expected: ShowcaseStage) {
        require(s.stage == expected) { "Недопустимый шаг процесса: ${s.stage}" }
    }

    fun verifyPhone(s: ShowcaseSession, code: String): ShowcaseSession {
        requireStage(s, ShowcaseStage.PHONE)
        require(code == "0000") { "В демонстрации используйте код 0000" }
        return s.copy(stage = ShowcaseStage.DOCUMENTS)
    }

    fun submitDocuments(s: ShowcaseSession, passport: Boolean, licence: Boolean): ShowcaseSession {
        requireStage(s, ShowcaseStage.DOCUMENTS)
        require(passport && licence) { "Оба образца документов должны быть отмечены" }
        return s.copy(stage = ShowcaseStage.REVIEW, documentsSubmitted = true)
    }

    fun approve(s: ShowcaseSession): ShowcaseSession {
        requireStage(s, ShowcaseStage.REVIEW)
        require(s.documentsSubmitted)
        return s.copy(stage = ShowcaseStage.READY, demoApproved = true)
    }

    fun reject(s: ShowcaseSession): ShowcaseSession {
        requireStage(s, ShowcaseStage.REVIEW)
        return s.copy(stage = ShowcaseStage.DOCUMENTS, documentsSubmitted = false, demoApproved = false)
    }

    fun reserve(
        s: ShowcaseSession, vehicleId: String, hours: Int, day: String, time: String,
        title: String = "Демо-грузовик", pickup: String = "Нальчик",
        fare: Int = 0
    ): ShowcaseSession {
        requireStage(s, ShowcaseStage.READY)
        require(s.demoApproved)
        require(vehicleId.isNotBlank() && hours in RentalRules.allowedHours)
        require(fare >= 0)
        return s.copy(stage = ShowcaseStage.CONTRACT, vehicleId = vehicleId,
            vehicleTitle = title, pickup = pickup, hours = hours,
            day = day, time = time, fare = fare)
    }

    fun signContract(s: ShowcaseSession, accepted: Boolean): ShowcaseSession {
        requireStage(s, ShowcaseStage.CONTRACT)
        require(accepted) { "Подтвердите ознакомление с демодоговором" }
        return s.copy(stage = ShowcaseStage.PAYMENT, signatureAccepted = true)
    }

    fun simulatePayment(s: ShowcaseSession, confirmed: Boolean): ShowcaseSession {
        requireStage(s, ShowcaseStage.PAYMENT)
        if (!confirmed) return s
        require(s.signatureAccepted)
        return s.copy(stage = ShowcaseStage.HANDOVER, rentPaid = true, depositHeld = true)
    }


    /** Drive-only virtual telemetry. This does not connect to an actual car or GPS. */
    fun remainingMinutes(s: ShowcaseSession): Int =
        ((s.hours + s.extraHours) * 60 - s.elapsedMinutes).coerceAtLeast(0)

    fun arriveAtVehicle(s: ShowcaseSession): ShowcaseSession {
        requireStage(s, ShowcaseStage.HANDOVER)
        require(s.rentPaid && s.depositHeld)
        return s.copy(stage = ShowcaseStage.PICKUP_INSPECTION)
    }

    fun confirmPickupInspection(s: ShowcaseSession, body: Boolean, tyres: Boolean,
                                cabin: Boolean, fuel: Boolean): ShowcaseSession {
        requireStage(s, ShowcaseStage.PICKUP_INSPECTION)
        require(body && tyres && cabin && fuel) { "Нужно проверить все четыре пункта" }
        return s.copy(preTripChecked = true, stage = ShowcaseStage.UNLOCK)
    }

    fun unlockForPickup(s: ShowcaseSession): ShowcaseSession {
        requireStage(s, ShowcaseStage.UNLOCK)
        require(s.preTripChecked && s.rentPaid)
        return s.copy(stage = ShowcaseStage.READY_TO_DRIVE, doorsOpen = true)
    }

    fun startDriving(s: ShowcaseSession): ShowcaseSession {
        requireStage(s, ShowcaseStage.READY_TO_DRIVE)
        require(s.preTripChecked && s.doorsOpen)
        return s.copy(stage = ShowcaseStage.DRIVING, doorsOpen = false)
    }

    fun advanceTrip(s: ShowcaseSession, minutes: Int, km: Int): ShowcaseSession {
        requireStage(s, ShowcaseStage.DRIVING)
        require(minutes > 0 && minutes <= 1440 && km >= 0 && km <= 300) { "Недопустимые данные поездки" }
        return s.copy(
            elapsedMinutes = s.elapsedMinutes + minutes,
            distanceKm = s.distanceKm + km,
            fuelPercent = (s.fuelPercent - (km / 10)).coerceAtLeast(1)
        )
    }

    fun handover(s: ShowcaseSession, inspected: Boolean): ShowcaseSession {
        requireStage(s, ShowcaseStage.HANDOVER)
        require(s.rentPaid && s.depositHeld)
        require(inspected) { "Перед выдачей подтвердите демонстрационный фотоосмотр" }
        return s.copy(stage = ShowcaseStage.DRIVING, preTripChecked = true)
    }

    fun toggleDoors(s: ShowcaseSession): ShowcaseSession {
        requireStage(s, ShowcaseStage.DRIVING)
        return s.copy(doorsOpen = !s.doorsOpen)
    }

    fun reportIncident(s: ShowcaseSession): ShowcaseSession {
        require(s.stage == ShowcaseStage.DRIVING || s.stage == ShowcaseStage.RETURN)
        return s.copy(incidentReported = true)
    }

    fun extend(s: ShowcaseSession, additionalHours: Int, hourPrice: Int): ShowcaseSession {
        requireStage(s, ShowcaseStage.DRIVING)
        require(additionalHours > 0 && hourPrice > 0)
        return s.copy(extraHours = s.extraHours + additionalHours,
            extensionCharge = s.extensionCharge + additionalHours * hourPrice)
    }

    fun requestReturn(s: ShowcaseSession): ShowcaseSession {
        requireStage(s, ShowcaseStage.DRIVING)
        return s.copy(stage = ShowcaseStage.RETURN, doorsOpen = false)
    }

    fun completeReturn(s: ShowcaseSession, inspected: Boolean): ShowcaseSession {
        requireStage(s, ShowcaseStage.RETURN)
        require(inspected) { "Перед завершением проверьте состояние автомобиля" }
        return s.copy(stage = ShowcaseStage.FINISHED,
            returnChecked = true, depositHeld = false, depositReturned = true)
    }

    fun reset(): ShowcaseSession = ShowcaseSession()
}
