package com.example.mapsgldemo.helpers

import android.content.res.Resources
import com.mapbox.bindgen.Value
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

    /** When true, Mapbox Dark place-name symbol layers (cities, states, countries) are hidden. */
    var hideMapboxPlaceNameLabels: Boolean = false

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
            applyMapboxPlaceNameLabelVisibility(loadedStyle)
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
        ) { loadedStyle ->
            currentStyle = loadedStyle
            applyMapboxPlaceNameLabelVisibility(loadedStyle)
        }
    }

    /**
     * Hides Mapbox Dark `settlement-*` / `country-label` / `state-label` (and older `place-city*` ids)
     * so MapsGL GLES place text is not sitting under the basemap’s own city names.
     */
    private fun applyMapboxPlaceNameLabelVisibility(style: Style) {
        if (!hideMapboxPlaceNameLabels) return
        val hidden = Value.valueOf("none")
        for (layerInfo in style.styleLayers) {
            if (!isMapboxPlaceNameLayerId(layerInfo.id)) continue
            runCatching {
                style.setStyleLayerProperty(layerInfo.id, "visibility", hidden)
            }
        }
    }

    companion object {
        private fun isMapboxPlaceNameLayerId(id: String): Boolean {
            val key = id.lowercase()
            return key.contains("settlement") ||
                key == "country-label" ||
                key == "state-label" ||
                key.startsWith("place-city") ||
                key.startsWith("place-town") ||
                key.startsWith("place-village") ||
                key.startsWith("place-hamlet") ||
                key.startsWith("place-suburb") ||
                key.startsWith("place-state") ||
                key.startsWith("place-country")
        }
    }
}
