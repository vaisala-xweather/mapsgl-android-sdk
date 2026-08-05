package com.example.mapsgldemo.stencil

import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.helpers.StencilMaskDemos
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.map.mapbox.MapboxMapController

/**
 * Creates a [MapboxMapController] in [onStart] and fully [MapboxMapController.shutdown]s it in
 * [onStop] so stencil demos (Demo 2 land / Demo 3 water) never reuse Mapbox style or MapsGL state
 * after navigating to the menu or another demo.
 */
class StencilMaskMapLifecycle(
    private val activity: AppCompatActivity,
    private val mapView: MapView,
    private val account: XweatherAccount,
    private val configureGlStencilSource: () -> Unit,
    private val onControllerCreated: (MapboxMapController, StencilMaskDemos) -> Unit,
    private val onMapReady: (MapboxMapController, StencilMaskDemos, MapboxMap) -> Unit,
    private val teardownDemo: (StencilMaskDemos) -> Unit,
) {
    var controller: MapboxMapController? = null
        private set

    var stencilDemos: StencilMaskDemos? = null
        private set

    var mapboxMap: MapboxMap? = null
        private set

    private var mapLoadedCancelable: Cancelable? = null
    private var styleLoadedCancelable: Cancelable? = null
    private var mapSetupComplete = false

    fun onStart() {
        ensureMapController()
    }

    fun onStop() {
        destroyMapController()
    }

    fun onDestroy() {
        destroyMapController()
    }

    fun destroyMapController() {
        stencilDemos?.let { demos ->
            teardownDemo(demos)
            demos.cancelPendingDemoCallbacks()
        }
        mapLoadedCancelable?.cancel()
        styleLoadedCancelable?.cancel()
        mapLoadedCancelable = null
        styleLoadedCancelable = null
        controller?.shutdown()
        controller = null
        stencilDemos = null
        mapboxMap = null
        mapSetupComplete = false
    }

    private fun ensureMapController() {
        if (controller != null) return
        if (mapView.width == 0 || mapView.height == 0) {
            mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    if (mapView.parent == null) return
                    createMapController()
                }
            })
        } else {
            createMapController()
        }
    }

    private fun createMapController() {
        if (controller != null) return
        configureGlStencilSource()
        val newController = MapboxMapController(mapView, account)
        controller = newController
        mapboxMap = newController.mapboxMap
        val demos = StencilMaskDemos(activity, newController)
        stencilDemos = demos
        onControllerCreated(newController, demos)

        mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
            onMapLoaded()
        }
        styleLoadedCancelable = mapboxMap?.subscribeStyleLoaded {
            mapView.post { onMapLoaded() }
        }
        if (mapboxMap?.style != null) {
            mapView.post { onMapLoaded() }
        }
    }

    private fun onMapLoaded() {
        if (mapSetupComplete) return
        val c = controller ?: return
        val demos = stencilDemos ?: return
        val map = mapboxMap ?: return
        mapSetupComplete = true
        map.style?.setProjection(projection(ProjectionName.MERCATOR))
        onMapReady(c, demos, map)
    }
}
