package com.example.mapsgldemo

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapLoadedCallback
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private lateinit var xweatherAccount: XweatherAccount
    private lateinit var timelineControls: TimelineControls
    private var layerMenu = LayerMenu()
    private var appSettings = AppSettings()
    private var mapLoadedCancelable: Cancelable? = null

    private val mapLoadedCallback = MapLoadedCallback {
        timelineControls.setupSeekBarChangeListener(binding, controller.timeline) {}
        timelineControls.setPosition(controller.timeline.position)
        TimelineTextFormatter.setTimeTextViews(binding.timelineView, controller.timeline)
        LayerButtonView.setAnimations(binding.outerScrollView)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        timelineControls = binding.timelineView.timelineControls
        timelineControls.adjustPaddingForNavigation(binding.timelineView.playControlsCS)
        mapView = binding.mapView

        xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret)
        )

        appSettings.handleOrientationChanges(resources.configuration, window)
        Location.getLocation(this, mapView = mapView)

        binding.mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove listener to avoid multiple calls
                binding.mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Initialize Controller and Get MapboxMap
                controller = MapboxMapController(mapView, xweatherAccount)
                layerMenu.createLayerButtons(controller, binding.outerLinearLayout)
                mapboxMap = controller.mapboxMap

                mapboxMap?.let { map ->
                    appSettings.setMapboxPreferences(controller) // Pass the non-null map instance
                    // Subscribe listeners using stored references
                    mapLoadedCancelable = map.subscribeMapLoaded(mapLoadedCallback)
                }


                // Set up timeline
                with(controller.timeline) {
                    setStartDateUsingRelativeTime("-1 day")
                    setEndDateUsingRelativeTime("now")
                    goTo(0.0)
                    duration = 2.0
                    delay = 0.0
                    endDelay = 1.0
                    repeat = true
                }

                // Setup other UI elements that depend on the controller
                timelineControls.setupButtonListeners(controller.timeline, binding)
                setupMapControllerListeners()
            }
        })

        timelineControls.setAnimations(this, binding)
        setupUIButtonListeners(binding)

        mapView.setOnTouchListener { _, _ ->
            if (layerMenu.visible) {
                timelineControls.showSettings(false, binding)
                LayerButtonView.showDatasetButtons(false, binding.outerScrollView, binding.timelineView.layerMenuButton)
            } else {
                timelineControls.show(true, binding)
            }

            layerMenu.visible = !layerMenu.visible
            false // Return false to allow map default touch handling
        }
    }

    private fun setupMapControllerListeners() {
        controller.onLoadStart.observe(this) {
            binding.timelineView.progressBar.isVisible = true
        }

        controller.onLoadComplete.observe(this) {
            binding.timelineView.progressBar.isVisible = false
        }

        timelineControls.setupTimelineListeners(controller, binding)
    }

    private fun setupUIButtonListeners(binding: ActivityMainBinding) {
        binding.timelineView.layerMenuButton.setOnClickListener {
            LayerButtonView.showDatasetButtons(true, binding.outerScrollView, binding.timelineView.layerMenuButton)
            layerMenu.visible = true
            timelineControls.show(false, binding)
        }

        binding.timelineView.locationButton.setOnClickListener {
            if (Location.retrieved) {
                mapboxMap?.let { map ->
                    Location.easeTo(map)
                }
            } else {
                Location.getLocation(this@MainActivity, mapboxMap, mapView)
            }
        }
    }

    /** Keep track of the orientation of the phone **/
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        appSettings.handleOrientationChanges(newConfig, window)
    }

    override fun onResume() {
        super.onResume()
        LayerButtonView.showDatasetButtons(
            layerMenu.visible,
            binding.outerScrollView,
            binding.timelineView.layerMenuButton
        )
    }

    override fun onDestroy() {
        mapLoadedCancelable?.cancel()
        mapboxMap = null // Clear the reference to the MapboxMap object
        mapLoadedCancelable = null // Clear the cancelable reference
        super.onDestroy() // Call super at the end
    }
}