package ru.kbr.cargoshare

/** All demo pickup points are illustrative, NOT current street positions. */
object DemoMapData {
    fun addressFor(id: String): String? = when (id) {
        "g01" -> "Нальчик, проспект Ленина, 27 (демо)"
        "g02" -> "Нальчик, улица Шогенцукова, 14 (демо)"
        "g03" -> "Нальчик, улица Кирова, 2 (демо)"
        else -> null
    }

    /** Fractional positions inside the offline illustrated Nalchik-style map. */
    fun pointFor(id: String, index: Int = 0): Pair<Float, Float> = when (id) {
        "g01" -> 0.27f to 0.34f
        "g02" -> 0.70f to 0.46f
        "g03" -> 0.44f to 0.71f
        else -> {
            val hash = id.hashCode().toLong() and 0xFFFFFFFFL
            (0.15f + (hash % 70L).toFloat() / 100f) to
                (0.16f + ((hash / 71L) % 60L).toFloat() / 100f)
        }
    }

    fun normalizePickup(v: DemoVehicle): DemoVehicle {
        val old = when (v.id) {
            "g01" -> setOf("Нальчик · Центр", "Нальчик, центр")
            "g02" -> setOf("Нальчик · Стрелка", "Нальчик, район Стрелка")
            "g03" -> setOf("Нальчик · Александровка")
            else -> emptySet()
        }
        return if (v.pickup in old) v.copy(pickup = addressFor(v.id) ?: v.pickup) else v
    }

    fun selectedVehicle(fleet: List<DemoVehicle>, id: String): DemoVehicle? =
        fleet.firstOrNull { it.id == id && it.enabled }
            ?: fleet.firstOrNull { it.enabled }
}
