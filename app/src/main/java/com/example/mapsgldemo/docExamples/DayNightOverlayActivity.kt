package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.maps.Style
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Satellite, shaded by twilight and night, following the timeline.**
 *
 * Android port of the JS example
 * [Adding a day/night overlay](https://www.xweather.com/docs/mapsgl/examples/day-night-overlay).
 *
 * ### One call, as in JS
 *
 * ```kotlin
 * controller.addWeatherLayer(LayerCode.SATELLITE)
 * controller.addDayNightOverlay(opacity = 0.85)
 * ```
 *
 * The night side is four nested caps — sunset, then civil, nautical and astronomical twilight — in
 * one black fill, so deep night is darkest. `opacity` is how dark that deepest night is.
 *
 * ### Following the timeline
 *
 * The overlay moves with the timeline on its own: scrub or play and the terminator follows. Nothing
 * here listens for timeline events.
 *
 * ### Changing it
 *
 * Calling `addDayNightOverlay` again while the shade is up changes its opacity and keeps the
 * geometry, which is what the JS slider does. `removeDayNightOverlay()` takes it off the map,
 * source and all.
 */
class DayNightOverlayActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption = "Satellite stays under the terminator. The overlay follows the timeline."

    /**
     * The JS example's center is [0, 20]; Coordinate is (lat, lon). JS uses zoom 1.5 for a desktop
     * window — a portrait phone at 1.5 shows about a third of the world, so this is 0.
     */
    override val cameraCenter = Coordinate(20.0, 0.0)
    override val cameraZoom = 0.0

    /** The JS example's style. */
    override val mapStyleUri: String = Style.LIGHT

    private var opacity = DEFAULT_OPACITY
    private var overlayEnabled = true

    override fun customizeLayers(controller: MapboxMapController) {
        // JS: controller.addWeatherLayer('satellite')
        controller.addWeatherLayer(LayerCode.SATELLITE)
        // JS: controller.addDayNightOverlay({ opacity })
        controller.addDayNightOverlay(opacity)
    }

    override fun buildControls() {
        addToggle("Show overlay", overlayEnabled) { enabled ->
            overlayEnabled = enabled
            if (enabled) controller.addDayNightOverlay(opacity) else controller.removeDayNightOverlay()
        }
        // JS: the opacity range input, re-calling addDayNightOverlay with the new value.
        addSlider("Opacity", 0.0, 1.0, opacity) { value ->
            opacity = value
            if (overlayEnabled) controller.addDayNightOverlay(opacity)
        }
    }

    private companion object {
        /** The JS example's starting opacity. */
        const val DEFAULT_OPACITY = 0.85
    }
}
