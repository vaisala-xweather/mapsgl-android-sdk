package com.example.mapsgldemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.mapbox.common.Cancelable
import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.MapLoadedCallback
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.controls.legend.LegendControl
import com.xweather.mapsgl.controls.legend.Point.Margin
import com.xweather.mapsgl.controls.legend.Point.PointLegend
import com.xweather.mapsgl.controls.legend.Point.PointLegendItem
import com.xweather.mapsgl.controls.legend.bar.BarLegend
import com.xweather.mapsgl.controls.legend.bar.BarLegendItem
import com.xweather.mapsgl.controls.legend.bar.BarLegendLabels
import com.xweather.mapsgl.extensions.UnitTemperature
import com.xweather.mapsgl.map.MeasurementUnits
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.CircleLayerPaint
import com.xweather.mapsgl.style.ColorScaleOptions
import com.xweather.mapsgl.style.ColorStop
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.weather.WeatherService
import com.xweather.mapsgl.weather.toColor

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private lateinit var xweatherAccount: XweatherAccount
    private var layerMenu = LayerMenu()
    private var appSettings = MapSettings()
    private var mapLoadedCancelable: Cancelable? = null // For MapLoaded
    private var cameraChangedCancelable: Cancelable? = null // For CameraChanged
    private val cameraChangeCallback = CameraChangedCallback { /* Optional: Handle camera changes */ }
    private val mapLoadedCallback = MapLoadedCallback {

        binding.timelineView.timelineControls.setupSeekBarChangeListener(binding, controller.timeline) {}
        binding.timelineView.timelineControls.setPosition(controller.timeline.position)
        TimelineTextFormatter.setTimeTextViews(binding.timelineView, controller.timeline)
        layerMenu.setupButtonListeners(controller)

    }

    override fun onAttachedToWindow() {
        setTurnScreenOn(true)
        setShowWhenLocked(true)
    }

    fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.timelineView.timelineControls.adjustPaddingForNavigation(binding.timelineView.playControlsCS)
        mapView = binding.mapView

        xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret)
        )

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }

        Location.getLocation(this, mapView = mapView)

        binding.mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove listener to avoid multiple calls
                binding.mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // --- Initialize Controller and Get MapboxMap ---
                if (mapView.parent != null) {

                    controller = MapboxMapController(mapView, xweatherAccount)
                    mapboxMap = controller.mapboxMap

                    // New for v1.4.0 is the LegendControl
                    val legendControl = LegendControl()
                    controller.add(legendControl)
                    val legendView = legendControl.containerView
                    if (legendView != null) {
                        legendView.id = View.generateViewId()
                        val menuIndex = binding.outerConstraint.indexOfChild(binding.layerMenuLinearLayout)
                        binding.outerConstraint.addView(legendView, menuIndex)
                        val params = legendView.layoutParams as ConstraintLayout.LayoutParams
                        val parentID = ConstraintLayout.LayoutParams.PARENT_ID
                        params.endToEnd = parentID
                        params.bottomToBottom = parentID
                        val ctx = legendView.context
                        params.bottomMargin = 180.dpToPx(mapView.context)
                        params.marginEnd   = 12.dpToPx(mapView.context)
                        params.width       = 300.dpToPx(mapView.context)
                        legendView.layoutParams = params
                    }

                    mapboxMap?.let { map -> // Use safe call 'let' block
                        appSettings.setMapboxPreferences(controller, resources) // Pass the non-null map instance
                        // Subscribe listeners using stored references
                        mapLoadedCancelable = map.subscribeMapLoaded(mapLoadedCallback)
                        cameraChangedCancelable = map.subscribeCameraChanged(cameraChangeCallback)
                    }

                    with(controller.timeline) {
                        duration = 2.0
                        delay = 0.0
                        endDelay = 1.0
                        repeat = true
                        setStartDateUsingRelativeTime("-12 hours")
                        setEndDateUsingRelativeTime("now")
                    }

                    // New for v1.3.0, the data inspector control is now available.
                    controller.addDataInspectorControl(mapView)

                    // Setup other UI elements that depend on the controller
                    binding.timelineView.timelineControls.setupButtonListeners(controller.timeline, binding)
                    setupTimelineListeners()
                    layerMenu.createLayerButtons(controller.service, binding.layerMenuLinearLayout,)
                    LayerButtonView.setAnimations(binding.layerMenuLinearLayout)
                }
            }
        })

        binding.timelineView.timelineControls.setAnimations(this, binding)
        binding.timelineView.timelineControls.setConfigAnimations(this, binding)
        setupUIButtonListeners(binding) // Setup listeners that don't directly need mapboxMap yet

        mapView.setOnTouchListener { _, _ ->
            if (layerMenu.visible) {
                binding.timelineView.timelineControls.showSettings(false, binding)
                LayerButtonView.showDatasetButtons(
                    false,
                    binding.layerMenuLinearLayout,
                    binding.timelineView.layerMenuButton
                )
            } else {
                binding.timelineView.timelineControls.show(true, binding)
            }

            layerMenu.visible = !layerMenu.visible
            layerMenu.hideKeyboard(this)
            false // Return false to allow map default touch handling
        }
    }

    private fun setupTimelineListeners(){
        controller.onLoadStart.observe(this) {
            binding.timelineView.progressBar.isVisible = true
        }

        controller.onLoadComplete.observe(this) {
            binding.timelineView.progressBar.isVisible = false
        }

        controller.timeline.on(AnimationEvent.play) {
            if (controller.timeline.state == AnimationState.playing) {
                binding.timelineView.timelineControls.updatePlayButtonImage(true, binding)
            } else {
                binding.timelineView.timelineControls.updatePlayButtonImage(false, binding)
            }
        }
        controller.timeline.on(AnimationEvent.stop) {
            binding.timelineView.timelineControls.updatePlayButtonImage(false, binding)
        }

        controller.timeline.on(AnimationEvent.advance) {
            binding.timelineView.timelineControls.setPosition(controller.timeline.position)
        }

        controller.timeline.on(AnimationEvent.range_change) {
            TimelineTextFormatter.setTimeTextViews(binding.timelineView, controller.timeline, controller.timeline.position)
            binding.timelineView.timelineControls.updatePlayButtonImage(false, binding)
        }

    }

    private fun setupUIButtonListeners(binding: ActivityMainBinding) {
        binding.timelineView.layerMenuButton.setOnClickListener {
            LayerButtonView.showDatasetButtons(
                true,
                binding.layerMenuLinearLayout,
                binding.timelineView.layerMenuButton
            )
            layerMenu.visible = true
            binding.timelineView.timelineControls.show(false, binding)
        }

        binding.timelineView.timelineConstraintLayout.visibility = View.INVISIBLE // Consider setting in XML initially
        this.baseContext.resources
        binding.timelineView.locationButton.setOnClickListener {
            if (Location.retrieved) {
                mapboxMap?.let { map -> Location.easeTo(map) }
            } else {
                Location.getLocation(this@MainActivity, mapboxMap, mapView)
            }
        }
    }

    /** Keep track of the screen orientation. **/
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Using WindowInsetsController requires API 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
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
        LayerButtonView.showDatasetButtons(
            layerMenu.visible,
            binding.layerMenuLinearLayout,
            binding.timelineView.layerMenuButton
        )
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