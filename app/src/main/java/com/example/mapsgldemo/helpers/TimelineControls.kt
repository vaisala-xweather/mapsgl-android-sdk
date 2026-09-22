package com.example.mapsgldemo.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.TimelineBinding
import com.example.mapsgldemo.databinding.TimelineSettingsPanelBinding
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.anim.Timeline
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class TimelineControls(context: Context, attrs: AttributeSet? = null) :
    androidx.appcompat.widget.AppCompatSeekBar(context, attrs) {

    var fromTouch = false
    private lateinit var slideOutAnimation: Animation
    private lateinit var slideInAnimation: Animation
    private lateinit var settingsSlideOutAnimation: Animation
    private lateinit var settingsSlideInAnimation: Animation
    private var timelineVisibility: Boolean = false
    private val seekbarRange = 10000.0
    var seekbarDoubleValue = 0.0

    /** 0..1 along the track for wall-clock now, clamped to the timeline range; null if the range is empty. */
    private var nowMarkerFraction: Float? = null
    private var nowMarkerStartMs: Long = 0L
    private var nowMarkerEndMs: Long = 0L

    private val nowMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9E9E9E.toInt()
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    private val refreshNowMarkerRunnable = Runnable {
        recomputeNowMarkerFraction()
        scheduleNowMarkerRefresh()
    }

    /** Hosts timeline date/speed controls; inflated beside the map so the sheet overlays without resizing [com.mapbox.maps.MapView]. */
    private lateinit var settingsPanelBinding: TimelineSettingsPanelBinding

    fun attachSettingsPanel(binding: TimelineSettingsPanelBinding) {
        settingsPanelBinding = binding
    }

    /** Set the position of the time seekbar from 0F to 1F **/
    fun setPosition(position: Double) {
        this.progress = (position * seekbarRange).toInt()
    }

    /**
     * Places a tick on the seekbar at wall-clock now. If now is before [start] or after [end],
     * the tick sits on the nearer end so a “through now” range still shows a mark.
     */
    fun setNowMarkerFromTimeline(start: Date, end: Date) {
        nowMarkerStartMs = start.time
        nowMarkerEndMs = end.time
        recomputeNowMarkerFraction()
        scheduleNowMarkerRefresh()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        recomputeNowMarkerFraction()
        scheduleNowMarkerRefresh()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(refreshNowMarkerRunnable)
        super.onDetachedFromWindow()
    }

    private fun recomputeNowMarkerFraction() {
        val span = nowMarkerEndMs - nowMarkerStartMs
        val fraction = if (span <= 0L) {
            null
        } else {
            val nowMs = System.currentTimeMillis()
            ((nowMs - nowMarkerStartMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        }
        if (fraction != nowMarkerFraction) {
            nowMarkerFraction = fraction
            invalidate()
        }
    }

    private fun scheduleNowMarkerRefresh() {
        removeCallbacks(refreshNowMarkerRunnable)
        if (nowMarkerEndMs > nowMarkerStartMs) {
            postDelayed(refreshNowMarkerRunnable, 30_000L)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val fraction = nowMarkerFraction ?: return
        val x = trackXForFraction(fraction)
        val centerY = (paddingTop + height - paddingBottom) / 2f
        val halfHeight = dp(5f)
        nowMarkerPaint.strokeWidth = dp(2.5f)
        canvas.drawLine(x, centerY - halfHeight, x, centerY + halfHeight, nowMarkerPaint)
    }

    private fun trackXForFraction(fraction: Float): Float {
        val innerWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        return paddingLeft + fraction * innerWidth
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    fun setupSeekBarChangeListener(
        binding: TimelineBinding,
        timeline: Timeline,
        onChange: (progress: Int) -> Unit
    ) {
        binding.timelineControls.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUserTouch: Boolean) {
                seekbarDoubleValue = progress.toDouble() / seekbarRange
                if (fromUserTouch) {

                    if (timeline.state == AnimationState.playing) {
                        binding.timelineControls.updatePlayButtonImage(true, binding)
                    }
                    timeline.goTo(binding.timelineControls.seekbarDoubleValue)

                }

                TimelineTextFormatter.setCurrentTimeTextView(
                    binding,
                    timeline,
                    binding.timelineControls.seekbarDoubleValue
                )

                //println("TimelineControls Flicker 2 setupSeekBarChangeListener() progress: $progress,  value: ${seekbarDoubleValue}"  )

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                binding.timelineControls.showSettings(false, binding)
                fromTouch = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                fromTouch = false
            }
        })
    }

    fun updatePlayButtonImage(isPlaying: Boolean, binding: TimelineBinding) {
        if (isPlaying) {
            binding.playButtonImage.setImageResource(R.drawable.pause_button_image)
        } else {
            binding.playButtonImage.setImageResource(R.drawable.play_button_image)
        }
    }

    fun setupButtonListeners(timeline: Timeline, binding: TimelineBinding) {
        val s = settingsPanelBinding
        with(binding) {

            playButton.setOnClickListener {
                if (timeline.state == AnimationState.playing) {
                    timeline.pause()
                    updatePlayButtonImage(false, binding)
                } else {
                    timeline.play()
                    updatePlayButtonImage(timeline.state == AnimationState.playing, binding)
                    timelineControls.showSettings(false, binding)
                }
            }

            timeline.on(AnimationEvent.PAUSE) {
                updatePlayButtonImage(false, binding)
            }

            configButton.setOnClickListener {
                if (s.root.isVisible) {
                    timelineControls.showSettings(false, binding)
                } else {
                    timelineControls.showSettings(true, binding)
                }
            }
        }

        with(s) {
            startPlusDayButton.setOnClickListener {
                timeline.setStartDateUsingOffset((24 * 3600 * 1000), timeline.start)
            }

            startMinusDayButton.setOnClickListener {
                timeline.setStartDateUsingOffset((-24 * 3600 * 1000), timeline.start)
            }

            startPlusHourButton.setOnClickListener {
                timeline.setStartDateUsingOffset((3600 * 1000), timeline.start)
            }

            startMinusHourButton.setOnClickListener {
                timeline.setStartDateUsingOffset((-3600 * 1000), timeline.start)
            }

            endPlusHourButton.setOnClickListener {
                timeline.setEndDateUsingOffset((3600 * 1000), timeline.end)
            }

            endMinusHourButton.setOnClickListener {
                timeline.setEndDateUsingOffset((-3600 * 1000), timeline.end)
            }

            endPlusDayButton.setOnClickListener {
                timeline.setEndDateUsingOffset((24 * 3600 * 1000), timeline.end)
            }

            endMinusDayButton.setOnClickListener {
                timeline.setEndDateUsingOffset((-24 * 3600 * 1000), timeline.end)
            }

            speedSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    applyTimeScale(timeline, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            // Start from the scale the timeline already carries rather than assuming 1x - a demo may
            // have set its own - quantised onto the slider's tenths.
            speedSeekbar.progress = progressForSpeed(timeline.timeScale, speedSeekbar.max)
            applyTimeScale(timeline, speedSeekbar.progress)

            settingsCloseButton.setOnClickListener {
                binding.timelineControls.showSettings(false, binding)
            }
        }
    }

    /**
     * Slider steps are tenths of a multiplier, offset by one so step 0 is .1x rather than a frozen
     * 0x: at a scale of zero the timeline would not advance at all, which reads as a hang.
     */
    private fun speedForProgress(progress: Int): Double = (progress + 1) / 10.0

    private fun progressForSpeed(speed: Double, max: Int): Int =
        ((speed * 10).roundToInt() - 1).coerceIn(0, max)

    private fun applyTimeScale(timeline: Timeline, progress: Int) {
        val speed = speedForProgress(progress)
        timeline.timeScale = speed
        settingsPanelBinding.speedValueText.text = String.format(Locale.US, "%.1fx", speed)
    }

    fun setAnimations(context: Context, binding: TimelineBinding) {
        slideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom)
        slideInAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom)

        slideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                binding.timelineConstraintLayout.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        slideInAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                binding.timelineConstraintLayout.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // [timelineVisibility] starts false; XML usually has the chrome visible. Sync so the first
        // [show] call does not run a spurious slide-in while the strip is already on screen.
        timelineVisibility = binding.timelineConstraintLayout.isVisible
    }

    fun setConfigAnimations(context: Context, binding: TimelineBinding) {
        floatingMapButtons = listOf(binding.timelineLeftButtonColumn, binding.locationButton)
        val panel = settingsPanelBinding.root
        settingsSlideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom_settings)
        settingsSlideInAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom_settings)
        settingsSlideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                panel.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        settingsSlideInAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                panel.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    fun show(show: Boolean = true, binding: TimelineBinding, animated: Boolean = true) {
        if (show == timelineVisibility) return

        binding.timelineConstraintLayout.clearAnimation()
        if (!animated) {
            binding.timelineConstraintLayout.visibility =
                if (show) View.VISIBLE else View.INVISIBLE
            timelineVisibility = show
            return
        }

        if (show) {
            binding.timelineConstraintLayout.startAnimation(slideInAnimation)
        } else {
            binding.timelineConstraintLayout.startAnimation(slideOutAnimation)
        }
        timelineVisibility = show
    }

    /**
     * The map buttons that float over the bottom-left of the map. They live in the timeline strip,
     * which now draws above the settings panel so the play controls stay on top of it - and that
     * lifts these over the panel too, where the panel used to cover them. Hide them for as long as
     * it is open.
     *
     * Restored to whatever they were rather than to visible: [location_button] and the back arrow
     * are both switched on per demo, so a blanket restore would reveal buttons that screen never had.
     */
    private var floatingMapButtons: List<View> = emptyList()
    private var hiddenForSettingsPanel: List<View> = emptyList()

    /** INVISIBLE, not GONE: [location_button] is constrained to the column and would move with it. */
    private fun setFloatingMapButtonsHidden(hidden: Boolean) {
        if (hidden) {
            hiddenForSettingsPanel = floatingMapButtons.filter { it.isVisible }
            hiddenForSettingsPanel.forEach { it.visibility = View.INVISIBLE }
        } else {
            hiddenForSettingsPanel.forEach { it.visibility = View.VISIBLE }
            hiddenForSettingsPanel = emptyList()
        }
    }

    fun showSettings(show: Boolean = true, binding: TimelineBinding) {
        setFloatingMapButtonsHidden(show)
        val panel = settingsPanelBinding.root
        if (show) {
            panel.visibility = View.VISIBLE
            panel.startAnimation(settingsSlideInAnimation)
        } else if (panel.isVisible) {
            panel.startAnimation(settingsSlideOutAnimation)
        }
    }

    fun isSettingsPanelVisible(): Boolean =
        ::settingsPanelBinding.isInitialized && settingsPanelBinding.root.isVisible

    /** Closes the start/end settings sheet when it is open. */
    fun dismissSettingsIfVisible(binding: TimelineBinding): Boolean {
        if (!isSettingsPanelVisible()) return false
        showSettings(false, binding)
        return true
    }

    /**
     * Dismisses the settings sheet on map tap without consuming the gesture (returns false from the listener).
     * [onMapTap] runs after a dismiss attempt on [MotionEvent.ACTION_UP].
     */
    @SuppressLint("ClickableViewAccessibility")
    fun installDismissSettingsOnMapTap(
        mapView: View,
        timelineBinding: TimelineBinding,
        onMapTap: ((MotionEvent) -> Unit)? = null,
    ) {
        mapView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                dismissSettingsIfVisible(timelineBinding)
                onMapTap?.invoke(event)
            }
            false
        }
    }

    /** Adjust Timeline Controls bottom padding based on system navigation settings**/
    fun adjustPaddingForNavigation(constraintLayout: ConstraintLayout) {
        val originalBottomPadding = constraintLayout.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(constraintLayout) { view, windowInsets ->
            val navBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = originalBottomPadding + navBarInsets.bottom)
            windowInsets
        }
    }

}
