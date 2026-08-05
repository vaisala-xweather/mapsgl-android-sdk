package com.example.mapsgldemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import android.widget.CheckBox
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.example.mapsgldemo.LocalActivity.Companion.FALLBACK_PIN_LAT
import com.example.mapsgldemo.LocalActivity.Companion.FALLBACK_PIN_LON
import com.example.mapsgldemo.databinding.ActivityLocalMapBinding
import com.example.mapsgldemo.helpers.MapSettings
import com.example.mapsgldemo.helpers.TimelineTextFormatter
import com.google.android.gms.location.LocationServices
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Point
import com.mapbox.maps.MapIdleCallback
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.controls.DataInspectorControl
import com.xweather.mapsgl.controls.legend.LegendControl
import com.xweather.mapsgl.controls.legend.Point.PointLegend
import com.xweather.mapsgl.controls.legend.bar.BarLegend
import com.xweather.mapsgl.map.mapbox.GlStencilOsm
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.ColorScale
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.LegendCode
import com.xweather.mapsgl.weather.common.Presentation
import com.xweather.mapsgl.weather.common.Units
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Regional map demo: abbreviated [CheckBox]es toggle weather products (temperature, gridded wind arrows,
 * humidity, precipitation, radar); any combination can be active. Basemap matches [MapLayersActivity] via [MapSettings].
 *
 * Encoded weather layers are inserted like [LayerMenu.setupButtonListeners]: using the first
 * Mapbox `tunnel-` / `road-` / `bridge-` style layer as `beforeId` so the field sits **below** roads
 * and remains visible together with [LayerCode.BOUNDARIES] admin linework.
 *
 * The camera is centered on the pin. The pin uses the device location when permission is granted
 * and fused last location is non-null; otherwise [FALLBACK_PIN_LAT] / [FALLBACK_PIN_LON]. After the
 * initial temperatures layer finishes loading, the data inspector opens at that pin. Encoded
 * temperature values in the inspector callout are shown in **°F only** (via [DataInspectorControl.setPresentation]).
 * Anchoring waits for [MapboxMap.subscribeMapIdle] so [MapboxMap.pixelForCoordinate] matches the camera after
 * [centerViewportOnPin]; otherwise [CalloutView.moveView] treats the point as off-screen and hides
 * the callout. [LegendControl] is centered horizontally and its bottom inset tracks the top of the
 * Local Weather checkbox strip so the panel sits just above it. Layer checkboxes share the Local Weather bar with **Previous day**
 * / **Next day** ([R.layout.timeline], gone by default); that bar sits directly above the play controls strip;
 * [LocalActivity] sets them visible. The [MapView] in [R.layout.activity_local_map] is sized between the screen top and the top of the timeline include so it does not sit under the timeline chrome (including the layer button when visible). The on-screen back control is hidden; the system back gesture
 * or button still returns to the main activity ([OnBackPressedCallback]). Day buttons step the
 * timeline by one local calendar day (**today** = current time through local midnight, always under 24 hours;
 * other days = 8:00→midnight).
 *
 * Forwards Mapbox [MapView] lifecycle to the SDK and pauses the weather timeline while stopped; playback resumes on return if it was playing.
 */
class LocalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalMapBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private val mapSettings = MapSettings()
    private var weatherStackInitialized: Boolean = false

    private val enableDataInspector: Boolean = true
    private var dataInspectorControl: DataInspectorControl? = null

    /** Bar / point legends for active weather layers; same control as [MapLayersActivity]. */
    private val legendControl = LegendControl()

    private var initialDiIdleCancelable: Cancelable? = null
    private var initialDiAnchorDeadlineElapsed: Long = 0L

    /** When true, show the data inspector at the pin once anchoring succeeds (map idle + on-screen pixel). */
    private var pendingAutoDataInspectorForInitialTemperature: Boolean = false

    /**
     * If we paused the timeline in [onStop] because it was playing, [onStart] calls [com.xweather.mapsgl.anim.Timeline.resume].
     */
    private var resumeTimelineAfterBackground: Boolean = false

    private var pinAnnotationManager: CircleAnnotationManager? = null
    private var pinAnnotation: CircleAnnotation? = null
    private var pinLatitude: Double = FALLBACK_PIN_LAT
    private var pinLongitude: Double = FALLBACK_PIN_LON
    private val mapZoom = 8.5

    private val weatherLayerCodes = listOf(
        LayerCode.TEMPERATURES,
        LayerCode.WIND_DIR,
        LayerCode.HUMIDITY,
        LayerCode.PRECIPITATION,
        LayerCode.RADAR,
    )

    /** Encoded products currently on the map (excluding [LayerCode.BOUNDARIES]). */
    private val activeWeatherLayersOnMap = mutableSetOf<LayerCode>()

    /**
     * First Mapbox Streets road/tunnel/bridge layer in the current style — same rule as
     * [LayerMenu.getRoadLayerId]. Cached so encoded layers stack like [MapLayersActivity].
     */
    private var roadStyleLayerAnchorId: String? = null

    /**
     * Timeline calendar day relative to the device’s local “today” (`0` = today, `-1` = yesterday).
     * Range rules: today → [now, local midnight] (never more than the rest of the calendar day); any other day → [8:00, end of that day] with a one-hour minimum span.
     */
    private var timelineDayOffsetFromToday: Int = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.timelineView.timelineControls.attachSettingsPanel(binding.timelineSettingsPanel)
        // MapLayers uses embedded [timeline.settingsCS]; Local uses the activity overlay only.
        binding.timelineView.settingsCS.root.visibility = View.GONE
        binding.timelineView.timelineControls.adjustPaddingForNavigation(binding.timelineView.playControlsCS)
        binding.timelineView.timelineControls.setAnimations(this, binding.timelineView)
        binding.timelineView.timelineControls.setConfigAnimations(this, binding.timelineView)
        binding.timelineView.layerMenuButton.visibility = View.GONE
        binding.timelineView.locationButton.visibility = View.GONE
        binding.timelineView.localControlStrip.visibility = View.VISIBLE
        binding.timelineView.localWeatherTimelinePrevDayButton.visibility = View.VISIBLE
        binding.timelineView.localWeatherTimelineNextDayButton.visibility = View.VISIBLE

        mapView = binding.localMapView
        binding.timelineView.timelineControls.installDismissSettingsOnMapTap(
            mapView,
            binding.timelineView,
        )

        requestPinLocation()

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.localBackButton.setOnClickListener { returnToMainActivity() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainActivity()
            }
        })

        setupWeatherLayerCheckboxes()

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (mapView.parent == null) return

                GlStencilOsm.source = GlStencilOsm.Source.MAPSGL
                GlStencilOsm.mapboxAccessToken = getString(R.string.mapbox_access_token)

                controller = MapboxMapController(mapView, xweatherAccount)
                mapboxMap = controller.mapboxMap

                controller.add(legendControl)
                // Match control strip (#CC000000); [LegendControl.backgroundColor] uses rounded [GradientDrawable].
                legendControl.backgroundColor = Color(0xCC000000)
                val legendView = legendControl.getView()
                legendView.id = View.generateViewId()
                val root = binding.root as ConstraintLayout
                val parentId = ConstraintLayout.LayoutParams.PARENT_ID
                val legendLp = ConstraintLayout.LayoutParams(
                    dp(300),
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    startToStart = parentId
                    endToEnd = parentId
                    horizontalBias = 0.5f
                    bottomToBottom = parentId
                    bottomMargin = dp(DEFAULT_LEGEND_BOTTOM_MARGIN_DP)
                }
                root.addView(legendView, legendLp)
                scheduleLocalLegendLayoutAboveCheckboxStrip()

                if (enableDataInspector) {
                    dataInspectorControl = controller.addDataInspectorControl(mapView)
                    applyLocalTemperatureDataInspectorFahrenheitOnly()
                    dataInspectorControl?.view?.post {
                        dataInspectorControl?.bringCalloutToFront()
                    }
                }

                mapboxMap?.let {
                    mapSettings.setMapboxPreferences(controller, resources) {
                        onLocalMapLoaded()
                    }
                }

                centerViewportOnPin()

                applyTimelineRangeForCurrentOffset()
                updateDayStepButtons()
                binding.timelineView.localWeatherTimelinePrevDayButton.setOnClickListener {
                    shiftTimelineByDays(-1)
                }
                binding.timelineView.localWeatherTimelineNextDayButton.setOnClickListener {
                    shiftTimelineByDays(1)
                }
                binding.timelineView.timelineControls.setupButtonListeners(
                    controller.timeline,
                    binding.timelineView,
                )
                setupLocalTimelineListeners()
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) return
        val anyLocationGranted = permissions.indices.any { i ->
            grantResults.getOrNull(i) == PackageManager.PERMISSION_GRANTED &&
                    (permissions[i] == Manifest.permission.ACCESS_COARSE_LOCATION ||
                            permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (anyLocationGranted) {
            requestPinLocation()
        } else {
            pinLatitude = FALLBACK_PIN_LAT
            pinLongitude = FALLBACK_PIN_LON
            placeOrUpdatePinIfReady()
            centerViewportOnPin()
        }
    }

    private fun onLocalMapLoaded() {
        if (weatherStackInitialized) return
        weatherStackInitialized = true

        mapboxMap?.style?.setProjection(projection(ProjectionName.MERCATOR))

        val roadAnchor = roadStyleLayerAnchor()
        desiredWeatherLayersFromCheckboxes().forEach { code ->
            controller.addWeatherLayer(code, beforeId = roadAnchor)
            activeWeatherLayersOnMap.add(code)
        }
        controller.addWeatherLayer(LayerCode.BOUNDARIES, beforeId = roadAnchor)

        pendingAutoDataInspectorForInitialTemperature = temperaturesLayerActive()

        binding.timelineView.timelineControls.setupSeekBarChangeListener(
            binding.timelineView,
            controller.timeline,
        ) {}
        binding.timelineView.timelineControls.setPosition(controller.timeline.position)
        TimelineTextFormatter.setTimeTextViews(
            binding.timelineView,
            controller.timeline,
            startEndLabels = binding.timelineSettingsPanel,
        )

        placeOrUpdatePinIfReady()
        centerViewportOnPin()

        if (pendingAutoDataInspectorForInitialTemperature) {
            requestInitialTemperatureDataInspectorAnchoring()
        }

        applyLocalTemperatureDataInspectorFahrenheitOnly()
        mapView.post { applyLocalLegendWhiteLabels() }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        if (::controller.isInitialized && resumeTimelineAfterBackground) {
            controller.timeline.resume()
            resumeTimelineAfterBackground = false
        }
    }

    override fun onStop() {
        if (::controller.isInitialized && controller.timeline.state == AnimationState.playing) {
            controller.timeline.pause()
            resumeTimelineAfterBackground = true
        }
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        cancelInitialTemperatureDataInspectorAnchoring()
        mapView.onDestroy()
        super.onDestroy()
    }

    /**
     * Sets [controller.timeline] start/end from [timelineDayOffsetFromToday]:
     * — **Today** (`0`): from **now** through local midnight at the end of the calendar day (never extended past midnight,
     *   so the span is always under 24 hours).
     * — **Any other day**: **8:00** local through midnight at end of that day (same minimum span as before).
     */
    private fun applyTimelineRangeForCurrentOffset() {
        val offset = timelineDayOffsetFromToday
        val nowMillis = System.currentTimeMillis()
        val endOfTargetDayMillis = startOfLocalDayMillis(offset + 1)
        val (startMs, endMs) = if (offset == 0) {
            val end = endOfTargetDayMillis
            nowMillis to end
        } else {
            val start = millisLocalHourOnDay(offset, 8, 0)
            var end = endOfTargetDayMillis
            if (end < start + TIMELINE_MIN_SPAN_MS) {
                end = start + TIMELINE_MIN_SPAN_MS
            }
            start to end
        }
        applyTimelineStartEnd(Date(startMs), Date(endMs))
    }

    private fun shiftTimelineByDays(deltaDays: Int) {
        if (!::controller.isInitialized) return
        timelineDayOffsetFromToday =
            (timelineDayOffsetFromToday + deltaDays).coerceIn(TIMELINE_MIN_DAY_OFFSET, TIMELINE_MAX_DAY_OFFSET)
        applyTimelineRangeForCurrentOffset()
        updateDayStepButtons()
    }

    private fun updateDayStepButtons() {
        binding.timelineView.localWeatherTimelinePrevDayButton.isEnabled =
            timelineDayOffsetFromToday > TIMELINE_MIN_DAY_OFFSET
        binding.timelineView.localWeatherTimelineNextDayButton.isEnabled =
            timelineDayOffsetFromToday < TIMELINE_MAX_DAY_OFFSET
    }

    /**
     * Assigns [controller.timeline] start/end and resets the playhead.
     *
     * Order matters: [com.xweather.mapsgl.anim.TimeAnimation] only applies `end` when the new end is **after**
     * the **current** start, and only applies `start` when the new start is **before** the **current** end.
     * Stepping from **today** to **yesterday** sets `end` to local midnight at the start of today, which is
     * *before* "now" — updating `end` first is a no-op, so we apply `start` first in that case.
     */
    private fun applyTimelineStartEnd(start: Date, end: Date) {
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

    /** Local midnight at the start of the calendar day that is [dayOffsetFromToday] away from today. */
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

    private fun millisLocalHourOnDay(dayOffsetFromToday: Int, hourOfDay: Int, minute: Int): Long {
        return Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, dayOffsetFromToday)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            timeInMillis
        }
    }

    private fun setupLocalTimelineListeners() {
        controller.onLoadStart.observe(this) {
            binding.timelineView.progressBar.isVisible = true
            binding.timelineView.progressTextView.isVisible = true
        }
        controller.onLoadComplete.observe(this) {
            binding.timelineView.progressBar.isVisible = false
            binding.timelineView.progressTextView.isVisible = false
            requestInitialTemperatureDataInspectorAnchoring()
            mapView.post { applyLocalLegendWhiteLabels() }
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
        controller.timeline.on(AnimationEvent.advance) {
            binding.timelineView.timelineControls.setPosition(controller.timeline.position)
        }
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

    /**
     * Subscribes to map-idle frames until the pin projects to a pixel inside the [MapView] (after
     * camera moves). [CalloutView.moveView] hides the inspector when `x !in 0 until mapView.width`.
     */
    private fun requestInitialTemperatureDataInspectorAnchoring() {
        if (!pendingAutoDataInspectorForInitialTemperature) return
        if (!enableDataInspector || dataInspectorControl == null) return
        if (!temperaturesLayerActive()) return
        val map = mapboxMap ?: return

        cancelInitialTemperatureDataInspectorAnchoring()
        initialDiAnchorDeadlineElapsed = SystemClock.elapsedRealtime() + 25_000L

        initialDiIdleCancelable = map.subscribeMapIdle(
            MapIdleCallback {
                mapView.post {
                    tryShowInitialTemperatureDataInspectorAtPinIfReady()
                }
            },
        )
    }

    private fun cancelInitialTemperatureDataInspectorAnchoring() {
        initialDiIdleCancelable?.cancel()
        initialDiIdleCancelable = null
    }

    /**
     * @return true if pending auto-show was consumed (shown or cancelled).
     */
    private fun tryShowInitialTemperatureDataInspectorAtPinIfReady(): Boolean {
        if (!pendingAutoDataInspectorForInitialTemperature) return true
        if (!enableDataInspector || dataInspectorControl == null) return false
        if (!temperaturesLayerActive()) {
            pendingAutoDataInspectorForInitialTemperature = false
            cancelInitialTemperatureDataInspectorAnchoring()
            return true
        }
        if (SystemClock.elapsedRealtime() > initialDiAnchorDeadlineElapsed) {
            pendingAutoDataInspectorForInitialTemperature = false
            cancelInitialTemperatureDataInspectorAnchoring()
            return true
        }
        if (mapView.width <= 0 || mapView.height <= 0) return false

        val map = mapboxMap ?: return false
        val point = Point.fromLngLat(pinLongitude, pinLatitude)
        val px = map.pixelForCoordinate(point)
        val x = px.x.toDouble()
        val y = px.y.toDouble()
        if (x.isNaN() || y.isNaN()) return false

        val w = mapView.width
        val h = mapView.height
        if (x < 0 || x >= w || y < 0 || y >= h) return false

        pendingAutoDataInspectorForInitialTemperature = false
        cancelInitialTemperatureDataInspectorAnchoring()
        controller.dataInspector.show(px, point)
        return true
    }

    private fun temperaturesLayerActive(): Boolean =
        LayerCode.TEMPERATURES in activeWeatherLayersOnMap

    private fun weatherLayerCheckbox(code: LayerCode): CheckBox = when (code) {
        LayerCode.TEMPERATURES -> binding.timelineView.localLayerTemp
        LayerCode.WIND_DIR -> binding.timelineView.localLayerWind
        LayerCode.HUMIDITY -> binding.timelineView.localLayerHumidity
        LayerCode.PRECIPITATION -> binding.timelineView.localLayerPrecip
        LayerCode.RADAR -> binding.timelineView.localLayerRadar
        else -> error("Local does not bind $code")
    }

    private fun desiredWeatherLayersFromCheckboxes(): List<LayerCode> =
        weatherLayerCodes.filter { weatherLayerCheckbox(it).isChecked }

    private fun setupWeatherLayerCheckboxes() {
        weatherLayerCodes.forEach { code ->
            weatherLayerCheckbox(code).setOnCheckedChangeListener { _, _ ->
                syncWeatherLayersFromCheckboxes()
            }
        }
    }

    private fun syncWeatherLayersFromCheckboxes() {
        if (!weatherStackInitialized || !::controller.isInitialized) return
        val desired = desiredWeatherLayersFromCheckboxes().toSet()
        val anchor = roadStyleLayerAnchor()
        val toRemove = activeWeatherLayersOnMap - desired
        toRemove.forEach {
            controller.removeWeatherLayer(it)
            activeWeatherLayersOnMap.remove(it)
        }
        weatherLayerCodes.forEach { code ->
            if (code in desired && code !in activeWeatherLayersOnMap) {
                controller.addWeatherLayer(code, beforeId = anchor)
                activeWeatherLayersOnMap.add(code)
            }
        }
        if (!temperaturesLayerActive()) {
            pendingAutoDataInspectorForInitialTemperature = false
            cancelInitialTemperatureDataInspectorAnchoring()
        }
        applyLocalTemperatureDataInspectorFahrenheitOnly()
        mapView.post { applyLocalLegendWhiteLabels() }
    }

    @SuppressLint("MissingPermission")
    private fun requestPinLocation() {
        val coarseOk = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val fineOk = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!coarseOk && !fineOk) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                LOCATION_PERMISSION_REQUEST_CODE,
            )
            pinLatitude = FALLBACK_PIN_LAT
            pinLongitude = FALLBACK_PIN_LON
            placeOrUpdatePinIfReady()
            centerViewportOnPin()
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                pinLatitude = loc.latitude
                pinLongitude = loc.longitude
            } else {
                pinLatitude = FALLBACK_PIN_LAT
                pinLongitude = FALLBACK_PIN_LON
            }
            placeOrUpdatePinIfReady()
            centerViewportOnPin()
        }
    }

    private fun placeOrUpdatePinIfReady() {
        if (!weatherStackInitialized || !::mapView.isInitialized) return
        mapView.post {
            if (pinAnnotationManager == null) {
                pinAnnotationManager = mapView.annotations.createCircleAnnotationManager()
            }
            pinAnnotation?.let { pinAnnotationManager?.delete(it) }
            val mainRadius = 3.0
            val mainStroke = 1.0
            val options = CircleAnnotationOptions()
                .withPoint(Point.fromLngLat(pinLongitude, pinLatitude))
                .withCircleRadius(mainRadius)
                .withCircleColor("#E53935")
                .withCircleStrokeWidth(mainStroke)
                .withCircleStrokeColor("#FFFFFF")
            pinAnnotation = pinAnnotationManager?.create(options)
            centerViewportOnPin()
        }
    }

    /**
     * Resolves and caches the same anchor [LayerMenu] uses: insert encoded products **before** the
     * first `tunnel-` / `road-` / `bridge-` layer so Mapbox roads and labels stay on top.
     */
    private fun roadStyleLayerAnchor(): String? {
        roadStyleLayerAnchorId?.let { return it }
        val style = mapboxMap?.style ?: return null
        val roadLayerRegex = Pattern.compile("^(?:tunnel|road|bridge)-")
        for (layerInfo in style.styleLayers) {
            if (roadLayerRegex.matcher(layerInfo.id).find()) {
                roadStyleLayerAnchorId = layerInfo.id
                return roadStyleLayerAnchorId
            }
        }
        return null
    }

    private fun centerViewportOnPin() {
        if (!::controller.isInitialized) return
        controller.setCenter(Coordinate(pinLatitude, pinLongitude))
        controller.setZoom(mapZoom)
    }

    private fun returnToMainActivity() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /**
     * Encoded grid temperature query passes **Celsius** in `value`; the default inspector shows °C and °F.
     * This registers a layer-specific [Presentation] so the callout shows **°F only**, matching the legend.
     * [MapController] reapplies the default when the layer is (re)added, so call this after layer sync and initial load.
     */
    private fun applyLocalTemperatureDataInspectorFahrenheitOnly() {
        if (!enableDataInspector) return
        val di = dataInspectorControl ?: return
        di.setPresentation(
            LOCAL_ENCODED_TEMPERATURE_LAYER_ID,
            Presentation(
                title = "Temperature",
                fn = { features ->
                    val map = features as? Map<*, *> ?: return@Presentation ""
                    val celsius = (map["value"] as? Number)?.toDouble() ?: return@Presentation ""
                    String.format(Locale.US, "%.2f°F", Units.CtoF(celsius))
                },
            ),
        )
    }

    /**
     * Legend tweaks for the dark panel: white typography, smaller titles, a 0–100 °F temperature bar,
     * and a 0–40 mph wind bar (via [ColorScaleOptions.range] in native units + fresh [ColorScale] on each [BarLegendItem]).
     */
    private fun applyLocalLegendWhiteLabels() {
        for ((id, leg) in legendControl.legends) {
            if (id == LegendCode.TEMPERATURE.id && leg is BarLegend<*>) {
                val minC = fahrenheitToCelsius(LOCAL_TEMP_LEGEND_MIN_F)
                val maxC = fahrenheitToCelsius(LOCAL_TEMP_LEGEND_MAX_F)
                for (item in leg.items) {
                    item.colorScaleOptions.range = minC..maxC
                    item.colorScale = ColorScale(item.colorScaleOptions)
                }
            }
            if (id == LegendCode.WIND_SPEED.id && leg is BarLegend<*>) {
                val maxMps = mphToMetersPerSecond(LOCAL_WIND_LEGEND_MAX_MPH)
                for (item in leg.items) {
                    item.colorScaleOptions.range = 0.0..maxMps
                    item.colorScale = ColorScale(item.colorScaleOptions)
                }
            }
            leg.titleFontSize = 10.sp
            leg.titleColorValue = Color.White
            when (leg) {
                is BarLegend<*> -> leg.labelColor = Color.White
                is PointLegend -> leg.labelColor = Color.White
                else -> {}
            }
            legendControl.replaceLegend(id, leg)
        }
        scheduleLocalLegendLayoutAboveCheckboxStrip()
    }

    /** Same convention as the SDK temperature palette: scale values are Celsius. */
    private fun fahrenheitToCelsius(f: Double): Double = (f - 32.0) * 5.0 / 9.0

    /** Statute mph → m/s (wind color stops in the SDK ramp are in m/s). */
    private fun mphToMetersPerSecond(mph: Double): Double = mph * 0.44704

    /**
     * Keeps the legend horizontally centered and sets [ConstraintLayout.LayoutParams.bottomMargin] so
     * the panel sits just above the Local Weather checkbox strip when geometry is known.
     */
    private fun scheduleLocalLegendLayoutAboveCheckboxStrip() {
        val legendView = legendControl.getView()
        legendView.post {
            layoutLocalLegendAboveCheckboxStrip()
            val strip = binding.timelineView.localControlStrip
            if (strip.isVisible && strip.height == 0) {
                strip.post { layoutLocalLegendAboveCheckboxStrip() }
            }
        }
    }

    private fun layoutLocalLegendAboveCheckboxStrip() {
        val legendView = legendControl.getView()
        val root = binding.root
        val lp = legendView.layoutParams as? ConstraintLayout.LayoutParams ?: return
        val parentId = ConstraintLayout.LayoutParams.PARENT_ID
        lp.startToStart = parentId
        lp.endToEnd = parentId
        lp.horizontalBias = 0.5f
        lp.bottomToBottom = parentId
        val strip = binding.timelineView.localControlStrip
        val gap = dp(LEGEND_GAP_ABOVE_CHECKBOX_STRIP_DP)
        val margin = if (strip.isVisible && root.height > 0) {
            val stripLoc = IntArray(2)
            val rootLoc = IntArray(2)
            strip.getLocationInWindow(stripLoc)
            root.getLocationInWindow(rootLoc)
            val stripTopInRoot = stripLoc[1] - rootLoc[1]
            if (stripTopInRoot > 0) {
                root.height - stripTopInRoot + gap
            } else {
                dp(DEFAULT_LEGEND_BOTTOM_MARGIN_DP)
            }
        } else {
            dp(DEFAULT_LEGEND_BOTTOM_MARGIN_DP)
        }
        lp.bottomMargin = margin.coerceIn(
            dp(MIN_LEGEND_BOTTOM_MARGIN_DP),
            dp(MAX_LEGEND_BOTTOM_MARGIN_DP),
        )
        legendView.layoutParams = lp
        dataInspectorControl?.bringCalloutToFront()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 93001

        /** Layer id for encoded surface temperatures ([LayerCode.TEMPERATURES] fill). */
        private const val LOCAL_ENCODED_TEMPERATURE_LAYER_ID = "conditions.temperature.fill"

        /** Past / future limits for the day stepper (local calendar days from today). */
        private const val TIMELINE_MIN_DAY_OFFSET = -30
        private const val TIMELINE_MAX_DAY_OFFSET = 14

        /** Smallest span from timeline start to end for non-today days (today never extends past midnight). */
        private const val TIMELINE_MIN_SPAN_MS = 3600L * 1000L

        /** Legend color bar domain for Local temperature (Fahrenheit); converted to Celsius for [ColorScaleOptions.range]. */
        private const val LOCAL_TEMP_LEGEND_MIN_F = 0.0
        private const val LOCAL_TEMP_LEGEND_MAX_F = 100.0

        /** Wind bar legend domain (mph); encoded ramp uses m/s internally ([LegendCode.WIND_SPEED] template). */
        private const val LOCAL_WIND_LEGEND_MAX_MPH = 40.0

        /** Space between the bottom of the legend panel and the top of the checkbox strip. */
        private const val LEGEND_GAP_ABOVE_CHECKBOX_STRIP_DP = 4

        /** Minimum bottom inset when clamping (avoids overlap glitches; keep small so legends can sit near the strip). */
        private const val MIN_LEGEND_BOTTOM_MARGIN_DP = 56

        /** Default bottom inset before strip geometry is known (clears timeline + strip roughly). */
        private const val DEFAULT_LEGEND_BOTTOM_MARGIN_DP = 220

        /** Prevents an oversized margin if window coordinates are odd on first layout. */
        private const val MAX_LEGEND_BOTTOM_MARGIN_DP = 400

        /** Used when location permission is denied or no fused fix is available. */
        private const val FALLBACK_PIN_LAT = 41.355701
        private const val FALLBACK_PIN_LON = -81.806944
    }
}
