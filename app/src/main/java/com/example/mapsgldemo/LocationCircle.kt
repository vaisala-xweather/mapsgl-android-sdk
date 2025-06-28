package com.example.mapsgldemo

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager

class LocationCircle {

    companion object {
        private var circleAnnotationManager: CircleAnnotationManager? = null

        /** Adds circles at and around the international date line*/
        private fun addCircleToMap(
            lat: Double,
            lon: Double,
            color: String,
            manager: CircleAnnotationManager?
        ) {
            val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                .withPoint(Point.fromLngLat(lon, lat))
                .withCircleRadius(4.0)
                .withCircleColor(color)
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#ffffff")
            manager?.create(circleAnnotationOptions)
        }

        fun addLocationMarkerToMap(
            lat: Double,
            lon: Double,
            mapView: MapView
        ) {

            if (circleAnnotationManager == null) {
                circleAnnotationManager = mapView.annotations.createCircleAnnotationManager()
            }

            val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                .withPoint(Point.fromLngLat(lon, lat))
                .withCircleRadius(2.0)
                .withCircleColor("#ff0000")
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#ffffff")
            circleAnnotationManager?.create(circleAnnotationOptions)
        }
    }
}