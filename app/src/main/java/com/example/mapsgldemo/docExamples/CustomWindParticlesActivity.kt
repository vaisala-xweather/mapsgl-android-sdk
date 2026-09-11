package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ColorStop
import com.xweather.mapsgl.style.ParticleDensity
import com.xweather.mapsgl.style.ParticleLayerPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.types.Size
import com.xweather.mapsgl.weather.WeatherService

/**
 * **A normalized colour scale and heavier particle settings on the wind-particles layer.**
 *
 * Android port of the JS example
 * [Customizing wind particles](https://www.xweather.com/docs/mapsgl/examples/custom-wind-particles).
 * Two blocks of paint are replaced before the layer is added: [ParticleLayerPaint.sample], which
 * colours a particle by the wind speed under it, and [ParticleLayerPaint.particle], which decides
 * how many particles there are and how they move.
 *
 * ### The paint maps across almost verbatim
 *
 * This is one of the closest ports in the set - the JS paint object and the Kotlin one line up
 * field for field:
 *
 * | JS | Kotlin |
 * | --- | --- |
 * | `density: ParticleDensity.extreme` | `density = ParticleDensity.EXTREME` |
 * | `size: 1` | `size = Size(1)` |
 * | `speed: 2` | `speed = 2.0` |
 * | `trailsFade: 0.9` | `trailsFade = 0.9` |
 *
 * [Size] takes one integer and squares it, exactly as the JS comment describes, so `Size(1)` is the
 * whole of `size: 1`. The two-argument form is there for wave and swell particles, which are drawn
 * as rectangles.
 *
 * ### JS merges the paint you pass; here you are mutating a built configuration
 *
 * `addWeatherLayer('wind-particles', { paint })` in JS deep-merges that object over the built-in
 * configuration, so naming `colorscale.stops` leaves every sibling field alone - and if the
 * resulting scale has no `range`, JS backfills one from the source dataset's own min and max.
 *
 * Android does neither: [WeatherService.WindParticles] hands back a fully-built configuration and
 * you edit it. That is why the scale is assigned with `copy()` and the particle block is mutated
 * field by field - replacing either object outright would take with it whatever the built-in had
 * set, for the scale `range = 0.0..53.64`, the span the normalized stop positions are fractions of.
 *
 * It renders the same either way, though: when the range is null the renderer falls back to the span
 * of the stops themselves, and that is sufficient - a normalized scale with no range at all was
 * pixel-identical to one keeping the built-in `0.0..53.64`. `copy()` is about saying what you mean,
 * not about avoiding a rendering bug.
 *
 * ### Changing the paint after the layer is added
 *
 * Not something this example does - like the JS one, it sets the paint up front and leaves it. If
 * you do need to restyle a live layer, [com.xweather.mapsgl.map.MapController.setPaintProperty]
 * takes the same key paths as the JS `setPaintProperty` and handles what each one needs:
 *
 * ```kotlin
 * controller.setPaintProperty(layerId, "sample.colorscale", scale)
 * controller.setPaintProperty(layerId, "particle.density", ParticleDensity.LOW)
 * ```
 */
class CustomWindParticlesActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Wind particles under a custom normalized colour scale, with extreme density, " +
            "single-pixel particles, double speed and longer trails."

    // The JS example's center: [10.33207, 47.60621], zoom: 3. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(47.60621, 10.33207)
    override val cameraZoom = 3.0

    override fun customizeLayers(controller: MapboxMapController) {
        val config = WeatherService.WindParticles(controller.service)
        val paint = config.layer.paint as ParticleLayerPaint

        // JS: paint.sample.colorscale. copy() rather than a fresh ColorScaleOptions, so the
        // built-in range survives - see the class doc on merging.
        paint.sample.colorScale = paint.sample.colorScale.copy(
            stops = NORMALIZED_STOPS,
            normalized = true,
        )

        // JS: paint.particle. Mutated in place for the same reason, which leaves kind, count,
        // trails and the drop rates at their built-in values.
        paint.particle.density = ParticleDensity.EXTREME
        paint.particle.size = Size(1)
        paint.particle.speed = 2.0
        paint.particle.trailsFade = 0.9

        controller.addWeatherLayer(config)
    }

    private companion object {
        /**
         * The JS example's scale. Positions are 0..1 fractions of the scale's range, not speeds,
         * which is what `normalized` means.
         */
        val NORMALIZED_STOPS = listOf(
            ColorStop(0.0, "#0b0089"),
            ColorStop(0.25, "#8800a8"),
            ColorStop(0.5, "#cf4875"),
            ColorStop(0.75, "#f99336"),
            ColorStop(1.0, "#f0fb00"),
        )
    }
}
