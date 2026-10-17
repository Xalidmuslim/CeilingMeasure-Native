package ru.kbr.cargoshare

import org.junit.Assert.*
import org.junit.Test

class DemoMapDataTest {
    @Test fun allSeedCarsHaveVisibleIllustrativeStreetAddresses() {
        demoFleet.forEach { v ->
            assertTrue(v.pickup.contains("Нальчик"))
            assertTrue(v.pickup.contains("демо"))
            val point = DemoMapData.pointFor(v.id)
            assertTrue(point.first in 0f..1f)
            assertTrue(point.second in 0f..1f)
        }
    }
    @Test fun existingOwnerAddressMustNotBeOverwritten() {
        val edited = demoFleet[0].copy(pickup="Мой адрес, 25")
        assertEquals("Мой адрес, 25", DemoMapData.normalizePickup(edited).pickup)
    }
    @Test fun legacySeedLocationsUpgradeToIllustrativeAddress() {
        val old = demoFleet[1].copy(pickup="Нальчик · Стрелка")
        assertEquals(demoFleet[1].pickup, DemoMapData.normalizePickup(old).pickup)
    }
    @Test fun newTruckMapPositionIsDeterministicAndWithinBounds() {
        val point = DemoMapData.pointFor("custom-truck-123")
        assertEquals(point, DemoMapData.pointFor("custom-truck-123"))
        assertTrue(point.first in 0f..1f)
        assertTrue(point.second in 0f..1f)
    }
    @Test fun selectionHonorsDisabledVehicles() {
        val cars=demoFleet.map { if(it.id=="g01")it.copy(enabled=false) else it }
        assertEquals("g02",DemoMapData.selectedVehicle(cars,"g01")?.id)
    }
}