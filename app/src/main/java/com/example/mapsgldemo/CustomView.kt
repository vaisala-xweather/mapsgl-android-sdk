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
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

class CustomView(context: Context, title: String, val id: String /*val classHolder: KClass<*>*/, /*service: WeatherService.WeatherLayerConfiguration,*/ status: Int = 0) : LinearLayout(context) {
    private val textView: TextView
    var active = false

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

        fun showDatasetButtons(show: Boolean = true, scrollView: ScrollView, layerButton: ImageView){
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

        fun createTextView(text: String, context: Context): View{
            val textView = TextView(context)
            textView.text = text
            textView.textSize = 16f
            textView.setTextColor(android.graphics.Color.WHITE)
            textView.setShadowLayer(10f, 0f,0f, android.graphics.Color.BLACK)
            textView.setPadding(32,0,0,0)
            return textView
        }
    }

    val outerView = LinearLayout(context).apply {
        val density = context.resources.displayMetrics.density
        layoutParams = LayoutParams(
            //LayoutParams.WRAP_CONTENT,
            (130*density).toInt(),
            (50*density).toInt() // resources.getDimensionPixelSize(R.dimen.outer_view_height)
        )
        setBackgroundResource(R.drawable.unselected_background)
        orientation = HORIZONTAL
        setPadding(8, 4, 8, 4) // Set equal padding for top and bottom
        gravity = Gravity.CENTER_VERTICAL // Center vertically

        textView = TextView(context).apply {
            id = R.id.checkTextbox
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            setTypeface(null, Typeface.BOLD)
            setShadowLayer(10f,0f,0f, 0xFF000000.toInt())
            setTextColor(ContextCompat.getColor(context, R.color.button_text))
            setPadding(8, 8, 8, 8)
        }
        addView(textView)
        val params = this.layoutParams as MarginLayoutParams
        params.setMargins(8, 4, 4, 4)
        this.layoutParams = params

        setTextColor(title, status)
    }

    fun click(){
        active = !active
        if(active){
            outerView.setBackgroundResource(R.drawable.selected_background)
            textView.setShadowLayer(10f,0f,0f, 0xFFFFFFFF.toInt())
            textView.setTextColor(ContextCompat.getColor(context, R.color.selected_button_text))

        } else{
            outerView.setBackgroundResource(R.drawable.unselected_background)
            textView.setShadowLayer(10f,0f,0f, 0xFF000000.toInt())
            textView.setTextColor(ContextCompat.getColor(context, R.color.button_text))
        }
    }

    private fun setTextColor(title: String, status: Int = 0): CustomView {
        textView.text = title
        if(status==1){
            textView.setTextColor(-65) //yellow text
        }else if (status==2){
            textView.setTextColor(-16449) //red text
        }

        //println ("Color(1f,1f,.75f).toArgb(): ${Color(1f,1f,.75f).toArgb()}    Color(1f,.75f,.75f).toArgb(): ${Color(1f,.75f,.75f).toArgb()}   ")
        return this
    }
}
