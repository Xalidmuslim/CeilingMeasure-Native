package ru.kbr.cargoshare

/** Fictional points drawn over real Nalchik OSM streets; not GPS or navigation. */
object NalchikTripMap {
    data class Point(val lat:Double,val lon:Double)
    const val disclaimer="Демо-маршрут по Нальчику · положение условное"
    private val starts=mapOf(
        "g01" to Point(43.4857,43.6058),
        "g02" to Point(43.4801,43.6209),
        "g03" to Point(43.4931,43.5912)
    )
    fun routeFor(id:String):List<Point> {
        val a=starts[id] ?: Point(43.485,43.608)
        return listOf(
            a,
            Point(a.lat+0.0022,a.lon+0.0022),
            Point(a.lat+0.0035,a.lon+0.0054),
            Point(a.lat+0.0013,a.lon+0.0081),
            Point(a.lat-0.0020,a.lon+0.0061)
        )
    }
    fun current(id:String,kilometers:Int):Point {
        val list=routeFor(id)
        val p=(kilometers.coerceAtLeast(0).toDouble()/12.0).coerceIn(0.0,1.0)
        val section=p*(list.size-1)
        val index=section.toInt().coerceAtMost(list.lastIndex-1)
        val fraction=section-index
        val start=list[index]
        val end=list[index+1]
        return Point(
            start.lat+(end.lat-start.lat)*fraction,
            start.lon+(end.lon-start.lon)*fraction
        )
    }
}
