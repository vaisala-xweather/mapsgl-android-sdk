package com.example.mapsgldemo.stencil

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.StencilMaskMenuActivity
import com.example.mapsgldemo.databinding.ActivityPortugalSatelliteMaskBinding
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
import com.xweather.mapsgl.map.mapbox.GlStencilOsm
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.GeoJSONSource
import com.xweather.mapsgl.sources.source.spec.GeoJSONSourceDescriptor
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.WeatherService

/**
 * Demo 1: satellite geocolor over Portugal, clipped to the admin polygon via GLES stencil
 * (`mask: { layerIds: ['admin-mask'] }` in the JS SDK).
 *
 * Layer setup is inlined in [onCreate] so readers can follow the full stencil + satellite flow.
 * Launched from [StencilMaskMenuActivity].
 */
class Demo1PortugalSatelliteMaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPortugalSatelliteMaskBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private var mapLoadedCancelable: Cancelable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPortugalSatelliteMaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.portugalSatelliteMapView

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.portugalSatelliteBackButton.setOnClickListener { returnToStencilMenu() }
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

                controller = MapboxMapController(mapView, xweatherAccount)
                mapboxMap = controller.mapboxMap

                controller.setCenter(Coordinate(39.2, -8.2))
                controller.setZoom(5.5)

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    mapboxMap?.style?.setProjection(projection(ProjectionName.MERCATOR))
                    // GeoJSON admin-boundaries + stencil fill admin-mask + satellite-geocolor masked inside.
                    if (!controller.hasLayer(SATELLITE_LAYER_ID)) {
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
                        val satelliteConfig = WeatherService.SatelliteGeocolor(controller.service)
                        satelliteConfig.layer.id = SATELLITE_LAYER_ID
                        satelliteConfig.layer.mask = MaskLayerKind.NONE
                        satelliteConfig.layer.maskLayerIds = listOf(ADMIN_MASK_LAYER_ID)
                        satelliteConfig.layer.paint.opacity = 0.7f
                        controller.addWeatherLayer(satelliteConfig)
                    }
                }
            }
        })
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
            if (controller.hasLayer(SATELLITE_LAYER_ID)) controller.removeLayer(SATELLITE_LAYER_ID)
            if (controller.hasLayer(ADMIN_MASK_LAYER_ID)) controller.removeLayer(ADMIN_MASK_LAYER_ID)
            if (controller.hasSource(ADMIN_SOURCE_ID)) controller.removeSource(ADMIN_SOURCE_ID)
        }
        mapLoadedCancelable?.cancel()
        mapLoadedCancelable = null
        mapView.onDestroy()
        super.onDestroy()
    }

    private fun returnToStencilMenu() {
        startActivity(
            Intent(this, StencilMaskMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        private const val ADMIN_SOURCE_ID = "admin-boundaries"
        private const val ADMIN_MASK_LAYER_ID = "admin-mask"
        private const val SATELLITE_LAYER_ID = "satellite"
    }
}
