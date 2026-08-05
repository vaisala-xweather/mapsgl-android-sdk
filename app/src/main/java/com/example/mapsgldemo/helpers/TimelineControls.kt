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
import android.widget.Button
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

class TimelineControls(context: Context, attrs: AttributeSet? = null) :
    androidx.appcompat.widget.AppCompatSeekBar(context, attrs) {

    var fromTouch = false
    private lateinit var slideOutAnimation: Animation
    private lateinit var slideInAnimation: Animation
    private lateinit var settingsSlideOutAnimation: Animation
    private lateinit var settingsSlideInAnimation: Animation
    private var timelineVisibility: Boolean = false
    private val seekbarRange = 10000.0
    private var initialSpeedButtonSet = false
    var seekbarDoubleValue = 0.0

    /** 0..1 along the track when wall-clock now lies within [timeline start, end]; null hides the marker. */
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
     * Shows a small marker on the track when wall-clock time is within the timeline range (inclusive).
     */
    fun setNowMarkerFromTimeline(start: Date, end: Date) {
        nowMarkerStartMs = start.time
        nowMarkerEndMs = end.time
        recomputeNowMarkerFraction()
        scheduleNowMarkerRefresh()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(refreshNowMarkerRunnable)
        super.onDetachedFromWindow()
    }

    private fun recomputeNowMarkerFraction() {
        val span = nowMarkerEndMs - nowMarkerStartMs
        val nowMs = System.currentTimeMillis()
        val fraction =
            if (span <= 0L || nowMs < nowMarkerStartMs || nowMs > nowMarkerEndMs) {
                null
            } else {
                ((nowMs - nowMarkerStartMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
            }
        if (fraction != nowMarkerFraction) {
            nowMarkerFraction = fraction
            invalidate()
        }
        if (fraction == null) {
            removeCallbacks(refreshNowMarkerRunnable)
        }
    }

    private fun scheduleNowMarkerRefresh() {
        removeCallbacks(refreshNowMarkerRunnable)
        if (nowMarkerFraction != null) {
            postDelayed(refreshNowMarkerRunnable, 60_000L)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val fraction = nowMarkerFraction ?: return
        val x = trackXForFraction(fraction)
        val notchTop = paddingTop + height * 0.28f
        val notchBottom = height - paddingBottom - height * 0.28f
        nowMarkerPaint.strokeWidth = dp(3f)
        canvas.drawLine(x, notchTop, x, notchBottom, nowMarkerPaint)
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

            speedQuarterButton.setOnClickListener {
                timeline.timeScale = .25
                activateSpeedButton(s, speedQuarterButton)
            }

            speedHalfButton.setOnClickListener {
                timeline.timeScale = .5
                activateSpeedButton(s, speedHalfButton)
            }

            speedOneButton.setOnClickListener {
                timeline.timeScale = 1.0
                activateSpeedButton(s, speedOneButton)
            }

            speedTwoButton.setOnClickListener {
                timeline.timeScale = 2.0
                activateSpeedButton(s, speedTwoButton)
            }

            settingsCloseButton.setOnClickListener {
                binding.timelineControls.showSettings(false, binding)
            }
        }
    }

    private fun activateSpeedButton(settings: TimelineSettingsPanelBinding, selectedButton: Button) {
        settings.speedQuarterButton.setBackgroundResource(R.drawable.button_background_selector)
        settings.speedHalfButton.setBackgroundResource(R.drawable.button_background_selector)
        settings.speedOneButton.setBackgroundResource(R.drawable.button_background_selector)
        settings.speedTwoButton.setBackgroundResource(R.drawable.button_background_selector)
        selectedButton.setBackgroundResource(R.drawable.button_background_pressed)
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

    fun setConfigAnimations(context: Context, @Suppress("UNUSED_PARAMETER") binding: TimelineBinding) {
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
        if (!initialSpeedButtonSet) {
            activateSpeedButton(settingsPanelBinding, settingsPanelBinding.speedOneButton)
            initialSpeedButtonSet = true
        }

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

    fun showSettings(show: Boolean = true, binding: TimelineBinding) {
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
