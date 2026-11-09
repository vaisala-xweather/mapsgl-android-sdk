package com.example.mapsgldemo.moreExamples

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.example.mapsgldemo.MoreExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.bindgen.Value
import com.mapbox.geojson.FeatureCollection
import com.xweather.mapsgl.layers.spec.DataQueryLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.GeoJSONSourceDescriptor
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.IconPaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.style.TextPaint
import com.xweather.mapsgl.types.Anchor
import com.xweather.mapsgl.types.AnchorOffset
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.types.DataQuality
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Values at your own points** - sample a weather layer at points you supply yourself.
 *
 * Same mechanism as querying an API feed, with the points written inline instead - a fixed set of
 * sites, your own stations, wherever you need a reading. The layer draws two text elements per
 * point: the sampled wind speed, and the place name from the feature's own properties beneath it.
 *
 * `quality` on the sampled layer decides how finely its data is sampled. Raising it to
 * [DataQuality.high] matters at close zoom, where neighbouring points would otherwise land in the
 * same cell and report the same value. The sampled layer is added here, rather than left to the
 * data-query layer, so that setting applies.
 */
class ValuesAtOwnPointsActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = MoreExamplesMenuActivity::class.java

    override val caption = "Wind speeds sampled at 19 cities around Puget Sound, supplied as inline GeoJSON."

    override val cameraCenter = Coordinate(47.60621, -122.33207)
    override val cameraZoom = 8.0

    override fun customizeLayers(controller: MapboxMapController) {
        controller.mapboxMap?.style?.setStyleImportConfigProperty(
            "basemap",
            "showPlaceLabels",
            Value.valueOf(false),
        )

        val winds = WeatherService.WindSpeeds(controller.service)
        winds.layer.quality = DataQuality.high
        controller.addWeatherLayer(winds)

        val cities = GeoJSONSourceDescriptor(id = SOURCE_ID)
        cities.data = FeatureCollection.fromJson(PUGET_SOUND_CITIES)
        controller.addSource(cities)

        controller.addLayer(
            DataQueryLayerDescriptor(
                id = LAYER_ID,
                source = SOURCE_ID,
                sampledLayerCode = winds.code,
                labelKeyPath = "place.name",
                paint = SymbolLayerPaint(
                    icon = IconPaint(),
                    text = listOf(
                        TextPaint(
                            value = StyleValue.Expression(Expression.variable("dataValue")),
                            size = StyleValue.Constant(20.0),
                            weight = StyleValue.Constant("bold"),
                            color = StyleValue.Constant(Color.Black),
                            outlineColor = StyleValue.Constant(Color.White),
                        ),
                        TextPaint(
                            value = StyleValue.Expression(Expression.get("place.name")),
                            size = StyleValue.Constant(12.0),
                            color = StyleValue.Constant(Color.Black),
                            outlineColor = StyleValue.Constant(Color.White),
                            anchor = StyleValue.Constant(Anchor.TOP),
                            offset = StyleValue.Constant(AnchorOffset(0.0, 14.0)),
                        ),
                    ),
                ),
            ),
            beforeID = null,
        )
    }

    private companion object {
        const val SOURCE_ID = "puget-sound-cities"
        const val LAYER_ID = "puget-sound-winds"

        /** Nineteen cities around Puget Sound, each carrying a `place.name` property. */
        val PUGET_SOUND_CITIES: String = run {
            val cities = listOf(
                Triple("Seattle", -122.33207, 47.60621),
                Triple("Tacoma", -122.44429, 47.25288),
                Triple("Bellevue", -122.20068, 47.61038),
                Triple("Kent", -122.23484, 47.38093),
                Triple("Everett", -122.20208, 47.97898),
                Triple("Renton", -122.21707, 47.48288),
                Triple("Federal Way", -122.31262, 47.32232),
                Triple("Kirkland", -122.20874, 47.68149),
                Triple("Auburn", -122.22845, 47.30732),
                Triple("Marysville", -122.17708, 48.05176),
                Triple("Redmond", -122.12151, 47.67399),
                Triple("Lakewood", -122.51846, 47.17176),
                Triple("Shoreline", -122.34152, 47.75565),
                Triple("South Hill", -122.27012, 47.14121),
                Triple("Sammamish", -122.0804, 47.64177),
                Triple("Burien", -122.34679, 47.47038),
                Triple("Olympia", -122.90169, 47.04491),
                Triple("Lacey", -122.82319, 47.03426),
                Triple("Bothell", -122.2054, 47.76232),
            )
            val features = cities.joinToString(",") { (name, lon, lat) ->
                """{"type":"Feature","geometry":{"type":"Point","coordinates":[$lon,$lat]},""" +
                    """"properties":{"place":{"name":"$name","state":"WA","country":"US"}}}"""
            }
            """{"type":"FeatureCollection","features":[$features]}"""
        }
    }
}
