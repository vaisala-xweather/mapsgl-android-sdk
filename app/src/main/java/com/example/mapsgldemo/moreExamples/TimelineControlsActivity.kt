package com.example.mapsgldemo.moreExamples

import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.MoreExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * **Timeline controls** - play, pause, resume and scrub the map's timeline from your own UI.
 *
 * `controller.timeline` animates every
 * layer on the map together:
 *
 * - `play()` and `stop()` start and end a run;
 * - `pause()` and `resume()` hold the playhead where it is without ending the run;
 * - `goTo(position)` scrubs to a normalized `0..1` position.
 *
 * Listening for [AnimationEvent.advance] keeps your own UI in step with the playhead - including
 * while it advances on its own, which is what makes the slider track playback rather than fight
 * it. The stock timeline bar is hidden here so the controls on screen are the ones in this file.
 */
class TimelineControlsActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = MoreExamplesMenuActivity::class.java

    override val caption =
        "Temperatures driven by the timeline API directly: Play/Stop, Pause/Resume and a scrubber " +
            "that follows the playhead."

    override val cameraCenter = Coordinate(40.0, -85.5)
    override val cameraZoom = 3.0
    override val showTimelineBar = false

    private lateinit var scrubber: SeekBar
    private lateinit var dateLabel: TextView
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button

    /** True while a finger is on the scrubber, so the playhead does not yank the thumb away. */
    private var scrubbing = false

    override fun customizeLayers(controller: MapboxMapController) {
        controller.addWeatherLayer(LayerCode.TEMPERATURES)
    }

    override fun buildControls() {
        buildScrubber()

        val (play, pause) = addButtonRow("Play", "Pause")
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

        // The timeline changes state on its own too - it stops at the end of a non-repeating run,
        // and loading can pause it - so the buttons follow its events, not only their own taps.
        for (event in listOf(AnimationEvent.play, AnimationEvent.stop, AnimationEvent.PAUSE, AnimationEvent.RESUME)) {
            controller.timeline.on(event) { runOnUiThread { syncPlaybackState() } }
        }
        controller.timeline.on(AnimationEvent.advance) { runOnUiThread { syncPlayhead() } }

        syncPlayhead()
        syncPlaybackState()
    }

    private fun buildScrubber() {
        scrubber = SeekBar(this).apply {
            max = SCRUB_STEPS
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) = Unit

                override fun onStartTrackingTouch(sb: SeekBar?) {
                    // Hold the playhead while dragging so it does not fight the thumb.
                    scrubbing = true
                    controller.timeline.pause()
                    syncPlaybackState()
                }

                override fun onStopTrackingTouch(sb: SeekBar?) {
                    scrubbing = false
                    controller.timeline.goTo(progress.toDouble() / SCRUB_STEPS)
                    syncPlayhead()
                }
            })
        }
        dateLabel = TextView(this).apply {
            setTextColor(getColor(R.color.xw_text_primary))
            typeface = Typeface.MONOSPACE
            fontFeatureSettings = "tnum"
            textSize = 13f
            gravity = Gravity.END
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(scrubber, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(dateLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        addControlView(row)
    }

    private fun syncPlayhead() {
        val timeline = controller.timeline
        if (!scrubbing) scrubber.progress = (timeline.position * SCRUB_STEPS).toInt()
        dateLabel.text = DATE_FORMAT.format(timeline.currentDate)
    }

    private fun syncPlaybackState() {
        val timeline = controller.timeline
        playButton.text = if (timeline.isActive) "Stop" else "Play"
        pauseButton.text = if (timeline.isPaused) "Resume" else "Pause"
        pauseButton.isEnabled = timeline.isActive
        pauseButton.alpha = if (timeline.isActive) 1f else 0.4f
    }

    private companion object {
        const val SCRUB_STEPS = 1000
        val DATE_FORMAT = SimpleDateFormat("EEE h:mm a", Locale.getDefault())
    }
}
