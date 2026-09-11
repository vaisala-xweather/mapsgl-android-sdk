package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ColorScaleOptions
import com.xweather.mapsgl.style.ColorStop
import com.xweather.mapsgl.style.SampleLayerPaint
import com.xweather.mapsgl.style.SamplePaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.utils.FtoCUnit
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherConfiguration
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
 * ### `interval` alone does nothing: it needs `interpolate = false`
 *
 * This is the one place the port cannot copy the JS paint verbatim.
 *
 * In JS, a non-zero `interval` is enough to band the fill. On Android the renderer hands both
 * settings to [com.xweather.mapsgl.style.ColorLookupTable], which quantizes only when **both**
 * conditions hold:
 *
 * ```kotlin
 * if (!interpolate && interval > 0.0) { … expand stops onto the interval grid … }
 * ```
 *
 * Leave [ColorScaleOptions.interpolate] at its default `true` and the interval is silently ignored
 * - the fill draws as a smooth gradient, with no error and no log line. Measured on a device, a
 * frame at `interval = FtoCUnit(10.0)` was pixel-identical to one at `interval = 0.0`; the only
 * thing that differed between the two screenshots was the picker's own label.
 *
 * So the Android form of the JS paint is:
 *
 * ```kotlin
 * ColorScaleOptions(
 *     stops = CUSTOM_RAMP,
 *     interval = FtoCUnit(2.0),
 *     interpolate = false,      // not in the JS example, and required here
 * )
 * ```
 *
 * The two flags are not redundant. `interval` sets the width of a band; `interpolate` decides
 * whether the lookup table is smoothed at all. Wanting a smooth gradient is `interval = 0.0` with
 * `interpolate = true`, which is what the built-in temperatures configuration ships.
 *
 * ### Changing it at runtime needs the layer re-added
 *
 * Same constraint as the
 * [radar colour scale example][CustomRadarColorscaleActivity]: a sample layer's scale is baked into
 * GL lookup-table textures when its program is created, so assigning [SamplePaint.colorScale] moves
 * the legend but not the map. The layer has to go back on for the new scale to reach the renderer.
 *
 * The initial state needs none of that - the JS example just passes `paint` to `addWeatherLayer`,
 * and mutating the configuration before adding it is the Android equivalent.
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

    /** Kept so the same configuration object can be re-added after its paint changes. */
    private lateinit var temperatures: WeatherConfiguration
    private lateinit var paint: SamplePaint
    private var intervalIndex = DEFAULT_INTERVAL_INDEX

    override fun customizeLayers(controller: MapboxMapController) {
        val config = WeatherService.Temperatures(controller.service)
        temperatures = config
        paint = (config.layer.paint as SampleLayerPaint).sample
        applyScale()
        controller.addWeatherLayer(config)
        syncLegendToPaint()
    }

    override fun buildControls() {
        addChoice("Band width", INTERVAL_LABELS, intervalIndex) {
            intervalIndex = it
            applyScale()
            reloadTemperatures()
        }
    }

    /**
     * The JS example's `paint.sample.colorscale`.
     *
     * `interpolate = false` is the one line the JS example does not have and Android needs - see
     * the class doc.
     */
    private fun applyScale() {
        val intervalF = INTERVALS_F[intervalIndex]
        paint.colorScale = ColorScaleOptions(
            stops = CUSTOM_RAMP,
            interval = intervalF?.let { FtoCUnit(it) } ?: 0.0,
            interpolate = intervalF == null,
        )
    }

    /**
     * Rebuilds the layer so the renderer picks up the new scale - see the class doc, and the radar
     * colour scale example for why an in-place refresh is not available for sample layers.
     */
    private fun reloadTemperatures() {
        controller.removeWeatherLayer(LayerCode.TEMPERATURES)
        controller.addWeatherLayer(temperatures)
        syncLegendToPaint()
    }

    /**
     * Re-assigns the scale so the legend catches up with the map.
     *
     * Adding a layer registers the *configuration's* legend template, whose own interval is `0`, so
     * the bar comes back smooth however the fill is drawn. Assigning [SamplePaint.colorScale] emits
     * `PAINT_CHANGE`, and the controller's handler for that pushes the paint's scale into the
     * legend. Harmless to the map - the renderer has already baked its lookup tables - so the two
     * assignments split the work cleanly: the one before the add styles the fill, this one styles
     * the legend.
     */
    private fun syncLegendToPaint() = applyScale()

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
