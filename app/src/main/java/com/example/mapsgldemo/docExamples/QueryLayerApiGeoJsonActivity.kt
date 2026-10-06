package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.bindgen.Value
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
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Temperatures at the 100 largest US cities, from the Xweather places API.**
 *
 * Android port of the JS example
 * [Temperature text labels using GeoJSON places](https://www.xweather.com/docs/mapsgl/examples/query-layer-api-geojson),
 * with two switches the JS example does not have: the place names, and the unit on each value.
 *
 * ### The points come from an API request, not inline data
 *
 * JS builds the request with `account.api()`:
 *
 * ```js
 * account.api().endpoint('places').action('search')
 *     .query('country:us,name:!NULL').sort('pop:-1').limit(100).format('geojson');
 * ```
 *
 * and hands its URL to a GeoJSON source. Android has no request builder, so the source takes the
 * same request as a URL. `{clientId}` and `{clientSecret}` are filled in from the account when the
 * source loads, so the credentials never appear in the code.
 *
 * The query layer then samples the temperatures layer at every place the request returned: a
 * [DataQueryLayerDescriptor], the JS `type: 'query'` layer with its `queryLayer`.
 *
 * ### The switches and the size slider
 *
 * - **Show names** adds a second text block with each place's name beneath its value. The JS
 *   example shows values only, so this starts off. Text blocks are part of the layer's paint, so
 *   the switch rebuilds the query layer.
 * - **Label size** scales both text blocks and the gap between them, from half to twice the JS
 *   size. Like the names switch, it rebuilds the layer, once the slider is released.
 * - **Show units** sets [MapboxMapController.showTextLabelUnits]: `72` becomes `72°F`. Like JS, the
 *   labels carry no unit by default. They reformat in place, without re-sampling.
 *
 * The JS value converts Celsius to Fahrenheit itself (`['to-unit', …, 'temperature', 'C', 'F', 0]`).
 * Here the value is the plain `dataValue`, which the SDK has already put in the map's units —
 * Fahrenheit by default, as in JS. JS also turns off `fadeOpacity`, which Android's symbol paint has
 * no setting for.
 */
class QueryLayerApiGeoJsonActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Temperatures at the 100 largest US cities, from the places API as GeoJSON."

    // The JS example's center: [-93, 40], zoom: 4. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(40.0, -93.0)
    override val cameraZoom = 4.0

    private var showNames = false

    override fun customizeLayers(controller: MapboxMapController) {
        // The basemap's own place names would sit under ours; Standard keeps them in its import.
        controller.mapboxMap?.style?.setStyleImportConfigProperty(
            "basemap",
            "showPlaceLabels",
            Value.valueOf(false),
        )

        // JS: controller.addWeatherLayer('temperatures')
        controller.addWeatherLayer(LayerCode.TEMPERATURES)

        // JS: controller.addSource('cities', { type: 'geojson', url: citiesRequest.url() })
        controller.addSource(
            GeoJSONSourceDescriptor(
                id = SOURCE_ID,
                url = "https://data.api.xweather.com/places/search" +
                    "?query=country:us,name:!NULL&sort=pop:-1&limit=100&format=geojson" +
                    "&client_id={clientId}&client_secret={clientSecret}",
            ),
        )

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
                sampledLayerCode = LayerCode.TEMPERATURES,
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
    }
}
