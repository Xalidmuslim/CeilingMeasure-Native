package ru.kbr.cargoshare

enum class ShowcaseStage(val title: String) {
    PHONE("Регистрация"),
    DOCUMENTS("Проверка документов"),
    REVIEW("Проверка заявки"),
    READY("Выбор автомобиля"),
    CONTRACT("Договор аренды"),
    PAYMENT("Оплата аренды"),
    HANDOVER("Получение автомобиля"),
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
    val damageCharge: Int = 0
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
