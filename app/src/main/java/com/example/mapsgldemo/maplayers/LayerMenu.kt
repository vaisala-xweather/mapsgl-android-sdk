package com.example.mapsgldemo.maplayers

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.VectorSourceDescriptor
import com.xweather.mapsgl.types.LayerType
import com.xweather.mapsgl.weather.CompositeWeatherLayerConfiguration
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherConfiguration
import com.xweather.mapsgl.weather.WeatherLayerConfiguration
import com.xweather.mapsgl.weather.WeatherService
import java.util.regex.Pattern

/** True when the product is backed by MapsGL/AMP vector tiles ([VectorSourceDescriptor]). */
internal fun WeatherConfiguration.usesVectorSource(): Boolean {
    return when (this) {
        is CompositeWeatherLayerConfiguration ->
            layers.any { it.usesVectorSource() }

        is WeatherLayerConfiguration<*, *> ->
            source is VectorSourceDescriptor

        else -> false
    }
}

/** Primary [LayerType] used to group a product in the vector layer browser menu. */
internal fun WeatherConfiguration.menuGroupLayerType(): LayerType? {
    return when (this) {
        is WeatherLayerConfiguration<*, *> ->
            if (source is VectorSourceDescriptor) layer.type else null

        is CompositeWeatherLayerConfiguration ->
            layers
                .filterIsInstance<WeatherLayerConfiguration<*, *>>()
                .firstOrNull { it.source is VectorSourceDescriptor }
                ?.layer
                ?.type

        else -> null
    }
}

private val VECTOR_MENU_TYPE_ORDER = listOf(
    LayerType.fill,
    LayerType.line,
    LayerType.circle,
    LayerType.symbol,
    LayerType.heatmap,
)

private fun layerTypeMenuHeading(type: LayerType): String =
    type.value.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

class LayerMenu {

    var visible = true
    /** Called when a layer row is toggled on or off in the menu. */
    var onLayerToggleListener: ((LayerButtonView, active: Boolean) -> Unit)? = null
    private val buttonList: MutableList<View> = mutableListOf() // Changed name for clarity
    private lateinit var filterEditText: EditText // Keep reference for filtering
    private lateinit var itemsContainerLayout: LinearLayout
    var roadLayerId: String? = null

    /**  Create menu buttons for all the available layers (or vector-tile products only). **/
    fun createLayerButtons(
        service: WeatherService,
        layout: LinearLayout,
        vectorLayersOnly: Boolean = false,
        vectorLayerType: LayerType? = null,
    ) {
        val context = layout.context
        buttonList.clear()

        fun makeButton(code: LayerCode, configuration: WeatherConfiguration): LayerButtonView =
            LayerButtonView(context, code.value, configuration)

        val vectorOnly = vectorLayersOnly || vectorLayerType != null

        fun addButtonFor(code: LayerCode) {
            val configuration = LayerCode.getConfigurationForLayerCode(code, service)
            if (vectorOnly && !configuration.usesVectorSource()) return
            if (vectorLayerType != null && configuration.menuGroupLayerType() != vectorLayerType) return
            buttonList.add(makeButton(code, configuration))
        }

        if (vectorOnly) {
            if (vectorLayerType != null) {
                val typedButtons = mutableListOf<LayerButtonView>()
                LayerCode.entries.forEach { code ->
                    val configuration = LayerCode.getConfigurationForLayerCode(code, service)
                    if (!configuration.usesVectorSource()) return@forEach
                    if (configuration.menuGroupLayerType() != vectorLayerType) return@forEach
                    typedButtons.add(makeButton(code, configuration))
                }
                buttonList.addAll(typedButtons.sortedBy { it.text.lowercase() })
            } else {
                val byType = mutableMapOf<LayerType, MutableList<LayerButtonView>>()
                LayerCode.entries.forEach { code ->
                    val configuration = LayerCode.getConfigurationForLayerCode(code, service)
                    if (!configuration.usesVectorSource()) return@forEach
                    val type = configuration.menuGroupLayerType() ?: return@forEach
                    byType.getOrPut(type) { mutableListOf() }.add(makeButton(code, configuration))
                }

                fun appendTypeSection(type: LayerType, buttons: List<LayerButtonView>) {
                    if (buttons.isEmpty()) return
                    buttonList.add(LayerButtonView.createHeadingTextView(layerTypeMenuHeading(type), context))
                    buttonList.addAll(buttons.sortedBy { it.text.lowercase() })
                }

                for (type in VECTOR_MENU_TYPE_ORDER) {
                    byType.remove(type)?.let { appendTypeSection(type, it) }
                }
                byType.keys.sortedBy { it.value }.forEach { type ->
                    appendTypeSection(type, byType[type].orEmpty())
                }
            }
        } else {
            LayerCode.entries.forEach {
                if (it.value.contains("temper")
                    || it.value.contains("hum")
                    || it.value.contains("prec")
                    || it.value.contains("wind-spe")
                ) {
                    addButtonFor(it)
                }
            }
            LayerCode.entries.forEach { addButtonFor(it) }
        }


        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0,
                1f,
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
                        roadLayerId = roadLayerId ?: getRoadLayerId(controller)
                        controller.addWeatherLayer(customView.configuration, beforeId = roadLayerId)
                        onLayerToggleListener?.invoke(customView, true)
                    } else {
                        customView.deactivate()
                        //controller.setWeatherLayerVisibility(layerCode, false) //hide the layer
                        controller.removeWeatherLayer(layerCode) //fully remove the layer
                        onLayerToggleListener?.invoke(customView, false)
                    }
                }
            }
        }
    }

    fun hideKeyboard(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(filterEditText.windowToken, 0)
    }

    /**  Find the first road/tunnel/bridge layer in the MapboxMap **/
    private fun getRoadLayerId(controller: MapboxMapController): String? {
        var foundId: String? = null
        controller.mapboxMap?.getStyle { style -> // Ensure style is loaded
            val roadLayerRegex = "^(?:tunnel|road|bridge)-"
            for (layerInfo in style.styleLayers) {
                if (Pattern.compile(roadLayerRegex).matcher(layerInfo.id).find()) {
                    foundId = layerInfo.id
                    break
                }
            }
        }
        return foundId
    }
}
