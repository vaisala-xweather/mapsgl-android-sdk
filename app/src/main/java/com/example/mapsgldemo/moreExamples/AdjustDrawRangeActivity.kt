package com.example.mapsgldemo.moreExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.MoreExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.SampleLayerPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherService
import kotlin.math.roundToInt

/**
 * **Adjust the draw range** - drawing only part of a layer's data range, changed live.
 *
 * `sample.drawRange` clips which values a sample layer paints at all - values outside it are left
 * transparent rather than clamped to the end colours. Narrowing it isolates a band of interest, like showing only freezing temperatures,
 * without touching the colour scale.
 *
 * The sliders read in Fahrenheit because that is what the map displays, but the range is set in
 * Celsius: a draw range is always in the data's own units, which for every MapsGL dataset is
 * metric.
 */
class AdjustDrawRangeActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = MoreExamplesMenuActivity::class.java

    override val caption =
        "Temperatures drawn only between the two slider values. Everything outside the range is " +
            "left transparent, not clamped to the end colours."

    override val cameraCenter = Coordinate(39.0, -80.0)
    override val cameraZoom = 2.0
    override val showLegend = true

    /** Both in Fahrenheit, matching what the sliders show. */
    private var lowerF = -40.0
    private var upperF = 50.0

    override fun customizeLayers(controller: MapboxMapController) = addTemperatures(controller)

    override fun buildControls() {
        // Both sliders span one fixed range. Bounding one by the other's current value would
        // rescale its track as the other moved, so the same thumb position would mean a different
        // temperature from one drag to the next.
        addSlider("Min", SLIDER_MIN_F, SLIDER_MAX_F, lowerF, ::formatF) {
            lowerF = it
            reAddTemperatures()
        }
        addSlider("Max", SLIDER_MIN_F, SLIDER_MAX_F, upperF, ::formatF) {
            upperF = it
            reAddTemperatures()
        }
    }

    /** Re-add rather than mutate: paint is captured when the layer is built. */
    private fun reAddTemperatures() = reAddLayer(LayerCode.TEMPERATURES) { addTemperatures(controller) }

    private fun addTemperatures(controller: MapboxMapController) {
        val config = WeatherService.Temperatures(controller.service)
        // The sliders share one scale and can be dragged past each other, so order the pair
        // rather than trusting which is which.
        val low = minOf(lowerF, upperF)
        val high = maxOf(lowerF, upperF)
        (config.layer.paint as SampleLayerPaint).sample.drawRange = celsius(low)..celsius(high)
        controller.addWeatherLayer(config)
    }

    private fun formatF(value: Double) = "${value.roundToInt()}°F"

    private fun celsius(fahrenheit: Double) = (fahrenheit - 32.0) * 5.0 / 9.0

    private companion object {
        const val SLIDER_MIN_F = -60.0
        const val SLIDER_MAX_F = 120.0
    }
}
