package com.example.mapsgldemo.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.mapbox.geojson.FeatureCollection
import com.xweather.mapsgl.anim.Timeline
import com.xweather.mapsgl.layers.spec.FillLayerDescriptor
import com.xweather.mapsgl.layers.spec.LayerMaskLayerConfig
import com.xweather.mapsgl.layers.spec.LayerMaskSpec
import com.xweather.mapsgl.layers.spec.LineLayerDescriptor
import com.xweather.mapsgl.layers.spec.MaskLayerKind
import com.xweather.mapsgl.layers.spec.StencilLayerMaskSpec
import com.xweather.mapsgl.layers.spec.StencilMaskCombineMode
import com.xweather.mapsgl.layers.spec.StencilMaskLayerRef
import com.xweather.mapsgl.layers.tile.TileLayer
import com.xweather.mapsgl.layers.tile.VectorTileLayer
import com.xweather.mapsgl.map.mapbox.GlStencilOsm
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.GeoJSONSource
import com.xweather.mapsgl.sources.source.spec.GeoJSONSourceDescriptor
import com.xweather.mapsgl.style.LineCap
import com.xweather.mapsgl.style.LineJoin
import com.xweather.mapsgl.style.LineLayerPaint
import com.xweather.mapsgl.style.StrokePaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherLayerConfiguration
import com.xweather.mapsgl.weather.WeatherService

/**
 * Setup/teardown for the demo activity's stencil-mask demos.
 *
 * Each `setupDemoN…` / `teardownDemoN…` mirrors a button in [StencilMaskMenuActivity]. Pulled out so
 * menu activities only wire UI; layer/source IDs and the embedded GeoJSON live here.
 */
