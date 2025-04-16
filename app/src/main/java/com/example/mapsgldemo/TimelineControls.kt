package com.example.mapsgldemo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.anim.Timeline

class TimelineControls(context: Context, attrs: AttributeSet? = null) :
    androidx.appcompat.widget.AppCompatSeekBar(context, attrs) {

    var fromTouch = false
    private lateinit var slideOutAnimation: Animation
    private lateinit var slideInAnimation: Animation
    private var timelineVisibility: Boolean = false

    // This must match the seekbar's "max" setting, whether in the XML or if it is set programmatically.
    private val seekbarRange = 10000.0
    var seekbarDoubleValue = 0.0

    /** Set the position of the time seekbar from 0F to 1F **/
    fun setPosition(position: Double) {
        this.progress = (position * seekbarRange).toInt()
    }

    fun setupSeekBarChangeListener(binding: ActivityMainBinding, timeline: Timeline, onChange: (progress: Int) -> Unit) {
        binding.timelineView.timelineControls.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUserTouch: Boolean) {
                seekbarDoubleValue = progress.toDouble() / seekbarRange
                if (fromUserTouch) {
                    //if (timeline.state == AnimationState.playing) {
                    if (timeline.state == AnimationState.playing) {
                        binding.timelineView.timelineControls.updatePlayButtonImage(true, binding)
                    }
                    timeline.goTo(binding.timelineView.timelineControls.seekbarDoubleValue)

                } else { // seekbar changed programmatically
                }
                TimelineTextFormatter.setCurrentTimeTextView(
                    binding.timelineView,
                    timeline,
                    binding.timelineView.timelineControls.seekbarDoubleValue
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                fromTouch = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                fromTouch = false
            }
        })
    }

    fun updatePlayButtonImage(isPlaying: Boolean, binding: ActivityMainBinding) {
        if (isPlaying) {
            binding.timelineView.playButtonImage.setImageResource(R.drawable.stop)
        } else {
            binding.timelineView.playButtonImage.setImageResource(R.drawable.play)
        }
    }

    fun setupButtonListeners(timeline: Timeline, binding: ActivityMainBinding) {

        binding.timelineView.playButton.setOnClickListener {
            timeline.play()
            if (timeline.state != AnimationState.playing && timeline.state !=AnimationState.initial) {
                timeline.stop()
                //updatePlayButtonImage(false, binding)
            } else {
                //updatePlayButtonImage(true, binding)
            }
        }

        binding.timelineView.intervalBackButton.setOnClickListener {
            timeline.prevInterval()
        }

        binding.timelineView.intervalForwardButton.setOnClickListener {
            timeline.nextInterval()
        }

        binding.timelineView.startPlusDayButton.setOnClickListener {
            timeline.setStartDateUsingOffset((24 * 3600 * 1000), timeline.start)
        }

        binding.timelineView.startMinusDayButton.setOnClickListener {
            timeline.setStartDateUsingOffset((-24 * 3600 * 1000), timeline.start)
        }

        binding.timelineView.startPlusHourButton.setOnClickListener {
            timeline.setStartDateUsingOffset((3600 * 1000), timeline.start)
        }

        binding.timelineView.startMinusHourButton.setOnClickListener {
            timeline.setStartDateUsingOffset((-3600 * 1000), timeline.start)
        }

        binding.timelineView.endPlusHourButton.setOnClickListener {
            timeline.setEndDateUsingOffset((3600 * 1000), timeline.end)
        }

        binding.timelineView.endMinusHourButton.setOnClickListener {
            timeline.setEndDateUsingOffset((-3600 * 1000), timeline.end)
        }

        binding.timelineView.endPlusDayButton.setOnClickListener {
            timeline.setEndDateUsingOffset((24 * 3600 * 1000), timeline.end)
        }

        binding.timelineView.endMinusDayButton.setOnClickListener {
            timeline.setEndDateUsingOffset((-24 * 3600 * 1000), timeline.end)
        }
    }

    fun setAnimations(context: Context, binding: ActivityMainBinding) {
        slideInAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom)
        slideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom)

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
    }

    fun show(show: Boolean = true, binding: ActivityMainBinding) {
        if (show != timelineVisibility) {
            if (show) {
                binding.timelineView.timelineConstraintLayout.startAnimation(slideInAnimation)
            } else {
                binding.timelineView.timelineConstraintLayout.startAnimation(slideOutAnimation)
            }
            timelineVisibility = show
        }
    }
}