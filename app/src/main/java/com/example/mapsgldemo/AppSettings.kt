package com.example.mapsgldemo

import android.content.res.Configuration
import android.os.Build
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate

class AppSettings {

    fun setMapboxPreferences(controller: MapboxMapController) {
        val mapboxMap = controller.mapboxMap
        val mapView = controller.mapView
        mapboxMap?.setProjection(projection(ProjectionName.MERCATOR))
        mapboxMap?.loadStyle(Style.DARK) { style ->
            style.addSource(geoJsonSource("continent-source") {
                data("https://raw.githubusercontent.com/datasets/geo-boundaries-world-110m/master/countries.geojson")
            })
            style.addLayer(lineLayer("continent-layer", "continent-source") {
                lineColor("#000000")
                lineWidth(.70)
                lineOpacity(0.4)
            })
        }

        controller.setCenter(Coordinate(28.0, -99.0))
        controller.setZoom(.9)
        controller.setBearing(0.0)
        controller.setPitch(0.0)

        mapView.scalebar.updateSettings {
            marginTop = 150f
            marginLeft = 8f
        }
        mapView.scalebar.enabled = false
    }

    fun handleOrientationChanges(config: Configuration, window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                // Show system bars and handle display cutout in portrait
                insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        } else {
            // Handle pre-API 30 orientation changes if needed.
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
        }
    }
}
