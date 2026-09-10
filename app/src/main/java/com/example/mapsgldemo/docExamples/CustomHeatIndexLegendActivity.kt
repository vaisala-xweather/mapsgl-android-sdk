package com.example.mapsgldemo.docExamples

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityCustomHeatIndexLegendBinding
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.controls.legend.LegendControl
import com.xweather.mapsgl.controls.legend.bar.BarLegend
import com.xweather.mapsgl.controls.legend.bar.BarLegendLabels
import com.xweather.mapsgl.extensions.UnitTemperature
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Customizing the heat index legend.**
 *
 * Android port of the JS example
 * [Customizing the heat index legend](https://www.xweather.com/docs/mapsgl/examples/custom-heat-index-legend).
 * The `heat-index` layer ships with a legend labelled every 10 degrees; five seconds after load
 * that is replaced with three qualitative labels positioned across the bar - Uncomfortable, Hot,
 * Dangerous - exactly as the JS example does.
 *
 * ### Getting hold of the legend
 *
 * The JS example reaches the legend through the controller's control registry:
 *
 * ```js
 * controller.addLegendControl('#legend');
 * const legend = controller.controls.legend.getLegend('heat-index');
 * ```
 *
 * On Android you own the [LegendControl] instance, hand it to the controller, and add its view
 * yourself; [LegendControl.getLegend] then takes the same id string:
 *
 * ```kotlin
 * controller.add(legendControl)
 * binding.root.addView(legendControl.getView())
 * val legend = legendControl.getLegend("heat-index")
 * ```
 *
 * The id is [com.xweather.mapsgl.weather.LegendCode.HEAT_INDEX]'s `id`, which is the same
 * `"heat-index"` string the JS SDK uses.
 *
 * A legend only exists once its layer has been added, so this waits for
 * [MapboxMapController.addWeatherLayer] before looking it up.
 *
 * ### Replacing the labels
 *
 * JS passes a partial update describing the new labels:
 *
 * ```js
 * legend.update({ bar: { labels: {
 *     normalized: true,
 *     values: [{ value: 0, label: 'Uncomfortable' }, … ],
 *     marks: 'none'
 * } } });
 * ```
 *
 * Android has no partial-update dictionary. The equivalent is to reach into the legend's
 * [BarLegend.items] and set [BarLegendLabels.values] to a
 * [BarLegendLabels.Values.FromLabels] - the variant whose accessor returns `(position, label)`
 * pairs, which is the direct counterpart of the JS `values` array - then hand the legend back to
 * [LegendControl.update].
 *
 * Two things worth knowing before copying this:
 *
 * 1. **`FromLabels` positions are always bar fractions.** The renderer uses the pair's first
 *    element directly as a `0..1` position across the bar, so this variant pairs with
 *    `normalized = true` and values in that range. Raw data values belong in
 *    [BarLegendLabels.Values.FromValues], which converts them through the colour ramp instead.
 * 2. **`marks: 'none'` has no counterpart, and needs none.** The Android bar legend draws the
 *    colour ramp and the label text and nothing else - there are no tick marks to switch off - so
 *    the JS example's third setting is simply the Android default.
 *
 * Launched from [DocsExamplesMenuActivity].
 */
class CustomHeatIndexLegendActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomHeatIndexLegendBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private var mapLoadedCancelable: Cancelable? = null

    private val legendControl = LegendControl()
    private val handler = Handler(Looper.getMainLooper())
    private var customLabelsApplied = false

    /** Captured before the first customization so the toggle can put the default back. */
    private var defaultValues: BarLegendLabels.Values<UnitTemperature>? = null
    private var defaultNormalized: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomHeatIndexLegendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.customHeatIndexLegendMapView
        binding.customHeatIndexLegendNote.text = NOTE_WAITING

        binding.customHeatIndexLegendBackButton.setOnClickListener { returnToMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = returnToMenu()
        })
        binding.customHeatIndexLegendToggleButton.setOnClickListener {
            applyLabels(custom = !customLabelsApplied)
        }

        val account = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (mapView.parent == null) return

                controller = MapboxMapController(mapView, account)
                mapboxMap = controller.mapboxMap

                // Flat before the first frame, like the other demo screens.
                mapboxMap?.setProjection(projection(ProjectionName.MERCATOR))

                // The JS example's center: [-93, 34], zoom: 3. Coordinate is (lat, lon).
                controller.setCenter(Coordinate(34.0, -93.0))
                controller.setZoom(3.0)

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    attachLegend()
                    controller.addWeatherLayer(LayerCode.HEAT_INDEX)

                    // JS parity: the published example swaps the labels on a five-second timer.
                    handler.postDelayed({ applyLabels(custom = true) }, CUSTOMIZE_DELAY_MS)
                }
            }
        })
    }

    /** Mirrors the JS `addLegendControl('#legend')`, positioned bottom-end like the other screens. */
    private fun attachLegend() {
        controller.add(legendControl)
        legendControl.setDarkTheme(true)
        val legendView = legendControl.getView()
        legendView.id = View.generateViewId()
        binding.customHeatIndexLegendRoot.addView(legendView)
        val params = legendView.layoutParams as ConstraintLayout.LayoutParams
        val parentId = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = parentId
        params.bottomToBottom = parentId
        params.bottomMargin = dp(24)
        params.marginEnd = dp(16)
        params.width = dp(300)
        legendView.layoutParams = params
    }

    /**
     * Swaps the bar's labels and pushes the legend back through [LegendControl.update].
     *
     * The heat-index legend is a [BarLegend] over [UnitTemperature]; [LegendControl.getLegend]
     * returns the [com.xweather.mapsgl.controls.legend.Legend] interface, so the cast is what
     * gives access to [BarLegend.items].
     */
    @Suppress("UNCHECKED_CAST")
    private fun applyLabels(custom: Boolean) {
        val legend = legendControl.getLegend(LEGEND_ID) as? BarLegend<UnitTemperature> ?: run {
            binding.customHeatIndexLegendNote.text = NOTE_NO_LEGEND
            return
        }

        legend.items.forEach { item ->
            if (defaultValues == null) {
                defaultValues = item.labels.values
                defaultNormalized = item.labels.normalized
            }
            if (custom) {
                // Direct counterpart of the JS `values: [{ value, label }, …]` array. The accessor
                // is handed the legend's current unit, which this example ignores because the
                // positions are normalized rather than in degrees.
                item.labels.values = BarLegendLabels.Values.FromLabels { CUSTOM_LABELS }
                item.labels.normalized = true
            } else {
                item.labels.values = defaultValues
                item.labels.normalized = defaultNormalized
            }
        }

        legendControl.update(legend)

        customLabelsApplied = custom
        binding.customHeatIndexLegendToggleButton.text =
            if (custom) "Default labels" else "Custom labels"
        binding.customHeatIndexLegendNote.text = if (custom) NOTE_CUSTOM else NOTE_DEFAULT
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun returnToMenu() {
        startActivity(
            Intent(this, DocsExamplesMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mapLoadedCancelable?.cancel()
        mapLoadedCancelable = null
        super.onDestroy()
    }

    private companion object {
        /** `LegendCode.HEAT_INDEX.id` - the same string the JS SDK uses. */
        const val LEGEND_ID = "heat-index"

        const val CUSTOMIZE_DELAY_MS = 5_000L

        /** JS `values: [{ value: 0, … }, { value: 0.5, … }, { value: 1, … }]`. */
        val CUSTOM_LABELS = listOf(
            0.0 to "Uncomfortable",
            0.5 to "Hot",
            1.0 to "Dangerous",
        )

        const val NOTE_WAITING =
            "The heat index legend starts with the SDK's default labels, one every 10 degrees. " +
                "After five seconds it swaps to three normalized labels, as the JS example does."
        const val NOTE_CUSTOM =
            "Custom labels: Values.FromLabels positions Uncomfortable / Hot / Dangerous at 0, 0.5 " +
                "and 1 across the bar, with normalized = true."
        const val NOTE_DEFAULT =
            "Default labels: Values.Every, one label per 10 degrees Fahrenheit, positioned through " +
                "the colour ramp."
        const val NOTE_NO_LEGEND =
            "No heat-index legend yet - a legend is registered when its layer is added."
    }
}
