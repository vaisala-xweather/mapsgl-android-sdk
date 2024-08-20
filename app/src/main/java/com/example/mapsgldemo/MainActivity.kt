package com.example.mapsgldemo

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.CustomView.Companion.createTextView
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapLoadedCallback
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.xweather.mapsglmaps.config.weather.account.XweatherAccount
import com.xweather.mapsglmaps.map.mapbox.MapboxMapController

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapController: MapboxMapController
    private lateinit var xweatherAccount: XweatherAccount
    private val customViewList: MutableList<View> = mutableListOf()
    private var showLayerMenu = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        xweatherAccount = XweatherAccount( //You can store account info in res\values\strings.xml
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret)
        )

        val mapLoadedCallback = MapLoadedCallback {
            //region add layer buttons
            customViewList.add(createTextView("Conditions", baseContext))
            customViewList.add(CustomView(baseContext, "Temperatures", "temperatures"))
            customViewList.add(CustomView(baseContext, "24hr Temp Change", "temperatures-24hr-change"))
            customViewList.add(CustomView(baseContext, "1hr Temp Change", "temperatures-1hr-change", ))
            customViewList.add(CustomView(baseContext, "Feels Like", "feels-like"))
            customViewList.add(CustomView(baseContext, "Heat Index", "heat-index", ))
            customViewList.add(CustomView(baseContext, "Wind Chill", "wind-chill"))
            customViewList.add(CustomView(baseContext, "Winds", "wind-speeds"))
            customViewList.add(CustomView(baseContext, "Wind Particles", "wind-particles"))
            customViewList.add(CustomView(baseContext, "Wind Gusts", "wind-gusts"))
            customViewList.add(CustomView(baseContext, "Dew Point", "dew-points"))
            customViewList.add(CustomView(baseContext, "Humidity", "humidity"))
            customViewList.add(CustomView(baseContext, "MSLP", "pressure-msl"))
            customViewList.add(CustomView(baseContext, "Cloud Cover", "cloud-cover"))
            customViewList.add(CustomView(baseContext, "Precipitation", "precip", ))
            customViewList.add(CustomView(baseContext, "Snow Depth", "snow-depth", ))
            customViewList.add(CustomView(baseContext, "Visibility", "visibility"))
            customViewList.add(CustomView(baseContext, "UV Index", "uvi", ))
            customViewList.add(CustomView(baseContext, "Radar", "radar",))

            customViewList.add(createTextView("Raster", baseContext))
            customViewList.add(CustomView(baseContext, "Satellite", "satellite", ))
            customViewList.add(CustomView(baseContext, "Satellite GeoColor", "satellite-geocolor", ))
            customViewList.add(CustomView(baseContext, "Satellite Visible", "satellite-visible", ))
            customViewList.add(CustomView(baseContext, "Satellite Infrared Color", "satellite-infrared-color", ))
            customViewList.add(CustomView(baseContext, "Satellite Water Vapor", "satellite-water-vapor", ))

            customViewList.add(createTextView("Maritime", baseContext))
            customViewList.add(CustomView(baseContext, "Sea Surface Temps", "sst"))
            customViewList.add(CustomView(baseContext,"Currents: Fill", "ocean-currents", )) //needs to look more like brains
            customViewList.add(CustomView(baseContext, "Wave Heights", "wave-heights"))
            customViewList.add(CustomView(baseContext, "Wave Periods", "wave-periods"))
            customViewList.add(CustomView(baseContext, "Swell 1 Heights", "swell-heights"))
            customViewList.add(CustomView(baseContext, "Swell Periods", "swell-periods"))
            customViewList.add(CustomView(baseContext, "Swell 2 Heights", "swell2-heights"))
            customViewList.add(CustomView(baseContext, "Swell 2 Periods", "swell2-periods"))
            customViewList.add(CustomView(baseContext, "Swell 3 Heights", "swell3-heights"))
            customViewList.add(CustomView(baseContext, "Swell 3 Periods", "swell3-periods"))
            customViewList.add(CustomView(baseContext, "Storm Surge", "storm-surge"))
            customViewList.add(CustomView(baseContext, "Tide Heights", "tide-heights"))

            customViewList.add(createTextView("Air Quality", baseContext))
            customViewList.add(CustomView(baseContext, "Air Quality Index", "air-quality-index"))
            customViewList.add(CustomView(baseContext, "AQI Categories", "air-quality-index-categories", ))
            customViewList.add(CustomView(baseContext, "Health Index", "air-quality-health", ))
            customViewList.add(CustomView(baseContext, "AQI: China", "air-quality-index-china", ))
            customViewList.add(CustomView(baseContext, "AQI: India", "air-quality-index-india", ))
            customViewList.add(CustomView(baseContext, "AQI: Common", "air-quality-index-caqi-categories", ))
            customViewList.add(CustomView(baseContext, "AQI: European", "air-quality-eaqi-categories"))
            customViewList.add(CustomView(baseContext, "AQI: UK", "air-quality-index-uk-daqi-categories", ))
            customViewList.add(CustomView(baseContext, "AQI: Germany", "air-quality-index-uba-daqi-categories"))
            customViewList.add(CustomView(baseContext, "AQI: Korea", "air-quality-index-cai-categories", ))

            customViewList.add(CustomView(baseContext, "Carbon Monoxide (CO)", "air-quality-co", ))
            customViewList.add(CustomView(baseContext, "Nitrogen Monoxide (NO)", "air-quality-no"))
            customViewList.add(CustomView(baseContext, "Nitrogen Dioxide (NO2)", "air-quality-no2"))
            customViewList.add(CustomView(baseContext, "Ozone (O3)", "air-quality-o3"))
            customViewList.add(CustomView(baseContext, "Sulfur Dioxide (SO2)", "air-quality-so2"))
            customViewList.add(CustomView(baseContext, "Particle Pollution (PM 2.5)", "air-quality-pm2p5"))
            customViewList.add(CustomView(baseContext, "Particle Pollution (PM 10)", "air-quality-pm10"))
            //endregion add layer buttons

            for (customView in customViewList) {
                if (customView is CustomView) { // Add layer button
                    binding.outerLinearLayout.addView(customView.outerView)
                    customView.outerView.setOnClickListener {
                        customView.click()

                        if (customView.active) {
                            mapController.addWeatherLayer(customView.id)
                        }
                        mapController.setLayerVisible(customView.id, customView.active)
                        mapController.mapboxMap.triggerRepaint()
                    }
                } else { //add label
                    binding.outerLinearLayout.addView(customView)
                }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        mapView = binding.mapView
        CustomView.setAnimations(this, binding.outerScrollView)

        mapView.setOnTouchListener { _, _ ->
            if (showLayerMenu) { //Hide menu when the map is touched
                CustomView.showDatasetButtons(false, binding.outerScrollView, binding.layerMenuButton)
                mapView.scalebar.enabled = true
                showLayerMenu = false
            }
            false
        }

        binding.mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                binding.mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                binding.mapView.setOnClickListener {
                }

                mapController = MapboxMapController(mapView, baseContext, xweatherAccount, this@MainActivity)
                with(mapController) {
                    mapboxMap.setProjection(projection(ProjectionName.MERCATOR))
                    mapboxMap.loadStyle(Style.DARK) { style ->
                        //mapboxMap.loadStyle(Style.LIGHT) { style ->
                        style.addSource(geoJsonSource("continent-source") {
                            data("https://raw.githubusercontent.com/datasets/geo-boundaries-world-110m/master/countries.geojson" )
                        })
                        style.addLayer(lineLayer("continent-layer", "continent-source") {
                            lineColor("#000000")
                            lineWidth(.50)
                            lineOpacity(0.4)
                        })
                    }

                    setZoom(.9)
                    setBearing(0.0)
                    setPitch(0.0)

                    mapboxMap.subscribeMapLoaded(mapLoadedCallback)

                    mapView.scalebar.updateSettings {
                        marginTop = 50f // Adjust this value as needed
                    }
                    mapView.scalebar.enabled = false
                    mapView.logo.enabled = false
                    mapView.attribution.enabled = false
                }
            }
        })

        binding.apply {
            setContentView(root)

            layerMenuButton.setOnClickListener {
                CustomView.showDatasetButtons(true, binding.outerScrollView, binding.layerMenuButton)
                mapView.scalebar.enabled = false
                showLayerMenu = true
            }

            zoomUpButton.setOnClickListener {
                CustomView.showDatasetButtons(false, binding.outerScrollView, binding.layerMenuButton)
                mapView.scalebar.enabled = true
                val cameraOptions = CameraOptions.Builder()
                    .zoom(mapController.getZoom() + 1)
                    .build()
                val mapAnimationOptions = MapAnimationOptions.Builder().duration(2000).build()
                mapController.mapboxMap.easeTo(cameraOptions, mapAnimationOptions)
            }

            zoomDownButton.setOnClickListener {
                CustomView.showDatasetButtons(false, binding.outerScrollView, binding.layerMenuButton)
                mapView.scalebar.enabled = true
                val cameraOptions = CameraOptions.Builder()
                    .zoom(mapController.getZoom() - 1)
                    .build()
                val mapAnimationOptions = MapAnimationOptions.Builder().duration(2000).build()
                mapController.mapboxMap.easeTo(cameraOptions, mapAnimationOptions)
            }
        }

    }

}