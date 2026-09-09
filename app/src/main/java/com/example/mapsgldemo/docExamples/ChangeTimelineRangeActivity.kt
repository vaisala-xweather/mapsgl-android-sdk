package com.example.mapsgldemo.docExamples

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityChangeTimelineRangeBinding
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.style.SampleLayerPaint
import com.xweather.mapsgl.weather.WeatherLayerConfiguration
import com.xweather.mapsgl.weather.WeatherService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Change timeline range — the Android port of the MapsGL JS example at
 * https://www.xweather.com/docs/mapsgl/examples/change-timeline-range
 *
 * Updates the timeline's start and end dates at runtime while a time-based layer
 * (`temperatures`) is on the map. When the range changes, new intervals are calculated for it, so
 * resuming playback may need to load data the previous range did not cover.
 *
 * The published JS example animates `wind-particles`. This uses `temperatures` instead: it is an
 * encoded raster product with no OpenGL ES 3.1 requirement, so the screen behaves the same on every
 * device, and the colour shift across the range makes the timeline moving easier to see.
 *
 * The JS example assigns `controller.timeline.startDate` / `endDate` directly. Android names those
 * [com.xweather.mapsgl.anim.Timeline.start] and `end`, and adds
 * [com.xweather.mapsgl.anim.TimeAnimation.setStartDateUsingOffset] /
 * `setEndDateUsingOffset`, which take an offset in milliseconds relative to a date (defaulting to
 * now) and keep at least an hour between the two ends:
 *
 * ```kotlin
 * controller.timeline.setStartDateUsingOffset(-hours * 3_600_000L, Date())
 * ```
 *
 * That clamp is why selecting "Now" for both ends still leaves a one-hour window.
 *
 * Playback state comes from [com.xweather.mapsgl.anim.Timeline.liveState] rather than the JS
 * `play` / `stop` / `pause` / `resume` events, and the loading indicator is driven by
 * [com.xweather.mapsgl.map.MapController.onLoadStart] and `onLoadComplete`.
 *
 * Launched from [DocsExamplesMenuActivity].
 */
class ChangeTimelineRangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangeTimelineRangeBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private var mapLoadedCancelable: Cancelable? = null

    private var syncingSpinners = false
    private var startIndex = DEFAULT_START_INDEX
    private var endIndex = DEFAULT_END_INDEX

    /**
     * The JS example labels with weekday and time only. That reads the same for "Now" and
     * "-7 days" — seven days back is the same weekday — so the day of month is included here.
     */
    private val labelFormat = SimpleDateFormat("EEE d MMM, h:mm a", Locale.US)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeTimelineRangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.changeTimelineRangeMapView

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.changeTimelineRangeBackButton.setOnClickListener { returnToDocsMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToDocsMenu()
            }
        })

        setupRangeSpinners()

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (mapView.parent == null) return

                controller = MapboxMapController(mapView, xweatherAccount)
                mapboxMap = controller.mapboxMap

                // Set the projection before the first frame is drawn so the map does not paint as
                // a globe and then snap flat.
                mapboxMap?.setProjection(projection(ProjectionName.MERCATOR))

                // Matches the JS example's `center: [20, 47], zoom: 4`. Coordinate is (lat, lon).
                controller.setCenter(Coordinate(47.0, 20.0))
                controller.setZoom(4.0)

                observePlaybackState()
                observeLoading()
                wirePlaybackButtons()

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    // Seed the range from the selectors before the layer is added, so the first
                    // set of intervals is calculated for the range actually shown.
                    //
                    // End first, then start. The offset helpers keep at least an hour between the
                    // two ends, and the timeline opens on now-24h..now — so setting start to "Now"
                    // while end is still the default would clamp it back an hour.
                    applyEndOffset(endIndex)
                    applyStartOffset(startIndex)
                    updateRangeLabels()

                    // Temperatures draws opaque by default and covers the base map entirely, which
                    // leaves no coastline to orient against while the timeline plays. Dropping the
                    // opacity keeps the geography visible. `addWeatherLayer(LayerCode.TEMPERATURES)`
                    // on its own is all the timeline needs.
                    val temperatures = run {
                        val config = WeatherService.Temperatures(controller.service)
                            as WeatherLayerConfiguration<*, *>
                        (config.layer.paint as SampleLayerPaint).opacity = 0.7f
                        config
                    }
                    controller.addWeatherLayer(temperatures)
                }
            }
        })
    }

    /** The JS example's two `<select>` elements: hours before now, and hours after now. */
    private fun setupRangeSpinners() {
        listOf(
            binding.changeTimelineRangeStartSpinner to START_LABELS,
            binding.changeTimelineRangeEndSpinner to END_LABELS,
        ).forEach { (spinner, labels) ->
            spinner.adapter = ArrayAdapter(this, R.layout.map_overlay_spinner_item, labels)
                .also { it.setDropDownViewResource(R.layout.map_overlay_spinner_dropdown_item) }
        }

        syncingSpinners = true
        binding.changeTimelineRangeStartSpinner.setSelection(startIndex)
        binding.changeTimelineRangeEndSpinner.setSelection(endIndex)
        syncingSpinners = false

        binding.changeTimelineRangeStartSpinner.onItemSelectedListener = itemSelected { position ->
            if (position == startIndex) return@itemSelected
            startIndex = position
            if (::controller.isInitialized) {
                applyStartOffset(position)
                updateRangeLabels()
            }
        }
        binding.changeTimelineRangeEndSpinner.onItemSelectedListener = itemSelected { position ->
            if (position == endIndex) return@itemSelected
            endIndex = position
            if (::controller.isInitialized) {
                applyEndOffset(position)
                updateRangeLabels()
            }
        }
    }

    /** The Android equivalent of `controller.timeline.startDate = new Date(...)`. */
    private fun applyStartOffset(index: Int) {
        val hours = RANGE_HOURS[index]
        controller.timeline.setStartDateUsingOffset(-hours * HOUR_MS, Date())
    }

    /** The Android equivalent of `controller.timeline.endDate = new Date(...)`. */
    private fun applyEndOffset(index: Int) {
        val hours = RANGE_HOURS[index]
        controller.timeline.setEndDateUsingOffset(hours * HOUR_MS, Date())
    }

    private fun updateRangeLabels() {
        binding.changeTimelineRangeStartLabel.text =
            getString(R.string.timeline_range_start, labelFormat.format(controller.timeline.start))
        binding.changeTimelineRangeEndLabel.text =
            getString(R.string.timeline_range_end, labelFormat.format(controller.timeline.end))
    }

    /**
     * Mirrors the JS `play` / `stop` / `pause` / `resume` handlers, which relabel the buttons.
     * Android exposes the same transitions as [AnimationState] on `timeline.liveState`.
     */
    private fun observePlaybackState() {
        controller.timeline.liveState.observe(this) { state ->
            val active = state == AnimationState.playing || state == AnimationState.paused
            binding.changeTimelineRangePlayButton.text =
                getString(if (active) R.string.timeline_stop else R.string.timeline_play)
            binding.changeTimelineRangePauseButton.text =
                getString(
                    if (state == AnimationState.paused) {
                        R.string.timeline_resume
                    } else {
                        R.string.timeline_pause
                    },
                )
            binding.changeTimelineRangePauseButton.isEnabled = active
        }
    }

    /**
     * Mirrors the JS `load:start` / `load:complete` handlers driving the loading indicator.
     *
     * The spinner and its label live inside the range panel rather than in a screen corner: a
     * range change is what triggers the reload, so the feedback belongs next to the control that
     * caused it, and it stays legible over animating weather data.
     */
    private fun observeLoading() {
        controller.onLoadStart.observe(this) {
            binding.changeTimelineRangeLoadingRow.visibility = View.VISIBLE
        }
        controller.onLoadComplete.observe(this) {
            binding.changeTimelineRangeLoadingRow.visibility = View.GONE
        }
    }

    private fun wirePlaybackButtons() {
        binding.changeTimelineRangePlayButton.setOnClickListener {
            val timeline = controller.timeline
            if (timeline.isActive) timeline.stop() else timeline.play()
        }
        binding.changeTimelineRangePauseButton.setOnClickListener {
            val timeline = controller.timeline
            if (timeline.isPaused) timeline.resume() else timeline.pause()
        }
    }

    private fun itemSelected(onSelected: (Int) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (syncingSpinners) return
                onSelected(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        if (::controller.isInitialized && controller.timeline.isActive) {
            controller.timeline.stop()
        }
        mapLoadedCancelable?.cancel()
        mapLoadedCancelable = null
        mapView.onDestroy()
        super.onDestroy()
    }

    private fun returnToDocsMenu() {
        startActivity(
            Intent(this, DocsExamplesMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        private const val HOUR_MS = 3_600_000L

        /** Same offsets as the JS example's `dateRanges`, in hours from now. */
        private val RANGE_HOURS = longArrayOf(0, 24, 72, 168)
        private val START_LABELS = arrayOf("Now", "-1 day", "-3 days", "-7 days")
        private val END_LABELS = arrayOf("Now", "+1 day", "+3 days", "+7 days")

        /** The JS example opens on `start = 0`, `end = 24`. */
        private const val DEFAULT_START_INDEX = 0
        private const val DEFAULT_END_INDEX = 1
    }
}
