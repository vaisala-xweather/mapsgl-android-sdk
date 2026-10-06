package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.controls.legend.Point.PointLegend
import com.xweather.mapsgl.controls.legend.Point.PointLegendItem
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ColorScaleOptions
import com.xweather.mapsgl.style.ColorStop
import com.xweather.mapsgl.style.SampleLayerPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.types.math_type.BoundedRange
import com.xweather.mapsgl.utils.mphToMs
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Wind speed categories** - named categories rather than a continuous field.
 *
 * The same two properties that build a frost map build this one: a one-sided `drawRange` starting
 * at 15 mph hides the calm majority of the map, and a colour scale with `interpolate = false`
 * paints each category as a flat band.
 *
 * Wind data arrives in metres per second, so the thresholds are written in miles per hour and
 * converted with the SDK's `mphToMs` - the same helper, and the same factor, as the MapsGL JS
 * example's `units.mphToMs`. A draw range and its colour stops are always in the data's own units,
 * never the map's display units.
 */
class WindSpeedCategoriesActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = DocsExamplesMenuActivity::class.java

    override val caption =
        "Wind speeds of 15 mph and up, painted as five named categories. Calmer air is not drawn."

    override val cameraCenter = Coordinate(47.60621, -122.33207)
    override val cameraZoom = 3.0
    override val showLegend = true
    override val showDataInspector = true

    override fun customizeLayers(controller: MapboxMapController) {
        val config = WeatherService.WindSpeeds(controller.service)
        val sample = (config.layer.paint as SampleLayerPaint).sample

        sample.drawRange = BoundedRange.atLeast(mphToMs(CATEGORIES.first().mph))
        sample.colorScale = ColorScaleOptions(
            stops = CATEGORIES.map { ColorStop(mphToMs(it.mph), it.color) },
            // The range the stops are placed against. Left off, it falls back to the stops' own
            // span, which would stretch the top category to fill the rest of the scale.
            range = 0.0..mphToMs(120.0),
            interpolate = false,
        )

        config.legend = PointLegend(
            id = "wind-speed-categories",
            title = "Wind Speed",
            items = CATEGORIES.map {
                PointLegendItem(android.graphics.Color.parseColor(it.color), it.label)
            },
        )

        controller.addWeatherLayer(config)
    }

    private class Category(val mph: Double, val color: String, val label: String)

    private companion object {
        val CATEGORIES = listOf(
            Category(15.0, "#AED9FE", "Breezy"),
            Category(25.0, "#FFDF2C", "Windy"),
            Category(39.0, "#FF9503", "Gale / Tropical Storm Force"),
            Category(55.0, "#DB2500", "Storm / Severe Gale"),
            Category(75.0, "#D802E0", "Hurricane Force"),
        )
    }
}
