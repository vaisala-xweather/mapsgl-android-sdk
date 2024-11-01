package com.example.mapsgldemo

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
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
import com.xweather.mapsgl.config.weather.WeatherService
import com.xweather.mapsgl.layers.style.SampleStyle
import com.xweather.mapsgl.weather.groups.ColorStop
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        isTablet = LayerButtonView.isTablet(baseContext)

        xweatherAccount = XweatherAccount( //You can store account info in res\values\strings.xml
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret)
        )

        binding = ActivityMainBinding.inflate(layoutInflater)

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
                            if(customView.configuration.layer.id == "satellite"){
                                val rPaint = mapController.getLayer(customView.configuration.layer.id)!!.paint as RasterStyle
                                rPaint.opacity = .6f
                            }

                            // Customize temperatures sample layer
                            else if (customView.configuration.layer.id == "temperatures") {
                                val sPaint = mapController.getLayer(customView.configuration.layer.id)!!.paint as SampleStyle
                                sPaint.opacity = .40f
                            }

                            // Customize wind-particles particle layer
                            else if (customView.configuration.layer.id == "wind-particles") {
                                val pPaint = mapController.getLayer(customView.configuration.layer.id)!!.paint as ParticleStyle
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

            binding.layerCloseButton.setOnClickListener{
                LayerButtonView.showDatasetButtons(false, binding.layerConstraintLayout, binding.layerMenuButton)
            }
        }
    }

    private fun createLayerButtons(){
        layerButtonList.add(createHeadingTextView("Conditions", baseContext))
        layerButtonList.add(LayerButtonView(baseContext, "Temperatures", com.xweather.mapsgl.config.weather.WeatherService.Temperatures(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "24hr Temp Change", WeatherService.Temperatures24HourChange(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "1hr Temp Change", WeatherService.Temperatures1HourChange(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Feels Like", WeatherService.FeelsLike(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Heat Index",  WeatherService.HeatIndex(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Wind Chill",  WeatherService.WindChill(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Winds",  WeatherService.WindSpeeds(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Wind Particles",  WeatherService.WindParticles(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Wind Gusts",  WeatherService.WindGusts(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Dew Point", WeatherService.Dewpoint(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Humidity", WeatherService.Humidity(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "MSLP", WeatherService.PressureMeanSeaLevel(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Cloud Cover", WeatherService.CloudCover(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Precipitation", WeatherService.Precipitation(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Snow Depth", WeatherService.SnowDepth(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Visibility", WeatherService.Visibility(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "UV Index", WeatherService.UVIndex(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Radar", WeatherService.Radar(mapController.service)))

        layerButtonList.add(createHeadingTextView("Raster", baseContext))
        layerButtonList.add(LayerButtonView(baseContext, "Satellite", WeatherService.Satellite(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Satellite GeoColor", WeatherService.SatelliteGeoColor(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Satellite Visible", WeatherService.SatelliteVisible(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Satellite Infrared Color", WeatherService.SatelliteInfraredColor(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Satellite Water Vapor", WeatherService.SatelliteWaterVapor(mapController.service)))
        layerButtonList.add(createHeadingTextView("Maritime", baseContext))
        layerButtonList.add(LayerButtonView(baseContext, "Sea Surface Temps", WeatherService.SST(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Currents: Fill", WeatherService.OceanCurrents(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Currents: Particles", WeatherService.OceanCurrentsParticles(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Wave Heights", WeatherService.WaveHeights(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Wave Periods", WeatherService.WavePeriods(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Swell 1 Heights", WeatherService.SwellHeights(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Swell Periods", WeatherService.SwellPeriods(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Swell 2 Heights", WeatherService.SwellHeights2(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Swell 2 Periods", WeatherService.SwellPeriods2(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Swell 3 Heights", WeatherService.SwellHeights3(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Swell 3 Periods", WeatherService.SwellPeriods3(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Storm Surge", WeatherService.StormSurge(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Tide Heights", WeatherService.TideHeights(mapController.service)))

        layerButtonList.add(createHeadingTextView("Air Quality", baseContext))
        layerButtonList.add(LayerButtonView(baseContext, "Air Quality Index", WeatherService.AirQualityIndex(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "AQI Categories", WeatherService.AirQualityIndexCategories(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Health Index", WeatherService.HealthIndex(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: China", WeatherService.AirQualityIndexChina(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: India", WeatherService.AirQualityIndexIndia(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: Common", WeatherService.AirQualityIndexCommon(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: European", WeatherService.AirQualityIndexEuropean(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: UK", WeatherService.AirQualityIndexUK(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: Germany", WeatherService.AirQualityIndexGermany(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "AQI: Korea", WeatherService.AirQualityIndexKorea(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Carbon Monoxide (CO)", WeatherService.CarbonMonoxide(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Nitrogen Monoxide (NO)", WeatherService.NitricOxide(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Nitrogen Dioxide (NO2)", WeatherService.NitrogenDioxide(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Ozone (O3)", WeatherService.Ozone(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Sulfur Dioxide (SO2)", WeatherService.SulfurDioxide(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Particle Pollution (PM 2.5)", WeatherService.ParticulateMatter25Micron(mapController.service)))
        layerButtonList.add(LayerButtonView(baseContext, "Particle Pollution (PM 10)", WeatherService.ParticulateMatter10Micron(mapController.service)))

        for (customView in layerButtonList) {
            if (customView is LayerButtonView) { // Add layer button
                binding.outerLinearLayout.addView(customView.outerView)

            } else { //Add category label
                binding.outerLinearLayout.addView(customView)
            }
        }
    }
}
