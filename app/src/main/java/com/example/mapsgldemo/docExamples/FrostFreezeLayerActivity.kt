package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.controls.legend.Point.PointLegend
import com.xweather.mapsgl.controls.legend.Point.PointLegendItem
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ColorStop
import com.xweather.mapsgl.style.SampleLayerPaint
import com.xweather.mapsgl.types.Coordinate
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
 * ### The anchor stop has to sit inside the layer's data range
 *
 * The stop list is the JS example's, with one value changed: the bottom anchor is [SCALE_FLOOR_C]
 * rather than JS's -90 C. Stops outside the layer's data range are dropped when the colour lookup
 * table is built, and the temperatures layer's range is roughly -62 C to 54 C - so a stop at -90 C
 * is discarded along with the colour it carries, and hard freeze becomes unreachable. Measured
 * before the anchor was moved: not one hard-freeze pixel anywhere on a September map, with the
 * Greenland ice sheet drawing as Freeze.
 *
 * Any anchor at or below the coldest value the layer can report does the job; -62 C is the floor
 * of that range.
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
 * ### Not ported: the custom inspector readout
 *
 * The JS example also passes a `data.evaluator`, a function turning the sampled value into
 * `"Freeze: -1.20C, 29.84F"` for the data inspector. The Android SDK has no equivalent - the hook
 * exists only as a commented-out `evaluator` field in `StyleInterface` - so tapping the map here
 * reads out the plain temperature. The map itself is unaffected; this is a readout difference.
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

        /**
         * The bottom of both the colour scale and the draw range: the coldest value the
         * temperatures layer reports. See the class doc on why this is not JS's -90.
         */
        const val SCALE_FLOOR_C = -90.0

        /** 28 F: at or below this is a hard freeze. */
        const val HARD_FREEZE_MAX_C = -2.22

        /** The JS example's stops, with the bottom anchor moved - see the class doc. */
        val FROST_FREEZE_STOPS = listOf(
            ColorStop(SCALE_FLOOR_C, HARD_FREEZE),
            ColorStop(HARD_FREEZE_MAX_C, FREEZE),
            ColorStop(0.0, FROST),
            ColorStop(FROST_MAX_C, FROST),
        )
    }
}
