package com.example.mapsgldemo

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.LayerButtonView.Companion.createHeadingTextView
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
import com.xweather.mapsglmaps.config.weather.account.XweatherAccount
import com.xweather.mapsglmaps.layers.style.ParticleStyle
import com.xweather.mapsglmaps.layers.style.RasterStyle
import com.xweather.mapsglmaps.layers.style.SampleStyle
import com.xweather.mapsglmaps.map.mapbox.MapboxMapController
import com.xweather.mapsglmaps.style.ParticleDensity
import com.xweather.mapsglmaps.style.ParticleTrailLength

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapController: MapboxMapController
    private lateinit var xweatherAccount: XweatherAccount

    private val layerButtonList: MutableList<View> = mutableListOf()
    private var showLayerMenu = true
    private var isTablet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        isTablet=LayerButtonView.isTablet(baseContext)

        xweatherAccount = XweatherAccount( //You can store account info in res\values\strings.xml
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret)
        )

        binding = ActivityMainBinding.inflate(layoutInflater)

        //Create menu buttons for all the available layers
        createLayerButtons()

        val mapLoadedCallback = MapLoadedCallback {
            for (customView in layerButtonList) {
                if (customView is LayerButtonView) { // Add layer button
                    customView.outerView.setOnClickListener {
                        customView.click()
                        if (customView.active) {
                            mapController.addWeatherLayer(customView.id)

                            // Customize satellite raster layer
                            if(customView.id == "satellite"){
                                val rPaint = mapController.getLayer(customView.id)!!.paint as RasterStyle
                                rPaint.opacity = .6f
                            }

                            // Customize temperatures sample layer
                            if (customView.id == "temperatures") {
                                val sPaint = mapController.getLayer(customView.id)!!.paint as SampleStyle
                                sPaint.opacity = 1.00f
                            }

                            // Customize wind-particles particle layer
                            if (customView.id == "wind-particles") {
                                val pPaint = mapController.getLayer(customView.id)!!.paint as ParticleStyle
                                pPaint.opacity = 1.0f
                                pPaint.density = ParticleDensity.NORMAL
                                pPaint.speedFactor = 1f
                                pPaint.trails = true
                                pPaint.trailsFadeFactor = ParticleTrailLength.NORMAL
                                pPaint.size = 2.0f
                            }
                        }
                        mapController.setLayerVisible(customView.id, customView.active)
                        mapController.mapboxMap.triggerRepaint()
                    }
                }
            }
        }

        mapView = binding.mapView
        LayerButtonView.setAnimations(this, binding.layerConstraintLayout)

        mapView.setOnTouchListener { _, _ ->
            if (showLayerMenu&&!isTablet) {
                //Hide menu when the map is touched
                LayerButtonView.showDatasetButtons(false, binding.layerConstraintLayout, binding.layerMenuButton)
                mapView.scalebar.enabled = true
                showLayerMenu = false
            }
            false
        }

        binding.mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                binding.mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                mapController = MapboxMapController(mapView, baseContext, xweatherAccount, this@MainActivity)
                with(mapController) {
                    mapboxMap.setProjection(projection(ProjectionName.MERCATOR))
                    mapboxMap.loadStyle(Style.LIGHT) { style ->
                        style.addSource(geoJsonSource("continent-source") {
                            data("https://raw.githubusercontent.com/datasets/geo-boundaries-world-110m/master/countries.geojson" )
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
                    }

                    mapView.scalebar.enabled = false
                }
            }
        })

        binding.apply {
            setContentView(root)

            layerMenuButton.setOnClickListener {
                LayerButtonView.showDatasetButtons(true, binding.layerConstraintLayout, binding.layerMenuButton)
                //mapView.scalebar.enabled = false
                showLayerMenu = true
            }

            binding.layerCloseButton.setOnClickListener{
                LayerButtonView.showDatasetButtons(false, binding.layerConstraintLayout, binding.layerMenuButton)
            }
        }
    }

    private fun createLayerButtons(){
        layerButtonList.add(createHeadingTextView("Conditions", baseContext))
        layerButtonList.add(LayerButtonView(baseContext, "Temperatures", "temperatures"))
        layerButtonList.add(LayerButtonView(baseContext, "24hr Temp Change", "temperatures-24hr-change"))
        layerButtonList.add(LayerButtonView(baseContext, "1hr Temp Change", "temperatures-1hr-change"))
        layerButtonList.add(LayerButtonView(baseContext, "Feels Like", "feels-like"))
        layerButtonList.add(LayerButtonView(baseContext, "Heat Index", "heat-index"))
        layerButtonList.add(LayerButtonView(baseContext, "Wind Chill", "wind-chill"))
        layerButtonList.add(LayerButtonView(baseContext, "Winds", "wind-speeds"))
        layerButtonList.add(LayerButtonView(baseContext, "Wind Particles", "wind-particles"))
        layerButtonList.add(LayerButtonView(baseContext, "Wind Gusts", "wind-gusts"))
        layerButtonList.add(LayerButtonView(baseContext, "Dew Point", "dew-points"))
        layerButtonList.add(LayerButtonView(baseContext, "Humidity", "humidity"))
        layerButtonList.add(LayerButtonView(baseContext, "MSLP", "pressure-msl"))
        layerButtonList.add(LayerButtonView(baseContext, "Cloud Cover", "cloud-cover"))
        layerButtonList.add(LayerButtonView(baseContext, "Precipitation", "precip"))
        layerButtonList.add(LayerButtonView(baseContext, "Snow Depth", "snow-depth"))
        layerButtonList.add(LayerButtonView(baseContext, "Visibility", "visibility"))
        layerButtonList.add(LayerButtonView(baseContext, "UV Index", "uvi"))
        layerButtonList.add(LayerButtonView(baseContext, "Radar", "radar"))

        layerButtonList.add(createHeadingTextView("Raster", baseContext))
        layerButtonList.add(LayerButtonView(baseContext, "Satellite", "satellite"))
        layerButtonList.add(LayerButtonView(baseContext, "Satellite GeoColor", "satellite-geocolor"))
        layerButtonList.add(LayerButtonView(baseContext, "Satellite Visible", "satellite-visible"))
        layerButtonList.add(LayerButtonView(baseContext, "Satellite Infrared Color", "satellite-infrared-color"))
        layerButtonList.add(LayerButtonView(baseContext, "Satellite Water Vapor", "satellite-water-vapor"))

        layerButtonList.add(createHeadingTextView("Maritime", baseContext))
        layerButtonList.add(LayerButtonView(baseContext, "Sea Surface Temps", "sst"))
        layerButtonList.add(LayerButtonView(baseContext, "Currents: Fill", "ocean-currents"))
        layerButtonList.add(LayerButtonView(baseContext, "Currents: Particles", "ocean-currents.particles"))
        layerButtonList.add(LayerButtonView(baseContext, "Wave Heights", "wave-heights"))
        layerButtonList.add(LayerButtonView(baseContext, "Wave Periods", "wave-periods"))
        layerButtonList.add(LayerButtonView(baseContext, "Swell 1 Heights", "swell-heights"))
        layerButtonList.add(LayerButtonView(baseContext, "Swell Periods", "swell-periods"))
        layerButtonList.add(LayerButtonView(baseContext, "Swell 2 Heights", "swell2-heights"))
        layerButtonList.add(LayerButtonView(baseContext, "Swell 2 Periods", "swell2-periods"))
        layerButtonList.add(LayerButtonView(baseContext, "Swell 3 Heights", "swell3-heights"))
        layerButtonList.add(LayerButtonView(baseContext, "Swell 3 Periods", "swell3-periods"))
        layerButtonList.add(LayerButtonView(baseContext, "Storm Surge", "storm-surge"))
        layerButtonList.add(LayerButtonView(baseContext, "Tide Heights", "tide-heights"))

        layerButtonList.add(createHeadingTextView("Air Quality", baseContext))
        layerButtonList.add(LayerButtonView(baseContext, "Air Quality Index", "air-quality-index"))
        layerButtonList.add(LayerButtonView(baseContext, "AQI Categories", "air-quality-index-categories"))
        layerButtonList.add(LayerButtonView(baseContext, "Health Index", "air-quality-health"))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: China", "air-quality-index-china"))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: India", "air-quality-index-india"))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: Common", "air-quality-index-caqi-categories"))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: European", "air-quality-eaqi-categories"))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: UK", "air-quality-index-uk-daqi-categories"))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: Germany", "air-quality-index-uba-daqi-categories"))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: Korea", "air-quality-index-cai-categories"))
        layerButtonList.add(LayerButtonView(baseContext, "Carbon Monoxide (CO)", "air-quality-co"))
        layerButtonList.add(LayerButtonView(baseContext, "Nitrogen Monoxide (NO)", "air-quality-no"))
        layerButtonList.add(LayerButtonView(baseContext, "Nitrogen Dioxide (NO2)", "air-quality-no2"))
        layerButtonList.add(LayerButtonView(baseContext, "Ozone (O3)", "air-quality-o3"))
        layerButtonList.add(LayerButtonView(baseContext, "Sulfur Dioxide (SO2)", "air-quality-so2"))
        layerButtonList.add(LayerButtonView(baseContext, "Particle Pollution (PM 2.5)", "air-quality-pm2p5"))
        layerButtonList.add(LayerButtonView(baseContext, "Particle Pollution (PM 10)", "air-quality-pm10"))

        for (customView in layerButtonList) {
            if (customView is LayerButtonView) { // Add layer button
                binding.outerLinearLayout.addView(customView.outerView)

            } else { //Add category label
                binding.outerLinearLayout.addView(customView)
            }

        }
    }
}
