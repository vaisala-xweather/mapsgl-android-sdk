package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ParticleLayerPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.utils.mphToMs
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherService
import kotlin.math.roundToInt

/**
 * **Adjusting the particle draw range** — wind particles drawn only inside a speed band.
 *
 * Android port of
 * [Adjusting particle draw range in real time](https://www.xweather.com/docs/mapsgl/examples/wind-particles-draw-range).
 * `sample.drawRange` is the same clip the temperature draw-range screen uses. Speeds outside it
 * spawn no particles. The sliders read miles per hour; the range is stored in metres per second,
 * which is the unit the wind field is encoded in.
 */
class ParticleDrawRangeActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = DocsExamplesMenuActivity::class.java

    override val caption =
        "Wind particles drawn only between the two speeds. Everything outside that band is left undrawn."

    override val cameraCenter = Coordinate(39.0, -93.0)
    override val cameraZoom = 4.0
    override val showLegend = true

    private var lowerMph = 0.0
    private var upperMph = 11.0

    override fun customizeLayers(controller: MapboxMapController) = addParticles(controller)

    override fun buildControls() {
        addSlider("Min", SLIDER_MIN_MPH, SLIDER_MAX_MPH, lowerMph, ::formatMph) {
            lowerMph = it
            reAddParticles()
        }
        addSlider("Max", SLIDER_MIN_MPH, SLIDER_MAX_MPH, upperMph, ::formatMph) {
            upperMph = it
            reAddParticles()
        }
    }

    private fun reAddParticles() = reAddLayer(LayerCode.WIND_PARTICLES) { addParticles(controller) }

    private fun addParticles(controller: MapboxMapController) {
        val config = WeatherService.WindParticles(controller.service)
        val low = minOf(lowerMph, upperMph)
        val high = maxOf(lowerMph, upperMph)
        (config.layer.paint as ParticleLayerPaint).sample.drawRange = mphToMs(low)..mphToMs(high)
        controller.addWeatherLayer(config)
    }

    private fun formatMph(value: Double) = "${value.roundToInt()} mph"

    private companion object {
        const val SLIDER_MIN_MPH = 0.0
        const val SLIDER_MAX_MPH = 60.0
    }
}
