package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.bindgen.Value
import com.xweather.mapsgl.layers.spec.DataQueryLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.GeoJSONSourceDescriptor
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Wind speed text labels using custom GeoJSON.**
 *
 * Android port of
 * [Wind speed text labels using custom GeoJSON](https://www.xweather.com/docs/mapsgl/examples/query-layer-static-geojson).
 * The points are a GeoJSON collection packaged with the app, the cities around Seattle from that
 * example. A data-query layer samples wind speed at each one.
 */
class WindSpeedGeoJsonQueryActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = DocsExamplesMenuActivity::class.java

    override val caption =
        "Wind speed sampled at cities around Seattle, from a GeoJSON file packaged with the app."

    override val cameraCenter = Coordinate(47.60621, -122.33207)
    override val cameraZoom = 8.0

    override fun customizeLayers(controller: MapboxMapController) {
        controller.mapboxMap?.style?.setStyleImportConfigProperty(
            "basemap",
            "showPlaceLabels",
            Value.valueOf(false),
        )
        controller.addWeatherLayer(LayerCode.WIND_SPEEDS)

        val cities = GeoJSONSourceDescriptor(id = CITIES_SOURCE)
        cities.bundledAssetPath = "seattle_wind_places.geojson"
        controller.addSource(cities)
        controller.addLayer(
            DataQueryLayerDescriptor(
                id = "cities-text",
                source = CITIES_SOURCE,
                sampledLayerCode = LayerCode.WIND_SPEEDS,
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
