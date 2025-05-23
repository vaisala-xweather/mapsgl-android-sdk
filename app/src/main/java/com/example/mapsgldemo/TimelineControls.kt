package com.example.mapsgldemo

import android.content.Context
import android.util.AttributeSet
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
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.anim.Timeline
import com.xweather.mapsgl.map.mapbox.MapboxMapController

class TimelineControls(context: Context, attrs: AttributeSet? = null) :
    androidx.appcompat.widget.AppCompatSeekBar(context, attrs) {

    var fromTouch = false
    private lateinit var slideOutAnimation: Animation
    private lateinit var slideInAnimation: Animation
    private lateinit var settingsSlideOutAnimation: Animation
    private lateinit var settingsSlideInAnimation: Animation
    private var timelineVisibility: Boolean = false
    private var initialSpeedButtonSet = false

    private val seekbarRange = 10000.0
    var seekbarDoubleValue = 0.0

    /** Set the position of the time seekbar from 0F to 1F **/
    fun setPosition(position: Double) {
        println("TimelineControls TimeMove setPosition($position)")
        this.progress = (position * seekbarRange).toInt()
    }

    fun setupSeekBarChangeListener(
        binding: ActivityMainBinding,
        timeline: Timeline,
        onChange: (progress: Int) -> Unit
    ) {
        binding.timelineView.timelineControls.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUserTouch: Boolean) {
                seekbarDoubleValue = progress.toDouble() / seekbarRange
                if (fromUserTouch) {

                    if (timeline.state == AnimationState.playing) {
                        binding.timelineView.timelineControls.updatePlayButtonImage(true, binding)
                    }
                    timeline.goTo(binding.timelineView.timelineControls.seekbarDoubleValue)

                } else { // seekbar changed programmatically
                }
                TimelineTextFormatter.setCurrentTimeTextView(
                    binding.timelineView,
                    timeline,
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                binding.timelineView.timelineControls.showSettings(false, binding)
                fromTouch = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                fromTouch = false
            }
        })
    }

    fun updatePlayButtonImage(isPlaying: Boolean, binding: ActivityMainBinding) {
        if (isPlaying) {
            binding.timelineView.playButtonImage.setImageResource(R.drawable.pause_button_image)
        } else {
            binding.timelineView.playButtonImage.setImageResource(R.drawable.play_button_image)
        }
    }

    fun setupButtonListeners(timeline: Timeline, binding: ActivityMainBinding) {
        with(binding.timelineView) {

            playButton.setOnClickListener {
                timeline.play()
                if (timeline.state != AnimationState.playing && timeline.state != AnimationState.initial) {
                    timeline.stop()
                } else {
                    timelineControls.showSettings(false, binding)
                }
            }

            configButton.setOnClickListener {
                if (settingsCS.isVisible) {
                    timelineControls.showSettings(false, binding)
                } else {
                    timelineControls.showSettings(true, binding)
                }
            }

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
                activateSpeedButton(binding, speedQuarterButton)
            }

            speedHalfButton.setOnClickListener {
                timeline.timeScale = .5
                activateSpeedButton(binding, speedHalfButton)
            }

            speedOneButton.setOnClickListener {
                timeline.timeScale = 1.0
                activateSpeedButton(binding, speedOneButton)
            }

            speedTwoButton.setOnClickListener {
                timeline.timeScale = 2.0
                activateSpeedButton(binding, speedTwoButton)
            }

            settingsCloseButton.setOnClickListener {
                timelineControls.showSettings(false, binding)
            }
        }
    }

    private fun activateSpeedButton(binding: ActivityMainBinding, selectedButton: Button) {
        binding.timelineView.speedQuarterButton.setBackgroundResource(R.drawable.button_background_selector)
        binding.timelineView.speedHalfButton.setBackgroundResource(R.drawable.button_background_selector)
        binding.timelineView.speedOneButton.setBackgroundResource(R.drawable.button_background_selector)
        binding.timelineView.speedTwoButton.setBackgroundResource(R.drawable.button_background_selector)
        selectedButton.setBackgroundResource(R.drawable.button_background_pressed)
    }

    fun setAnimations(context: Context, binding: ActivityMainBinding) {
        slideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom)
        slideInAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom)

        slideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                binding.timelineView.timelineConstraintLayout.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        slideInAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                binding.timelineView.timelineConstraintLayout.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        setConfigAnimations(context, binding)
    }

    private fun setConfigAnimations(context: Context, binding: ActivityMainBinding) {
        settingsSlideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom_settings)
        settingsSlideInAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom_settings)
        settingsSlideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                binding.timelineView.settingsCS.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        settingsSlideInAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                binding.timelineView.settingsCS.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    fun show(show: Boolean = true, binding: ActivityMainBinding) {
        if (!initialSpeedButtonSet) {
            activateSpeedButton(binding, binding.timelineView.speedOneButton)
            initialSpeedButtonSet = true
        }

        if (show != timelineVisibility) {
            if (show) {
                binding.timelineView.timelineConstraintLayout.startAnimation(slideInAnimation)
            } else {

                binding.timelineView.timelineConstraintLayout.startAnimation(slideOutAnimation)
            }
            timelineVisibility = show
        }
    }

    fun showSettings(show: Boolean = true, binding: ActivityMainBinding) {
        if (show) {
            binding.timelineView.settingsCS.startAnimation(settingsSlideInAnimation)
        } else if (binding.timelineView.settingsCS.isVisible) {
            binding.timelineView.settingsCS.startAnimation(settingsSlideOutAnimation)
        }

    }

    fun setupTimelineListeners(controller: MapboxMapController, binding: ActivityMainBinding) {

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
            TimelineTextFormatter.setTimeTextViews(binding.timelineView, controller.timeline)
            binding.timelineView.timelineControls.updatePlayButtonImage(false, binding)
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
