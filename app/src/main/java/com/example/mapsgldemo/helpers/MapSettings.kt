package com.example.mapsgldemo.helpers

import android.content.res.Resources
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.xweather.mapsgl.map.mapbox.MapboxMapController


class MapSettings {
    var styleString = Style.DARK
    private var currentStyle: Style? = null

    /**
     * @param onDarkStyleLoaded Optional hook invoked on the Mapbox thread after the dark style
     * (including continent outline layers) finishes loading. Use this to add style-dependent
     * layers without racing an in-flight [com.mapbox.maps.MapboxMap.loadStyle] replacement.
     */
    fun setMapboxPreferences(
        controller: MapboxMapController,
        resources: Resources,
        onDarkStyleLoaded: (() -> Unit)? = null,
    ) {
        val mapboxMap = controller.mapboxMap
        val mapView = controller.mapView
        mapboxMap?.setProjection(projection(ProjectionName.MERCATOR))

        mapView.mapboxMap.loadStyle(
            style(Style.DARK) {
                +geoJsonSource("continent-source") {
                    data("https://raw.githubusercontent.com/datasets/geo-boundaries-world-110m/master/countries.geojson") // Use url() for consistency if it's a URL
                }

                +lineLayer("continent-layer", "continent-source") {
                    lineColor("#000000")
                    lineWidth(0.7)
                    lineOpacity(0.5)
                }

            }
        ) { loadedStyle ->
            currentStyle = loadedStyle
            onDarkStyleLoaded?.invoke()
        }

        //controller.setCenter(Coordinate(28.0, -99.0))
        //controller.setZoom(5.0)

        //controller.setCenter(Coordinate(0.0, -0.0))
        controller.setZoom(1.0)

        controller.setBearing(0.0)
        controller.setPitch(0.0)

        mapView.scalebar.updateSettings {
            marginTop = 150f
            marginLeft = 8f
        }
        mapView.scalebar.enabled = false
        mapView.logo.enabled = false
        mapView.attribution.enabled = false
    }

    fun toggleDarkMode(mapView: MapView) {
        styleString = if (styleString == Style.DARK) {
            Style.LIGHT
        } else {
            Style.DARK
        }

        mapView.mapboxMap.loadStyle(
            style(styleString) {
                +geoJsonSource("continent-source") {
                    data("https://raw.githubusercontent.com/datasets/geo-boundaries-world-110m/master/countries.geojson") // Use url() for consistency if it's a URL
                }

                +lineLayer("continent-layer", "continent-source") {
                    lineColor("#000000")
                    lineWidth(0.7)
                    lineOpacity(0.5)
                }

            }
        )
    }
}
