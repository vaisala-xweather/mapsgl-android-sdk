package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityDataQueryTextSizeBinding
import com.example.mapsgldemo.helpers.loadFlatStyle
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.groups.DataQuery
import java.util.Locale

/**
 * Runtime text sizing for data-query (`*-text`) layers.
 *
 * A data-query layer draws two labels per point from one `SymbolLayerPaint`: `text[0]` is the
 * sampled weather value and `text[1]` the place name. They are separately sized (14 / 11 by
 * default), so this screen gives each its own slider.
 *
 * Each slider calls [com.xweather.mapsgl.map.MapController.setPaintProperty] with the corresponding
 * `text[n].size` key path when the finger lifts. The size changes on the live layer with no re-add.
 */
class DataQueryTextSizeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataQueryTextSizeBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private var mapLoadedCancelable: Cancelable? = null
    private lateinit var controller: MapboxMapController
    private var weatherStackInitialized = false

    /** Catalog entries offered in the spinner, in catalog order. */
    private val catalog: List<DataQuery.Spec> = DataQuery.CATALOG

    private var activeSpec: DataQuery.Spec? = null

    /** Guards slider callbacks while the UI is being synced programmatically. */
    private var syncingUi = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataQueryTextSizeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.dataQueryTextSizeMapView
        mapView.loadFlatStyle(Style.DARK)
        mapView.logo.enabled = false
        mapView.attribution.enabled = false
        mapView.scalebar.enabled = false

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.dataQueryTextSizeBackButton.setOnClickListener { returnToMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMenu()
            }
        })

        setupLayerSpinner()
        bindSlider(binding.dataQueryValueSizeSeekBar, VALUE_TEXT_INDEX)
        bindSlider(binding.dataQueryNameSizeSeekBar, NAME_TEXT_INDEX)
        binding.dataQueryTextSizeResetButton.setOnClickListener { resetToDefaults() }

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (mapView.parent == null) return

                controller = MapboxMapController(mapView, xweatherAccount)
                mapboxMap = controller.mapboxMap

                controller.setCenter(START_CENTER)
                controller.setZoom(START_ZOOM)
                with(controller.timeline) {
                    duration = 4.0
                    delay = 0.0
                    endDelay = 1.0
                    repeat = false
                    setStartDateUsingOffset(-24L * 60L * 60L * 1000L)
                    setEndDateUsingOffset(0)
                }

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded { onMapLoaded() }
            }
        })
    }

    private fun setupLayerSpinner() {
        val labels = catalog.map { it.code.value }
        binding.dataQueryTextSizeLayerSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels,
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.dataQueryTextSizeLayerSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (syncingUi) return
                    selectLayer(catalog[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    private fun onMapLoaded() {
        if (weatherStackInitialized) return
        weatherStackInitialized = true
        controller.setCenter(START_CENTER)
        controller.setZoom(START_ZOOM)
        selectLayer(catalog.firstOrNull() ?: return)
    }

    /** Swaps the active data-query layer, removing the previous one so only one is on the map. */
    private fun selectLayer(spec: DataQuery.Spec) {
        if (!::controller.isInitialized) return

        activeSpec?.let { controller.removeWeatherLayer(it.code) }
        activeSpec = spec
        controller.addWeatherLayer(spec.code)

        mapView.post { syncSlidersFromLayer() }
        binding.dataQueryTextSizeStatus.text =
            "${spec.code.value} → samples ${spec.sampled.value} · sizes apply on release"
    }

    /**
     * Updates the label while dragging and pushes the size to the map on release.
     *
     * Applying on every progress tick re-rasterizes every visible label at a new size. Committing
     * on release keeps that to one atlas rebuild per gesture.
     */
    private fun bindSlider(seekBar: SeekBar, textIndex: Int) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser || syncingUi) return
                updateSizeLabels(
                    valueSize = if (textIndex == VALUE_TEXT_INDEX) {
                        progressToSize(progress)
                    } else {
                        currentSize(VALUE_TEXT_INDEX)
                    },
                    nameSize = if (textIndex == NAME_TEXT_INDEX) {
                        progressToSize(progress)
                    } else {
                        currentSize(NAME_TEXT_INDEX)
                    },
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val bar = seekBar ?: return
                if (syncingUi) return
                val snapped = progressToSize(bar.progress)
                syncingUi = true
                bar.progress = sizeToProgress(snapped)
                syncingUi = false
                applySize(textIndex, snapped)
            }
        })
    }

    private fun applySize(textIndex: Int, sizePx: Double) {
        if (!::controller.isInitialized) return
        val layerId = activeSpec?.layerId ?: return

        controller.setPaintProperty(layerId, "text[$textIndex].size", sizePx)
        updateSizeLabels(
            valueSize = if (textIndex == VALUE_TEXT_INDEX) sizePx else currentSize(VALUE_TEXT_INDEX),
            nameSize = if (textIndex == NAME_TEXT_INDEX) sizePx else currentSize(NAME_TEXT_INDEX),
        )
    }

    private fun resetToDefaults() {
        applySize(VALUE_TEXT_INDEX, DEFAULT_VALUE_SIZE)
        applySize(NAME_TEXT_INDEX, DEFAULT_NAME_SIZE)
        syncSlidersFromLayer()
    }

    /** Reads the sizes back off the live paint so the sliders reflect what is actually drawn. */
    private fun syncSlidersFromLayer() {
        val valueSize = currentSize(VALUE_TEXT_INDEX) ?: DEFAULT_VALUE_SIZE
        val nameSize = currentSize(NAME_TEXT_INDEX) ?: DEFAULT_NAME_SIZE

        syncingUi = true
        binding.dataQueryValueSizeSeekBar.progress = sizeToProgress(valueSize)
        binding.dataQueryNameSizeSeekBar.progress = sizeToProgress(nameSize)
        syncingUi = false

        updateSizeLabels(valueSize, nameSize)
    }

    /** Current size of `text[textIndex]` on the active layer, or null if it is not resolvable yet. */
    private fun currentSize(textIndex: Int): Double? {
        if (!::controller.isInitialized) return null
        val layerId = activeSpec?.layerId ?: return null
        val paint = controller.getLayer(layerId)?.paint as? SymbolLayerPaint ?: return null
        return paint.text.getOrNull(textIndex)?.size?.constantValue
    }

    private fun updateSizeLabels(valueSize: Double?, nameSize: Double?) {
        binding.dataQueryValueSizeLabel.text = String.format(
            Locale.US,
            "Value size (text[0]): %.0f px",
            valueSize ?: DEFAULT_VALUE_SIZE,
        )
        binding.dataQueryNameSizeLabel.text = String.format(
            Locale.US,
            "Name size (text[1]): %.0f px",
            nameSize ?: DEFAULT_NAME_SIZE,
        )
    }

    private fun returnToMenu() {
        mapLoadedCancelable?.cancel()
        if (::controller.isInitialized) {
            runCatching { controller.shutdown() }
        }
        startActivity(
            Intent(this, DataQueryMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    override fun onStop() {
        if (::controller.isInitialized) {
            runCatching { controller.shutdown() }
        }
        super.onStop()
    }

    private companion object {
        const val VALUE_TEXT_INDEX = 0
        const val NAME_TEXT_INDEX = 1

        const val DEFAULT_VALUE_SIZE = 14.0
        const val DEFAULT_NAME_SIZE = 11.0

        /** Sliders run 6..42 px; SeekBar progress is zero-based so it carries a fixed offset. */
        const val MIN_SIZE_PX = 6

        val START_CENTER = Coordinate(39.5, -98.35)
        const val START_ZOOM = 5.0

        fun progressToSize(progress: Int): Double = (progress + MIN_SIZE_PX).toDouble()

        fun sizeToProgress(sizePx: Double): Int =
            (sizePx.toInt() - MIN_SIZE_PX).coerceAtLeast(0)
    }
}
