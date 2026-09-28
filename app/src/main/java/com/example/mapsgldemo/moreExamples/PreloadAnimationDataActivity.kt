package com.example.mapsgldemo.moreExamples

import android.graphics.Typeface
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.MoreExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Preload animation data** - fetch every frame up front so the first loop plays without stalling.
 *
 * By default a layer loads each
 * interval as the playhead reaches it, so the first pass through the timeline is gated by the
 * network. Setting `animationOptions.shouldPreloadData` makes layers fetch their whole interval set
 * for the visible viewport while paused; `preloadAnimationData()` starts that work immediately
 * rather than waiting for the next timeline change.
 *
 * The tradeoff is bandwidth and memory against a smooth first loop, so it suits a short timeline or
 * a deliberate "buffering" step in your own UI.
 *
 * There is no switch here for pausing the timeline while data loads: that setting lives on an
 * internal type and has no public API yet.
 */
class PreloadAnimationDataActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = MoreExamplesMenuActivity::class.java

    override val caption =
        "Wind speeds with every animation frame fetched up front. Toggle preloading, then press " +
            "Play to compare the first loop."

    override val cameraCenter = Coordinate(40.0, -85.5)
    override val cameraZoom = 3.0
    override val showTimelineBar = false

    private lateinit var status: TextView
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button

    override fun customizeLayers(controller: MapboxMapController) {
        controller.animationOptions.shouldPreloadData = PRELOAD_BY_DEFAULT
        controller.addWeatherLayer(LayerCode.WIND_SPEEDS)
        // Start the fetch now rather than waiting for the first timeline change.
        controller.preloadAnimationData()
    }

    override fun buildControls() {
        addToggle("Preload animation data", PRELOAD_BY_DEFAULT) { enabled ->
            controller.animationOptions.shouldPreloadData = enabled
        }

        val (play, pause, preload) = addButtonRow("Play", "Pause", "Preload now")
        playButton = play
        pauseButton = pause
        playButton.setOnClickListener {
            val timeline = controller.timeline
            if (timeline.isActive) timeline.stop() else timeline.play()
            syncPlaybackState()
        }
        pauseButton.setOnClickListener {
            val timeline = controller.timeline
            if (timeline.isPaused) timeline.resume() else timeline.pause()
            syncPlaybackState()
        }
        preload.setOnClickListener { controller.preloadAnimationData() }

        status = TextView(this).apply {
            setTextColor(getColor(R.color.xw_text_secondary))
            typeface = Typeface.MONOSPACE
            fontFeatureSettings = "tnum"
            textSize = 13f
            text = "Idle"
        }
        addControlView(status)

        // The stock timeline bar carries the load readout; with it hidden, the demo reads the
        // controller's load events itself.
        controller.onLoadStart.observe(this) { status.text = "Loading…" }
        controller.onLoadProgress.observe(this) { progress ->
            if (progress.total > 0) status.text = "Loading ${progress.completed} / ${progress.total} tiles"
        }
        controller.onLoadComplete.observe(this) { status.text = "All frames loaded" }

        for (event in listOf(AnimationEvent.play, AnimationEvent.stop, AnimationEvent.PAUSE, AnimationEvent.RESUME)) {
            controller.timeline.on(event) { runOnUiThread { syncPlaybackState() } }
        }
        syncPlaybackState()
    }

    private fun syncPlaybackState() {
        val timeline = controller.timeline
        playButton.text = if (timeline.isActive) "Stop" else "Play"
        pauseButton.text = if (timeline.isPaused) "Resume" else "Pause"
        pauseButton.isEnabled = timeline.isActive
        pauseButton.alpha = if (timeline.isActive) 1f else 0.4f
    }

    private companion object {
        const val PRELOAD_BY_DEFAULT = true
    }
}
