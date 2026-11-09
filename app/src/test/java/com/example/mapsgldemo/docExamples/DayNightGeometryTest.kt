package com.example.mapsgldemo.docExamples

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Date
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

class DayNightGeometryTest {

    private fun at(iso: String) = Date.from(Instant.parse(iso))

    @Test
    fun `declination peaks at the June solstice and crosses zero at the March equinox`() {
        assertEquals(23.44, DayNightGeometry.subsolarPoint(at("2026-06-21T08:24:00Z")).first, 0.05)
        assertEquals(0.0, DayNightGeometry.subsolarPoint(at("2026-03-20T14:46:00Z")).first, 0.05)
        assertEquals(-23.44, DayNightGeometry.subsolarPoint(at("2026-12-21T20:50:00Z")).first, 0.05)
    }

    @Test
    fun `sun is overhead near Greenwich at noon UTC, shifted by the equation of time`() {
        // Early November the sun runs ~16.4 minutes fast: it crossed Greenwich at 11:44 and by noon
        // is ~4.1° west.
        assertEquals(-4.1, DayNightGeometry.subsolarPoint(at("2026-11-03T12:00:00Z")).second, 0.2)
        // Mid-February it runs ~14.2 minutes slow, so at noon it is still ~3.55° east.
        assertEquals(3.55, DayNightGeometry.subsolarPoint(at("2026-02-11T12:00:00Z")).second, 0.2)
    }

    @Test
    fun `every band stays within the visible world`() {
        for (iso in listOf("2026-06-21T00:00:00Z", "2026-09-22T18:00:00Z", "2026-12-21T06:00:00Z")) {
            for (altitude in DayNightGeometry.BAND_ALTITUDES) {
                val (sunLat, sunLon) = DayNightGeometry.subsolarPoint(at(iso))
                val rings = DayNightGeometry.capRings(-sunLat, sunLon + 180.0, 90.0 + altitude)
                assertTrue("no rings for $iso @ $altitude", rings.isNotEmpty())
                rings.flatten().forEach { (lon, lat) ->
                    assertTrue(lon in -180.0..180.0)
                    assertTrue(lat in -DayNightGeometry.MAX_LAT..DayNightGeometry.MAX_LAT)
                }
            }
        }
    }

    /**
     * The real check: sample the globe and compare "inside some ring" against the sun's actual
     * altitude there. Covers the antimeridian split and the pole closure, at a solstice (a pole in
     * the cap) and an equinox (no pole in the cap).
     */
    @Test
    fun `ring containment agrees with the sun's altitude`() {
        for (iso in SAMPLE_TIMES) {
            val date = at(iso)
            val (sunLat, sunLon) = DayNightGeometry.subsolarPoint(date)
            for (altitude in DayNightGeometry.BAND_ALTITUDES) {
                val rings = DayNightGeometry.capRings(-sunLat, sunLon + 180.0, 90.0 + altitude)
                var lat = -80.0
                while (lat <= 80.0) {
                    var lon = -179.0
                    while (lon <= 179.0) {
                        val sun = sunAltitude(lat, lon, sunLat, sunLon)
                        // Skip a thin margin around the boundary, where polygon sampling is coarse.
                        if (kotlin.math.abs(sun - altitude) > 1.0) {
                            val inside = rings.any { contains(it, lon, lat) }
                            if (sun < altitude) {
                                assertTrue("$iso h=$altitude ($lat,$lon) sun=$sun should be night", inside)
                            } else {
                                assertFalse("$iso h=$altitude ($lat,$lon) sun=$sun should be day", inside)
                            }
                        }
                        lon += 7.0
                    }
                    lat += 5.0
                }
            }
        }
    }

    /**
     * Earcut needs simple rings. Flattening points onto the map edge used to leave a zero-height
     * strip that doubled back on itself, and the SDK drew a triangle across half the map from it.
     */
    @Test
    fun `every ring is simple, with no edge crossing or overlapping another`() {
        for (iso in SAMPLE_TIMES) {
            val (sunLat, sunLon) = DayNightGeometry.subsolarPoint(at(iso))
            for (altitude in DayNightGeometry.BAND_ALTITUDES) {
                for (ring in DayNightGeometry.capRings(-sunLat, sunLon + 180.0, 90.0 + altitude)) {
                    val n = ring.size - 1
                    for (i in 0 until n) for (j in i + 2 until n) {
                        if (i == 0 && j == n - 1) continue
                        assertFalse(
                            "$iso h=$altitude edges $i and $j of $n intersect",
                            segmentsTouch(ring[i], ring[i + 1], ring[j], ring[j + 1]),
                        )
                    }
                }
            }
        }
    }

