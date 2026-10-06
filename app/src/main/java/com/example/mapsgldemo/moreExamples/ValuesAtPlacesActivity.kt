package com.example.mapsgldemo.moreExamples

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.example.mapsgldemo.MoreExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.bindgen.Value
import com.xweather.mapsgl.layers.spec.DataQueryLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.GeoJSONSourceDescriptor
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.IconPaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.style.SymbolRank
import com.xweather.mapsgl.style.SymbolRankDirection
import com.xweather.mapsgl.style.TextPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Values at API places** - label cities with temperatures sampled from a weather layer.
 *
 * A [DataQueryLayerDescriptor] fetches no weather data of its own: it takes a point source,
 * samples another weather layer at each point, and draws the result as text. Here the points are
 * the 100 most populous US cities, pulled from the Xweather places endpoint as GeoJSON.
 *
 * - `{clientId}` and `{clientSecret}` in the URL are filled in by the SDK from the map's account,
 *   so the request needs no keys written into it.
 * - `Expression.variable("dataValue")` is the sampled value, formatted in whatever units the map
 *   displays - the labels follow `controller.setUnits(…)` without any work here.
 * - The sampled layer does not have to be added first. If it is not on the map, the data-query
 *   layer adds it hidden and removes it again when the labels go.
 * - Where two labels collide, the rank decides which one stays: here, the larger population.
 */
class ValuesAtPlacesActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = MoreExamplesMenuActivity::class.java

    override val caption = "Temperatures at the 100 largest US cities, from the places API as GeoJSON."

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

        controller.addSource(
            GeoJSONSourceDescriptor(
                id = SOURCE_ID,
                url = "https://data.api.xweather.com/places/search" +
                    "?query=country:us,name:!NULL&sort=pop:-1&limit=100&format=geojson" +
                    "&client_id={clientId}&client_secret={clientSecret}",
            ),
        )
        controller.addLayer(
            DataQueryLayerDescriptor(
                id = LAYER_ID,
                source = SOURCE_ID,
                sampledLayerCode = LayerCode.TEMPERATURES,
                labelKeyPath = "place.name",
                rankKeyPath = "profile.pop",
                paint = SymbolLayerPaint(
                    icon = IconPaint(),
                    rank = SymbolRank("profile.pop", SymbolRankDirection.DESC),
                    text = listOf(
                        TextPaint(
                            value = StyleValue.Expression(Expression.variable("dataValue")),
                            size = StyleValue.Constant(20.0),
                            weight = StyleValue.Constant("bold"),
                            color = StyleValue.Constant(Color.Black),
                            outlineColor = StyleValue.Constant(Color.White),
                        ),
                    ),
                ),
            ),
            beforeID = null,
        )
    }

    private companion object {
        const val SOURCE_ID = "us-cities"
        const val LAYER_ID = "us-city-temperatures"
    }
}
