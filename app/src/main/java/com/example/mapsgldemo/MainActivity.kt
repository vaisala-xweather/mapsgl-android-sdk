package com.example.mapsgldemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityMainBinding
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
import com.mapbox.maps.plugin.scalebar.scalebar
import com.xweather.mapsgl.layers.style.SampleStyle
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.layers.style.ParticleStyle
import com.xweather.mapsgl.layers.style.RasterStyle
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ParticleDensity
import com.xweather.mapsgl.style.ParticleTrailLength

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapController: MapboxMapController
    private lateinit var xweatherAccount: XweatherAccount
    private val layerButtonList: MutableList<View> = mutableListOf()
    private var showLayerMenu = true
    private var isTablet = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        isTablet = LayerButtonView.isTablet(baseContext)

        //You can store account info in res\values\strings.xml
        xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret)
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        Location.getLocation(this, binding.mapView)

        val mapLoadedCallback = MapLoadedCallback {
            // Create menu buttons for all the available layers
            createLayerButtons()

            for (customView in layerButtonList) {
                if (customView is LayerButtonView) { // Add layer button
                    customView.outerView.setOnClickListener {
                        customView.click()
                        if (customView.active) {
                            mapController.addWeatherLayer(customView.configuration)

                            // Customize satellite raster layer
                            if (customView.configuration.layer.id == "satellite") {
                                val rPaint =
                                    mapController.getLayer(customView.configuration.layer.id)!!.paint as RasterStyle
                                rPaint.opacity = 1.0f
                            }

                            // Customize temperatures sample layer
                            else if (customView.configuration.layer.id == "temperatures") {
                                val sPaint =
                                    mapController.getLayer(customView.configuration.layer.id)!!.paint as SampleStyle
                                sPaint.opacity = 1.00f
                            }

                            // Customize wind-particles particle layer
                            else if (customView.configuration.layer.id == "wind-particles") {
                                val pPaint =
                                    mapController.getLayer(customView.configuration.layer.id)!!.paint as ParticleStyle
                                pPaint.opacity = 1.0f
                                pPaint.density = ParticleDensity.NORMAL
                                pPaint.speedFactor = 1f
                                pPaint.trails = true
                                pPaint.trailsFadeFactor = ParticleTrailLength.NORMAL
                                pPaint.size = 2.0f
                            }
                        }
                        mapController.setLayerVisible(customView.configuration.layer.id, customView.active)
                        mapController.mapboxMap.triggerRepaint()
                    }
                }
            }
        }

        mapView = binding.mapView
        LayerButtonView.setAnimations(this, binding.layerConstraintLayout)

        mapView.setOnTouchListener { _, _ ->
            determineMenuVisibility()
            false
        }

        binding.mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                binding.mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                mapController = MapboxMapController(mapView, baseContext, xweatherAccount, this@MainActivity)
                LayerMenu.createLayerButtons(baseContext, layerButtonList, mapController.service, binding.outerLinearLayout)
                with(mapController) {
                    mapboxMap.setProjection(projection(ProjectionName.MERCATOR))
                    mapboxMap.loadStyle(Style.LIGHT) { style ->
                        style.addSource(geoJsonSource("continent-source") {
                            data("https://raw.githubusercontent.com/datasets/geo-boundaries-world-110m/master/countries.geojson")
                        })
                        style.addLayer(lineLayer("continent-layer", "continent-source") {
                            lineColor("#000000")
                            lineWidth(.70)
                            lineOpacity(0.4)
                        })
                    }

                    setZoom(.9)
                    mapboxMap.subscribeMapLoaded(mapLoadedCallback)

                    mapView.scalebar.updateSettings {
                        marginTop = 50f // Adjust this value as needed
                        enabled = false
                    }
                }
            }
        })

        binding.apply {
            setContentView(root)

            layerMenuButton.setOnClickListener {
                LayerButtonView.showDatasetButtons(true, binding.layerConstraintLayout, binding.layerMenuButton)
                showLayerMenu = true

                val customColors: List<ColorStop> = listOf(
                    // The first value is temperature in degrees Celcius
                    // The color values are red, green, blue, alpha
                    ColorStop(-62.22, Color(0.0f, 0.0f, 0.0f, 1.0f)),
                    ColorStop(12.11, Color(0.0f, 0.0f, 1.0f, 1.0f)),
                    ColorStop(26.00, Color(1.0f, 0.0f, 0.0f, 1.0f)),
                    ColorStop(34.44, Color(1.0f, 1.0f, 1.0f, 1.0f)),
                )

                mapController.addWeatherLayer(
                    com.xweather.mapsgl.config.weather.WeatherService.Temperatures(mapController.service).apply {
                        with(layer.paint as SampleStyle) {
                            opacity = 1.0f
                            colorScale.stops = customColors
                        }
                    }
                )
            }

            locationButton.setOnClickListener {
                if (Location.retrieved) {
                    determineMenuVisibility()
                    Location.easeTo(mapController.mapboxMap)
                } else {
                    Location.getLocation(this@MainActivity, mapView, mapController.mapboxMap)
                }
            }

            binding.layerMenuCloseButton.setOnClickListener {
                LayerButtonView.showDatasetButtons(false, binding.layerConstraintLayout, binding.layerMenuButton)
            }
        }
    }

    private fun determineMenuVisibility(){
        if (showLayerMenu&&!isTablet) { //Hide menu when the map is touched
            LayerButtonView.showDatasetButtons(false, binding.layerConstraintLayout, binding.layerMenuButton)
            mapView.scalebar.enabled = true
            showLayerMenu = false
        }
    }
}
