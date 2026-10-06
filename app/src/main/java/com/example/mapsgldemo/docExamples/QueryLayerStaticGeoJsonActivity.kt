package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.example.mapsgldemo.DocsExamplesMenuActivity
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
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Wind speeds sampled at your own points, from inline GeoJSON.**
 *
 * Android port of the JS example
 * [Wind speed text labels using custom GeoJSON](https://www.xweather.com/docs/mapsgl/examples/query-layer-static-geojson),
 * with two switches the JS example does not have: the place names, and the unit on each value.
 *
 * ### Three pieces, as in JS
 *
 * 1. The wind speeds layer at [DataQuality.high], the layer the labels sample.
 * 2. A GeoJSON source of 19 cities around Puget Sound, each with a `place.name` property.
 * 3. A query layer over that source, sampling the wind layer at each point: a
 *    [DataQueryLayerDescriptor], the JS `type: 'query'` layer with its `queryLayer`.
 *
 * The labels are two text blocks, as in JS: the value, large and bold, and the place name small
 * beneath it.
 *
 * ### The value is the SDK's, not the JS expression
 *
 * The JS value is `['to-unit', ['var', 'dataValue'], 'temperature', 'C', 'F', 0]`, which converts
 * the wind speed as if it were a temperature. Here the value is the plain `dataValue`, which the SDK
 * has already converted from the wind layer's own units into the map's speed unit.
 *
 * ### The switches and the size slider
 *
 * - **Show names** rebuilds the query layer with or without the name text block. Text blocks are
 *   part of the layer's paint, set when it is added.
 * - **Label size** scales both text blocks and the gap between them, from half to twice the JS
 *   size. Like the names switch, it rebuilds the layer, once the slider is released.
 * - **Show units** sets [MapboxMapController.showTextLabelUnits]: `12` becomes `12 mph`. Like JS,
 *   the labels carry no unit by default. The labels reformat in place, without re-sampling.
 *
 * JS also turns off `fadeOpacity`; Android's symbol paint has no such setting, so the labels fade in
 * as symbols normally do.
 */
class QueryLayerStaticGeoJsonActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Wind speeds sampled at 19 cities around Puget Sound, from inline GeoJSON."

    // The JS example's center: [-122.33207, 47.60621], zoom: 8. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(47.60621, -122.33207)
    override val cameraZoom = 8.0

    private var showNames = true

    /** The sampled wind layer's code, known once [customizeLayers] has added it. */
    private var windCode: LayerCode? = null

    override fun customizeLayers(controller: MapboxMapController) {
        // The basemap's own place names would sit under ours; Standard keeps them in its import.
        controller.mapboxMap?.style?.setStyleImportConfigProperty(
            "basemap",
            "showPlaceLabels",
            Value.valueOf(false),
        )

        // JS: controller.addWeatherLayer('wind-speeds', { data: { quality: DataQuality.high } })
        val winds = WeatherService.WindSpeeds(controller.service)
        winds.layer.quality = DataQuality.high
        controller.addWeatherLayer(winds)
        windCode = winds.code

        // JS: controller.addSource('cities', { type: 'geojson', data: cityData })
        val cities = GeoJSONSourceDescriptor(id = SOURCE_ID)
        cities.data = FeatureCollection.fromJson(PUGET_SOUND_CITIES)
        controller.addSource(cities)

        // JS: controller.addLayer('cities-text', { type: 'query', source, queryLayer, paint })
        addQueryLayer(controller)
    }

    override fun buildControls() {
        addToggle("Show names", showNames) { on ->
            showNames = on
            controller.removeLayer(LAYER_ID)
            addQueryLayer(controller)
        }
        addToggle("Show units", controller.showTextLabelUnits) { on ->
            controller.showTextLabelUnits = on
        }
        // Scales the value, the name and the gap between them together, so they stay aligned.
        addSlider("Label size", MIN_SCALE, MAX_SCALE, labelScale, format = { "%.1fx".format(it) }) { scale ->
            labelScale = scale
            controller.removeLayer(LAYER_ID)
            addQueryLayer(controller)
        }
    }

    /** Multiplies the JS sizes (24 for values, 12 for names) and the 20px gap between them. */
    private var labelScale = 1.0

    private fun addQueryLayer(controller: MapboxMapController) {
        val code = windCode ?: return
        val text = buildList {
            add(
                TextPaint(
                    value = StyleValue.Expression(Expression.variable("dataValue")),
                    size = StyleValue.Constant(VALUE_SIZE * labelScale),
                    weight = StyleValue.Constant("bold"),
                    color = StyleValue.Constant(Color.Black),
                    outlineColor = StyleValue.Constant(Color.White),
                    allowOverlap = StyleValue.Constant(true),
                ),
            )
            if (showNames) {
                add(
                    TextPaint(
                        value = StyleValue.Expression(Expression.get("place.name")),
                        size = StyleValue.Constant(NAME_SIZE * labelScale),
                        weight = StyleValue.Constant("regular"),
                        color = StyleValue.Constant(Color.Black),
                        outlineColor = StyleValue.Constant(Color.White),
                        anchor = StyleValue.Constant(Anchor.TOP),
                        offset = StyleValue.Constant(AnchorOffset(0.0, NAME_OFFSET * labelScale)),
                    ),
                )
            }
        }
        controller.addLayer(
            DataQueryLayerDescriptor(
                id = LAYER_ID,
                source = SOURCE_ID,
                sampledLayerCode = code,
                labelKeyPath = "place.name",
                paint = SymbolLayerPaint(
                    icon = IconPaint(),
                    text = text,
                    // JS: symbol.pitchWithMap / rotateWithMap = false
                    pitchWithMap = false,
                    rotateWithMap = false,
                ),
            ),
            beforeID = null,
        )
    }

    private companion object {
        const val SOURCE_ID = "cities"
        const val LAYER_ID = "cities-text"

        /** The JS example's text sizes and the name's offset below the value, at scale 1. */
        const val VALUE_SIZE = 24.0
        const val NAME_SIZE = 12.0
        const val NAME_OFFSET = 20.0

        const val MIN_SCALE = 0.5
        const val MAX_SCALE = 2.0

        /** The JS example's 19 cities, each carrying a `place.name` property. */
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
