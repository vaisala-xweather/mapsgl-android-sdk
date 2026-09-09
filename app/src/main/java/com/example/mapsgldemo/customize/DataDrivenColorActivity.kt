package com.example.mapsgldemo.customize

import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.CircleLayerPaint
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Colour a layer from a feature property.**
 *
 * Lightning strikes ship with a `step` expression on the `age` property, painting each strike one
 * of a few discrete colours. Toggle "smooth" to swap it for `interpolate`, which blends between
 * the same stops - age then reads as a fade rather than as bands.
 *
 * That toggle is the lesson: `step` and `interpolate` take the same shape of arguments and differ
 * only in whether they blend. Use `step` for categories and legends that must match exactly, and
 * `interpolate` for continuous quantities.
 *
 * The "oldest strike" slider moves the last stop, which compresses or stretches the whole ramp -
 * a good way to see that stops are absolute data values, not percentages.
 */
class DataDrivenColorActivity : CustomizationDemoActivity() {

    override val caption =
        "Strike colour is driven by the \"age\" property (seconds since the strike). Toggle " +
            "between interpolate (smooth) and step (bands), and move the oldest-strike stop."

    override val cameraCenter = Coordinate(32.0, -95.0)
    override val cameraZoom = 5.0

    private lateinit var paint: CircleLayerPaint
    private var smooth = true
    private var oldest = OLDEST_DEFAULT
    private var radius = RADIUS_DEFAULT

    override fun customizeLayers(controller: MapboxMapController) = addStrikes()

    private fun addStrikes() {
        val lightning = WeatherService.LightningStrikes(controller.service)
        paint = lightning.layer.paint

        applyColor()
        paint.circle.radius = StyleValue.Constant(radius)

        controller.addWeatherLayer(lightning)
    }

    private fun rebuild() = reAddLayer(LayerCode.LIGHTNING_STRIKES) { addStrikes() }

    override fun buildControls() {
        addToggle("Smooth (interpolate) instead of bands (step)", true) {
            smooth = it
            rebuild()
        }
        addSlider("Oldest strike (s)", 300.0, 3600.0, OLDEST_DEFAULT, { "%.0f".format(it) }) {
            oldest = it
            rebuild()
        }
        addSlider("Circle radius", 2.0, 14.0, RADIUS_DEFAULT, { "%.0f".format(it) }) {
            radius = it
            rebuild()
        }
    }

    /**
     * Both branches describe the same ramp; only the operator differs. Note the two APIs want it
     * shaped differently:
     *
     * - `interpolate` takes a flat alternating list: stop, value, stop, value, …
     * - `step` takes [Expression.Step] objects, and the first one has a `null` value - that is the
     *   colour used below the first real stop, which `step` requires and `interpolate` infers.
     */
    private fun applyColor() {
        val ramp = listOf(
            0.0 to "rgba(255, 255, 255, 1)",
            oldest * 0.17 to "rgba(255, 238, 88, 1)",
            oldest * 0.5 to "rgba(255, 138, 0, 1)",
            oldest to "rgba(21, 45, 130, 1)",
        )
        paint.fill.color = StyleValue.Expression(
            if (smooth) {
                Expression.interpolate(
                    Expression.get("age"),
                    ramp.flatMap { (stop, color) -> listOf(stop, color) },
                )
            } else {
                Expression.step(
                    Expression.get("age"),
                    ramp.mapIndexed { index, (stop, color) ->
                        Expression.Step(if (index == 0) null else stop, color)
                    },
                )
            },
        )
    }

    private companion object {
        const val OLDEST_DEFAULT = 1800.0
        const val RADIUS_DEFAULT = 5.0
    }
}
