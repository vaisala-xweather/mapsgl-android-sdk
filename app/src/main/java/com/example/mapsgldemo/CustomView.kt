package com.example.mapsgldemo

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.xweather.mapsglmaps.config.weather.WeatherService

class CustomView(context: Context, title: String, service: WeatherService.WeatherLayerConfiguration, status: Int = 0) : LinearLayout(context) {
    val checkBox: CheckBox
    private val textView: TextView
    lateinit var service: WeatherService.WeatherLayerConfiguration

    companion object{
        lateinit var slideOutAnimation : Animation
        lateinit var slideInAnimation : Animation
        var datasetVisibility = true

        fun setAnimations(context: Context, scrollView: ScrollView){
            slideInAnimation  = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            slideOutAnimation  = AnimationUtils.loadAnimation(context, R.anim.slide_out_left)

            slideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    scrollView.visibility = View.GONE
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

        fun showLayerButtons(show: Boolean = true, scrollView: ScrollView, layerButton: ImageView){
            if(show != datasetVisibility){
                if(show){
                    scrollView.startAnimation(slideInAnimation)
                    layerButton.startAnimation(slideOutAnimation)
                    layerButton.isVisible=false
                } else{
                    scrollView.startAnimation(slideOutAnimation)
                    layerButton.startAnimation(slideInAnimation)
                    layerButton.isVisible=true
                }
                datasetVisibility = show
            }
        }
    }

    val outerView = LinearLayout(context).apply {
        val density = context.resources.displayMetrics.density
        layoutParams = LayoutParams(
            (150*density).toInt(),
            (50*density).toInt()
        )
        setBackgroundResource(R.drawable.grey_square_background)
        orientation = HORIZONTAL
        setPadding(8, 4, 8, 4) // Set equal padding for top and bottom
        gravity = Gravity.CENTER_VERTICAL // Center vertically

        checkBox = CheckBox(context).apply {
            id = R.id.check
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8
            }
            isChecked = false
            buttonTintList = ContextCompat.getColorStateList(context, R.color.button_text)
            setShadowLayer(10f,0f,0f, 0xFF000000.toInt())
            setPadding(8, 8, 0, 8)
        }
        addView(checkBox)

        textView = TextView(context).apply {
            id = R.id.checkTextbox
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            setTypeface(null, Typeface.BOLD)
            setShadowLayer(10f,0f,0f, 0xFF000000.toInt())
            setTextColor(ContextCompat.getColor(context, R.color.button_text))
            setPadding(0, 8, 8, 8)
        }
        addView(textView)
        val params = this.layoutParams as MarginLayoutParams
        params.setMargins(8, 4, 4, 4)
        this.layoutParams = params
        setOnClickListener { checkBox.isChecked = !checkBox.isChecked }
        setService(title, service, status)
    }

    fun setService(title: String, service: WeatherService.WeatherLayerConfiguration, status: Int = 0): CustomView {
        this.service = service
        textView.text = title
        if(status==1){
            textView.setTextColor(Color(1f,1f,.75f).toArgb())
        }else if (status==2){
            textView.setTextColor(Color(1f,.75f,.75f).toArgb())
        }
        return this
    }
}
