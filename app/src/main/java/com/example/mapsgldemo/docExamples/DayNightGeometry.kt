package com.example.mapsgldemo.docExamples

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import java.util.Date
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The night side of the Earth at a given instant, as GeoJSON polygons for a fill layer.
 *
 * MapsGL JS has `addDayNightOverlay()`; the Android SDK has no counterpart, so the demo builds the
 * same picture from parts the SDK does have: a GeoJSON source and a fill layer.
 *
 * Every point where the sun sits below altitude `h` lies within `90° + h` of the antisolar point, so
 * each band is a spherical cap around that point. Four caps are produced — sunset, then civil,
 * nautical and astronomical twilight — nested inside one another. Drawn with the same translucent
 * fill they stack, which is what makes twilight a gradient rather than one hard edge.
 */
object DayNightGeometry {

    /** Sun altitudes, in degrees, at which each band begins. -0.833° allows for refraction. */
    val BAND_ALTITUDES = doubleArrayOf(-0.833, -6.0, -12.0, -18.0)

    /** Web Mercator's latitude limit; polygons are clamped to it so nothing projects to infinity. */
    const val MAX_LAT = 85.0511

    /** Boundary vertices per cap. 2° of bearing each — smooth at every zoom the overlay makes sense at. */
    private const val STEPS = 180

    /** Subsolar point and declination in degrees: `(latitude, longitude)`. */
    fun subsolarPoint(date: Date): Pair<Double, Double> {
        // Low-precision solar position (Astronomical Almanac), good to ~0.01° — far below a pixel.
        val n = date.time / 86_400_000.0 + 2440587.5 - 2451545.0
        val meanLongitude = normalize360(280.460 + 0.9856474 * n)
        val meanAnomaly = Math.toRadians(normalize360(357.528 + 0.9856003 * n))
        val eclipticLongitude = Math.toRadians(
            meanLongitude + 1.915 * sin(meanAnomaly) + 0.020 * sin(2 * meanAnomaly),
        )
        val obliquity = Math.toRadians(23.439 - 0.0000004 * n)

        val declination = asin(sin(obliquity) * sin(eclipticLongitude))
        val rightAscension = Math.toDegrees(
            atan2(cos(obliquity) * sin(eclipticLongitude), cos(eclipticLongitude)),
        )
        val siderealTime = normalize360(280.46061837 + 360.98564736629 * n)
        return Math.toDegrees(declination) to normalize180(rightAscension - siderealTime)
    }

    /** All four bands for [date], one feature each, tagged with its `altitude`. */
    fun nightBands(date: Date): FeatureCollection {
        val (sunLat, sunLon) = subsolarPoint(date)
        val features = BAND_ALTITUDES.map { altitude ->
            val rings = capRings(centerLat = -sunLat, centerLon = sunLon + 180.0, radius = 90.0 + altitude)
            val polygons = rings.map { ring -> listOf(ring.map { (lon, lat) -> Point.fromLngLat(lon, lat) }) }
            // One polygon per world-copy piece; a MultiPolygon would do, but separate features keep
            // each piece independently valid for the triangulator.
            polygons.map { Feature.fromGeometry(Polygon.fromLngLats(it)).apply { addNumberProperty("altitude", altitude) } }
        }.flatten()
        return FeatureCollection.fromFeatures(features)
    }

