package com.example.mapsgldemo.maplayers

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.VectorSourceDescriptor
import com.xweather.mapsgl.style.GridLayerPaint
import com.xweather.mapsgl.style.SymbolLayerPaint
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

private fun isLightningLayerButton(button: LayerButtonView): Boolean =
    button.code.contains("lightning", ignoreCase = true)

private fun isPlaceLayerButton(button: LayerButtonView): Boolean =
    button.code.contains("place", ignoreCase = true)

private fun WeatherLayerConfiguration<*, *>.layerPaintHasText(): Boolean =
    when (val paint = layer.paint) {
        is SymbolLayerPaint -> paint.text.isNotEmpty()
        is GridLayerPaint -> paint.text.isNotEmpty()
        else -> false
    }

internal fun WeatherConfiguration.hasSymbolText(): Boolean =
    when (this) {
        is WeatherLayerConfiguration<*, *> -> layerPaintHasText()
        is CompositeWeatherLayerConfiguration ->
            layers.any { (it as? WeatherConfiguration)?.hasSymbolText() == true }
        else -> false
    }

/** One layer row in a demo [LayerMenuSection] (optional display title overrides [LayerCode.value]). */
data class LayerMenuEntry(
    val code: LayerCode,
    val title: String? = null,
)

/** Headed group of layer buttons for demo allowlists (e.g. mapTime filter). */
data class LayerMenuSection(
    val heading: String,
    val entries: List<LayerMenuEntry>,
) {
    constructor(heading: String, vararg codes: LayerCode) : this(
        heading,
        codes.map { LayerMenuEntry(it) },
    )
}

class LayerMenu {

    var visible = false
    /** Called when a layer row is toggled on or off in the menu. */
    var onLayerToggleListener: ((LayerButtonView, active: Boolean) -> Unit)? = null
    private val buttonList: MutableList<View> = mutableListOf() // Changed name for clarity
    private lateinit var filterEditText: EditText // Keep reference for filtering
    private lateinit var itemsContainerLayout: LinearLayout
    var roadLayerId: String? = null

    fun layerButtons(): List<LayerButtonView> =
        buttonList.filterIsInstance<LayerButtonView>()

    fun activateLayerIfPresent(controller: MapboxMapController, code: LayerCode): Boolean {
        val button = layerButtons().firstOrNull { it.configuration.code == code } ?: return false
        if (button.active) return false
        button.activate()
        roadLayerId = roadLayerId ?: getRoadLayerId(controller)
        controller.addWeatherLayer(button.configuration, beforeId = roadLayerId)
        onLayerToggleListener?.invoke(button, true)
        return true
    }

    /**  Create menu buttons for all the available layers (or vector-tile products only). **/
    fun createLayerButtons(
        service: WeatherService,
        layout: LinearLayout,
        vectorLayersOnly: Boolean = false,
        vectorLayerType: LayerType? = null,
        vectorLayerTypes: Set<LayerType>? = null,
        lightningSectionFirst: Boolean = false,
        placesSectionFirst: Boolean = false,
        textLayersOnly: Boolean = false,
        layerMenuSections: List<LayerMenuSection>? = null,
    ) {
        val context = layout.context
        buttonList.clear()

        fun makeButton(code: LayerCode, configuration: WeatherConfiguration, title: String? = null): LayerButtonView =
            LayerButtonView(context, title ?: code.value, configuration)

        val vectorOnly = vectorLayersOnly || vectorLayerType != null || vectorLayerTypes != null

        fun matchesTypeFilter(configuration: WeatherConfiguration): Boolean {
            val effectiveTypes = vectorLayerTypes ?: vectorLayerType?.let { setOf(it) }
            if (effectiveTypes == null) return true
            return configuration.menuGroupLayerType() in effectiveTypes
        }

        fun addButtonFor(code: LayerCode, title: String? = null) {
            val configuration = LayerCode.getConfigurationForLayerCode(code, service)
            if (vectorOnly && !configuration.usesVectorSource()) return
            if (!matchesTypeFilter(configuration)) return
            buttonList.add(makeButton(code, configuration, title))
        }

        if (layerMenuSections != null) {
            for (section in layerMenuSections) {
                if (section.entries.isEmpty()) continue
                buttonList.add(LayerButtonView.createHeadingTextView(section.heading, context))
                section.entries.forEach { addButtonFor(it.code, it.title) }
            }
        } else if (textLayersOnly) {
            val textButtons = mutableListOf<LayerButtonView>()
            LayerCode.entries.forEach { code ->
                val configuration = LayerCode.getConfigurationForLayerCode(code, service)
                if (code.value.endsWith("-text")) return@forEach
                if (!configuration.hasSymbolText()) return@forEach
                textButtons.add(makeButton(code, configuration))
            }
            if (placesSectionFirst) {
                val placeButtons = textButtons.filter(::isPlaceLayerButton).sortedBy { it.text.lowercase() }
                val otherButtons = textButtons.filterNot(::isPlaceLayerButton).sortedBy { it.text.lowercase() }
                if (placeButtons.isNotEmpty()) {
                    buttonList.add(LayerButtonView.createHeadingTextView("Places", context))
                    buttonList.addAll(placeButtons)
                }
                if (otherButtons.isNotEmpty()) {
                    buttonList.add(LayerButtonView.createHeadingTextView("Other text", context))
                    buttonList.addAll(otherButtons)
                }
            } else {
                buttonList.addAll(textButtons.sortedBy { it.text.lowercase() })
            }
        } else if (vectorOnly) {
            if (vectorLayerType != null && vectorLayerTypes == null) {
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
                    if (!matchesTypeFilter(configuration)) return@forEach
                    val type = configuration.menuGroupLayerType() ?: return@forEach
                    byType.getOrPut(type) { mutableListOf() }.add(makeButton(code, configuration))
                }

                fun appendTypeSection(type: LayerType, buttons: List<LayerButtonView>) {
                    if (buttons.isEmpty()) return
                    buttonList.add(LayerButtonView.createHeadingTextView(layerTypeMenuHeading(type), context))
                    buttonList.addAll(buttons.sortedBy { it.text.lowercase() })
                }

                if (lightningSectionFirst) {
                    val lightningButtons = byType.values
                        .flatten()
                        .filter(::isLightningLayerButton)
                        .sortedBy { it.text.lowercase() }
                    if (lightningButtons.isNotEmpty()) {
                        buttonList.add(LayerButtonView.createHeadingTextView("Lightning", context))
                        buttonList.addAll(lightningButtons)
                        byType.values.forEach { buttons ->
                            buttons.removeAll { isLightningLayerButton(it) }
                        }
                    }
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
        if (!::filterEditText.isInitialized) return
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
