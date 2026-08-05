package com.example.mapsgldemo.maplayers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.example.mapsgldemo.R
import com.xweather.mapsgl.weather.WeatherConfiguration
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("ViewConstructor")
class LayerButtonView(context: Context, title: String, val configuration: WeatherConfiguration, status: Int = 0) :
    LinearLayout(context) {

    val code = configuration.code.toString()

    val text: String
        get() = textView.text.toString()

    private val textView: TextView
    var active = false

    companion object {
        private lateinit var slideOutAnimation: Animation
        private lateinit var slideInAnimation: Animation
        private var layerButtonVisibility = true

        fun isTablet(context: Context): Boolean {
            val metrics = context.resources.displayMetrics
            val widthInches = metrics.widthPixels / metrics.xdpi
            val heightInches = metrics.heightPixels / metrics.ydpi
            val diagonalInches = sqrt(widthInches.toDouble().pow(2.0) + heightInches.toDouble().pow(2.0))
            return diagonalInches >= 7.0 // 7 inches is a common cutoff for tablets
        }

        fun setAnimations(menuLinearLayout: LinearLayout) {

            // Sync our cached "is the menu currently shown" flag to the actual View state for this
            // activity's layout. The flag is a process-wide static, so when MainActivity is finished
            // and recreated (e.g. via a stencil demo → back → relaunch MainActivity) the value carries
            // over from the previous instance while the fresh layout starts in its XML default state
            // (VISIBLE). Without this sync, showDatasetButtons(false, ...) would early-return
            // because the static says "already hidden", leaving the menu stuck open.
            layerButtonVisibility = menuLinearLayout.visibility == View.VISIBLE

            val context = menuLinearLayout.context
            slideInAnimation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            slideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_left)

            slideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    menuLinearLayout.visibility = View.INVISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })

            slideInAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    menuLinearLayout.visibility = View.VISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }

        fun showDatasetButtons(show: Boolean = true, layerMenu: LinearLayout, layerButton: ImageView) {
            if (show != layerButtonVisibility) {
                if (show) {
                    layerMenu.startAnimation(slideInAnimation)
                    //layerButton.startAnimation(slideOutAnimation)
                    //layerButton.visibility=View.INVISIBLE
                } else {
                    layerMenu.startAnimation(slideOutAnimation)
                    //layerButton.startAnimation(slideInAnimation)
                    layerButton.visibility = View.VISIBLE
                }
                layerButtonVisibility = show
            }
        }

        fun createHeadingTextView(text: String, context: Context): View {
            val density = context.resources.displayMetrics.density
            val textView = TextView(context)
            textView.text = text
            textView.textSize = 17f
            textView.setTypeface(null, Typeface.BOLD)
            textView.setBackgroundResource(R.drawable.unselected_background)
            textView.setTextColor(ContextCompat.getColor(context, R.color.layer_section_heading))
            textView.setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
            textView.setPadding(
                (40 * density).toInt(),
                (6 * density).toInt(),
                (8 * density).toInt(),
                (2 * density).toInt(),
            )
            return textView
        }
    }

    val outerView = LinearLayout(context).apply {
        val density = context.resources.displayMetrics.density
        layoutParams = LayoutParams(
            //LayoutParams.WRAP_CONTENT,
            (250 * density).toInt(),
            (50 * density).toInt() // resources.getDimensionPixelSize(R.dimen.outer_view_height)
        )
        setBackgroundResource(R.drawable.unselected_background)
        orientation = HORIZONTAL
        setPadding(8, 4, 8, 4) // Set equal padding for top and bottom
        gravity = Gravity.CENTER_VERTICAL // Center vertically

        textView = TextView(context).apply {
            //id = R.id.checkTextbox
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            //setTypeface(null, Typeface.BOLD)
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

    /** Highlight button when selected **/
    fun activate() {
        outerView.setBackgroundResource(R.drawable.selected_background)
        textView.setShadowLayer(10f, 0f, 0f, 0xFFFFFFFF.toInt())
        textView.setTextColor(ContextCompat.getColor(context, R.color.selected_button_text))
        active = true
    }

    /** Remove highlight on button when unselected **/
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
