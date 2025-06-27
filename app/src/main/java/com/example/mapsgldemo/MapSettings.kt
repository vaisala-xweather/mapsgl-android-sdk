package com.example.mapsgldemo

import android.content.res.Resources
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
import com.xweather.mapsgl.types.Coordinate


class MapSettings {

    private var currentStyle: Style? = null

    fun setMapboxPreferences(controller: MapboxMapController, resources: Resources) {
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
                    lineOpacity(0.4)
                }
            }
        ) { loadedStyle ->
            currentStyle = loadedStyle
        }

        controller.setCenter(Coordinate(28.0, -99.0))
        controller.setZoom(0.0)
        controller.setBearing(0.0)
        controller.setPitch(0.0)

        mapView.scalebar.updateSettings {
            marginTop = 150f
            marginLeft = 8f
        }
        mapView.scalebar.enabled = false
        mapView.logo.enabled = true
        mapView.attribution.enabled = true
    }
}