package com.example.mapsgldemo.customize

import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.FillPaint
import com.xweather.mapsgl.style.SortDirection
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Decide which overlapping polygon draws on top.**
 *
 * Threat products nest: the 60-minute zone contains the 30-minute zone, which contains the
 * 15-minute one. Draw them in source order and the widest, least urgent polygon can land on top
 * and bury the imminent one.
 *
 * `FillPaint.sortKey` gives each feature a number and `sortDirection` says which end wins. Flip
 * the toggle and watch the imminent zone disappear under the wider ones - that swap is the entire
 * feature, and it is why the default is [SortDirection.ASCENDING] only because Mapbox chose it.
 *
 * Turning the sort key off entirely falls back to source order, which is arbitrary.
 */
class FillSortOrderActivity : CustomizationDemoActivity() {

    override val caption =
        "Hail threat polygons sorted by lead time. DESCENDING draws the most imminent zone on " +
            "top; flip to ASCENDING to watch it get buried under the wider ones."

    override val cameraCenter = Coordinate(35.0, -97.0)
    override val cameraZoom = 5.5

    private lateinit var fill: FillPaint
    private var descending = true
    private var sorted = true

    /**
     * Lead time in minutes: how far ahead of issue time this polygon's window starts. The
     * built-in colour expression derives the same number, so this reuses that arithmetic.
     */
    private val leadTimeMinutes: Expression
        get() = Expression.divide(
            Expression.subtract(
                Expression.get("period.range.minTimestamp"),
                Expression.get("details.issuedTimestamp"),
            ),
            60,
        )

    override fun customizeLayers(controller: MapboxMapController) = addThreats()

    private fun addThreats() {
        val threats = WeatherService.HailThreatsPolygons(controller.service)
        fill = threats.layer.paint.fill

        fill.sortKey = if (sorted) StyleValue.Expression(leadTimeMinutes) else null
        fill.sortDirection = if (descending) SortDirection.DESCENDING else SortDirection.ASCENDING

        controller.addWeatherLayer(threats)
    }

    private fun rebuild() = reAddLayer(LayerCode.HAIL_THREATS_POLYGONS) { addThreats() }

    override fun buildControls() {
        addToggle("Imminent zone on top (DESCENDING)", true) {
            descending = it
            rebuild()
        }
        addToggle("Sort by lead time at all", true) {
            sorted = it
            rebuild()
        }
    }
}
