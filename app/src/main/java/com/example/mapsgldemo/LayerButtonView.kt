package com.example.mapsgldemo

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.xweather.mapsgl.config.weather.WeatherService
import kotlin.math.pow
import kotlin.math.sqrt

class LayerButtonView(
    context: Context,
    title: String,
    val configuration: WeatherService.WeatherLayerConfiguration,
    status: Int = 0
) : LinearLayout(context) {
    private val textView: TextView
    var active = false

    companion object {
        lateinit var slideOutAnimation: Animation
        lateinit var slideInAnimation: Animation
        var datasetVisibility = true


        fun isTablet(context: Context): Boolean {
            val metrics = context.resources.displayMetrics
            val widthInches = metrics.widthPixels / metrics.xdpi
            val heightInches = metrics.heightPixels / metrics.ydpi
            val diagonalInches = sqrt(widthInches.toDouble().pow(2.0) + heightInches.toDouble().pow(2.0))
            return diagonalInches >= 7.0 // 7 inches is a common cutoff for tablets
        }

        fun setAnimations(scrollView: ScrollView) {
            val context = scrollView.context
            slideInAnimation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            slideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_left)

            slideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    scrollView.visibility = View.INVISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })

            slideInAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    scrollView.visibility = View.VISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }

        fun showDatasetButtons(show: Boolean = true, scrollView: ScrollView, layerButton: ImageView) {
            if (show != datasetVisibility) {
                if (show) {
                    scrollView.startAnimation(slideInAnimation)
                } else {
                    scrollView.startAnimation(slideOutAnimation)
                    layerButton.visibility = View.VISIBLE
                }
                datasetVisibility = show
            }
        }

        fun createHeadingTextView(text: String, context: Context): View {
            val textView = TextView(context)
            textView.text = text
            textView.textSize = 20f
            textView.setBackgroundResource(R.drawable.unselected_background)
            //textView.setBackgroundColor(0xFF373737.toInt())
            textView.setTextColor(ContextCompat.getColor(context, R.color.bright_text))
            textView.setShadowLayer(10f, 0f, 0f, android.graphics.Color.BLACK)
            textView.setPadding(40, 60, 0, 40)
            return textView
        }
    }

    val outerView = LinearLayout(context).apply {
        val density = context.resources.displayMetrics.density
        layoutParams = LayoutParams(
            //LayoutParams.WRAP_CONTENT,
            (170 * density).toInt(),
            (50 * density).toInt() // resources.getDimensionPixelSize(R.dimen.outer_view_height)
        )
        setBackgroundResource(R.drawable.unselected_background)
        orientation = HORIZONTAL
        setPadding(8, 4, 8, 4) // Set equal padding for top and bottom
        gravity = Gravity.CENTER_VERTICAL // Center vertically

        textView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            setTypeface(null, Typeface.NORMAL)
            textSize = 14f
            setShadowLayer(10f, 0f, 0f, 0xFF000000.toInt())
            setTextColor(ContextCompat.getColor(context, R.color.bright_text))
            setPadding(40, 0, 8, 0)
        }
        addView(textView)
        val params = this.layoutParams as MarginLayoutParams
        params.setMargins(0, 0, 0, 0) //space around individual buttons
        this.layoutParams = params
        setTextColor(title, status)
    }

    fun click() {
        if (!active) {
            activate()
        } else {
            deactivate()
        }
    }

    fun activate() {
        outerView.setBackgroundResource(R.drawable.selected_background)
        textView.setShadowLayer(10f, 0f, 0f, 0xFFFFFFFF.toInt())
        textView.setTextColor(ContextCompat.getColor(context, R.color.selected_button_text))
        active = true
    }

    fun deactivate() {
        outerView.setBackgroundResource(R.drawable.unselected_background)
        textView.setShadowLayer(10f, 0f, 0f, 0xFF000000.toInt())
        textView.setTextColor(ContextCompat.getColor(context, R.color.bright_text))
        active = false
    }

    private fun setTextColor(title: String, status: Int = 0): LayerButtonView {
        textView.text = title
        if (status == 1) {
            textView.setTextColor(Color(1f, 1f, .75f).toArgb())
        } else if (status == 2) {
            textView.setTextColor(Color(1f, .75f, .75f).toArgb())
        }
        return this
    }
}
