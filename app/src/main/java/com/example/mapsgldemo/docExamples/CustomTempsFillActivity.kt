package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ColorScaleOptions
import com.xweather.mapsgl.style.ColorStop
import com.xweather.mapsgl.style.SampleLayerPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.utils.FtoCUnit
import com.xweather.mapsgl.weather.WeatherService

/**
 * **A custom colour ramp on the temperatures layer, banded every 2 degrees Fahrenheit.**
 *
 * Android port of the JS example
 * [Customizing temperature colors](https://www.xweather.com/docs/mapsgl/examples/custom-temps-fill).
 * Two things are being set: the ramp itself, and [ColorScaleOptions.interval], which quantizes it
 * into bands instead of a smooth gradient.
 *
 * ### Everything is in Celsius, including the interval
 *
 * The temperatures source publishes Celsius, so stop values are Celsius - that part is easy to get
 * right. The interval is the trap: `2` degrees **Fahrenheit** of band width is not `FtoC(2)`.
 *
 * `FtoC` converts a temperature reading and subtracts the 32-degree offset, which for a width is
 * simply wrong - it would give -16.7. A width is a difference, so only the scale factor applies.
 * The SDK ships both, and the one to use here is the `Unit` variant:
 *
 * ```kotlin
 * import com.xweather.mapsgl.utils.FtoCUnit
 *
 * interval = FtoCUnit(2.0)   // 1.11 C of band width
 * ```
 *
 * That is the same helper, under the same name, as the JS example's
 * `aerisweather.mapsgl.units.FtoCUnit(2)`.
 *
 * ### Discrete bands need `interpolate = false`
 *
 * A smooth gradient is `interval = 0.0` with `interpolate = true`, which is what the built-in
 * temperatures configuration ships. Discrete bands set both a band width and `interpolate = false`:
 *
 * ```kotlin
 * ColorScaleOptions(
 *     stops = CUSTOM_RAMP,
 *     interval = FtoCUnit(2.0),
 *     interpolate = false,
 * )
 * ```
 *
 * `interval` is the width of a band. `interpolate = false` keeps each band a flat color.
 *
 * ### Applying the scale
 *
 * Edit the configuration, add the layer, then pass the same scale to
 * [com.xweather.mapsgl.map.MapController.setPaintProperty]. That call updates the fill and the
 * legend together. Use the same call when the band-width picker changes later:
 *
 * ```kotlin
 * controller.setPaintProperty(layerId, "sample.colorscale", scale)
 * ```
 */
class CustomTempsFillActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "The temperatures layer under a custom colour ramp, quantized into bands. The JS example " +
            "fixes the band width at 2 degrees Fahrenheit; the picker sweeps it."

    // The JS example's center: [-122.33207, 47.60621], zoom: 4. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(47.60621, -122.33207)
    override val cameraZoom = 4.0

    /** The ramp is the point of the example, so show the legend that describes it. */
    override val showLegend = true

    private var layerId: String? = null
    private var intervalIndex = DEFAULT_INTERVAL_INDEX

    override fun customizeLayers(controller: MapboxMapController) {
        val config = WeatherService.Temperatures(controller.service)
        val paint = (config.layer.paint as SampleLayerPaint).sample
        val scale = scaleFor(intervalIndex)
        paint.colorScale = scale
        val layer = controller.addWeatherLayer(config) ?: return
        layerId = layer.id
        controller.setPaintProperty(layer.id, "sample.colorscale", scale)
    }

    override fun buildControls() {
        addChoice("Band width", INTERVAL_LABELS, intervalIndex) {
            intervalIndex = it
            val id = layerId ?: return@addChoice
            controller.setPaintProperty(id, "sample.colorscale", scaleFor(intervalIndex))
        }
    }

    /** The JS example's `paint.sample.colorscale`, with `interpolate = false` for a banded fill. */
    private fun scaleFor(index: Int): ColorScaleOptions {
        val intervalF = INTERVALS_F[index]
        return ColorScaleOptions(
            stops = CUSTOM_RAMP,
            interval = intervalF?.let { FtoCUnit(it) } ?: 0.0,
            interpolate = intervalF == null,
        )
    }

    private companion object {
        /**
         * Band widths in degrees Fahrenheit; `null` is no quantizing at all, which is what
         * [ColorScaleOptions.interval] `0.0` means.
         */
        val INTERVALS_F = listOf(null, 2.0, 5.0, 10.0)
        val INTERVAL_LABELS = listOf("Smooth", "2 F", "5 F", "10 F")

        /** The JS example's own setting. */
        const val DEFAULT_INTERVAL_INDEX = 1

        /**
         * The JS example's ramp, unchanged, in degrees Celsius - the units the temperatures source
         * publishes.
         */
        val CUSTOM_RAMP = listOf(
            ColorStop(-60.0, "#FFFFFF"),
            ColorStop(-50.0, "#9C619B"),
            ColorStop(-40.0, "#58005b"),
            ColorStop(-30.0, "#ce00d7"),
            ColorStop(-20.0, "#121475"),
            ColorStop(-10.0, "#5b97f8"),
            ColorStop(0.0, "#81e8ff"),
            ColorStop(5.0, "#0f7001"),
            ColorStop(15.0, "#ecf93d"),
            ColorStop(25.0, "#e90f0b"),
            ColorStop(35.0, "#6b0001"),
            ColorStop(45.0, "#fff7e2"),
            ColorStop(50.0, "#7b7b7b"),
        )
    }
}
