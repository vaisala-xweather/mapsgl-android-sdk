package com.example.mapsgldemo.maplayers

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager


class CircleHelper {

    companion object {
        private var circleAnnotationManager: CircleAnnotationManager? = null
        private var pointAnnotationManager: PointAnnotationManager? = null
        private var touchAnnotation: PointAnnotation? = null
        var touchCircleAnnotation: CircleAnnotation? = null
        var fColorString = "#0000ff"
        private var pointAnnotationOptions: PointAnnotationOptions? = null
        var circleAnnotationOptions: CircleAnnotationOptions? = null

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
                .withCircleRadius(3.0)
                .withCircleColor("#ff0000")
                .withCircleStrokeWidth(1.0)
                .withCircleStrokeColor("#ffffff")
            circleAnnotationManager?.create(circleAnnotationOptions)
        }


        fun addTouchMarkerToMap(
            lat: Double,
            lon: Double,
            mapView: MapView,
            message: String? = null
        ) {

            // if(lat< -90 || lon < 180) return
            mapView.post {
                if (pointAnnotationManager == null) {
                    pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
                }

                if (circleAnnotationManager == null) {
                    circleAnnotationManager = mapView.annotations.createCircleAnnotationManager()
                }


                if (touchAnnotation != null) {
                    pointAnnotationManager?.delete(touchAnnotation!!)
                }
                if (touchCircleAnnotation != null) {
                    circleAnnotationManager?.delete(touchCircleAnnotation!!)
                }

                if (pointAnnotationOptions == null) {
                    pointAnnotationOptions = PointAnnotationOptions()
                    pointAnnotationOptions!!.withTextAnchor(TextAnchor.TOP_LEFT)
                    pointAnnotationOptions!!.withIconColor("#FF0000")
                    pointAnnotationOptions!!.withTextColor("#FFFFFF")
                    pointAnnotationOptions!!.withTextOffset(
                        listOf(
                            0.5,  // X-offset: Move 0.5em to the right from the anchor point
                            0.5   // Y-offset: Move 0.5em downwards from the anchor point
                        )
                    )
                }
                if (circleAnnotationOptions == null) {
                    circleAnnotationOptions = CircleAnnotationOptions()
                    circleAnnotationOptions!!.withCircleRadius(3.0)
                }

                if (message != null) {
                    pointAnnotationOptions!!.withTextField(message)
                    if (message.contains("#")) {
                        fColorString = extractFirstHexColorSimple(message)!!
                    }
                }

                pointAnnotationOptions!!.withPoint(Point.fromLngLat(lon, lat))
                touchAnnotation = pointAnnotationManager?.create(pointAnnotationOptions!!)


                circleAnnotationOptions!!.withPoint(Point.fromLngLat(lon, lat))
                circleAnnotationOptions!!.withCircleColor(fColorString)
                circleAnnotationOptions!!.withCircleStrokeWidth(1.0)
                circleAnnotationOptions!!.withCircleStrokeColor("#ffffff")
                touchCircleAnnotation = circleAnnotationManager?.create(circleAnnotationOptions!!)
            }
        }

        private fun extractFirstHexColorSimple(longString: String): String? {
            val startIndex = longString.indexOf('#')

            if (startIndex != -1) {
                // Try to match an 8-digit color first (AARRGGBB)
                if (startIndex + 9 <= longString.length) {
                    val potentialColor8 = longString.substring(startIndex, startIndex + 9)
                    if (potentialColor8.matches(Regex("#[A-Fa-f0-9]{8}"))) {
                        return potentialColor8
                    }
                }

                // If no 8-digit color was found at this '#', try to match a 6-digit color (RRGGBB)
                if (startIndex + 7 <= longString.length) {
                    val potentialColor6 = longString.substring(startIndex, startIndex + 7)
                    if (potentialColor6.matches(Regex("#[A-Fa-f0-9]{6}"))) {
                        return potentialColor6
                    }
                }
            }
            return null
        }
    }
}
