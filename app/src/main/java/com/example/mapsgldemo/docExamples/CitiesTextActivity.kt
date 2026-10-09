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
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.style.SymbolRank
import com.xweather.mapsgl.style.SymbolRankDirection
import com.xweather.mapsgl.style.TextPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.types.TileSize

/**
 * **Cities text scaled by importance.**
 *
 * Android stand-in for
 * [Add a Nextzen cities text layer](https://www.xweather.com/docs/mapsgl/examples/nextzen-cities).
 * That example reads Nextzen vector tiles, which need a Nextzen API key. This draws the same kind
 * of place labels from the cities tileset the SDK already uses for `temperatures-text`, and sizes
 * each name from `profile.rankScore` so larger places win the collisions and read larger.
 */
class CitiesTextActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = DocsExamplesMenuActivity::class.java

    override val caption =
        "City names from the SDK cities tileset. Larger places are drawn larger and win collisions."

    override val cameraCenter = Coordinate(40.0, -93.0)
    override val cameraZoom = 4.0
    override val showTimelineBar = false

    override fun customizeLayers(controller: MapboxMapController) {
        controller.mapboxMap?.style?.setStyleImportConfigProperty(
            "basemap",
            "showPlaceLabels",
            Value.valueOf(false),
        )
        controller.addSource(
            VectorSourceDescriptor(
                id = SOURCE_ID,
                url = "https://cdn.aerisapi.com/sdk/js/mapsgl/data/cities/{z}/{x}/{y}.pbf",
                maxZoom = 14f,
                tileSize = TileSize(512, 512),
            ),
        )
        controller.addLayer(
            SymbolLayerDescriptor(
                id = "cities-text",
                source = SOURCE_ID,
                sourceLayer = "cities",
                useGlRendering = false,
                paint = SymbolLayerPaint(
                    pitchWithMap = false,
                    rotateWithMap = false,
                    rank = SymbolRank("profile.rankScore", SymbolRankDirection.DESC),
                    text = listOf(
                        TextPaint(
                            value = StyleValue.Expression(Expression.get("place.name")),
                            size = StyleValue.Expression(
                                Expression.interpolate(
                                    Expression.get("profile.rankScore"),
                                    listOf(0.0, 11.0, 100.0, 22.0),
                                ),
                            ),
                            color = StyleValue.Constant(Color(0xFF555555)),
                            outlineColor = StyleValue.Constant(Color.White),
                        ),
                    ),
                ),
            ),
            beforeID = null,
        )
    }

    private companion object {
        const val SOURCE_ID = "cities"
    }
}
