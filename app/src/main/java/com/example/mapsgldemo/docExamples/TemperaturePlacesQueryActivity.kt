package com.example.mapsgldemo.docExamples

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.bindgen.Value
import com.xweather.mapsgl.layers.spec.DataQueryLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.GeoJSONSourceDescriptor
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Temperature text labels using GeoJSON places.**
 *
 * Android port of
 * [Temperature text labels using GeoJSON places](https://www.xweather.com/docs/mapsgl/examples/query-layer-api-geojson).
 * The points are the top 100 US places from the Xweather places search, as GeoJSON. A data-query
 * layer samples the temperatures layer at each point and draws the result.
 *
 * The JavaScript example formats every reading as Fahrenheit in a paint transform. Here the
 * number is the map's current unit, the same reading the legend and the data inspector use.
 */
class TemperaturePlacesQueryActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = DocsExamplesMenuActivity::class.java

    override val caption =
        "Temperatures sampled at the 100 largest US places, loaded as GeoJSON from the places search."

    override val cameraCenter = Coordinate(40.0, -93.0)
    override val cameraZoom = 4.0

    override fun customizeLayers(controller: MapboxMapController) {
        controller.mapboxMap?.style?.setStyleImportConfigProperty(
            "basemap",
            "showPlaceLabels",
            Value.valueOf(false),
        )
        controller.addWeatherLayer(LayerCode.TEMPERATURES)

        val places = Uri.parse("https://data.api.xweather.com/places/search").buildUpon()
            .appendQueryParameter("query", "country:us,name:!NULL")
            .appendQueryParameter("sort", "pop:-1")
            .appendQueryParameter("limit", "100")
            .appendQueryParameter("format", "geojson")
            .appendQueryParameter("client_id", getString(R.string.xweather_client_id))
            .appendQueryParameter("client_secret", getString(R.string.xweather_client_secret))
            .build()
            .toString()
        controller.addSource(GeoJSONSourceDescriptor(id = CITIES_SOURCE, url = places))
        controller.addLayer(
            DataQueryLayerDescriptor(
                id = "cities-text",
                source = CITIES_SOURCE,
                sampledLayerCode = LayerCode.TEMPERATURES,
                labelKeyPath = "place.name",
                paint = queryValueLabels(),
            ),
            beforeID = null,
        )
    }

    private companion object {
        const val CITIES_SOURCE = "cities"
    }
}
