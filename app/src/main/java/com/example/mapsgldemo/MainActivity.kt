package com.example.mapsgldemo

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.mapbox.common.Cancelable
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

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapView: MapView // Keep reference to MapView itself
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private lateinit var xweatherAccount: XweatherAccount
    private var isTablet = false
    private var layerMenu = LayerMenu()
    private var mapLoadedCancelable: Cancelable? = null
    private var cameraChangedCancelable: Cancelable? = null
    private val cameraChangeCallback = CameraChangedCallback { /* Optional: Handle camera changes */ }
    private val mapLoadedCallback = MapLoadedCallback {
        binding.timelineView.timelineControls.setupSeekBarChangeListener(binding, controller.timeline) {}
        controller.timeline.start = Date(Date().time - (3600 * 1000 * 24  )) // 24 hours in the past
        controller.timeline.end = Date() // The current time
        controller.timeline.duration = 3.0
        controller.timeline.goTo(1.0)
        binding.timelineView.timelineControls.setPosition(controller.timeline.position)
        TimelineTextFormatter.setTimeTextViews(binding.timelineView, controller.timeline)
        controller.redraw()
        layerMenu.setupButtonListeners(controller)
    }

    override fun onAttachedToWindow() {
        setTurnScreenOn(true)
        setShowWhenLocked(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapView = binding.mapView
        isTablet = LayerButtonView.isTablet(baseContext)

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }

        xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret)
        )

        Location.getLocation(this, mapView = mapView)

        binding.mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove listener to avoid multiple calls
                binding.mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // --- Initialize Controller and Get MapboxMap ---
                if (mapView.parent != null) {
                    controller = MapboxMapController(mapView, baseContext, xweatherAccount, this@MainActivity)

                    // Store the MapboxMap reference locally and nullably
                    this@MainActivity.mapboxMap = controller.mapboxMap

                    mapboxMap?.let { map -> // Use safe call 'let' block
                        setMapboxPreferences(map) // Pass the non-null map instance
                        // Subscribe listeners using stored references
                        mapLoadedCancelable = map.subscribeMapLoaded(mapLoadedCallback)
                        cameraChangedCancelable = map.subscribeCameraChanged(cameraChangeCallback)
                    }
                    // Setup other UI elements that depend on the controller
                    binding.timelineView.timelineControls.setupButtonListeners(controller.timeline, binding)
                    setupTimelineListeners()
                    layerMenu.createLayerButtons(baseContext, controller.service, binding.outerLinearLayout)
                    LayerButtonView.setAnimations(baseContext, binding.outerScrollView)
                }
            }
        })

        binding.timelineView.timelineControls.setAnimations(this, binding)
        setupUIButtonListeners(binding) // Setup listeners that don't directly need mapboxMap yet

        mapView.setOnTouchListener { _, _ ->
            determineLayerMenuVisibility()
            false // Return false to allow map default touch handling
        }
    }

    private fun setupTimelineListeners(){
        controller.timeline.isDownloading.observe(this) { isLoading ->
            binding.loadingBar.isVisible = isLoading
        }

        // Setup other timeline listeners
        controller.timeline.on(AnimationEvent.PLAY){
            if (controller.timeline.state == AnimationState.playing) {
                binding.timelineView.timelineControls.updatePlayButtonImage(true, binding)
            } else {
                binding.timelineView.timelineControls.updatePlayButtonImage(false, binding)
            }
        }

        controller.timeline.on(AnimationEvent.ADVANCE){
            binding.timelineView.timelineControls.setPosition(controller.timeline.position)
        }

        controller.timeline.on(AnimationEvent.RANGE_CHANGE) {
            TimelineTextFormatter.setTimeTextViews(binding.timelineView, controller.timeline, controller.timeline.position)
            binding.timelineView.timelineControls.updatePlayButtonImage(false, binding)
        }
    }

    private fun setupUIButtonListeners(binding: ActivityMainBinding){
        binding.layerMenuButton.setOnClickListener {
            LayerButtonView.showDatasetButtons(true, binding.outerScrollView, binding.layerMenuButton)
            mapView.scalebar.enabled = false // Use safe call
            layerMenu.visible = true
            binding.timelineView.timelineControls.show(false, binding)
        }
        binding.timelineView.timelineConstraintLayout.visibility = View.INVISIBLE // Consider setting in XML initially

        binding.locationButton.setOnClickListener {
            determineLayerMenuVisibility()
            if (Location.retrieved) {
                mapboxMap?.let { map ->
                    Location.easeTo(map)
                }
            } else {
                Location.getLocation(this@MainActivity, mapboxMap, mapView)
            }
        }
    }

    private fun setMapboxPreferences(mapboxMap: MapboxMap){
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

        if (::controller.isInitialized) {
            controller.setCenter(Coordinate(28.0, -99.0))
            controller.setZoom(.9)
            controller.setBearing(0.0)
            controller.setPitch(0.0)
        }

        mapView.scalebar.updateSettings {
            marginTop = 150f
        }
        mapView.scalebar.enabled = false
        mapView.logo.enabled = false
        mapView.attribution.enabled = false
    }

    /**  Hide the layer menu and other UI elements when appropriate **/
    private fun determineLayerMenuVisibility(){
        if (layerMenu.visible /* &&!isTablet */ ) {
            LayerButtonView.showDatasetButtons(false, binding.outerScrollView, binding.layerMenuButton)
            mapView.scalebar.enabled = true // Use safe call
            layerMenu.visible = false
        }

        binding.timelineView.timelineControls.show(true, binding)
    }

    /** Keep track of if the phone is in landscape or portrait mode **/
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Using WindowInsetsController requires API 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                // Optionally hide status/navigation bars in landscape for immersive mode
                insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                // Show system bars and handle display cutout in portrait
                insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        } else {
            // Handle pre-API 30 orientation changes if needed.
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LayerButtonView.showDatasetButtons(layerMenu.visible, binding.outerScrollView, binding.layerMenuButton)
    }

    override fun onDestroy() {
        mapLoadedCancelable?.cancel()
        cameraChangedCancelable?.cancel()
        mapboxMap = null // Clear the reference to the MapboxMap object
        mapLoadedCancelable = null // Clear the cancelable reference
        cameraChangedCancelable = null // Clear the cancelable reference
        super.onDestroy() // Call super at the end
    }
}