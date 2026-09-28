package com.example.mapsgldemo.moreExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.MoreExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.layers.spec.MaskLayerKind
import com.xweather.mapsgl.layers.spec.RasterLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.ImageSourceDescriptor
import com.xweather.mapsgl.style.RasterLayerPaint
import com.xweather.mapsgl.style.RasterPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.types.TileSize

/**
 * **Layer masking** - clip a layer so it draws over land only, or over water only.
 *
 * Raster and weather layer descriptors carry a `mask`. [MaskLayerKind.LAND] and
 * [MaskLayerKind.WATER] reference the SDK's built-in water mask - land is that same mask inverted -
 * so confining a layer to one or the other is a single assignment before the layer is added, with
 * no second layer to style as a stencil.
 *
 * The subject is a Blue Marble raster served as a plain `{z}/{x}/{y}` image source, which makes the
 * clip easy to see against the basemap's own coastlines.
 */
class LayerMaskingActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = MoreExamplesMenuActivity::class.java

    override val caption = "Blue Marble imagery clipped by the SDK's built-in land and water mask."

    override val cameraCenter = Coordinate(47.60621, 10.0)
    override val cameraZoom = 3.0

    private var mask = MASKS[0]

    override fun customizeLayers(controller: MapboxMapController) {
        // The layer is re-added when the mask changes; the source stays put.
        if (!controller.hasSource(SOURCE_ID)) {
            val clientId = getString(R.string.xweather_client_id)
            val clientSecret = getString(R.string.xweather_client_secret)
            controller.addSource(
                ImageSourceDescriptor(
                    id = SOURCE_ID,
                    url = "https://maps.api.xweather.com/${clientId}_$clientSecret/blue-marble/{z}/{x}/{y}/0@2x.png",
                    tileSize = TileSize(512),
                ),
            )
        }

        val layer = RasterLayerDescriptor(
            id = LAYER_ID,
            source = SOURCE_ID,
            paint = RasterLayerPaint(opacity = 0.7f, raster = RasterPaint()),
        )
        layer.mask = mask.kind
        controller.addLayer(layer, beforeID = null)
    }

    override fun buildControls() {
        addChoice("Mask", MASKS.map { it.label }) { index ->
            mask = MASKS[index]
            // The mask is read when the layer is built, so re-add it.
            controller.removeLayer(LAYER_ID)
            customizeLayers(controller)
            controller.mapboxMap?.triggerRepaint()
        }
    }

    private class Mask(val label: String, val kind: MaskLayerKind)

    private companion object {
        const val SOURCE_ID = "blue-marble"
        const val LAYER_ID = "blue-marble-raster"

        val MASKS = listOf(
            Mask("Land", MaskLayerKind.LAND),
            Mask("Water", MaskLayerKind.WATER),
            Mask("Both", MaskLayerKind.NONE),
        )
    }
}
