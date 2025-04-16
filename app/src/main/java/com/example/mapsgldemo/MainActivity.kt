package com.example.mapsgldemo

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.MapLoadedCallback
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private lateinit var controller: MapboxMapController
    private lateinit var xweatherAccount: XweatherAccount
    private var showLayerMenu = true
    private var isTablet = false

    override fun onAttachedToWindow() {
        setTurnScreenOn(true)
        setShowWhenLocked(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }

        isTablet = LayerButtonView.isTablet(baseContext)

        xweatherAccount = XweatherAccount( //You can store account info in res\values\strings.xml
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret)
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        mapView = binding.mapView
        Location.getLocation(this, mapView = mapView)
        setContentView(binding.root)

        val cameraChangeCallBack = CameraChangedCallback { }

        val mapLoadedCallback = MapLoadedCallback {

            binding.timelineView.timelineControls.setupSeekBarChangeListener(binding, controller.timeline) {}

            controller.timeline.start = Date(Date().time - (3600 * 1000 * 24)) // 24 hours in the past
            controller.timeline.end = Date() // The current time
            controller.timeline.duration = 3.0
            controller.timeline.goTo(1.0)

            binding.timelineView.timelineControls.setPosition(controller.timeline.position)

            TimelineTextFormatter.setTimeTextViews(binding.timelineView, controller.timeline)

            controller.timeline.on(AnimationEvent.PLAY) {
                if (controller.timeline.state == AnimationState.playing) {
                    binding.timelineView.timelineControls.updatePlayButtonImage(true, binding)
                } else {
                    binding.timelineView.timelineControls.updatePlayButtonImage(false, binding)
                }
            }

            controller.timeline.on(AnimationEvent.ADVANCE) {
                binding.timelineView.timelineControls.setPosition(controller.timeline.position)
            }

            controller.timeline.on(AnimationEvent.RANGE_CHANGE) {
                TimelineTextFormatter.setTimeTextViews(
                    binding.timelineView,
                    controller.timeline,
                    controller.timeline.position
                )
            }

            controller.redraw()
            LayerMenu.setupButtonListeners(controller)
        }

        binding.mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                controller = MapboxMapController(mapView, baseContext, xweatherAccount, this@MainActivity)
                binding.timelineView.timelineControls.setupButtonListeners(controller.timeline, binding)
                setupTimelineListeners()
                setMapboxPreferences(controller.mapboxMap)
                controller.mapboxMap.subscribeMapLoaded(mapLoadedCallback)
                controller.mapboxMap.subscribeCameraChanged(cameraChangeCallBack)
                LayerMenu.createLayerButtons(baseContext, controller.service, binding.outerLinearLayout)
                LayerButtonView.setAnimations(baseContext, binding.outerScrollView)
            }
        })

        binding.timelineView.timelineControls.setAnimations(this, binding)
        setupUIButtonListeners(binding)

        mapView.setOnTouchListener { _, _ ->
            determineLayerMenuVisibility()
            false
        }
    }

    private fun setupTimelineListeners() {
        controller.timeline.isDownloading.observe(this) { isLoading -> // Or just 'this' in an Activity
            binding.loadingBar.isVisible = isLoading
        }
    }

    private fun setupUIButtonListeners(binding: ActivityMainBinding) {
        binding.layerMenuButton.setOnClickListener {
            LayerButtonView.showDatasetButtons(true, binding.outerScrollView, binding.layerMenuButton)
            mapView.scalebar.enabled = false
            showLayerMenu = true
            binding.timelineView.timelineControls.show(false, binding)
        }
        binding.timelineView.timelineConstraintLayout.visibility = View.INVISIBLE

        binding.locationButton.setOnClickListener {
            if (Location.retrieved) {
                determineLayerMenuVisibility()
                Location.easeTo(controller.mapboxMap)
            } else {
                Location.getLocation(this@MainActivity, controller.mapboxMap, mapView)
            }
        }
    }

    private fun setMapboxPreferences(mapboxMap: MapboxMap) {
        mapboxMap.setProjection(projection(ProjectionName.MERCATOR)) // This is necessary for this version of MapsGL

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

        controller.setCenter(Coordinate(28.0, -99.0))
        controller.setZoom(.9)
        controller.setBearing(0.0)
        controller.setPitch(0.0)

        mapView.scalebar.updateSettings {
            marginTop = 50f // Position the scalebar
        }

        mapView.scalebar.enabled = false
        mapView.logo.enabled = false
        mapView.attribution.enabled = false
    }

    /**  Hide the layer menu and other UI elements  when appropriate **/
    private fun determineLayerMenuVisibility() {
        if (showLayerMenu /* &&!isTablet */) {
            LayerButtonView.showDatasetButtons(false, binding.outerScrollView, binding.layerMenuButton)
            mapView.scalebar.enabled = true
            showLayerMenu = false
        }

        binding.timelineView.timelineControls.show(true, binding)
    }

    /** Keep track of if the phone is in landscape or portrait mode **/
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                // Account for front-facing camera
                controller?.show(WindowInsets.Type.displayCutout())
            }
        }
    }
}