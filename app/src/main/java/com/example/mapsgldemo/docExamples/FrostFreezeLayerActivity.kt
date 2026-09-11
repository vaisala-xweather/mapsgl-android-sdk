package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.controls.legend.Point.PointLegend
import com.xweather.mapsgl.controls.legend.Point.PointLegendItem
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ColorStop
import com.xweather.mapsgl.style.SampleLayerPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.common.Presentation
import com.xweather.mapsgl.weather.WeatherService

/**
 * **A frost/freeze map built from the temperatures layer.**
 *
 * Android port of the JS example
 * [Create a freeze layer](https://www.xweather.com/docs/mapsgl/examples/frost-freeze-layer).
 * Nothing here is a dedicated product: it is the ordinary temperatures layer with a three-colour
 * scale and everything above freezing-ish clipped away, which is how an agricultural frost map is
 * usually made.
 *
 * ### Three bands, and nothing above them
 *
 * The scale is quantized rather than a ramp, so each category reads as one flat colour:
 *
 * | Band | Range | Colour |
 * | --- | --- | --- |
 * | Hard freeze | at or below 28 F (-2.22 C) | `#992BFF` |
 * | Freeze | above 28 F, at or below 32 F (0 C) | `#0046FF` |
 * | Frost | above 32 F, at or below 36 F (2.22 C) | `#73DAFC` |
 *
 * [com.xweather.mapsgl.style.ColorScaleOptions.interpolate] `= false` is what turns the ramp into
 * bands - left on, the scale fades one colour into the next and there is no line at 32 F to look
 * at.
 *
 * ### `drawRange` is what makes it a frost map rather than a temperature map
 *
 * Everything warmer than 36 F is simply not drawn:
 *
 * ```kotlin
 * paint.sample.drawRange = SCALE_FLOOR_C..FROST_MAX_C
 * ```
 *
 * Without it the layer still covers the whole map in the coldest colour wherever it has data, and
 * the point of the map - *where frost is possible tonight* - is lost in it.
 *
 * One difference from the JS example worth knowing. JS sets `drawRange: { max: 2.22 }` and leaves
 * the minimum open; [com.xweather.mapsgl.style.SamplePaint.drawRange] is a `ClosedRange<Double>`,
 * so Android needs both ends. [SCALE_FLOOR_C] supplies the bottom, and it is deliberately the same
 * value as the lowest colour stop: any temperature the scale can colour is a temperature the range
 * admits, so the floor never clips anything the JS version would have drawn.
 *
 * ### The legend is a point legend, not a bar
 *
 * Three named categories are not a continuous ramp, so the legend is three labelled swatches
 * ([PointLegend]) rather than the bar the temperatures layer normally carries. Assigning
 * [com.xweather.mapsgl.weather.WeatherLayerConfiguration.legend] before the layer is added is what
 * replaces the built-in one.
 *
 * ### The inspector names the band, not just the temperature
 *
 * [com.xweather.mapsgl.weather.common.Presentation] is the counterpart of the JS example's
 * `data.evaluator`: a title and a function turning the sampled value into the row the data
 * inspector shows. Assigning one to
 * [com.xweather.mapsgl.weather.WeatherLayerConfiguration.presentation] replaces the built-in
 * temperature readout, so a tap on the ice sheet reads `Hard Freeze: -18.30C, -0.94F` rather than
 * a bare temperature.
 *
 * Returning an empty string is what suppresses the row, which is how a tap on water the map does
 * not draw says nothing at all instead of naming a category it does not belong to. The JS function
 * returns `''` for the same case.
 */
class FrostFreezeLayerActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "The temperatures layer as a frost map: three flat bands, and everything warmer than " +
            "36 F left undrawn."

    // The JS example's center: [-70, 50], zoom: 2. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(50.0, -70.0)
    override val cameraZoom = 2.0

    override val showLegend = true

    /** The JS example customizes the inspector readout; tapping here at least names the value. */
    override val showDataInspector = true

    override fun customizeLayers(controller: MapboxMapController) {
        val config = WeatherService.Temperatures(controller.service)
        val paint = config.layer.paint as SampleLayerPaint

        // JS: paint.sample.drawRange. Clips the layer to the temperatures this map is about.
        paint.sample.drawRange = SCALE_FLOOR_C..FROST_MAX_C

        // JS: paint.sample.colorscale. copy() so the built-in scale's other fields survive - only
        // the stops, the interval and the interpolation are being replaced.
        paint.sample.colorScale = paint.sample.colorScale.copy(
            stops = FROST_FREEZE_STOPS,
            interval = 1.0,
            interpolate = false,
        )

        // JS: the layer's `legend` option. Replaces the temperature bar with three named swatches.
        config.legend = PointLegend(
            id = "temps-freeze",
            title = "Frost/Freeze",
            items = listOf(
                PointLegendItem(android.graphics.Color.parseColor(HARD_FREEZE), "Hard Freeze"),
                PointLegendItem(android.graphics.Color.parseColor(FREEZE), "Freeze"),
                PointLegendItem(android.graphics.Color.parseColor(FROST), "Frost"),
            ),
        )

        // JS: data.evaluator. Names the band the value falls in, rather than reading out a bare
        // temperature, and says nothing at all for a value the map does not draw.
        config.presentation = Presentation(
            title = "Frost/Freeze",
            fn = { features ->
                val celsius = ((features as? Map<*, *>)?.get("value") as? Number)?.toDouble()
                val fahrenheit = celsius?.let { it * 9.0 / 5.0 + 32.0 }
                val band = when {
                    fahrenheit == null -> null
                    fahrenheit <= 28.0 -> "Hard Freeze"
                    fahrenheit <= 32.0 -> "Freeze"
                    fahrenheit <= 36.0 -> "Frost"
                    else -> null
                }
                if (band == null) {
                    ""
                } else {
                    String.format(Locale.US, "%s: %.2f°C, %.2f°F", band, celsius, fahrenheit)
                }
            },
        )

        // JS: addWeatherLayer('temperatures', { id: 'freeze-temps', … }). The id keeps this layer
        // distinct from an unmodified temperatures layer added alongside it.
        controller.addWeatherLayer(config, id = "freeze-temps")
    }

    private companion object {
        const val HARD_FREEZE = "#992BFF"
        const val FREEZE = "#0046FF"
        const val FROST = "#73DAFC"

        /** 36 F, the warmest temperature this map draws. */
        const val FROST_MAX_C = 2.22

        /** The bottom of both the colour scale and the draw range, as in the JS example. */
        const val SCALE_FLOOR_C = -90.0

        /** 28 F: at or below this is a hard freeze. */
        const val HARD_FREEZE_MAX_C = -2.22

        /** The JS example's stops, verbatim. */
        val FROST_FREEZE_STOPS = listOf(
            ColorStop(SCALE_FLOOR_C, HARD_FREEZE),
            ColorStop(HARD_FREEZE_MAX_C, FREEZE),
            ColorStop(0.0, FROST),
            ColorStop(FROST_MAX_C, FROST),
        )
    }
}
