package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ParticleLayerPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.utils.mphToMs
import com.xweather.mapsgl.weather.WeatherService
import kotlin.math.roundToInt

/**
 * **Wind particles drawn only between two wind speeds, adjusted live.**
 *
 * Android port of the JS example
 * [Adjusting particle draw range in real time](https://www.xweather.com/docs/mapsgl/examples/wind-particles-draw-range).
 * Two sliders set the slowest and fastest wind the particle layer draws; everything outside the
 * pair is left undrawn.
 *
 * ### The same `drawRange` as a sample layer
 *
 * A particle layer carries a [com.xweather.mapsgl.style.SamplePaint] of its own, so the draw range
 * lives in the same place as on the temperatures layer - [ParticleLayerPaint.sample]:
 *
 * ```kotlin
 * (config.layer.paint as ParticleLayerPaint).sample.drawRange = mphToMs(low)..mphToMs(high)
 * ```
 *
 * ### The sliders are in mph, the data is in m/s
 *
 * A draw range is always in the data's own units, which for wind is metres per second. [mphToMs]
 * is the same helper, under the same name, as the JS example's `aerisweather.mapsgl.units.mphToMs`.
 *
 * ### Changed live with `setPaintProperty`
 *
 * The layer is added once. Each slider release writes the new range onto the live layer:
 *
 * ```kotlin
 * controller.setPaintProperty(layerId, "sample.drawRange", mphToMs(low)..mphToMs(high))
 * ```
 *
 * The JS example writes one end at a time with `sample.drawRange.min` and `sample.drawRange.max`,
 * and both of those paths work here too. This example writes the whole range because its sliders
 * can be dragged past each other, and ordering the pair first keeps the lower slider from ever
 * becoming the upper bound. The sliders commit on release rather than on every touch event.
 *
 * ### Particles outside the range are hidden, not recoloured
 *
 * Particles inside the range keep the colour the scale gives them; the range decides which are
 * drawn, not how. This is the JS behaviour, and needs an SDK built with the particle discard fix and
 * the particle `sample.drawRange` paint path - before them, the range stretched the colour scale
 * across itself and drew every particle, and `setPaintProperty` rejected the path.
 */
class WindParticlesDrawRangeActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Wind particles drawn only between the two slider speeds. Particles outside the range " +
            "are not drawn."

    // The JS example's center: [-93, 39], zoom: 4. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(39.0, -93.0)
    override val cameraZoom = 4.0

    override val showLegend = true

    /** Both in mph, matching what the sliders show. The JS sliders start fully open. */
    private var lowerMph = SLIDER_MIN_MPH
    private var upperMph = SLIDER_MAX_MPH

    /** Id of the particle layer as added, for [MapboxMapController.setPaintProperty]. */
    private var layerId: String? = null

    override fun customizeLayers(controller: MapboxMapController) {
        val config = WeatherService.WindParticles(controller.service)
        (config.layer.paint as ParticleLayerPaint).sample.drawRange = currentRange()
        layerId = controller.addWeatherLayer(config)?.id
    }

    override fun buildControls() {
        // Both sliders span one fixed range, so the same thumb position always means the same
        // speed, as in the JS example.
        addSlider("Min", SLIDER_MIN_MPH, SLIDER_MAX_MPH, lowerMph, ::formatMph) {
            lowerMph = it
            applyRange()
        }
        addSlider("Max", SLIDER_MIN_MPH, SLIDER_MAX_MPH, upperMph, ::formatMph) {
            upperMph = it
            applyRange()
        }
    }

    /** JS: windLayer.setPaintProperty('sample.drawRange.min' / '.max', …). */
    private fun applyRange() {
        val id = layerId ?: return
        controller.setPaintProperty(id, "sample.drawRange", currentRange())
        controller.mapboxMap?.triggerRepaint()
    }

    /**
     * The sliders' range in m/s. They can be dragged past each other, so the pair is ordered
     * rather than trusting which is which.
     */
    private fun currentRange(): ClosedRange<Double> =
        mphToMs(minOf(lowerMph, upperMph))..mphToMs(maxOf(lowerMph, upperMph))

    private fun formatMph(value: Double) = "${value.roundToInt()} mph"

    private companion object {
        /** The JS example's slider track. */
        const val SLIDER_MIN_MPH = 0.0
        const val SLIDER_MAX_MPH = 110.0
    }
}
