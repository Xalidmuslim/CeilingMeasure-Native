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
    val odometerStart: Int = 42150,
    val ridesCompleted: Int = 0
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
        return s.copy(stage = ShowcaseStage.READY, documentsSubmitted = true, demoApproved = true)
    }

    fun approve(s: ShowcaseSession): ShowcaseSession {
        if (s.stage == ShowcaseStage.READY && s.demoApproved) return s
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
        return s.copy(stage = ShowcaseStage.HANDOVER, rentPaid = true,
            depositHeld = false, depositReturned = false)
    }


    /** Drive-only virtual telemetry. This does not connect to an actual car or GPS. */
    fun remainingMinutes(s: ShowcaseSession): Int =
        ((s.hours + s.extraHours) * 60 - s.elapsedMinutes).coerceAtLeast(0)

    fun arriveAtVehicle(s: ShowcaseSession): ShowcaseSession {
        requireStage(s, ShowcaseStage.HANDOVER)
        require(s.rentPaid)
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
            returnChecked = true, depositHeld = false, depositReturned = false,
            ridesCompleted = s.ridesCompleted + 1)
    }

    /** Legacy sessions created before automatic demo verification are upgraded once on read. */
    fun resume(s: ShowcaseSession): ShowcaseSession {
        // No deposit is charged in this demo, including restored earlier sessions.
        val clean = s.copy(depositHeld = false, depositReturned = false)
        return when {
            clean.stage == ShowcaseStage.REVIEW && clean.documentsSubmitted ->
                clean.copy(stage = ShowcaseStage.READY, demoApproved = true)
            clean.stage == ShowcaseStage.DOCUMENTS && clean.demoApproved ->
                clean.copy(stage = ShowcaseStage.READY, documentsSubmitted = true)
            else -> clean
        }
    }

    /** A new rental clears only booking-specific details, not verified driver status. */
    fun beginNextRental(s: ShowcaseSession): ShowcaseSession {
        require(s.stage == ShowcaseStage.FINISHED) { "Текущую поездку необходимо завершить" }
        require(s.demoApproved)
        return ShowcaseSession(
            stage = ShowcaseStage.READY,
            documentsSubmitted = true,
            demoApproved = true,
            ridesCompleted = s.ridesCompleted
        )
    }

    /** Explicit showcase reset retains completed registration and driver verification. */
    fun resetRetainingVerification(s: ShowcaseSession): ShowcaseSession {
        require(s.stage == ShowcaseStage.FINISHED || s.stage == ShowcaseStage.READY ||
            s.stage == ShowcaseStage.PHONE || s.stage == ShowcaseStage.DOCUMENTS)
        return ShowcaseSession(
            stage = if (s.demoApproved) ShowcaseStage.READY else ShowcaseStage.PHONE,
            documentsSubmitted = s.demoApproved,
            demoApproved = s.demoApproved,
            ridesCompleted = s.ridesCompleted
        )
    }

    /** Android Back is safe: paid/active bookings cannot be rolled back. */
    fun backStep(s: ShowcaseSession): ShowcaseSession = when (s.stage) {
        ShowcaseStage.DOCUMENTS -> s.copy(stage = ShowcaseStage.PHONE)
        ShowcaseStage.CONTRACT -> s.copy(stage = ShowcaseStage.READY, vehicleId = "",
            vehicleTitle = "", pickup = "", fare = 0)
        ShowcaseStage.PAYMENT -> s.copy(stage = ShowcaseStage.CONTRACT,
            signatureAccepted = false)
        ShowcaseStage.PICKUP_INSPECTION -> s.copy(stage = ShowcaseStage.HANDOVER,
            preTripChecked = false)
        ShowcaseStage.UNLOCK -> s.copy(stage = ShowcaseStage.PICKUP_INSPECTION,
            preTripChecked = false)
        else -> s
    }

    fun reset(): ShowcaseSession = ShowcaseSession()
}
