package com.example.mapsgldemo.stencil

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.StencilMaskMenuActivity
import com.example.mapsgldemo.databinding.ActivityDemo4PortugalMaskTemperatureBinding
import com.example.mapsgldemo.helpers.StencilMaskDemos
import com.mapbox.common.Cancelable
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.layers.spec.FillLayerDescriptor
import com.xweather.mapsgl.layers.spec.MaskLayerKind
import com.xweather.mapsgl.layers.spec.StencilLayerMaskSpec
import com.xweather.mapsgl.layers.spec.StencilMaskCombineMode
import com.xweather.mapsgl.layers.spec.StencilMaskLayerRef
import com.xweather.mapsgl.layers.tile.TileLayer
import com.xweather.mapsgl.map.mapbox.GlStencilOsm
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.GeoJSONSource
import com.xweather.mapsgl.sources.source.spec.GeoJSONSourceDescriptor
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.WeatherService

/**
 * Demo 3: surface temperatures over Portugal, clipped to the admin polygon via GLES stencil
 * (`mask: { layerIds: ['admin-mask'] }` or `{ …, invert: true }` in the JS SDK).
 *
 * **Invert mask** toggles inside ↔ outside the polygon (former Demo 5). Layer setup is inlined in
 * [onCreate]. Launched from [StencilMaskMenuActivity].
 */
class Demo4PortugalMaskTemperatureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDemo4PortugalMaskTemperatureBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private var mapLoadedCancelable: Cancelable? = null

    /** false → inside polygon; true → outside (former Demo 5). */
    private var portugalMaskInverted = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemo4PortugalMaskTemperatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.demo4PortugalMaskTemperatureMapView

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.demo4PortugalMaskTemperatureInvertMaskButton.setOnClickListener {
            portugalMaskInverted = !portugalMaskInverted
            binding.demo4PortugalMaskTemperatureInvertMaskButton.text =
                if (portugalMaskInverted) "Uninvert mask" else "Invert mask"
            if (::controller.isInitialized) {
                setPortugalAdminMaskTemperatureInverted(portugalMaskInverted)
            }
        }
        binding.demo4PortugalMaskTemperatureInvertMaskButton.isEnabled = false

        binding.demo4PortugalMaskTemperatureBackButton.setOnClickListener { returnToStencilMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToStencilMenu()
            }
        })

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (mapView.parent == null) return

                GlStencilOsm.source = GlStencilOsm.Source.MAPSGL
                GlStencilOsm.mapboxAccessToken = getString(R.string.mapbox_access_token)

                controller = MapboxMapController(mapView, xweatherAccount)
                mapboxMap = controller.mapboxMap

                controller.setCenter(Coordinate(39.2, -8.2))
                controller.setZoom(5.5)

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    mapboxMap?.style?.setProjection(projection(ProjectionName.MERCATOR))
                    applyPortugalAdminMaskTemperature(invert = portugalMaskInverted)
                    binding.demo4PortugalMaskTemperatureInvertMaskButton.isEnabled = true
                }
            }
        })
    }

    private fun applyPortugalAdminMaskTemperature(invert: Boolean) {
        if (!controller.hasSource(ADMIN_SOURCE_ID)) {
            controller.addSource(GeoJSONSourceDescriptor(id = ADMIN_SOURCE_ID))
        }
        (controller.getSource(ADMIN_SOURCE_ID) as? GeoJSONSource)?.data =
            FeatureCollection.fromJson(StencilMaskDemos.PORTUGAL_ADMIN_BOUNDARY_GEOJSON)
        if (!controller.hasLayer(ADMIN_MASK_LAYER_ID)) {
            controller.addLayer(
                FillLayerDescriptor(
                    id = ADMIN_MASK_LAYER_ID,
                    source = ADMIN_SOURCE_ID,
                    stencilOnly = true,
                ),
                beforeID = null,
            )
        }
        if (!controller.hasLayer(TEMP_LAYER_ID)) {
            val temperatureConfig = WeatherService.Temperatures(controller.service)
            temperatureConfig.layer.id = TEMP_LAYER_ID
            temperatureConfig.layer.mask = MaskLayerKind.NONE
            temperatureConfig.layer.stencilLayerMask = StencilLayerMaskSpec(
                layers = listOf(StencilMaskLayerRef(ADMIN_MASK_LAYER_ID)),
                invert = invert,
                mode = StencilMaskCombineMode.ALL,
            )
            controller.addWeatherLayer(temperatureConfig)
            controller.reregisterCustomStencilMask(TEMP_LAYER_ID)
        } else {
            setPortugalAdminMaskTemperatureInverted(invert)
        }
    }

    /** Mutates [TileLayer.stencilLayerMask.invert] in place and repaints (fast path). */
    private fun setPortugalAdminMaskTemperatureInverted(invert: Boolean) {
        val tempLayer = controller.getLayer(TEMP_LAYER_ID) as? TileLayer ?: return
        val current = tempLayer.stencilLayerMask ?: return
        if (current.invert == invert) return
        tempLayer.stencilLayerMask = current.copy(invert = invert)
        controller.mapboxMap?.triggerRepaint()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        if (::controller.isInitialized) {
            teardownPortugalAdminMaskTemperature()
        }
        mapLoadedCancelable?.cancel()
        mapLoadedCancelable = null
        mapView.onDestroy()
        super.onDestroy()
    }

    private fun teardownPortugalAdminMaskTemperature() {
        if (controller.hasLayer(TEMP_LAYER_ID)) controller.removeLayer(TEMP_LAYER_ID)
        if (controller.hasLayer(ADMIN_MASK_LAYER_ID)) controller.removeLayer(ADMIN_MASK_LAYER_ID)
        if (controller.hasSource(ADMIN_SOURCE_ID)) controller.removeSource(ADMIN_SOURCE_ID)
    }

    private fun returnToStencilMenu() {
        startActivity(
            Intent(this, StencilMaskMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        private const val ADMIN_SOURCE_ID = "stencil-demo4-portugal-admin-boundaries"
        private const val ADMIN_MASK_LAYER_ID = "stencil-demo4-portugal-admin-mask"
        private const val TEMP_LAYER_ID = "stencil-demo4-portugal-temperature"
    }
}
