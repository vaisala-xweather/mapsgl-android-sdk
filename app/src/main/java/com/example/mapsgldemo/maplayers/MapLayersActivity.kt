package com.example.mapsgldemo.maplayers

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.example.mapsgldemo.MainActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityMapLayersBinding
import com.example.mapsgldemo.helpers.MapSettings
import com.example.mapsgldemo.helpers.StencilMaskDemos
import com.example.mapsgldemo.helpers.TimelineTextFormatter
import com.example.mapsgldemo.stencil.HighwaysConusMaskTemperatureActivity
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Point
import com.mapbox.maps.MapLoadedCallback
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.controls.DataInspectorControl
import com.xweather.mapsgl.controls.legend.LegendControl
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ColorScaleOptions
import com.xweather.mapsgl.style.ColorStop
import com.xweather.mapsgl.style.ParticleDensity
import com.xweather.mapsgl.style.ParticleLayerPaint
import com.xweather.mapsgl.style.ParticleTrailLength
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.types.Size
import com.xweather.mapsgl.types.LayerType
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherLayerConfiguration
import com.xweather.mapsgl.weather.WeatherService
import java.util.Calendar
import java.util.Date

/**
 * Hosts the Mapbox map plus the weather-layer browser, timeline scrubber, and stencil-mask demo
 * buttons. Launched from [MainActivity]. Was previously named `MainActivity`; moved out so
 * that [MainActivity] could be a thin chooser between demo sub-menus.
 *
 * In [R.layout.activity_map_layers], the [MapView] sits inside [R.id.map_container] above the grey timeline chrome; [R.id.map_timeline_split] tracks the top of [TimelineBinding.playControlsCS] so the map is not shortened by the floating icon row inside [R.id.timeline_view].
 *
 * Forwards Mapbox [MapView] lifecycle to the SDK and pauses the weather timeline while stopped; playback resumes on return if it was playing.
 *
 * System back relaunches [MainActivity] (see [returnToMainActivity]) so returning from auto-launch or a cleared back stack still reaches the menu, matching [LocalActivity] and the stencil demos.
 */
open class MapLayersActivity : AppCompatActivity(), OnMapClickListener {

    /** When true, the layer menu lists only [VectorSourceDescriptor] weather products. */
    protected open fun vectorLayersOnly(): Boolean = false

    /** When set with [vectorLayersOnly], only products of this render type appear in the layer menu. */
    protected open fun vectorLayerTypeFilter(): LayerType? = null

    /** Activity opened by the map back button and system back. */
    protected open fun backNavigationActivity(): Class<out AppCompatActivity> = MainActivity::class.java

    /** Stencil-mask shortcut buttons on the timeline row (not used in the vector-only browser). */
    protected open fun showStencilDemoButtons(): Boolean = true

    /** Hook after the layer menu is wired; used by typed vector layer browsers (e.g. fill paint sliders). */
    protected open fun onLayerMenuReady(layerMenu: LayerMenu, controller: MapboxMapController) = Unit

    protected fun isMapControllerReady(): Boolean = ::controller.isInitialized

    companion object {
        /** USGS: worldwide earthquakes, all magnitudes, past 30 days (smaller: `all_week.geojson`, `all_day.geojson`). */
        private const val USGS_EARTHQUAKES_ALL_MONTH =
            "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_month.geojson"
    }

    protected lateinit var binding: ActivityMapLayersBinding
    protected lateinit var mapView: MapView
    protected var mapboxMap: MapboxMap? = null
    protected lateinit var controller: MapboxMapController
    private lateinit var xweatherAccount: XweatherAccount
    private lateinit var stencilDemos: StencilMaskDemos
    private var layerMenu = LayerMenu()
    private var mapSettings = MapSettings()
    private var mapLoadedCancelable: Cancelable? = null // For MapLoaded
    private var cameraChangedCancelable: Cancelable? = null // For CameraChanged
    private var windToggle: Boolean = false
    private var demoUiVisible: Boolean = true
    private val enableDI: Boolean = true
    private var control: DataInspectorControl? = null
    var legendControl = LegendControl()

    /**
     * If we paused the timeline in [onStop] because it was playing, [onStart] calls [com.xweather.mapsgl.anim.Timeline.resume].
     */
    private var resumeTimelineAfterBackground: Boolean = false

    private val timelineAdvanceListener: (Any) -> Unit = {
        // This runs on the thread the timeline uses
        binding.timelineView.timelineControls.setPosition(controller.timeline.position)

    }

