package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.bindgen.Value
import com.xweather.mapsgl.layers.spec.SymbolLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.VectorSourceDescriptor
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.IconPaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.style.TextPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.types.TileSize

/**
 * **City names from a Nextzen vector tile source, sized by population.**
 *
 * Android port of the JS example
 * [Add a Nextzen cities text layer](https://www.xweather.com/docs/mapsgl/examples/nextzen-cities).
 *
 * ### Two pieces, as in JS
 *
 * 1. A [VectorSourceDescriptor] for the Nextzen tile endpoint, the JS `type: 'vector'` source.
 * 2. A [SymbolLayerDescriptor] with one [TextPaint] and no icon, the JS `type: 'text'` layer. It
 *    reads the tiles' `places` layer and keeps only features whose `kind` is `locality`.
 *
 * ### The paint keeps only the branches the filter can reach
 *
 * The JS `size`, `weight`, `transform`, `letterSpacing` and `color` all branch on `kind` for
 * countries and regions. The filter only lets localities through, so those branches never run;
 * the port keeps the locality branch: 12 px below zoom 4, then 12 to 20 px by population.
 *
 * ### Your own Nextzen key
 *
 * Nextzen needs an API key of its own. Request one at https://developers.nextzen.org and put it
 * in [NEXTZEN_API_KEY]. Without one the map shows no labels.
 */
class NextzenCitiesActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption = "City names from Nextzen vector tiles, sized by population."

    // The JS example's center: [-93, 40], zoom: 4. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(40.0, -93.0)
    override val cameraZoom = 4.0

    override val showTimelineBar = false

    override fun customizeLayers(controller: MapboxMapController) {
        // JS hides every Mapbox symbol layer; Standard keeps its place names in the basemap import.
        controller.mapboxMap?.style?.setStyleImportConfigProperty(
            "basemap",
            "showPlaceLabels",
            Value.valueOf(false),
        )

        // JS: controller.addSource('nextzen', { type: 'vector', url: '…/{z}/{x}/{y}.mvt?api_key=…' })
        controller.addSource(
            VectorSourceDescriptor(
                id = SOURCE_ID,
                url = "https://tile.nextzen.org/tilezen/vector/v1/256/all/{z}/{x}/{y}.mvt" +
                    "?api_key=$NEXTZEN_API_KEY",
                tileSize = TileSize(256, 256),
                attribution = "© Nextzen, OpenStreetMap contributors",
            ),
        )

        // JS: controller.addLayer('nextzen-cities', { type: 'text', sourceLayer: 'places', filter, paint })
        controller.addLayer(
            SymbolLayerDescriptor(
                id = LAYER_ID,
                source = SOURCE_ID,
                sourceLayer = "places",
                filter = Expression.equals(Expression.get("kind"), "locality"),
                paint = SymbolLayerPaint(
                    icon = IconPaint(),
                    text = listOf(
                        TextPaint(
                            value = StyleValue.Expression(
                                Expression.coalesce(
                                    listOf(Expression.get("name:en"), Expression.get("name")),
                                ),
                            ),
                            size = StyleValue.Expression(citySize()),
                            weight = StyleValue.Constant("normal"),
                            color = StyleValue.Constant(Color(0xFF555555)),
                            outlineColor = StyleValue.Constant(Color.White),
                        ),
                    ),
                    // JS: symbol: { pitchWithMap: false, rotateWithMap: false }
                    pitchWithMap = false,
                    rotateWithMap = false,
                ),
            ),
            null,
        )
    }

    /**
     * JS: `['case', ['<', ['zoom'], 4], 12, ['interpolate', ['linear'],
     * ['min', 5000000, ['max', 0, ['get', 'population']]], 0, 12, 5000000, 20]]`
     */
    private fun citySize(): Expression = Expression.switchCase(
        listOf(Expression.Case(Expression.lessThan(Expression.zoom, 4), 12.0)),
        Expression.interpolate(
            Expression.min(MAX_POPULATION, Expression.max(0, Expression.get("population"))),
            listOf(0, 12.0, MAX_POPULATION, 20.0),
        ),
    )

    private companion object {
        /** Your Nextzen API key, from https://developers.nextzen.org. */
        const val NEXTZEN_API_KEY = "YOUR_NEXTZEN_API_KEY"

        const val SOURCE_ID = "nextzen"
        const val LAYER_ID = "nextzen-cities"
        const val MAX_POPULATION = 5_000_000
    }
}
