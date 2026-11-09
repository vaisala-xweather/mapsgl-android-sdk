package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.layers.spec.FillLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.GeoJSONSource
import com.xweather.mapsgl.sources.source.spec.GeoJSONSourceDescriptor
import com.xweather.mapsgl.style.FillLayerPaint
import com.xweather.mapsgl.style.FillPaint
import com.xweather.mapsgl.style.StrokePaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import java.util.Date
import kotlin.math.pow

/**
 * **Satellite, shaded by twilight and night, following the timeline.**
 *
 * Android port of the JS example
 * [Adding a day/night overlay](https://www.xweather.com/docs/mapsgl/examples/day-night-overlay).
 *
 * ### There is no `addDayNightOverlay` on Android
 *
 * JS does the whole thing with one call:
 *
 * ```js
 * controller.addWeatherLayer('satellite');
 * controller.addDayNightOverlay({ opacity: 0.85 });
 * ```
 *
 * The Android SDK has no counterpart, so this example builds the same picture from two things it
 * does have — a GeoJSON source and a fill layer — with the shapes from [DayNightGeometry]:
 *
 * ```kotlin
 * controller.addSource(GeoJSONSourceDescriptor(id = "day-night").apply {
 *     data = DayNightGeometry.nightBands(controller.timeline.currentDate)
 * })
 * controller.addLayer(FillLayerDescriptor("day-night.fill", "day-night", paint = …), beforeID = null)
 * ```
 *
 * ### Twilight is four shapes stacked, not a gradient
 *
 * The night side is four nested bands — sunset, then civil, nautical and astronomical twilight —
 * drawn in one translucent colour. Deep night is covered by all four, so it is darkest, and each
 * band's own alpha is chosen so the four together come to the opacity the slider asks for.
 *
 * ### Following the timeline
 *
 * The JS overlay follows the timeline by itself; here the example listens for it and recomputes
 * the bands, assigning [GeoJSONSource.data]. Every assignment rebuilds the layer's mesh, so the
 * bands are only recomputed once map time has moved [UPDATE_MINUTES] — the terminator moves a
 * quarter of a degree a minute, well under a pixel at these zooms.
 *
 * ### Satellite or temperatures underneath
 *
 * The JS example shows satellite only. Here a drop-down switches the layer under the overlay to
 * temperatures as well, to show the overlay shading any weather layer. The new layer is inserted
 * with `beforeId` set to the overlay's fill layer, so it lands underneath it instead of on top.
 *
 * ### Opacity re-adds the layer
 *
 * Fill opacity is not one of the paths `setPaintProperty` writes, so a new opacity removes and
 * re-adds the fill layer — which is also what the JS example does, calling `addDayNightOverlay`
 * again from its slider.
 */
class DayNightOverlayActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Satellite or temperatures, shaded by twilight and night. The overlay follows the timeline."

    /**
     * The JS example's center is [0, 20]; Coordinate is (lat, lon). JS uses zoom 1.5 for a desktop
     * window — a portrait phone at 1.5 shows about a third of the world, so this is 0.
     */
    override val cameraCenter = Coordinate(20.0, 0.0)
    override val cameraZoom = 0.0

    /** Satellite has no legend; temperatures does, and it appears when that layer is chosen. */
    override val showLegend = true

    private var opacity = DEFAULT_OPACITY
    private var overlayEnabled = true
    private var lastDrawnDate: Date? = null

    /** Index into [BASE_LAYERS] of the weather layer under the overlay. */
    private var baseLayerIndex = 0

    /** Id of that layer as added, so a switch can remove it. */
    private var baseLayerId: String? = null

    /** Set once [customizeLayers] has run; [controller] is not usable before then. */
    private var layersAdded = false

    private val advanceListener: (Any) -> Unit = { runOnUiThread { refreshBands(force = false) } }
    private val changeListener: (Any) -> Unit = { runOnUiThread { refreshBands(force = true) } }

    override fun customizeLayers(controller: MapboxMapController) {
        layersAdded = true
        // JS: controller.addDayNightOverlay({ opacity })
        addOverlay(controller)
        // JS: controller.addWeatherLayer('satellite'). Added after the overlay, under it.
        addBaseLayer(controller)

        controller.timeline.on(AnimationEvent.advance, advanceListener)
        controller.timeline.on(AnimationEvent.stop, changeListener)
        controller.timeline.on(AnimationEvent.TIMELINE_CHANGE, changeListener)
        controller.timeline.on(AnimationEvent.range_change, changeListener)
    }

    override fun buildControls() {
        addChoice("Base layer", BASE_LAYERS.map { it.first }, baseLayerIndex) { index ->
            baseLayerIndex = index
            addBaseLayer(controller)
        }
        // JS: the "Show overlay" checkbox, calling addDayNightOverlay / removeDayNightOverlay.
        addToggle("Show overlay", overlayEnabled) { on ->
            overlayEnabled = on
            if (on) addOverlay(controller) else removeOverlay(controller)
        }
        // JS: the opacity range input, re-calling addDayNightOverlay with the new value.
        addSlider("Opacity", 0.0, 1.0, opacity) { value ->
            opacity = value
            if (overlayEnabled) {
                controller.removeLayer(FILL_LAYER_ID)
                controller.addLayer(fillDescriptor(), beforeID = null)
            }
        }
    }

    override fun onDestroy() {
        if (layersAdded) {
            with(controller.timeline) {
                off(AnimationEvent.advance, advanceListener)
                off(AnimationEvent.stop, changeListener)
                off(AnimationEvent.TIMELINE_CHANGE, changeListener)
                off(AnimationEvent.range_change, changeListener)
            }
        }
        super.onDestroy()
    }

    /** Swaps in the chosen weather layer, underneath the overlay when the overlay is on. */
    private fun addBaseLayer(controller: MapboxMapController) {
        baseLayerId?.let { controller.removeLayer(it) }
        val beforeId = if (overlayEnabled) FILL_LAYER_ID else null
        baseLayerId = controller.addWeatherLayer(BASE_LAYERS[baseLayerIndex].second, beforeId = beforeId)?.id
    }

    private fun addOverlay(controller: MapboxMapController) {
        val date = controller.timeline.currentDate
        val source = GeoJSONSourceDescriptor(id = SOURCE_ID)
        source.data = DayNightGeometry.nightBands(date)
        controller.addSource(source)
        // No beforeID: the overlay goes on top of the satellite layer, as JS's overlay slot does.
        controller.addLayer(fillDescriptor(), beforeID = null)
        lastDrawnDate = date
    }

    private fun removeOverlay(controller: MapboxMapController) {
        controller.removeLayer(FILL_LAYER_ID)
        controller.removeSource(SOURCE_ID)
        lastDrawnDate = null
    }

    /** Recomputes the bands for the playhead's instant, if the terminator has visibly moved. */
    private fun refreshBands(force: Boolean) {
        if (!layersAdded || !overlayEnabled) return
        val date = controller.timeline.currentDate
        if (!force && !DayNightGeometry.movedEnough(lastDrawnDate, date, UPDATE_MINUTES)) return
        if (force && lastDrawnDate?.time == date.time) return
        val source = controller.getSource(SOURCE_ID) as? GeoJSONSource ?: return
        source.data = DayNightGeometry.nightBands(date)
        lastDrawnDate = date
    }

    /** Four nested bands in one colour: each band's alpha `a` satisfies `1 - (1 - a)^4 = opacity`. */
    private fun fillDescriptor(): FillLayerDescriptor {
        val perBand = 1.0 - (1.0 - opacity).pow(1.0 / DayNightGeometry.BAND_ALTITUDES.size)
        return FillLayerDescriptor(
            FILL_LAYER_ID,
            SOURCE_ID,
            paint = FillLayerPaint(
                fill = FillPaint(
                    color = StyleValue.Constant(NIGHT_COLOR),
                    opacity = StyleValue.Constant(perBand),
                ),
                stroke = StrokePaint(thickness = StyleValue.Constant(0.0)),
            ),
        )
    }

    private companion object {
        const val SOURCE_ID = "day-night"
        const val FILL_LAYER_ID = "day-night.fill"

        /** The JS example's default. */
        const val DEFAULT_OPACITY = 0.85

        /** Map-time movement before the bands are recomputed during playback. */
        const val UPDATE_MINUTES = 10L

        val NIGHT_COLOR = Color(0xFF0A1330)

        /** The layers the drop-down offers, satellite first as in the JS example. */
        val BASE_LAYERS = listOf(
            "Satellite" to LayerCode.SATELLITE,
            "Temperatures" to LayerCode.TEMPERATURES,
        )
    }
}
