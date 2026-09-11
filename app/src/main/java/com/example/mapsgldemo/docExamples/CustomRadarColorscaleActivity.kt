package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.controls.legend.bar.BarLegend
import com.xweather.mapsgl.controls.legend.bar.BarLegendLabels
import com.xweather.mapsgl.extensions.Dimension
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ColorScaleOptions
import com.xweather.mapsgl.style.ColorStop
import com.xweather.mapsgl.style.SampleLayerPaint
import com.xweather.mapsgl.style.SamplePaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.ColorScales
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.LegendCode
import com.xweather.mapsgl.weather.WeatherConfiguration
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Swapping the radar layer's colour scale at runtime.**
 *
 * Android port of the JS example
 * [Customizing radar color scale](https://www.xweather.com/docs/mapsgl/examples/custom-radar-colorscale).
 * The radar layer can carry a separate colour scale per precipitation type, or one scale for
 * everything. This screen swaps between the built-in per-type scales, the custom ones from the JS
 * example, and three generic palettes - with the legend on screen so you can see it follow.
 *
 * ### Assigning the scale is not enough on its own
 *
 * The JS example calls `radarLayer.setPaintProperty('sample.colorscale', …)` and the map repaints.
 * On Android the assignment exists - [SamplePaint.colorScale] has a setter that emits
 * `PAINT_CHANGE` carrying the same `"sample.colorscale"` property name the JS SDK uses - but the
 * controller's handler for that event only calls `syncLegend`. The legend re-draws with the new
 * colours and the map keeps the old ones, which is exactly the sort of half-applied change that is
 * easy to miss without a legend on screen to disagree with the map - so this screen shows one.
 *
 * The reason is where the colours live. A sample layer's scale is baked into GL lookup-table
 * textures by `EncodedDataRenderer.setup`, and that runs from `createProgram` / `resetProgram` -
 * once per program, not per frame. Nothing short of rebuilding the program picks up a new scale, and
 * no public entry point does that in place. So this screen assigns the scale and then re-adds the
 * layer:
 *
 * ```kotlin
 * paint.sample.colorScale = ColorScaleOptions(stops = …)
 * controller.removeWeatherLayer(LayerCode.RADAR)
 * controller.addWeatherLayer(config)          // same config object, new program, new LUTs
 * ```
 *
 * Re-adding costs a brief flicker while tiles come back, which is the honest cost of the feature
 * today rather than something this example papers over. Contrast
 * [MapboxMapController.refreshGlVectorLayerPaint], which does exist for vector layers - the
 * equivalent for sample layers is the gap.
 *
 * ### Per-precipitation-type scales, and the transparent first stop
 *
 * The JS example passes `masks: [rain, mix, snow]`, relying on array order. Android has two ways
 * to say this. The built-in radar configuration nests one stop list per type inside
 * [ColorScaleOptions.stops], and that is what this screen follows:
 *
 * ```kotlin
 * ColorScaleOptions(stops = listOf(rainStops, mixStops, snowStops))
 * ```
 *
 * There is also an explicit [ColorScaleOptions.masks] form, whose entries carry the raster band
 * and the discrete precipitation-type code rather than depending on position - worth knowing
 * about, because the JS example's own comments label its three masks rain / snow / mix while the
 * array is read rain / mix / snow.
 *
 * `normalized` stays `false` throughout this screen: every stop value here is a real dBZ number.
 *
 * Every scale here opens with a fully transparent pair below 6 dBZ. That is not decoration: it is
 * what stops the lowest colour in the scale from flooding the whole map where there is no
 * precipitation. The JS example does the same thing by passing `['rgba(0,0,0,0)']` as the prefix
 * argument to `getColorScale`.
 *
 * ### What the JS example has that Android does not
 *
 * JS reaches for `styles.getColorScaleNames()` and `styles.getColorScale(name, prefix)` to pull
 * ready-made normalized scales - rainbow, viridis, plasma and so on. This SDK ships the weather
 * scales ([ColorScales]) but no generic palettes and no lookup by name, so the three offered here
 * are defined in this file: the standard viridis and plasma control points and a plain hue sweep.
 *
 * They are written as 0..1 control points, the shape JS hands back, and then mapped onto real dBZ
 * stops. [ColorScaleOptions.normalized] would take the 0..1 positions directly and does draw on
 * the map - but it leaves the legend blank, because a bar legend intersects the paint's range with
 * its own template range and a 0..1 scale then gets sampled across 0..75 dBZ. Real stops keep map
 * and legend in agreement, and hand the palette its whole span rather than crowding every real
 * echo into the bottom fifth of it.
 *
 * [ColorStop] has a secondary constructor taking a hex string, which is what makes those
 * definitions readable from app code - the SDK's `toColor` string helper is internal, so a
 * `Color` object is otherwise the only way in.
 *
 * That constructor is hex **only**: it hands the string to `android.graphics.Color.parseColor`,
 * which understands `#RRGGBB`, `#AARRGGBB` and the named colours and throws
 * `NumberFormatException` on anything else. The JS example's `'rgba(0,0,0,0)'` therefore becomes
 * `"#00000000"` here - alpha leads in the Android form.
 */
class CustomRadarColorscaleActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "The radar layer's colour scale, swapped at runtime: the built-in per-precipitation-type " +
            "scales, the custom ones from the JS example, then one scale for every type. Watch the " +
            "legend follow."

    // The JS example's center: [-93, 40], zoom: 4. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(40.0, -93.0)
    override val cameraZoom = 4.0

    /** The scale is the subject of this screen, so the legend that describes it is worth showing. */
    override val showLegend = true

    /** Kept so the same configuration object can be re-added after its paint changes. */
    private lateinit var radar: WeatherConfiguration
    private lateinit var paint: SamplePaint
    private var scaleIndex = 0

    override fun customizeLayers(controller: MapboxMapController) {
        val config = WeatherService.Radar(controller.service)
        radar = config
        paint = (config.layer.paint as SampleLayerPaint).sample
        applyScale()
        controller.addWeatherLayer(config)
        combineLegendBarsIfOneScale()
    }

    override fun buildControls() {
        // The JS example uses a <select>; addChoice is the scaffold's drop-down.
        addChoice("Colour scale", SCALE_NAMES, scaleIndex) {
            scaleIndex = it
            applyScale()
            reloadRadar()
        }
    }

    /**
     * Rebuilds the layer so the renderer picks up the new scale. The assignment in [applyScale]
     * updates the legend but not the GL lookup tables - see the class doc.
     */
    private fun reloadRadar() {
        controller.removeWeatherLayer(LayerCode.RADAR)
        controller.addWeatherLayer(radar)
        combineLegendBarsIfOneScale()
    }

    /**
     * Folds the three per-type bars into one when every type is drawn with the same scale, which is
     * what the JS demo does.
     *
     * The built-in radar legend is three bars by construction - rain, mix, snow. Feed it one scale
     * for everything and all three ramps come out identical, labelled RAIN / MIX / SNOW, which
     * reads as three findings where there is only one. So drop to a single bar and drop the type
     * word from its labels; the legend's own "Radar" title already says what it describes.
     *
     * Called after the layer is added, because that is when the legend is created. It does not need
     * an inverse: `removeWeatherLayer` drops the legend and the next `addWeatherLayer` registers
     * the configuration's own three-bar template again, so switching back restores all three.
     */
    @Suppress("UNCHECKED_CAST")
    private fun combineLegendBarsIfOneScale() {
        if (SCALE_NAMES[scaleIndex] in PER_TYPE_SCALES) return
        val legend = legendControl.getLegend(LEGEND_ID) as? BarLegend<Dimension> ?: return
        val bar = legend.items.firstOrNull() ?: return
        val combined = bar.copy(
            labels = bar.labels.copy(
                values = BarLegendLabels.Values.FromLabels<Dimension> {
                    listOf(0.0 to "Light", 1.0 to "Heavy")
                },
            ),
        )
        legendControl.update(legend.copy(items = listOf(combined)))
    }

    private fun applyScale() {
        paint.colorScale = when (SCALE_NAMES[scaleIndex]) {
            // One stop list per precipitation type, in the same rain / mix / snow order the
            // built-in radar configuration declares.
            "Default" -> ColorScaleOptions(
                stops = listOf(
                    ColorScales.radarRain.stops,
                    ColorScales.radarMix.stops,
                    ColorScales.radarSnow.stops,
                ),
            )

            "Custom" -> ColorScaleOptions(
                stops = listOf(CUSTOM_RAIN, CUSTOM_MIX, CUSTOM_SNOW),
            )

            // One scale for all precipitation types, spread across the dBZ span.
            else -> ColorScaleOptions(
                stops = paletteOverDbz(PALETTES[SCALE_NAMES[scaleIndex]].orEmpty()),
            )
        }
    }

    private companion object {
        val SCALE_NAMES = listOf("Default", "Custom", "Rainbow", "Viridis", "Plasma")

        /** The two that carry a separate scale per precipitation type, so the legend keeps 3 bars. */
        val PER_TYPE_SCALES = setOf("Default", "Custom")

        /** [LegendCode.RADAR]'s id, which is what [LegendControl.getLegend] takes. */
        val LEGEND_ID = LegendCode.RADAR.id

        /** Transparent below 6 dBZ so clear air stays clear rather than taking the first colour. */
        private val CLEAR = listOf(
            ColorStop(0.0, "#00000000"),
            ColorStop(5.9, "#00000000"),
        )

        // The JS example's custom masks, unchanged. Note its rain / snow / mix comment order -
        // the array order is what matters, and masks are read rain, mix, snow.
        val CUSTOM_RAIN = CLEAR + listOf(
            ColorStop(6.0, "#588d45"),
            ColorStop(40.0, "#a1943a"),
            ColorStop(70.0, "#fa0000"),
        )

        val CUSTOM_MIX = CLEAR + listOf(
            ColorStop(25.0, "#9B9BE5"),
            ColorStop(45.0, "#0000af"),
            ColorStop(58.0, "#535DFF"),
            ColorStop(87.0, "#373EA8"),
        )

        val CUSTOM_SNOW = CLEAR + listOf(
            ColorStop(6.0, "#E9AAC0"),
            ColorStop(25.0, "#E93D7A"),
            ColorStop(58.0, "#DB1E16"),
            ColorStop(87.0, "#58003c"),
        )

        /**
         * The generic palettes, as 0..1 control points - the shape `getColorScale` returns in JS,
         * and the form the published viridis and plasma numbers are quoted in.
         */
        val PALETTES: Map<String, List<Pair<Double, String>>> = mapOf(
            "Rainbow" to listOf(
                0.00 to "#4b0082",
                0.25 to "#0000ff",
                0.45 to "#00ff00",
                0.65 to "#ffff00",
                0.82 to "#ff7f00",
                1.00 to "#ff0000",
            ),
            "Viridis" to listOf(
                0.00 to "#440154",
                0.25 to "#3b528b",
                0.50 to "#21918c",
                0.75 to "#5ec962",
                1.00 to "#fde725",
            ),
            "Plasma" to listOf(
                0.00 to "#0d0887",
                0.25 to "#7e03a8",
                0.50 to "#cc4778",
                0.75 to "#f89540",
                1.00 to "#f0f921",
            ),
        )

        /**
         * The dBZ span the palettes are spread over.
         *
         * These are the numbers [LegendCode.RADAR]'s own bars use (`range = 6.0..87.0`), so the
         * palette covers exactly what the legend draws.
         */
        private const val DBZ_MIN = 6.0
        private const val DBZ_MAX = 87.0

        /**
         * Maps a 0..1 palette onto real dBZ stops, keeping the transparent lead-in.
         *
         * The alternative - handing the 0..1 positions straight over with
         * [ColorScaleOptions.normalized] set - draws on the map but leaves the legend blank. The
         * bar legend intersects the paint's range with its own template range, so a scale whose
         * stops run 0..1 ends up sampled across 0..75 dBZ and the ramp comes out empty. Real dBZ
         * stops keep the two in agreement, and give the palette the whole span instead of
         * crowding every real echo into its bottom fifth.
         */
        private fun paletteOverDbz(palette: List<Pair<Double, String>>): List<ColorStop> =
            CLEAR + palette.map { (position, hex) ->
                ColorStop(DBZ_MIN + position * (DBZ_MAX - DBZ_MIN), hex)
            }
    }
}