    private val mapLoadedCallback = MapLoadedCallback {
        if (!::controller.isInitialized) return@MapLoadedCallback
        binding.timelineView.timelineControls.setupSeekBarChangeListener(binding.timelineView, controller.timeline) {}
        binding.timelineView.timelineControls.setPosition(controller.timeline.position)
        TimelineTextFormatter.setTimeTextViews(
            binding.timelineView,
            controller.timeline,
            startEndLabels = binding.timelineSettingsPanel,
        )
        layerMenu.setupButtonListeners(controller)
        onLayerMenuReady(layerMenu, controller)

        val calendarStart = Calendar.getInstance()
        calendarStart.set(2025, Calendar.MAY, 1, 10, 30, 0)
        val calendarEnd = Calendar.getInstance()
        calendarEnd.set(2025, Calendar.MAY, 2, 10, 30, 0)
    }

    /** Positions [R.id.map_timeline_split] at the top of [TimelineBinding.playControlsCS] so the map fills down to the grey timeline chrome, not the floating icon row. */
    private fun syncMapBottomSplitToPlayControlsTop() {
        if (!::binding.isInitialized) return
        val split = binding.mapTimelineSplit
        val play = binding.timelineView.playControlsCS
        if (play.height == 0 && play.width == 0) return
        val lp = split.layoutParams as ConstraintLayout.LayoutParams
        val margin = play.top
        if (lp.topMargin != margin) {
            lp.topMargin = margin
            split.layoutParams = lp
        }
    }

