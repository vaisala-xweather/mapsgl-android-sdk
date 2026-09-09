package com.example.mapsgldemo.customize

import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.HeatmapPaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Tune how a heatmap reads - with the knobs live.**
 *
 * A heatmap has four settings and they interact, which is why the defaults rarely suit a specific
 * map. Drag the sliders and watch which one actually matters:
 *
 * - `radius` - how far one point spreads, in screen pixels. The biggest lever by far, and
 *   constant-only: an expression here is silently ignored.
 * - `weight` - how much one feature contributes. Data-driven, so rare-but-important features can
 *   count for more.
 * - `intensity` - multiplies the accumulated total before colouring.
 * - `blur` - softens the result; lower gives crisper cores.
 *
 * `color` interpolates over `heatmap-density`, which the renderer supplies as a normalised 0..1
 * value - not one of your feature properties.
 *
 * Every handler goes through [reAddLayer]. Mutating a heatmap's paint after the layer is on the
 * map does nothing on its own, and `refreshGlVectorLayerPaint` does not help here either - it only
 * re-reads paint for symbol layers. Removing and re-adding is what actually takes effect, which is
 * also why the sliders commit on release instead of on every touch event.
 */
class HeatmapTuningActivity : CustomizationDemoActivity() {

    override val caption =
        "Lightning density heatmap. Drag the sliders to see how radius, intensity and blur trade " +
            "off - radius is the one that changes everything."

    override val cameraCenter = Coordinate(32.0, -95.0)
    override val cameraZoom = 4.5

    private lateinit var heatmap: HeatmapPaint
    private var radius = RADIUS_DEFAULT
    private var intensity = INTENSITY_DEFAULT
    private var blur = BLUR_DEFAULT
    private var weightByAge = true

    override fun customizeLayers(controller: MapboxMapController) = addHeatmap()

    private fun addHeatmap() {
        val heat = WeatherService.LightningStrikesHeat(controller.service)
        heatmap = heat.layer.paint.heatmap

        // Recent strikes count for more than old ones.
        heatmap.weight = if (weightByAge) {
            StyleValue.Expression(
                Expression.interpolate(Expression.get("age"), listOf(0.0, 1.0, 1800.0, 0.2)),
            )
        } else {
            StyleValue.Constant(1.0)
        }

        // There is no Expression.heatmapDensity helper - the operator is referenced by name, the
        // same way Expression.zoom is just listOf("zoom").
        heatmap.color = StyleValue.Expression(
            Expression.interpolate(
                listOf("heatmap-density"),
                listOf(
                    0.0, "rgba(0, 0, 0, 0)",
                    0.2, "rgba(33, 102, 172, 0.6)",
                    0.5, "rgba(103, 216, 132, 0.8)",
                    0.8, "rgba(253, 219, 45, 0.9)",
                    1.0, "rgba(214, 31, 31, 1)",
                ),
            ),
        )

        applyRadius(radius)
        heatmap.intensity = StyleValue.Constant(intensity)
        heatmap.blur = StyleValue.Constant(blur)

        controller.addWeatherLayer(heat)
    }

    private fun rebuild() = reAddLayer(LayerCode.LIGHTNING_STRIKES_HEAT) { addHeatmap() }

    override fun buildControls() {
        addSlider("Radius (px)", 4.0, 60.0, RADIUS_DEFAULT, { "%.0f".format(it) }) {
            radius = it
            rebuild()
        }
        addSlider("Intensity", 0.2, 4.0, INTENSITY_DEFAULT) {
            intensity = it
            rebuild()
        }
        addSlider("Blur", 0.1, 2.0, BLUR_DEFAULT) {
            blur = it
            rebuild()
        }
        addToggle("Weight recent strikes more", true) {
            weightByAge = it
            rebuild()
        }
    }

    /**
     * Radius in screen pixels.
     *
     * A zoom-driven `interpolate` here has no effect: the heatmap renderer reads `radius` as a
     * constant, so an expression silently leaves the default in place. Keeping it constant is what
     * actually works - re-apply it yourself on zoom if you need it to scale.
     */
    private fun applyRadius(px: Double) {
        heatmap.radius = StyleValue.Constant(px)
    }

    private companion object {
        const val RADIUS_DEFAULT = 20.0
        const val INTENSITY_DEFAULT = 1.4
        const val BLUR_DEFAULT = 0.6
    }
}