class StencilMaskDemos(
    private val context: Context,
    private val controller: MapboxMapController,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingDemoCallbacks = mutableListOf<Runnable>()

    private fun postDemoCallback(delayMs: Long, block: () -> Unit) {
        val session = controller.mapSessionId
        val runnable = Runnable {
            if (session != Timeline.mapSessionId) return@Runnable
            block()
        }
        pendingDemoCallbacks += runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    /** Cancel delayed stencil/preload work when leaving a demo (avoids OOM after activity switch). */
    fun cancelPendingDemoCallbacks() {
        pendingDemoCallbacks.forEach { mainHandler.removeCallbacks(it) }
        pendingDemoCallbacks.clear()
    }

    companion object {
        private const val LOG_TAG = "StencilMaskDemos"

        // Demo 1: Portugal polygon ∩ motorways + temperature
        const val DEMO1_SOURCE_ID = "admin-boundaries"
        const val DEMO1_MASK_LAYER_ID = "admin-mask"
        const val DEMO1_HIGHWAY_LAYER_ID = "highway-mask-line"
        const val DEMO1_TEMP_LAYER_ID = "custom-temperature-highway-portugal"

        // Demo 2: Portugal polygon only + temperature (ids separate from demo 1 so both can toggle independently)
        const val DEMO2_SOURCE_ID = "demo2-admin-boundaries"
        const val DEMO2_MASK_LAYER_ID = "demo2-admin-mask"
        const val DEMO2_TEMP_LAYER_ID = "demo2-temperature-stencil-admin"

        // Demo 3: motorway ribbon mask ∩ CONUS + temperature
        const val DEMO3_HIGHWAY_LAYER_ID = "custom-highway-mask"
        const val DEMO3_CONUS_SOURCE_ID = "demo3-natural-earth-conus"
        const val DEMO3_CONUS_MASK_LAYER_ID = "demo3-natural-earth-conus-mask"
        const val DEMO3_NATURAL_EARTH_CONUS_ASSET = "stored_asset/conus.geojson"
        const val DEMO3_TEMP_LAYER_ID = "custom-temperature-highway-conus"

        // Demo 4: Natural Earth-style admin polygon invert mask + temperature
        const val DEMO4_SOURCE_ID = "demo4-natural-earth-portugal"
        const val DEMO4_MASK_LAYER_ID = "demo4-natural-earth-mask"
        const val DEMO4_TEMP_LAYER_ID = "demo4-temperature-invert"
        const val DEMO4_NATURAL_EARTH_ASSET = "stored_asset/portugal.geojson"

        // Demo 5: JS-style mask API using road-motorway with miter join + square caps
        const val DEMO5_TEMP_LAYER_ID = "demo5-temperature-js-mask-road-miter-round"
        private const val DEMO5_MASK_ID = "road-motorway::mask-miter-round"

        // CONUS-only mask: Natural Earth Continental-US polygon used to clip a temperature layer.
        /** Layer id for [setupLandMaskTemperature] ([MaskLayerKind.LAND] via GLES stencil / OSM water mesh). */
        const val LAND_MASK_TEMP_LAYER_ID = "temperature-land-mask"

        /** Motorway stencil for [setupLandHighwayMaskTemperature] (land ∩ highways). */
        const val LAND_HIGHWAY_HIGHWAY_LAYER_ID = "land-highway-mask-motorway"
        const val LAND_HIGHWAY_TEMP_LAYER_ID = "temperature-land-highway-mask"

        /**
         * JS `addWeatherLayer('temperatures', { mask: { type: 'land' } })` or
         * `{ mask: { type: 'water' } }` — [Demo2LandMaskTemperatureActivity] (toggle).
         */
        const val DEMO2_LAND_WATER_MASK_TEMP_LAYER_ID = "demo2-land-water-temperatures"

        /** Embedded Portugal admin boundary polygon (shared by Portugal stencil menu demos). */
        val PORTUGAL_ADMIN_BOUNDARY_GEOJSON: String =
            """
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {},
      "geometry": {
        "type": "Polygon",
        "coordinates": [[
          [-9.034817674180246, 41.88057058365967],
          [-8.67194576662672, 42.13468943945496],
          [-8.263856980817792, 42.28046865495034],
          [-8.013174607769912, 41.790886135417125],
          [-7.422512986673795, 41.79207469335983],
          [-7.251308966490824, 41.91834605566505],
          [-6.668605515967656, 41.883386949219584],
          [-6.389087693700915, 41.381815497394655],
          [-6.851126674822552, 41.11108266861753],
          [-6.864019944679385, 40.33087189387483],
          [-7.026413133156595, 40.184524237624245],
          [-7.066591559263529, 39.71189158788277],
          [-7.498632371439725, 39.62957103124181],
          [-7.098036668313128, 39.03007274022378],
          [-7.374092169616318, 38.37305858006492],
          [-7.029281175148796, 38.07576406508977],
          [-7.166507941099865, 37.803894354802225],
          [-7.537105475281024, 37.42890432387623],
          [-7.453725551778092, 37.09778758396607],
          [-7.855613165711985, 36.83826854099627],
          [-8.382816127953689, 36.97888011326246],
          [-8.898856980820327, 36.86880931248078],
          [-8.746101446965554, 37.65134552667661],
          [-8.839997524439879, 38.26624339451761],
          [-9.287463751655224, 38.3584858261586],
          [-9.526570603869715, 38.73742910415491],
          [-9.446988898140232, 39.39206614842837],
          [-9.048305223008427, 39.75509308527877],
          [-8.977353481471681, 40.15930613866581],
          [-8.768684047877102, 40.76063894303019],
          [-8.79085323733031, 41.18433401139126],
          [-8.99078935386757, 41.54345937760364],
          [-9.034817674180246, 41.88057058365967]
        ]]
      }
    }
  ]
}
            """.trimIndent()
    }

    // region Demo 1: Portugal polygon ∩ motorways + temperature

    fun setupDemo1PortugalHighwaysTemperature() {
        if (!controller.hasSource(DEMO1_SOURCE_ID)) {
            controller.addSource(GeoJSONSourceDescriptor(id = DEMO1_SOURCE_ID))
        }
        (controller.getSource(DEMO1_SOURCE_ID) as? GeoJSONSource)?.data =
            FeatureCollection.fromJson(PORTUGAL_ADMIN_BOUNDARY_GEOJSON)
        if (!controller.hasLayer(DEMO1_MASK_LAYER_ID)) {
            controller.addLayer(
                FillLayerDescriptor(id = DEMO1_MASK_LAYER_ID, source = DEMO1_SOURCE_ID, stencilOnly = true),
                beforeID = null,
            )
        }
        ensureMotorwayStencilLayer(DEMO1_HIGHWAY_LAYER_ID, thicknessUv = 0.8)
        if (!controller.hasLayer(DEMO1_TEMP_LAYER_ID)) {
            val temperatureConfig = WeatherService.Temperatures(controller.service)
            temperatureConfig.layer.id = DEMO1_TEMP_LAYER_ID
            temperatureConfig.layer.mask = MaskLayerKind.NONE
            temperatureConfig.layer.stencilLayerMask = StencilLayerMaskSpec(
                layers = listOf(
                    StencilMaskLayerRef(DEMO1_MASK_LAYER_ID),
                    StencilMaskLayerRef(DEMO1_HIGHWAY_LAYER_ID),
                ),
                invert = false,
                mode = StencilMaskCombineMode.ALL,
            )
            controller.addWeatherLayer(temperatureConfig)
            controller.reregisterCustomStencilMask(DEMO1_TEMP_LAYER_ID)
        }
    }

    fun teardownDemo1PortugalHighwaysTemperature() {
        removeLayer(DEMO1_TEMP_LAYER_ID)
        removeLayer(DEMO1_HIGHWAY_LAYER_ID)
        removeLayer(DEMO1_MASK_LAYER_ID)
        removeSource(DEMO1_SOURCE_ID)
    }

    // endregion

    // region Demo 2: Portugal admin polygon (invert) + temperature

    /** Portugal polygon stencil only + temperature (formerly test button / applyMultiLayerStencilMaskDemo). */
    fun setupDemo2PortugalAdminStencilTemperature() {
        if (!controller.hasSource(DEMO2_SOURCE_ID)) {
            controller.addSource(GeoJSONSourceDescriptor(id = DEMO2_SOURCE_ID))
        }
        (controller.getSource(DEMO2_SOURCE_ID) as? GeoJSONSource)?.data =
            FeatureCollection.fromJson(PORTUGAL_ADMIN_BOUNDARY_GEOJSON)
        if (!controller.hasLayer(DEMO2_MASK_LAYER_ID)) {
            controller.addLayer(
                FillLayerDescriptor(id = DEMO2_MASK_LAYER_ID, source = DEMO2_SOURCE_ID, stencilOnly = true),
                beforeID = null,
            )
        }
        if (!controller.hasLayer(DEMO2_TEMP_LAYER_ID)) {
            val temperatureConfig = WeatherService.Temperatures(controller.service)
            temperatureConfig.layer.id = DEMO2_TEMP_LAYER_ID
            temperatureConfig.layer.stencilLayerMask = StencilLayerMaskSpec(
                layers = listOf(StencilMaskLayerRef(DEMO2_MASK_LAYER_ID)),
                invert = true,
                mode = StencilMaskCombineMode.ALL,
            )
            controller.addWeatherLayer(temperatureConfig)
            controller.reregisterCustomStencilMask(DEMO2_TEMP_LAYER_ID)
        }
    }

    fun teardownDemo2PortugalAdminStencilTemperature() {
        removeLayer(DEMO2_TEMP_LAYER_ID)
        removeLayer(DEMO2_MASK_LAYER_ID)
        removeSource(DEMO2_SOURCE_ID)
    }

    // endregion

    // region Highways ∩ CONUS mask + temperature

    /**
     * Tunable knobs for [HighwaysConusMaskTemperatureActivity]. Defaults reproduce the original
     * button behaviour from [MapLayersActivity]'s `testButton2`: a thin motorway ribbon unioned
     * with the CONUS polygon.
     */
    data class HighwaysConusMaskOptions(
        val invert: Boolean = false,
        val mode: StencilMaskCombineMode = StencilMaskCombineMode.ALL,
        /** Thickness of the motorway ribbon in tile-UV units (0–[4.0] per tile axis; Demo 5 default 1.5). */
        val thicknessUv: Double = 1.5,
        val lineCap: LineCap = LineCap.BUTT,
        val lineJoin: LineJoin = LineJoin.MITER,
        val highwayMaskEnabled: Boolean = true,
        val conusMaskEnabled: Boolean = true,
    )

    private fun buildHighwaysConusMaskRefs(opts: HighwaysConusMaskOptions): List<StencilMaskLayerRef> {
        val refs = ArrayList<StencilMaskLayerRef>(2)
        if (opts.highwayMaskEnabled) {
            refs.add(StencilMaskLayerRef(DEMO3_HIGHWAY_LAYER_ID))
        }
        if (opts.conusMaskEnabled) {
            refs.add(StencilMaskLayerRef(DEMO3_CONUS_MASK_LAYER_ID))
        }
        return refs
    }

    fun setupHighwaysConusMaskTemperature(opts: HighwaysConusMaskOptions = HighwaysConusMaskOptions()) {
        val conusGeoJson = readAssetGeoJsonOrEmpty(DEMO3_NATURAL_EARTH_CONUS_ASSET)
        if (!controller.hasSource(DEMO3_CONUS_SOURCE_ID)) {
            controller.addSource(GeoJSONSourceDescriptor(id = DEMO3_CONUS_SOURCE_ID))
        }
        (controller.getSource(DEMO3_CONUS_SOURCE_ID) as? GeoJSONSource)?.data =
            FeatureCollection.fromJson(conusGeoJson)
        if (!controller.hasLayer(DEMO3_CONUS_MASK_LAYER_ID)) {
            controller.addLayer(
                FillLayerDescriptor(
                    id = DEMO3_CONUS_MASK_LAYER_ID,
                    source = DEMO3_CONUS_SOURCE_ID,
                    stencilOnly = true,
                ),
                beforeID = null,
            )
        }
        ensureMotorwayStencilLayer(
            DEMO3_HIGHWAY_LAYER_ID,
            thicknessUv = opts.thicknessUv,
            lineCap = opts.lineCap,
            lineJoin = opts.lineJoin,
        )
        val maskRefs = buildHighwaysConusMaskRefs(opts)
        if (!controller.hasLayer(DEMO3_TEMP_LAYER_ID)) {
            val temperatureConfig = WeatherService.Temperatures(controller.service)
            temperatureConfig.layer.id = DEMO3_TEMP_LAYER_ID
            temperatureConfig.layer.mask = MaskLayerKind.NONE
            temperatureConfig.layer.stencilLayerMask = if (maskRefs.isEmpty()) {
                null
            } else {
                StencilLayerMaskSpec(
                    layers = maskRefs,
                    invert = opts.invert,
                    mode = opts.mode,
                )
            }
            controller.addWeatherLayer(temperatureConfig)
            if (maskRefs.isNotEmpty()) {
                controller.reregisterCustomStencilMask(DEMO3_TEMP_LAYER_ID)
            }
        }
    }

    /**
     * Re-applies the highways ∩ CONUS demo with [opts] when the **stencil geometry** must change (thickness, cap, or
     * join). Mutates the existing motorway ribbon's paint in place, then asks the SDK to refresh
     * the temperature layer's custom-stencil registration so its cached MVT listener picks up the
     * new line paint — see [MapboxMapController.refreshCustomStencilMask].
     *
     * The temperature weather layer is **not** torn down: it owns the expensive bits (timeline
     * phases, raster tile fetches, GPU texture uploads) and none depend on the ribbon's paint.
     *
     * We also avoid removing + re-adding the highway [VectorTileLayer]: that would fire
     * [com.xweather.mapsgl.sources.VectorTileSource.invalidate] twice (consumer-count goes 1→0→1),
     * and each invalidate expires every cached MVT tile and forces Mapbox to re-decode the bytes
     * from scratch. With a populated tile cache that was the OOM path even after the
     * coordinator re-registration was optimised. In-place mutation has no side effects on the
     * vector source.
     *
     * The CONUS polygon source + fill stencil are static across calls.
     *
     * For [HighwaysConusMaskOptions.invert] / [HighwaysConusMaskOptions.mode], use [updateHighwaysConusMaskInvertAndMode]
     * instead — those flags are read by the renderer every frame so no re-registration is needed.
     */
    fun applyHighwaysConusMaskOptions(opts: HighwaysConusMaskOptions) {
        val highwayLayer = controller.getLayer(DEMO3_HIGHWAY_LAYER_ID) as? VectorTileLayer
        val tempLayer = controller.getLayer(DEMO3_TEMP_LAYER_ID) as? TileLayer
        val highwayPaint = highwayLayer?.paint as? LineLayerPaint
        if (highwayLayer == null || tempLayer == null || highwayPaint == null) {
            // First-time setup, or one of the layers got torn down (e.g. before `mapLoaded`
            // fired). Fall back to a fresh build with the new opts baked in.
            removeLayer(DEMO3_TEMP_LAYER_ID)
            removeLayer(DEMO3_HIGHWAY_LAYER_ID)
            setupHighwaysConusMaskTemperature(opts)
            return
        }
        // The highway VectorTileLayer is stencilOnly, so the paint isn't attached to a Mapbox
        // style layer — mutating in place doesn't require any Mapbox-side resync.
        highwayPaint.stroke.thickness = StyleValue.Constant(opts.thicknessUv)
        highwayPaint.stroke.lineCap = StyleValue.Constant(opts.lineCap)
        highwayPaint.stroke.lineJoin = StyleValue.Constant(opts.lineJoin)
        highwayPaint.stroke.miterLimit = StyleValue.Constant(2.0)
        applyHighwaysConusMaskLayerSet(opts)
    }

    /**
     * Enables, disables, or changes which mask layers clip the temperature field. An empty
     * selection clears [TileLayer.stencilLayerMask] so weather is unmasked; checking a box again
     * restores masking via [MapboxMapController.reregisterCustomStencilMask].
     */
    fun applyHighwaysConusMaskLayerSet(opts: HighwaysConusMaskOptions): Boolean {
        val tempLayer = controller.getLayer(DEMO3_TEMP_LAYER_ID) as? TileLayer ?: return false
        val maskRefs = buildHighwaysConusMaskRefs(opts)
        val hadSpec = tempLayer.stencilLayerMask != null

        if (maskRefs.isEmpty()) {
            if (!hadSpec) return true
            controller.clearCustomStencilMask(DEMO3_TEMP_LAYER_ID)
            return true
        }

        val newSpec = StencilLayerMaskSpec(
            layers = maskRefs,
            invert = opts.invert,
            mode = opts.mode,
        )
        val layersChanged = tempLayer.stencilLayerMask?.layers != maskRefs
        val modeChanged = tempLayer.stencilLayerMask?.mode != opts.mode
        tempLayer.stencilLayerMask = newSpec
        if (!hadSpec || layersChanged || modeChanged) {
            controller.reregisterCustomStencilMask(DEMO3_TEMP_LAYER_ID)
        } else {
            controller.refreshCustomStencilMask(DEMO3_TEMP_LAYER_ID)
        }
        return true
    }

    /** @see [applyHighwaysConusMaskLayerSet] */
    fun updateHighwaysConusActiveMaskLayers(opts: HighwaysConusMaskOptions): Boolean =
        applyHighwaysConusMaskLayerSet(opts)

    /**
     * Fast path for the [HighwaysConusMaskOptions.invert] / [HighwaysConusMaskOptions.mode] toggles. Mutates the
     * temperature layer's [TileLayer.stencilLayerMask] in place and asks Mapbox for a repaint —
     * the next frame's `TileLayerRenderer.draw()` picks up the new flags without us having to
     * rebuild any stencil meshes or re-upload any weather tiles.
     *
     * Returns `false` (no-op) if the temperature layer isn't on the map yet (e.g. called before
     * `mapLoaded`), in which case the caller should fall back to [applyHighwaysConusMaskOptions] so the
     * layer is added with the desired flags from the start.
     */
    fun updateHighwaysConusMaskInvertAndMode(
        invert: Boolean,
        mode: StencilMaskCombineMode,
    ): Boolean {
        val tempLayer = controller.getLayer(DEMO3_TEMP_LAYER_ID) as? TileLayer ?: return false
        val current = tempLayer.stencilLayerMask ?: return false
        if (current.invert == invert && current.mode == mode) return true
        val modeChanged = current.mode != mode
        tempLayer.stencilLayerMask = current.copy(invert = invert, mode = mode)
        if (modeChanged) {
            // ALL clips line meshes to fill polygons; ANY keeps roads outside the area mask.
            controller.refreshCustomStencilMask(DEMO3_TEMP_LAYER_ID)
        } else {
            controller.mapboxMap?.triggerRepaint()
        }
        return true
    }

    // endregion

    // region Demo 4: Natural Earth admin polygon (asset-backed) + temperature

    fun setupDemo4NaturalEarthInvertTemperature() {
        val naturalEarthGeoJson = runCatching {
            context.assets.open(DEMO4_NATURAL_EARTH_ASSET).bufferedReader().use { it.readText() }
        }.getOrElse { error ->
            Log.w(
                LOG_TAG,
                "Failed to load Natural Earth asset: $DEMO4_NATURAL_EARTH_ASSET; falling back to embedded demo polygon.",
                error,
            )
            PORTUGAL_ADMIN_BOUNDARY_GEOJSON
        }
        if (!controller.hasSource(DEMO4_SOURCE_ID)) {
            controller.addSource(GeoJSONSourceDescriptor(id = DEMO4_SOURCE_ID))
        }
        (controller.getSource(DEMO4_SOURCE_ID) as? GeoJSONSource)?.data =
            FeatureCollection.fromJson(naturalEarthGeoJson)
        if (!controller.hasLayer(DEMO4_MASK_LAYER_ID)) {
            controller.addLayer(
                FillLayerDescriptor(id = DEMO4_MASK_LAYER_ID, source = DEMO4_SOURCE_ID, stencilOnly = true),
                beforeID = null,
            )
        }
        if (!controller.hasLayer(DEMO4_TEMP_LAYER_ID)) {
            val temperatureConfig = WeatherService.Temperatures(controller.service)
            temperatureConfig.layer.id = DEMO4_TEMP_LAYER_ID
            temperatureConfig.layer.mask = MaskLayerKind.NONE
            temperatureConfig.layer.stencilLayerMask = StencilLayerMaskSpec(
                layers = listOf(StencilMaskLayerRef(DEMO4_MASK_LAYER_ID)),
                invert = false,
                mode = StencilMaskCombineMode.ALL,
            )
            controller.addWeatherLayer(temperatureConfig)
            controller.reregisterCustomStencilMask(DEMO4_TEMP_LAYER_ID)
        }
    }

    // endregion

    // region Demo 5: JS-style mask API on road-motorway with miter join + square caps

    fun setupDemo5JsStyleRoadMaskMiterRoundTemperature() {
        if (controller.hasLayer(DEMO5_TEMP_LAYER_ID)) return
        val temperatureConfig = WeatherService.Temperatures(controller.service)
        temperatureConfig.layer.id = DEMO5_TEMP_LAYER_ID
        temperatureConfig.layer.mask = MaskLayerKind.NONE
        temperatureConfig.layer.layerMask = LayerMaskSpec(
            layers = listOf(
                LayerMaskLayerConfig(
                    id = LayerCode.ROAD_MOTORWAY.value,
                    maskId = DEMO5_MASK_ID,
                    overrides = { cfg ->
                        val wx = cfg as? WeatherLayerConfiguration<*, *>
                        val lineLayer = wx?.layer as? LineLayerDescriptor
                        if (lineLayer != null) {
                            lineLayer.paint.stroke.thickness = StyleValue.Constant(1.5)
                            lineLayer.paint.stroke.lineJoin = StyleValue.Constant(LineJoin.MITER)
                            lineLayer.paint.stroke.lineCap = StyleValue.Constant(LineCap.SQUARE)
                            lineLayer.paint.stroke.miterLimit = StyleValue.Constant(2.0)
                        }
                    },
                ),
            ),
            mode = StencilMaskCombineMode.ALL,
            invert = false,
        )
        controller.addWeatherLayer(temperatureConfig)
    }

    // endregion

    // region Demo 2: temperatures + built-in land/water mask (JS mask.type land | water)

    /**
     * JS equivalent: `addWeatherLayer('temperatures', { mask: { type: 'land' } })` or
     * `{ mask: { type: 'water' } }`. [waterMask] false → land (MAPSGL OSM); true → water (Mapbox Streets).
     */
    fun setupDemo2LandWaterMaskTemperature(waterMask: Boolean = false) {
        if (controller.hasLayer(DEMO2_LAND_WATER_MASK_TEMP_LAYER_ID)) {
            refreshDemo2LandWaterMaskTemperature(waterMask)
            return
        }
        GlStencilOsm.source = if (waterMask) GlStencilOsm.Source.MAPBOX else GlStencilOsm.Source.MAPSGL
        val temperatureConfig = WeatherService.Temperatures(controller.service)
        temperatureConfig.layer.id = DEMO2_LAND_WATER_MASK_TEMP_LAYER_ID
        temperatureConfig.layer.mask = if (waterMask) MaskLayerKind.WATER else MaskLayerKind.LAND
        controller.addWeatherLayer(temperatureConfig)
        controller.schedulePausedDemoLandWaterMaskRefresh(DEMO2_LAND_WATER_MASK_TEMP_LAYER_ID)
    }

    /** Re-bind land/water stencil + encoded tiles after menu navigation. */
    fun refreshDemo2LandWaterMaskTemperature(waterMask: Boolean) {
        if (!controller.hasLayer(DEMO2_LAND_WATER_MASK_TEMP_LAYER_ID)) return
        GlStencilOsm.source = if (waterMask) GlStencilOsm.Source.MAPBOX else GlStencilOsm.Source.MAPSGL
        controller.schedulePausedDemoLandWaterMaskRefresh(DEMO2_LAND_WATER_MASK_TEMP_LAYER_ID)
    }

    /** Toggle land ↔ water (complementary built-in masks). */
    fun setDemo2LandWaterMaskInverted(waterMask: Boolean) {
        cancelPendingDemoCallbacks()
        if (controller.hasLayer(DEMO2_LAND_WATER_MASK_TEMP_LAYER_ID)) {
            controller.removeLayer(DEMO2_LAND_WATER_MASK_TEMP_LAYER_ID)
        }
        setupDemo2LandWaterMaskTemperature(waterMask)
    }

    fun teardownDemo2LandWaterMaskTemperature() {
        cancelPendingDemoCallbacks()
        removeLayer(DEMO2_LAND_WATER_MASK_TEMP_LAYER_ID)
    }

    // endregion

    // region Built-in land mask + temperature

    /**
     * Surface temperatures clipped to land using the SDK's built-in [MaskLayerKind.LAND] stencil
     * (marine areas excluded).
     */
    fun setupLandMaskTemperature() {
        if (controller.hasLayer(LAND_MASK_TEMP_LAYER_ID)) return
        val temperatureConfig = WeatherService.Temperatures(controller.service)
        temperatureConfig.layer.id = LAND_MASK_TEMP_LAYER_ID
        temperatureConfig.layer.mask = MaskLayerKind.LAND
        controller.addWeatherLayer(temperatureConfig)
    }

    fun teardownLandMaskTemperature() {
        removeLayer(LAND_MASK_TEMP_LAYER_ID)
    }

    /**
     * Surface temperatures on **land** only where OSM motorways meet the built-in marine/land stencil
     * (highways on land, not oceans).
     */
    fun setupLandHighwayMaskTemperature() {
        if (controller.hasLayer(LAND_HIGHWAY_TEMP_LAYER_ID)) return
        ensureMotorwayStencilLayer(LAND_HIGHWAY_HIGHWAY_LAYER_ID, thicknessUv = 0.2)
        val temperatureConfig = WeatherService.Temperatures(controller.service)
        temperatureConfig.layer.id = LAND_HIGHWAY_TEMP_LAYER_ID
        temperatureConfig.layer.mask = MaskLayerKind.LAND
        temperatureConfig.layer.stencilLayerMask = StencilLayerMaskSpec(
            layers = listOf(StencilMaskLayerRef(LAND_HIGHWAY_HIGHWAY_LAYER_ID)),
            invert = false,
            mode = StencilMaskCombineMode.ALL,
        )
        controller.addWeatherLayer(temperatureConfig)
        controller.configureHybridLandHighwayStencil(LAND_HIGHWAY_TEMP_LAYER_ID)
    }

    fun teardownLandHighwayMaskTemperature() {
        removeLayer(LAND_HIGHWAY_TEMP_LAYER_ID)
        removeLayer(LAND_HIGHWAY_HIGHWAY_LAYER_ID)
    }

    // endregion

    // region shared helpers

    /**
     * Add a stencil-only line layer driven by [WeatherService.RoadMotorway]'s MVT source/filter so the
     * temperature stencil follows OSM motorways. [thicknessUv] is in tile-UV units (0–1 per tile axis).
     * [lineCap] and [lineJoin] control end-cap and segment-join geometry; they're baked into the
     * descriptor at add-time, so callers reconfiguring these values must remove the layer first
     * (see [applyHighwaysConusMaskOptions]).
     */
    private fun ensureMotorwayStencilLayer(
        layerId: String,
        thicknessUv: Double,
        lineCap: LineCap = LineCap.BUTT,
        lineJoin: LineJoin = LineJoin.MITER,
    ) {
        if (controller.hasLayer(layerId)) return
        val roadCfg = WeatherService.RoadMotorway(controller.service)
        if (!controller.hasSource(roadCfg.source.id)) {
            controller.addSource(roadCfg.source)
        }
        val paint = (roadCfg.layer.paint as? LineLayerPaint)?.copy()
            ?: LineLayerPaint(stroke = StrokePaint())
        paint.stroke.thickness = StyleValue.Constant(thicknessUv)
        paint.stroke.lineCap = StyleValue.Constant(lineCap)
        paint.stroke.lineJoin = StyleValue.Constant(lineJoin)
        // Keep miter spikes bounded when the user picks `LineJoin.MITER`; default matches Demo 5.
        paint.stroke.miterLimit = StyleValue.Constant(2.0)
        controller.addLayer(
            LineLayerDescriptor(
                id = layerId,
                source = roadCfg.source.id,
                sourceLayer = roadCfg.layer.sourceLayer,
                paint = paint,
                filter = roadCfg.layer.filter,
                stencilOnly = true,
            ),
            beforeID = null,
        )
    }

    private fun readAssetGeoJsonOrEmpty(assetPath: String): String =
        runCatching {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        }.getOrElse { error ->
            Log.w(LOG_TAG, "Failed to load asset: $assetPath; using empty FeatureCollection.", error)
            "{\"type\":\"FeatureCollection\",\"features\":[]}"
        }

    private fun removeLayer(layerId: String) {
        if (controller.hasLayer(layerId)) controller.removeLayer(layerId)
    }

    private fun removeSource(sourceId: String) {
        if (controller.hasSource(sourceId)) controller.removeSource(sourceId)
    }

    // endregion
}