    private fun attachMapTimelineSplitSync() {
        val play = binding.timelineView.playControlsCS
        val timelineRoot = binding.timelineView.root
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            syncMapBottomSplitToPlayControlsTop()
        }
        play.addOnLayoutChangeListener(listener)
        timelineRoot.addOnLayoutChangeListener(listener)
        binding.root.post { syncMapBottomSplitToPlayControlsTop() }
    }

    override fun onAttachedToWindow() {
        setTurnScreenOn(true)
        setShowWhenLocked(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMapLayersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Embedded [timeline.settingsCS] would grow the timeline and shrink [map_container]; use activity overlay instead.
        binding.timelineView.settingsCS.root.visibility = View.GONE
        binding.timelineView.timelineControls.attachSettingsPanel(binding.timelineSettingsPanel)
        binding.timelineView.timelineControls.adjustPaddingForNavigation(binding.timelineView.playControlsCS)
        attachMapTimelineSplitSync()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMenuActivity()
            }
        })
        binding.timelineView.mapLayersBackButton.visibility = View.VISIBLE
        binding.timelineView.mapLayersBackButton.setOnClickListener { returnToMenuActivity() }
        if (!showStencilDemoButtons()) {
            binding.timelineView.locationButton.visibility = View.GONE
            binding.testButton1.visibility = View.GONE
            binding.testButton2.visibility = View.GONE
            binding.testButton3.visibility = View.GONE
            binding.testButton4.visibility = View.GONE
        }
        mapView = binding.mapView


        // Inflate the custom view

        xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id), getString(R.string.xweather_client_secret)
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

                    // Toggle: which vector tile source to use for the GLES stencil land/water mask.
                    //
                    //   GlStencilOsm.Source.MAPSGL — Xweather `prod.v1.mapsgl.api.xweather.com/vector/
                    //                                open-street-map/...`. (Default. Note: ocean
                    //                                polygons currently missing — temperature can
                    //                                bleed across open water. Support ticket filed.)
                    //   GlStencilOsm.Source.MAPBOX — Mapbox Streets v8 (`api.mapbox.com/v4/
                    //                                mapbox.mapbox-streets-v8/...`). Includes ocean
                    //                                polygons. Requires `mapboxAccessToken`.
                    //
                    // Both must be set BEFORE constructing `MapboxMapController`. The Mapbox token
                    // below is harmless when source = MAPSGL (only consulted on the MAPBOX branch),
                    // so flipping between the two is a one-line change.
                    com.xweather.mapsgl.map.mapbox.GlStencilOsm.source =
                        com.xweather.mapsgl.map.mapbox.GlStencilOsm.Source.MAPSGL
                    com.xweather.mapsgl.map.mapbox.GlStencilOsm.mapboxAccessToken =
                        getString(R.string.mapbox_access_token)

                    controller = MapboxMapController(mapView, xweatherAccount)
                    mapboxMap = controller.mapboxMap
                    stencilDemos = StencilMaskDemos(this@MapLayersActivity, controller)

                    val myLocation = Coordinate(52.4194, 17.7749)
                    controller.setCenter(myLocation)
                    controller.setZoom(2.0)

                    controller.add(legendControl)
                    legendControl.setDarkTheme(true)
                    val legendView = legendControl.getView()

                    legendView.id = View.generateViewId()
                    binding.outerConstraint.addView(legendView, 1)
                    val params = legendView.layoutParams as ConstraintLayout.LayoutParams
                    val parentID = ConstraintLayout.LayoutParams.PARENT_ID
                    params.endToEnd = parentID
                    params.bottomToBottom = parentID
                    params.bottomMargin = 160.dpToPx(mapView.context) //150
                    params.marginEnd = 16.dpToPx(mapView.context)
                    params.width = 300.dpToPx(mapView.context)
                    legendView.layoutParams = params


                    control = controller.addDataInspectorControl(mapView)

                    mapView.gestures.addOnMapClickListener(this@MapLayersActivity)
                    cameraChangedCancelable?.cancel()

                    applyMapLayersTimelineRange()

                    // Setup other UI elements that depend on the controller
                    binding.timelineView.timelineControls.setupButtonListeners(
                        controller.timeline,
                        binding.timelineView
                    )
                    setupTimelineListeners()
                    layerMenu.createLayerButtons(
                        controller.service,
                        binding.layerMenuLinearLayout,
                        vectorLayersOnly = vectorLayersOnly(),
                        vectorLayerType = vectorLayerTypeFilter(),
                    )
                    LayerButtonView.setAnimations(binding.layerMenuLinearLayout)

                    binding.timelineView.timelineControls.setAnimations(this@MapLayersActivity, binding.timelineView)
                    binding.timelineView.timelineControls.setConfigAnimations(
                        this@MapLayersActivity,
                        binding.timelineView
                    )
                    setupUIButtonListeners(binding)

                    mapboxMap?.let { map -> // Use safe call 'let' block
                        mapSettings.setMapboxPreferences(controller, resources) // Pass the non-null map instance
                        // Subscribe after timeline controls + settings animations are ready (map load can invoke the callback synchronously).
                        mapLoadedCancelable = map.subscribeMapLoaded(mapLoadedCallback)
                    }
                    //=============================================================================

                }
            }

        })

        binding.timelineView.timelineControls.installDismissSettingsOnMapTap(
            mapView,
            binding.timelineView,
        ) { _ ->
            if (!layerMenu.visible) return@installDismissSettingsOnMapTap
            LayerButtonView.showDatasetButtons(
                false, binding.layerMenuLinearLayout, binding.timelineView.layerMenuButton,
            )
            layerMenu.visible = false
            layerMenu.hideKeyboard(this)
        }
    }

    private fun setupTimelineListeners() {

        controller.onLoadStart.observe(this) {
            binding.timelineView.progressBar.isVisible = true
            binding.timelineView.progressTextView.isVisible = true
        }

        controller.onLoadComplete.observe(this) {
            binding.timelineView.progressBar.isVisible = false
            binding.timelineView.progressTextView.isVisible = false
        }

        controller.onLoadProgress.observe(this) { progress ->
            val percentInt = if (progress.total > 0) {
                ((progress.completed.toFloat() / progress.total.toFloat()) * 100f).toInt()
            } else {
                0
            }
            if (percentInt != 0 && percentInt != 100) {
                binding.timelineView.progressTextView.text = "${percentInt}%"
            } else {
                binding.timelineView.progressTextView.text = ""
            }
        }

        controller.timeline.on(AnimationEvent.play) {
            if (controller.timeline.state == AnimationState.playing) {
                binding.timelineView.timelineControls.updatePlayButtonImage(true, binding.timelineView)
            } else {
                binding.timelineView.timelineControls.updatePlayButtonImage(false, binding.timelineView)
            }
        }
        controller.timeline.on(AnimationEvent.stop) {
            binding.timelineView.timelineControls.updatePlayButtonImage(false, binding.timelineView)
        }

        controller.timeline.on(AnimationEvent.advance, timelineAdvanceListener)

        controller.timeline.on(AnimationEvent.range_change) {
            TimelineTextFormatter.setTimeTextViews(
                binding.timelineView,
                controller.timeline,
                position = controller.timeline.position,
                startEndLabels = binding.timelineSettingsPanel,
            )
            binding.timelineView.timelineControls.updatePlayButtonImage(false, binding.timelineView)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun setupUIButtonListeners(binding: ActivityMapLayersBinding) {
        binding.timelineView.layerMenuButton.setOnClickListener {
            LayerButtonView.showDatasetButtons(
                true, binding.layerMenuLinearLayout, binding.timelineView.layerMenuButton
            )
            layerMenu.visible = true
        }

        // Demo 1: Portugal ∩ motorways + temperature
        binding.timelineView.locationButton.setOnClickListener {
            if (!controller.hasLayer(StencilMaskDemos.DEMO1_TEMP_LAYER_ID)) {
                stencilDemos.setupDemo1PortugalHighwaysTemperature()
                return@setOnClickListener
            }
            val isVisible = controller.getLayer(StencilMaskDemos.DEMO1_TEMP_LAYER_ID)?.visible == true
            controller.setLayerVisible(StencilMaskDemos.DEMO1_TEMP_LAYER_ID, !isVisible)
        }

        // Demo 2: Portugal admin polygon stencil + temperature only
        binding.testButton1.setOnClickListener {
            if (controller.hasLayer(StencilMaskDemos.DEMO2_TEMP_LAYER_ID)) {
                stencilDemos.teardownDemo2PortugalAdminStencilTemperature()
            } else {
                stencilDemos.setupDemo2PortugalAdminStencilTemperature()
            }
        }

        // Motorway MVT mask ∩ CONUS + temperature
        binding.testButton2.setOnClickListener {
            if (!controller.hasLayer(StencilMaskDemos.DEMO3_TEMP_LAYER_ID)) {
                stencilDemos.setupHighwaysConusMaskTemperature()
                return@setOnClickListener
            }
            val isVisible = controller.getLayer(StencilMaskDemos.DEMO3_TEMP_LAYER_ID)?.visible == true
            controller.setLayerVisible(StencilMaskDemos.DEMO3_TEMP_LAYER_ID, !isVisible)
        }

        // Demo 4: Natural Earth-style admin polygon invert mask + temperature
        binding.testButton3.setOnClickListener {
            if (!controller.hasLayer(StencilMaskDemos.DEMO4_TEMP_LAYER_ID)) {
                stencilDemos.setupDemo4NaturalEarthInvertTemperature()
                return@setOnClickListener
            }
            val isVisible = controller.getLayer(StencilMaskDemos.DEMO4_TEMP_LAYER_ID)?.visible == true
            controller.setLayerVisible(StencilMaskDemos.DEMO4_TEMP_LAYER_ID, !isVisible)
        }

        // Demo 5: launch a dedicated activity that hosts its own Mapbox map. We finish() this
        // activity afterwards so only one MapView is alive at a time — the MapsGL stencil renderer
        // caches GL handles in a process-wide singleton, and two simultaneous EGL contexts (one
        // per MapView) crash the second activity with `glUseProgram -> GL_INVALID_OPERATION`.
        binding.testButton4.setOnClickListener {
            startActivity(android.content.Intent(this, HighwaysConusMaskTemperatureActivity::class.java))
            finish()
        }

        binding.timelineView.locationButton.setOnLongClickListener {
            if (true) {

                controller.clearPending()

            }

            if (false) { // put callout on current location
                if (Location.retrieved) {
                    val point = Point.fromLngLat(Location.longitude, Location.latitude)
                    controller.dataInspector.show(mapboxMap!!.pixelForCoordinate(point), point)
                }
            }

            true
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                toggleDemoUi()
                return true // Consume the event, so the system volume doesn't change
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                toggleOrientation()
                return true // Consume the event
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun toggleDemoUi() {
        demoUiVisible = !demoUiVisible
        setDemoUiVisible(demoUiVisible)
    }

    private fun setDemoUiVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.INVISIBLE
        binding.timelineView.root.visibility = visibility
        binding.layerMenuLinearLayout.visibility = visibility
        legendControl.getView().visibility = visibility
        binding.testButton1.visibility = visibility
        binding.testButton2.visibility = visibility
        binding.testButton3.visibility = visibility
        binding.testButton4.visibility = visibility
    }

    private fun toggleOrientation() {
        val isLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    /** Keep track the screen orientation. **/
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
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
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
        }
        binding.root.post { syncMapBottomSplitToPlayControlsTop() }
    }

    override fun onDestroy() {
        mapLoadedCancelable?.cancel()
        cameraChangedCancelable?.cancel()
        mapboxMap = null // Clear the reference to the MapboxMap object
        mapLoadedCancelable = null // Clear the cancelable reference
        cameraChangedCancelable = null // Clear the cancelable reference
        mapView.onDestroy()
        super.onDestroy() // Call super at the end
    }

    override fun onMapClick(point: Point): Boolean {
        return true
    }

    private fun addWindLayer(controller: MapboxMapController) {

        run {
            try {
                val config = WeatherService.WindParticles(controller.service) as WeatherLayerConfiguration<*, *>
                val paint = config.layer.paint as ParticleLayerPaint
                paint.opacity = 1.0f
                paint.particle.density = ParticleDensity.LOW
                paint.particle.speed = 4.0
                paint.particle.trails = true
                paint.particle.trailsFade = ParticleTrailLength.LONG
                paint.particle.size = Size(4)
                paint.sample.colorScale = ColorScaleOptions(
                    stops = listOf(
                        ColorStop(-62.22, "#FFFFFF"),
                        ColorStop(12.11, "#FFFFFF"),
                        ColorStop(26.00, "#FFFFFF"),
                        ColorStop(34.44, "#FFFFFF")
                    ), interpolate = false
                )

                val manager = this.baseContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val info = ActivityManager.MemoryInfo()

                manager.getMemoryInfo(info)
                if (!info.lowMemory && !controller.hasWeatherLayer(LayerCode.WIND_PARTICLES)) {
                    try {
                        controller.addWeatherLayer(config)
                    } catch (exception: Exception) {
                    }
                }
            } catch (exception: Exception) {
            }
        }
    }

    private fun removeWindLayer(controller: MapboxMapController) {
        controller.let {
            if (controller.hasWeatherLayer(LayerCode.WIND_PARTICLES)) {
                controller.removeWeatherLayer(LayerCode.WIND_PARTICLES)
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (::controller.isInitialized) {
            controller.timeline.on(AnimationEvent.advance, timelineAdvanceListener)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        if (::controller.isInitialized && resumeTimelineAfterBackground) {
            controller.timeline.resume()
            resumeTimelineAfterBackground = false
        }
        mapboxMap?.let { map ->
            if (mapLoadedCancelable == null) {
                mapLoadedCancelable = map.subscribeMapLoaded(mapLoadedCallback)
            }
        }
    }

    override fun onStop() {
        if (::controller.isInitialized) {
            if (controller.timeline.state == AnimationState.playing) {
                controller.timeline.pause()
                resumeTimelineAfterBackground = true
            }
            controller.timeline.off(AnimationEvent.advance, timelineAdvanceListener)
        }
        mapView.onStop()
        mapLoadedCancelable?.cancel()
        cameraChangedCancelable?.cancel()
        mapLoadedCancelable = null
        cameraChangedCancelable = null
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    fun hideButtonsForScreenShots() {
        binding.timelineView.timelineControls.show(false, binding.timelineView)
        binding.timelineView.locationButton.visibility = View.INVISIBLE
        binding.timelineView.mapLayersBackButton.visibility = View.INVISIBLE
        binding.testButton1.visibility = View.INVISIBLE
        binding.testButton2.visibility = View.INVISIBLE
        binding.testButton3.visibility = View.INVISIBLE
        binding.testButton4.visibility = View.INVISIBLE
        binding.timelineView.layerMenuButton.visibility = View.INVISIBLE
    }

    fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

    /** Timeline from **now** through local midnight at the end of today (device timezone). */
    private fun applyMapLayersTimelineRange() {
        val nowMillis = System.currentTimeMillis()
        val endOfTodayMillis = startOfLocalDayMillis(dayOffsetFromToday = 1)
        applyMapLayersTimelineStartEnd(Date(nowMillis), Date(endOfTodayMillis))
    }

    /**
     * Assigns [controller.timeline] start/end and resets the playhead.
     * Order matches [LocalActivity.applyTimelineStartEnd] for [TimeAnimation] setter rules.
     */
    private fun applyMapLayersTimelineStartEnd(start: Date, end: Date) {
        with(controller.timeline) {
            duration = 4.0
            delay = 0.0
            endDelay = 1.0
            repeat = true
            if (end.time <= this.start.time) {
                this.start = start
                this.end = end
            } else {
                this.end = end
                this.start = start
            }
            goTo(0.0)
        }
        binding.timelineView.timelineControls.setPosition(controller.timeline.position)
        TimelineTextFormatter.setTimeTextViews(
            binding.timelineView,
            controller.timeline,
            startEndLabels = binding.timelineSettingsPanel,
        )
    }

    /** Local midnight at the start of the calendar day [dayOffsetFromToday] days from today. */
    private fun startOfLocalDayMillis(dayOffsetFromToday: Int): Long {
        return Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, dayOffsetFromToday)
            timeInMillis
        }
    }

    protected fun returnToMenuActivity() {
        startActivity(
            Intent(this, backNavigationActivity())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

}
