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

        fun syncMenuShownState(layerMenu: LinearLayout) {
            layerButtonVisibility = layerMenu.visibility == View.VISIBLE
        }

        fun setAnimations(menuLinearLayout: LinearLayout) {
            // Process-wide static: resync to this layout so a leftover "already shown" flag
            // cannot make showDatasetButtons() no-op on a fresh (or still-hidden) menu.
            syncMenuShownState(menuLinearLayout)

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
                override fun onAnimationStart(animation: Animation?) {
                    menuLinearLayout.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animation?) {
                    menuLinearLayout.visibility = View.VISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }

        fun showDatasetButtons(show: Boolean = true, layerMenu: LinearLayout, layerButton: ImageView) {
            if (layerMenu.visibility == View.GONE) {
                layerButtonVisibility = show
                return
            }
            val menuShown = layerMenu.visibility == View.VISIBLE
            if (show == menuShown) {
                layerButtonVisibility = show
                return
            }
            layerMenu.clearAnimation()
            if (show) {
                layerMenu.visibility = View.VISIBLE
                layerMenu.startAnimation(slideInAnimation)
            } else {
                layerMenu.startAnimation(slideOutAnimation)
                layerButton.visibility = View.VISIBLE
            }
            layerButtonVisibility = show
        }

        fun createHeadingTextView(text: String, context: Context): View {
            val density = context.resources.displayMetrics.density
            val textView = TextView(context)
            textView.text = text
            // Xweather overline: small, Medium weight, uppercase, wide tracking, teal accent.
            textView.textSize = 12f
            textView.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textView.isAllCaps = true
            textView.letterSpacing = 0.08f
            textView.setBackgroundResource(R.drawable.unselected_background)
            textView.setTextColor(ContextCompat.getColor(context, R.color.layer_section_heading))
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
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.xw_text_primary))
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
        textView.setTextColor(ContextCompat.getColor(context, R.color.xw_text_inverse))
        active = true
    }

    /** Remove highlight on button when unselected **/
    fun deactivate() {
        outerView.setBackgroundResource(R.drawable.unselected_background)
        textView.setTextColor(ContextCompat.getColor(context, R.color.xw_text_primary))
        active = false
    }

    private fun setTextColor(title: String, status: Int = 0): LayerButtonView {
        textView.text = title
        if (status == 1) {
            textView.setTextColor(ContextCompat.getColor(context, R.color.xw_orange_500))
        } else if (status == 2) {
            textView.setTextColor(ContextCompat.getColor(context, R.color.xw_ember))
        }
        return this
    }
}