    /**
     * The cap of angular [radius] around a centre point, as rings in `[-180, 180]` longitude and
     * `[-MAX_LAT, MAX_LAT]` latitude.
     *
     * The boundary is walked by bearing with longitude unwrapped, so a cap that crosses the
     * antimeridian is one continuous ring. The unwrapped pieces are then shifted by ±360° and clipped
     * to the visible world, giving one piece per side of the antimeridian they touch.
     *
     * Rings are *cut* at [MAX_LAT], never clamped to it. Clamping flattens everything above the line
     * onto it, and where that happens alongside a closing edge the ring doubles back on itself along
     * a zero-height strip — which earcut, in the SDK's fill path, triangulates into a triangle
     * spanning half the map.
     */
    internal fun capRings(centerLat: Double, centerLon: Double, radius: Double): List<List<Pair<Double, Double>>> {
        val lat1 = Math.toRadians(centerLat)
        val d = Math.toRadians(radius)
        // Start inside the world so the unwrapped ring lies within ±540°, which the shifts below cover.
        val centerLon = normalize180(centerLon)

        val boundary = ArrayList<Pair<Double, Double>>(STEPS + 1)
        var previousLon = Double.NaN
        for (i in 0..STEPS) {
            val bearing = Math.toRadians(360.0 * i / STEPS)
            val lat2 = asin(sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(bearing))
            var lon = centerLon + Math.toDegrees(
                atan2(sin(bearing) * sin(d) * cos(lat1), cos(d) - sin(lat1) * sin(lat2)),
            )
            if (!previousLon.isNaN()) {
                while (lon - previousLon > 180.0) lon -= 360.0
                while (lon - previousLon < -180.0) lon += 360.0
            }
            previousLon = lon
            boundary.add(lon to Math.toDegrees(lat2))
        }

        val containsNorthPole = 90.0 - centerLat < radius
        val containsSouthPole = 90.0 + centerLat < radius
        val pieces = when {
            containsNorthPole -> poleCapPieces(boundary, MAX_LAT)
            containsSouthPole -> poleCapPieces(boundary, -MAX_LAT)
            // A cap that avoids both poles is spherically convex, so a horizontal cut leaves it in
            // one piece with no bridges.
            else -> listOfNotNull(
                clipAgainst(clipAgainst(boundary.dropLast(1), MAX_LAT, Axis.LAT, keepAbove = false), -MAX_LAT, Axis.LAT, keepAbove = true)
                    .takeIf { it.size >= 3 },
            )
        }

        return pieces.flatMap { piece ->
            listOf(-360.0, 0.0, 360.0).mapNotNull { shift ->
                clipToWorld(piece.map { (lon, lat) -> lon + shift to lat })
            }
        }
    }

    /**
     * A cap around a pole, as the region between its boundary and the edge of the map at [edgeLat].
     *
     * The boundary of a pole-covering cap sweeps a full 360° of longitude, one latitude per
     * longitude. Wherever it rises past [edgeLat] the cap has no visible area, so the sweep is split
     * there into runs; each run is closed along the map edge on its own. A sweep that never crosses
     * the edge is a single band around the whole world.
     */
    private fun poleCapPieces(
        boundary: List<Pair<Double, Double>>,
        edgeLat: Double,
    ): List<List<Pair<Double, Double>>> {
        val north = edgeLat > 0
        fun visible(lat: Double) = if (north) lat < edgeLat else lat > edgeLat

        val runs = ArrayList<ArrayList<Pair<Double, Double>>>()
        var run: ArrayList<Pair<Double, Double>>? = null
        for (i in boundary.indices) {
            val point = boundary[i]
            if (i > 0) {
                val previous = boundary[i - 1]
                if (visible(previous.second) != visible(point.second)) {
                    val t = (edgeLat - previous.second) / (point.second - previous.second)
                    val crossing = previous.first + t * (point.first - previous.first) to edgeLat
                    if (visible(point.second)) {
                        run = arrayListOf(crossing).also { runs.add(it) }
                    } else {
                        run?.add(crossing)
                        run = null
                    }
                }
            }
            if (visible(point.second)) {
                if (run == null) run = arrayListOf<Pair<Double, Double>>().also { runs.add(it) }
                run.add(point)
            }
        }

        // The sweep is a loop: a run still open at the end continues into the first one.
        val closedFirst = boundary.first().let { visible(it.second) }
        if (runs.size > 1 && closedFirst && visible(boundary.last().second)) {
            val last = runs.removeAt(runs.size - 1)
            val sweep = boundary.last().first - boundary.first().first
            runs[0] = ArrayList(last + runs[0].drop(1).map { (lon, lat) -> lon + sweep to lat })
        }

        return runs.filter { it.size >= 2 }.map { r ->
            val closed = ArrayList(r)
            if (closed.last().second != edgeLat) closed.add(closed.last().first to edgeLat)
            if (closed.first().second != edgeLat) closed.add(closed.first().first to edgeLat)
            closed
        }
    }

