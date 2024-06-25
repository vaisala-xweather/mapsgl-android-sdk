package com.example.mapsgldemo

import android.os.Bundle
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapIdleCallback
import com.mapbox.maps.MapLoadedCallback
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.xweather.mapsglmaps.config.weather.WeatherService
import com.xweather.mapsglmaps.config.weather.account.XweatherAccount
import com.xweather.mapsglmaps.map.mapbox.MapboxMapController
import com.xweather.mapsglmaps.sources.ImageTileSource
import com.xweather.mapsglmaps.sources.RasterTileApiEvent
import com.xweather.mapsglmaps.types.Coordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapController: MapboxMapController
    private lateinit var xweatherAccount: XweatherAccount
    private val customViewList: MutableList<CustomView> = mutableListOf()

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //You can store your account info in res\values\strings.xml
        xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret)
        )

        val mapLoadedCallback= MapLoadedCallback{
            val service = mapController.service

            /** You can create a layer like this:
             *      val temperatureLayer = WeatherService.Temperatures(service)
             *  and add the layer to the MapController like this:
             *      mapController.addWeatherLayer(temperatureLayer)
             *  You can refer to the layerby it's layerDescriptor.id:
             *      mapController.setLayerVisible(temperatureLayer.service.layerDescriptor.id, true)
             **/

            //Create layers of multiple datasets and attach them to custom buttons:
            customViewList.add(CustomView(baseContext,"Temperatures", WeatherService.Temperatures(service)))
            customViewList.add(CustomView(baseContext,"24hr Temp Change", WeatherService.Temperatures24HourChange(service) ))
            customViewList.add(CustomView(baseContext,"1hr Temp Change", WeatherService.Temperatures1HourChange(service)))
            customViewList.add(CustomView(baseContext,"Feels Like", WeatherService.FeelsLike(service)))
            customViewList.add(CustomView(baseContext,"Heat Index", WeatherService.HeatIndex(service)))
            customViewList.add(CustomView(baseContext,"Wind Chill", WeatherService.WindChill(service)))
            customViewList.add(CustomView(baseContext,"Winds", WeatherService.WindSpeeds(service)))
            customViewList.add(CustomView(baseContext,"Dew Point", WeatherService.DewPoint(service)))
            customViewList.add(CustomView(baseContext,"Humidity", WeatherService.Humidity(service)))
            customViewList.add(CustomView(baseContext,"Cloud Cover", WeatherService.CloudCover(service)))
            customViewList.add(CustomView(baseContext,"Precipitation", WeatherService.Precipitation(service)))
            customViewList.add(CustomView(baseContext,"Visibility", WeatherService.Visibility(service)))
            customViewList.add(CustomView(baseContext,"UV Index", WeatherService.UVIndex(service)))
            customViewList.add(CustomView(baseContext,"Radar", WeatherService.Radar(service)))
            customViewList.add(CustomView(baseContext,"Alerts", WeatherService.Alerts(service)))
            customViewList.add(CustomView(baseContext,"Satellite", WeatherService.Satellite(service)))
            customViewList.add(CustomView(baseContext,"Satellite Geocolor", WeatherService.SatelliteGeoColor(service)))
            customViewList.add(CustomView(baseContext,"Satellite Visible", WeatherService.SatelliteVisible(service)))
            customViewList.add(CustomView(baseContext,"Satellite Infrared Color", WeatherService.SatelliteInfraredColor(service)))
            customViewList.add(CustomView(baseContext,"Satellite Water Vapor", WeatherService.SatelliteWaterVapor(service)))
            customViewList.add(CustomView(baseContext,"AQI", WeatherService.AirQualityIndex(service)))
            customViewList.add(CustomView(baseContext,"AQI Categories", WeatherService.AirQualityIndexCategories(service)))
            customViewList.add(CustomView(baseContext,"Carbon Monoxide (CO2)", WeatherService.CarbonMonoxide(service)))
            customViewList.add(CustomView(baseContext,"Nitrogen Monoxide", WeatherService.NitricOxide(service)))
            customViewList.add(CustomView(baseContext,"Nitrogen Dioxide", WeatherService.NitrogenDioxide(service)))
            customViewList.add(CustomView(baseContext,"Health Index", WeatherService.HealthIndex(service)))
            customViewList.add(CustomView(baseContext,"Ozone", WeatherService.Ozone(service)))
            customViewList.add(CustomView(baseContext,"SO2", WeatherService.SulfurDioxide(service)))
            customViewList.add(CustomView(baseContext,"Sea Surface Temps", WeatherService.SST(service)))

            for (customView in customViewList){
                mapController.addWeatherLayer(customView.service)
                mapController.setLayerVisible(customView.service.layerDescriptor.id, false)
                binding.outerLinearLayout.addView(customView.outerView)

                customView.checkBox.setOnCheckedChangeListener { _, isChecked ->
                    mapController.setLayerVisible(customView.service.layerDescriptor.id, isChecked)
                }
            }
        }

        val mapIdleCallback=MapIdleCallback{
            //optional: Perform operations after movement stops
        }

        val cameraChangeCallBack=CameraChangedCallback{
            CoroutineScope(Dispatchers.Main).launch {
                mapController.onMove()
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        mapView = binding.mapView
        CustomView.setAnimations(this, binding.outerScrollView)

        mapView.setOnTouchListener({ _, _ ->
            //Hide menu when the map is touched
            CustomView.showLayerButtons(false, binding.outerScrollView, binding.layerMenuButton)
            false
        })

        binding.mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                binding.mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                binding.mapView.setOnClickListener{
                }

                mapController = MapboxMapController(mapView, baseContext, xweatherAccount)
                with(mapController) {
                    mapboxMap.setProjection(projection(ProjectionName.MERCATOR))
                    mapboxMap.loadStyle(Style.DARK)
                    setCenter(Coordinate(0.0, -70.0))
                    setZoom(.9)
                    setBearing(0.0)
                    setPitch(0.0)
                    mapboxMap.subscribeMapLoaded(mapLoadedCallback)
                    mapboxMap.subscribeMapIdle(mapIdleCallback)
                    mapboxMap.subscribeCameraChanged(cameraChangeCallBack)

                }
            }
        })

        binding.apply {
            setContentView(root)
            statusText = statusTextView

            layerMenuButton.setOnClickListener {
                CustomView.showLayerButtons(true, binding.outerScrollView, binding.layerMenuButton)
            }

            zoomUpButton.setOnClickListener {
                CustomView.showLayerButtons(false, binding.outerScrollView, binding.layerMenuButton)
                val cameraOptions = CameraOptions.Builder()
                    .zoom(mapController.getZoom() + 1)
                    .build()
                val mapAnimationOptions = MapAnimationOptions.Builder().duration(2000).build()
                mapController.mapboxMap.easeTo(cameraOptions, mapAnimationOptions)
            }

            zoomDownButton.setOnClickListener {
                CustomView.showLayerButtons(false, binding.outerScrollView, binding.layerMenuButton)
                val cameraOptions = CameraOptions.Builder()
                    .zoom(mapController.getZoom() - 1)
                    .build()
                val mapAnimationOptions = MapAnimationOptions.Builder().duration(2000).build()
                mapController.mapboxMap.easeTo(cameraOptions, mapAnimationOptions)
            }
        }
    }

    override fun onResume(){
        super.onResume()
        ImageTileSource.apiEvent.observe(this,::onRasterTileEvent)
    }
    private fun onRasterTileEvent(event: RasterTileApiEvent) {
        when (event) {
            is RasterTileApiEvent.Success -> {  }
            is RasterTileApiEvent.Error -> println("onRasterTileEvent Error")
            is RasterTileApiEvent.Reload -> {
                mapController.updateTiles() //Updates map when tiles load
            }
        }
    }

}