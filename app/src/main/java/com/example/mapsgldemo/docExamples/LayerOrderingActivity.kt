package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.maps.Style
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.map.slots.BuiltinSlot
import com.xweather.mapsgl.map.slots.LayerSlotOptions
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Weather layers that stack themselves, and a band pinned below the basemap.**
 *
 * Android port of the JS example
 * [Controlling layer ordering](https://www.xweather.com/docs/mapsgl/examples/layer-ordering).
 * Four layers are added with no `beforeId` and in no particular order; layer ordering slots stack
 * them by what they are. Two switches then move things around at runtime, without re-adding
 * anything.
 *
 * ### Slots are opt-in
 *
 * A controller only stacks by slot when it is built with layer slot options, the counterpart of the
 * JS controller's `slots` option:
 *
 * ```kotlin
 * val controller = MapboxMapController(mapView, account, LayerSlotOptions())
 * ```
 *
 * This screen does it through [layerSlots]. The whole feature is also behind the
 * `MapsGLFeatures.layerSlots` switch.
 *
 * ### Where each layer lands
 *
 * Temperatures and radar are sample and raster layers, so they go in the `underlay` band, with radar
 * ranked above temperatures. Wind particles go in `inlay`, and the alert outlines, a line layer, sit
 * above the particles in the same band. On a classic style like this one, the `underlay` band
 * defaults to sitting below the basemap's admin boundaries, so boundaries stay visible over the
 * colour fills.
 *
 * ### Pinning a band, moving a layer
 *
 * The first switch pins the whole `underlay` band below the basemap's water labels, and back:
 *
 * ```kotlin
 * controller.setSlotBeforeId(BuiltinSlot.UNDERLAY, "waterway-label")
 * controller.setSlotBeforeId(BuiltinSlot.UNDERLAY, null)
 * ```
 *
 * The second moves radar out of `underlay` into `inlay`, keeping its rank:
 *
 * ```kotlin
 * controller.moveLayerToSlot(radarId, BuiltinSlot.INLAY)
 * ```
 *
 * ### Classic style
 *
 * `waterway-label` is a layer of the classic Mapbox styles, so this screen loads `light-v11`, as the
 * JS example does. On Mapbox Standard, whose basemap layers cannot be named, slots use Standard's own
 * bands instead: `underlay` in `middle`, below the labels, and the rest in `top`.
 */
class LayerOrderingActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Temperatures, radar, wind particles and alert outlines, stacked by layer ordering slots " +
            "rather than by the order they were added."

    // The JS example's center: [-85.5, 40], zoom: 3. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(40.0, -85.5)
    override val cameraZoom = 3.0

    override val layerSlots = LayerSlotOptions()
    override val mapStyleUri: String = Style.LIGHT

    private var radarId: String? = null

    override fun customizeLayers(controller: MapboxMapController) {
        // JS: addWeatherLayer for each, no beforeId. The slots decide the stack.
        controller.addWeatherLayer(LayerCode.TEMPERATURES)
        radarId = controller.addWeatherLayer(LayerCode.RADAR)?.id
        controller.addWeatherLayer(LayerCode.WIND_PARTICLES)
        controller.addWeatherLayer(LayerCode.ALERTS_OUTLINE)
    }

    override fun buildControls() {
        // JS: the "pinUnderlay" checkbox.
        addToggle("Pin underlay below water labels", false) { pinned ->
            controller.setSlotBeforeId(BuiltinSlot.UNDERLAY, if (pinned) WATER_LABELS else null)
        }
        // JS: the "radarInlay" checkbox.
        addToggle("Radar in the inlay band", false) { inlay ->
            radarId?.let { controller.moveLayerToSlot(it, if (inlay) BuiltinSlot.INLAY else BuiltinSlot.UNDERLAY) }
        }
    }

    private companion object {
        /** The classic-style water label layer the JS example pins the underlay below. */
        const val WATER_LABELS = "waterway-label"
    }
}
