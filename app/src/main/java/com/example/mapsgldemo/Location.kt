package com.example.mapsgldemo

import android.Manifest
import android.animation.Animator
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager

class Location {

    companion object {
        private var latitude = 0.0
        private var longitude = 0.0
        var retrieved = false
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
        private lateinit var fusedLocationClient: FusedLocationProviderClient

        fun getLocation(activity: Activity, mapView: MapView, mapboxMap: MapboxMap? = null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                location?.let {
                    latitude = it.latitude
                    longitude = it.longitude
                    retrieved = true
                    addLocationMarkerToMap(latitude, longitude, mapView)
                    if (mapboxMap != null) easeTo(mapboxMap)
                } ?: run {
                    if (mapboxMap != null) {
                        Toast.makeText(activity, "Please enable GPS.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener { e -> }
        }

        fun easeTo(mapboxMap: MapboxMap) {
            val cameraOptions = CameraOptions.Builder().center(
                Point.fromLngLat(
                    longitude, latitude
                )
            ).zoom(4.0).build()

            mapboxMap.easeTo(cameraOptions = cameraOptions, animatorListener = object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        private fun addLocationMarkerToMap(
            lat: Double,
            lon: Double,
            //color: String,
            mapView: MapView
        ) {
            val circleAnnotationManager = mapView.annotations.createCircleAnnotationManager()
            val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                .withPoint(Point.fromLngLat(lon, lat))
                .withCircleRadius(2.0)
                .withCircleColor("#ff0000")
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#ffffff")
            circleAnnotationManager.create(circleAnnotationOptions)
        }
    }
}