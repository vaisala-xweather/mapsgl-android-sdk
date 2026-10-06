package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.layers.spec.LayerMaskLayerConfig
import com.xweather.mapsgl.layers.spec.LayerMaskSpec
import com.xweather.mapsgl.layers.spec.RasterLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.ImageSourceDescriptor
import com.xweather.mapsgl.style.RasterLayerPaint
import com.xweather.mapsgl.style.RasterPaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.types.TileSize
import com.xweather.mapsgl.weather.WeatherService

/**
 * **A raster layer drawn only over land, masked by the water layer.**
 *
 * Android port of the JS example
 * [Adding a land mask to a raster layer](https://www.xweather.com/docs/mapsgl/examples/layer-masking).
 *
 * ### Three steps, as in JS
 *
 * 1. The built-in water layer, filled white, under the id `water-mask`.
 * 2. A raster tile source for NASA's Blue Marble imagery, built by hand.
 * 3. A raster layer on that source, at 0.7 opacity, masked by the water layer and inverted.
 *
 * ### The mask is `layerMask`, and `invert` is what makes it a *land* mask
 *
 * ```kotlin
 * RasterLayerDescriptor(
 *     id = "satellite",
 *     source = "satellite-geocolor",
 *     paint = RasterLayerPaint(opacity = 0.7f, raster = RasterPaint()),
 *     layerMask = LayerMaskSpec(
 *         layers = listOf(LayerMaskLayerConfig(id = waterLayer.id)),
 *         invert = true,
 *     ),
 * )
 * ```
 *
 * is the JS `mask: { layers: [{ id: waterLayer.id }], invert: true }`, field for field. A mask on its
 * own restricts a layer to where the mask layer's geometry is — water. Inverted, the raster draws
 * everywhere *except* there, which leaves it on land and lets the white water layer show through.
 *
 * `layerMask` is resolved into a GLES stencil when the layer is added, so the water layer must be
 * on the map first: it is referenced by the id [MapboxMapController.addWeatherLayer] returned.
 */
class LayerMaskingActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Blue Marble imagery drawn only over land: the raster is masked by the water layer, inverted."

    // The JS example's center: [10, 47.60621], zoom: 3. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(47.60621, 10.0)
    override val cameraZoom = 3.0

    override fun customizeLayers(controller: MapboxMapController) {
        // JS: controller.addWeatherLayer('water', { id: 'water-mask', paint: { fill: { color: '#fff' } } })
        val water = WeatherService.Water(controller.service)
        water.layer.paint.fill?.color = StyleValue.Constant(Color.White)
        val waterLayer = controller.addWeatherLayer(water, id = WATER_LAYER_ID) ?: return

        // JS: controller.addSource('satellite-geocolor', { type: 'raster', url: … })
        val clientId = getString(R.string.xweather_client_id)
        val clientSecret = getString(R.string.xweather_client_secret)
        controller.addSource(
            ImageSourceDescriptor(
                id = SOURCE_ID,
                url = "$AMP_SERVER/${clientId}_$clientSecret/blue-marble/{z}/{x}/{y}/0@2x.png",
                tileSize = TileSize(512),
            ),
        )

        // JS: controller.addLayer('satellite', { type: 'raster', …, mask: { layers, invert: true } })
        controller.addLayer(
            RasterLayerDescriptor(
                id = LAYER_ID,
                source = SOURCE_ID,
                paint = RasterLayerPaint(opacity = 0.7f, raster = RasterPaint()),
                layerMask = LayerMaskSpec(
                    layers = listOf(LayerMaskLayerConfig(id = waterLayer.id)),
                    invert = true,
                ),
            ),
            beforeID = null,
        )
    }

    private companion object {
        const val WATER_LAYER_ID = "water-mask"
        const val SOURCE_ID = "satellite-geocolor"
        const val LAYER_ID = "satellite"
        const val AMP_SERVER = "https://maps.api.xweather.com"
    }
}