    /**
     * The SDK unwraps any edge spanning more than 180° of longitude as an antimeridian crossing. A
     * band closed along the map edge used to carry such an edge whenever night sat near 97°W, and
     * the misread ring drew a wedge across half the map for one frame of playback. Checked across a
     * whole year, because it only happened at particular times of day.
     */
    @Test
    fun `no edge is long enough for the SDK to mistake it for an antimeridian crossing`() {
        val start = at("2026-01-01T00:00:00Z").time
        var t = start
        while (t < start + 365L * 86_400_000L) {
            val (sunLat, sunLon) = DayNightGeometry.subsolarPoint(Date(t))
            for (altitude in DayNightGeometry.BAND_ALTITUDES) {
                for (ring in DayNightGeometry.capRings(-sunLat, sunLon + 180.0, 90.0 + altitude)) {
                    for (i in 0 until ring.size - 1) {
                        val span = kotlin.math.abs(ring[i + 1].first - ring[i].first)
                        assertTrue(
                            "${Date(t).toInstant()} h=$altitude edge $i spans $span°",
                            span <= DayNightGeometry.MAX_EDGE_LON + 1e-9,
                        )
                    }
                }
            }
            t += 30 * 60_000L
        }
    }

    private fun segmentsTouch(
        a: Pair<Double, Double>, b: Pair<Double, Double>,
        c: Pair<Double, Double>, d: Pair<Double, Double>,
    ): Boolean {
        fun cross(o: Pair<Double, Double>, p: Pair<Double, Double>, q: Pair<Double, Double>) =
            (p.first - o.first) * (q.second - o.second) - (p.second - o.second) * (q.first - o.first)
        fun onSegment(p: Pair<Double, Double>, q: Pair<Double, Double>, r: Pair<Double, Double>) =
            minOf(p.first, q.first) - 1e-9 <= r.first && r.first <= maxOf(p.first, q.first) + 1e-9 &&
                minOf(p.second, q.second) - 1e-9 <= r.second && r.second <= maxOf(p.second, q.second) + 1e-9
        val d1 = cross(c, d, a)
        val d2 = cross(c, d, b)
        val d3 = cross(a, b, c)
        val d4 = cross(a, b, d)
        if (((d1 > 1e-12 && d2 < -1e-12) || (d1 < -1e-12 && d2 > 1e-12)) &&
            ((d3 > 1e-12 && d4 < -1e-12) || (d3 < -1e-12 && d4 > 1e-12))
        ) return true
        val eps = 1e-12
        return (kotlin.math.abs(d1) < eps && onSegment(c, d, a)) ||
            (kotlin.math.abs(d2) < eps && onSegment(c, d, b)) ||
            (kotlin.math.abs(d3) < eps && onSegment(a, b, c)) ||
            (kotlin.math.abs(d4) < eps && onSegment(a, b, d))
    }

    private companion object {
        /** Solstices (a pole in the cap), an equinox, and the late-September time seen on device. */
        val SAMPLE_TIMES = listOf(
            "2026-06-21T03:00:00Z",
            "2026-12-21T15:30:00Z",
            "2026-03-20T22:00:00Z",
            "2026-09-29T12:30:00Z",
        )
    }

    private fun sunAltitude(lat: Double, lon: Double, sunLat: Double, sunLon: Double): Double {
        val p = Math.toRadians(lat)
        val d = Math.toRadians(sunLat)
        val h = Math.toRadians(lon - sunLon)
        return Math.toDegrees(asin(sin(p) * sin(d) + cos(p) * cos(d) * cos(h)))
    }

    private fun contains(ring: List<Pair<Double, Double>>, x: Double, y: Double): Boolean {
        var inside = false
        for (i in 0 until ring.size - 1) {
            val (x1, y1) = ring[i]
            val (x2, y2) = ring[i + 1]
            if ((y1 > y) != (y2 > y) && x < (x2 - x1) * (y - y1) / (y2 - y1) + x1) inside = !inside
        }
        return inside
    }
}
