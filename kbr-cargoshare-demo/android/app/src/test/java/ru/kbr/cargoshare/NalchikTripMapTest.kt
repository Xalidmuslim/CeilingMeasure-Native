package ru.kbr.cargoshare

import org.junit.Assert.*
import org.junit.Test

class NalchikTripMapTest {
    @Test fun sampleTripUsesCoordinatesNearNalchikNotGenericWorldMap() {
        for (vehicle in demoFleet) {
            val points = NalchikTripMap.routeFor(vehicle.id)
            assertTrue(points.size >= 4)
            points.forEach { p ->
                assertTrue("Latitude",p.lat in 43.45..43.53)
                assertTrue("Longitude",p.lon in 43.57..43.66)
            }
        }
    }
    @Test fun vehicleStartsAtItsPickupPoint() {
        val start=NalchikTripMap.current("g01",0)
        val first=NalchikTripMap.routeFor("g01").first()
        assertEquals(first,start)
    }
    @Test fun tripProgressIsClampedAndMonotonic() {
        val route=NalchikTripMap.routeFor("g01")
        val last=NalchikTripMap.current("g01",1000)
        assertEquals(route.last(),last)
        assertEquals(last,NalchikTripMap.current("g01",5000))
        assertNotEquals(route.first(),NalchikTripMap.current("g01",5))
    }
    @Test fun noActualLocationOrNavigationIsImplied() {
        assertTrue(NalchikTripMap.disclaimer.contains("Демо"))
        assertTrue(NalchikTripMap.disclaimer.contains("маршрут"))
    }
}