    /**
     * Sutherland–Hodgman against the two antimeridian edges, then [densify]; `null` when nothing is
     * left.
     */
    private fun clipToWorld(ring: List<Pair<Double, Double>>): List<Pair<Double, Double>>? {
        var points = ring
        points = clipAgainst(points, -180.0, Axis.LON, keepAbove = true)
        points = clipAgainst(points, 180.0, Axis.LON, keepAbove = false)
        if (points.size < 3) return null
        val width = points.maxOf { it.first } - points.minOf { it.first }
        if (width < 1e-9) return null
        return densify(points + points.first())
    }

    /**
     * Splits any edge longer than [MAX_EDGE_LON] degrees of longitude into equal steps.
     *
     * The SDK's GeoJSON fill path unwraps each ring before triangulating: whenever two consecutive
     * vertices are more than half a world apart it assumes the edge crossed the antimeridian and
     * shifts one by 360°. The edges that close a band along the top or bottom of the map, or run
     * along a clip line, can legitimately span more than 180° — and unwrapped, the ring crosses
     * itself and earcut draws a wedge across half the map. It showed for one frame at a time during
     * playback, whenever the night side sat near 97°W. Short edges leave nothing to misread.
     */
    private fun densify(ring: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
        val out = ArrayList<Pair<Double, Double>>(ring.size + 16)
        for (i in 0 until ring.size - 1) {
            val (lon1, lat1) = ring[i]
            val (lon2, lat2) = ring[i + 1]
            out.add(ring[i])
            val steps = kotlin.math.ceil(abs(lon2 - lon1) / MAX_EDGE_LON).toInt()
            for (s in 1 until steps) {
                val t = s.toDouble() / steps
                out.add(lon1 + t * (lon2 - lon1) to lat1 + t * (lat2 - lat1))
            }
        }
        out.add(ring.last())
        return out
    }

    /** Longest edge, in degrees of longitude, a ring may carry into the SDK — well under 180°. */
    const val MAX_EDGE_LON = 10.0

    private enum class Axis { LON, LAT }

    /** One Sutherland–Hodgman pass against the line `axis = edge`, on an open ring. */
    private fun clipAgainst(
        points: List<Pair<Double, Double>>,
        edge: Double,
        axis: Axis,
        keepAbove: Boolean,
    ): List<Pair<Double, Double>> {
        if (points.isEmpty()) return points
        fun coord(p: Pair<Double, Double>) = if (axis == Axis.LON) p.first else p.second
        fun inside(p: Pair<Double, Double>) = if (keepAbove) coord(p) >= edge else coord(p) <= edge
        val out = ArrayList<Pair<Double, Double>>(points.size + 4)
        for (i in points.indices) {
            val current = points[i]
            val previous = points[(i + points.size - 1) % points.size]
            val currentIn = inside(current)
            val previousIn = inside(previous)
            if (currentIn != previousIn) {
                val t = (edge - coord(previous)) / (coord(current) - coord(previous))
                val lon = previous.first + t * (current.first - previous.first)
                val lat = previous.second + t * (current.second - previous.second)
                out.add(if (axis == Axis.LON) edge to lat else lon to edge)
            }
            if (currentIn) out.add(current)
        }
        return out
    }

    private fun normalize360(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

    private fun normalize180(degrees: Double): Double {
        val d = normalize360(degrees)
        return if (d > 180.0) d - 360.0 else d
    }

    /** Whether two instants are far enough apart in map time that the terminator visibly moved. */
    fun movedEnough(a: Date?, b: Date, thresholdMinutes: Long): Boolean =
        a == null || abs(a.time - b.time) >= thresholdMinutes * 60_000L
}
