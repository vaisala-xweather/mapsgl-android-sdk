package com.example.mapsgldemo

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import com.xweather.mapsgl.weather.WeatherService
import com.xweather.mapsgl.map.MapController
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.weather.LayerCode

class LayerMenu {

    var visible = true
    private val buttonList: MutableList<View> = mutableListOf() // Changed name for clarity
    private lateinit var filterEditText: EditText // Keep reference for filtering
    private lateinit var itemsContainerLayout: LinearLayout

    /**  Create menu buttons for all the available layers **/
    fun createLayerButtons(
        service: WeatherService,
        layout: LinearLayout,/* scrollView: ScrollView*/
    ) {
        val context = layout.context

        LayerCode.entries.forEach {
            if (!it.value.contains("lightning-density")) {
                buttonList.add(LayerButtonView(context, it.value, LayerCode.getConfigurationForLayerCode(it, service)))
            }
        }

        layout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, // Make ScrollView2 as wide as its parent 'layout'
            LinearLayout.LayoutParams.MATCH_PARENT  // Or 0dp and weight if 'layout' uses weights for height
        )

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT  // Or 0dp and weight if 'layout' uses weights for height
            )
            isFillViewport = true // Good to have, helps when content is shorter than ScrollView
        }

        itemsContainerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val layerFilter = LayerFilter(context, buttonList, itemsContainerLayout)
        filterEditText = layerFilter.editText
        layout.addView(filterEditText)
        layout.addView(scrollView)

        scrollView.removeAllViews()
        scrollView.addView(itemsContainerLayout)
        layerFilter.addAllViews(itemsContainerLayout)
    }

    fun setupButtonListeners(controller: MapboxMapController) {
        for (customView in buttonList) { //for each item created in createLayerButtons()
            if (customView is LayerButtonView) { // If is custom clickable button
                customView.outerView.setOnClickListener {
                    val layerCode = customView.configuration.code
                    if (!customView.active) {

                        customView.activate()

                        if (controller.hasWeatherLayer(layerCode)) {
                            controller.setWeatherLayerVisibility(layerCode, true)
                        } else {
                            controller.addWeatherLayer(customView.configuration)
                        }
                    } else {
                        customView.deactivate()
                        controller.setWeatherLayerVisibility(layerCode, false)
                    }
                }
            }
        }
    }

    fun hideKeyboard(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(filterEditText.windowToken, 0)
    }
}