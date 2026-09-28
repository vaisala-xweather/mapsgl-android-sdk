package com.example.mapsgldemo.moreExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.MoreExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.bindgen.Value
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Values at API places** - label cities with temperatures sampled from a weather layer.
 *
 * A data-query layer fetches no weather data of its own: it takes a set of points, samples another
 * weather layer at each one, and draws the result as text. The sampled layer has to be on the map
 * - it is the thing being sampled.
 *
 * **Where the points come from.** Pulling places from the Xweather places endpoint as GeoJSON is
 * not an option yet: the data-query coordinator only collects points from a vector-tile source.
 * `temperatures-text` is the SDK's built-in equivalent: the same sampling, over the shared
 * `base.cities` place tileset, ranked so the largest cities win collisions.
 *
 * The labels are formatted in whatever units the map displays, so they follow
 * `controller.setUnits(…)` without any work here.
 */
class ValuesAtPlacesActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = MoreExamplesMenuActivity::class.java

    override val caption = "City temperatures sampled from the temperatures layer at each place point."

    override val cameraCenter = Coordinate(40.0, -93.0)
    override val cameraZoom = 4.0

    override fun customizeLayers(controller: MapboxMapController) {
        // The values would otherwise sit on top of the basemap's own place names.
        controller.mapboxMap?.style?.setStyleImportConfigProperty(
            "basemap",
            "showPlaceLabels",
            Value.valueOf(false),
        )

        controller.addWeatherLayer(LayerCode.TEMPERATURES)
        controller.addWeatherLayer(LayerCode.TEMPERATURES_TEXT)
    }
}
