package com.example.mapsgldemo.stencil

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.StencilMaskMenuActivity
import com.example.mapsgldemo.databinding.ActivityHighwaysConusMaskTemperatureBinding
import com.example.mapsgldemo.helpers.StencilMaskDemos
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.layers.spec.StencilMaskCombineMode
import com.xweather.mapsgl.map.mapbox.GlStencilOsm
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.LineCap
import com.xweather.mapsgl.style.LineJoin
import com.xweather.mapsgl.types.Coordinate

/**
 * Demo 5: mask customization — temperature clipped to highway-motorway ∪ Continental-US polygon
 * with tunable mask options (invert, ALL/ANY, layer toggles, line width, cap, join). Each control
 * reapplies via [StencilMaskDemos.applyHighwaysConusMaskOptions] because
 * [com.xweather.mapsgl.layers.spec.StencilLayerMaskSpec] and line-paint properties are baked in at add-time.
 * Launched from [StencilMaskMenuActivity].
 */
class HighwaysConusMaskTemperatureActivity : AppCompatActivity() {

    companion object {
        /**
         * Slider maps integer progress [`0` .. [WIDTH_SLIDER_MAX]] linearly onto tile-UV thickness
         * [`0.0` .. [WIDTH_SLIDER_UV_MAX]]. We start above zero so the slider remains useful for very
         * thin masks; the upper bound is intentionally well past anything real-world useful so the
         * GC / clipper behaviour at extreme widths stays visible in the demo.
         */
        private const val WIDTH_SLIDER_MAX = 100
        private const val WIDTH_SLIDER_UV_MAX = 4.0

        /** Matches [StencilMaskDemos.HighwaysConusMaskOptions.thicknessUv] default (1.5 UV). */
        private const val DEFAULT_WIDTH_PROGRESS =
            ((1.5 / WIDTH_SLIDER_UV_MAX) * WIDTH_SLIDER_MAX).toInt()

        private val CAP_OPTIONS = LineCap.entries.toList()
        private val JOIN_OPTIONS = LineJoin.entries.toList()
    }

    private lateinit var binding: ActivityHighwaysConusMaskTemperatureBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private lateinit var stencilDemos: StencilMaskDemos
    private var mapLoadedCancelable: Cancelable? = null

    /**
     * Current set of mask parameters; mutated by each control's listener and pushed through
     * [StencilMaskDemos.applyHighwaysConusMaskOptions]. Starts at [StencilMaskDemos.HighwaysConusMaskOptions]'s
     * defaults so the initial render matches `MapLayersActivity`'s `testButton2`.
     */
    private var maskOptions = StencilMaskDemos.HighwaysConusMaskOptions()
    private var controlPanelEnabled = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHighwaysConusMaskTemperatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.highwaysConusMapView

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.highwaysConusBackButton.setOnClickListener { returnToMainActivity() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainActivity()
            }
        })

        setupControlPanel()

        // Defer controller creation until the MapView has been laid out, matching the other demo
        // activities so the GLES stencil surface has a valid size before MapsGL attaches to it.
        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (mapView.parent == null) return

                GlStencilOsm.source = GlStencilOsm.Source.MAPSGL
                GlStencilOsm.mapboxAccessToken = getString(R.string.mapbox_access_token)

                controller = MapboxMapController(mapView, xweatherAccount)
                mapboxMap = controller.mapboxMap
                stencilDemos = StencilMaskDemos(this@HighwaysConusMaskTemperatureActivity, controller)

                controller.setCenter(Coordinate(39.5, -98.35))
                controller.setZoom(3.0)

                with(controller.timeline) {
                    duration = 4.0
                    delay = 0.0
                    endDelay = 1.0
                    repeat = true
                    setStartDateUsingOffset(-3600 * 1000 * 24)
                    setEndDateUsingOffset(0)
                }

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    mapboxMap?.style?.setProjection(projection(ProjectionName.MERCATOR))
                    stencilDemos.setupHighwaysConusMaskTemperature(maskOptions)
                    setControlsEnabled(true)
                }
            }
        })
    }

    /**
     * Initialise spinners and seek-bar to reflect [maskOptions] and attach listeners. Each listener
     * updates [maskOptions] then calls [reapply]. Controls stay disabled until the initial
     * `mapLoaded` callback fires (see [setControlsEnabled]); otherwise an early tap could try to
     * mutate layers that aren't on the map yet.
     */
    private fun setupControlPanel() {
        val capAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            CAP_OPTIONS.map { it.name },
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.highwaysConusCapSpinner.adapter = capAdapter
        binding.highwaysConusCapSpinner.setSelection(CAP_OPTIONS.indexOf(maskOptions.lineCap).coerceAtLeast(0))
        binding.highwaysConusCapSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newCap = CAP_OPTIONS[position]
                if (newCap == maskOptions.lineCap) return
                maskOptions = maskOptions.copy(lineCap = newCap)
                reapply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val joinAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            JOIN_OPTIONS.map { it.name },
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.highwaysConusJoinSpinner.adapter = joinAdapter
        binding.highwaysConusJoinSpinner.setSelection(JOIN_OPTIONS.indexOf(maskOptions.lineJoin).coerceAtLeast(0))
        binding.highwaysConusJoinSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newJoin = JOIN_OPTIONS[position]
                if (newJoin == maskOptions.lineJoin) return
                maskOptions = maskOptions.copy(lineJoin = newJoin)
                reapply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.highwaysConusWidthSeekBar.max = WIDTH_SLIDER_MAX
        binding.highwaysConusWidthSeekBar.progress = DEFAULT_WIDTH_PROGRESS
        updateWidthLabel(maskOptions.thicknessUv)
        binding.highwaysConusWidthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val uv = progress.toDouble() / WIDTH_SLIDER_MAX * WIDTH_SLIDER_UV_MAX
                updateWidthLabel(uv)
                // Only commit when the user lets go — re-adding two style layers on every pixel of
                // a drag would thrash the map. Final value is pushed in [onStopTrackingTouch].
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val uv = (seekBar?.progress ?: 0).toDouble() / WIDTH_SLIDER_MAX * WIDTH_SLIDER_UV_MAX
                if (uv == maskOptions.thicknessUv) return
                maskOptions = maskOptions.copy(thicknessUv = uv)
                reapply()
            }
        })

        binding.highwaysConusInvertButton.setOnClickListener {
            maskOptions = maskOptions.copy(invert = !maskOptions.invert)
            binding.highwaysConusInvertButton.text = if (maskOptions.invert) "Uninvert mask" else "Invert mask"
            reapplyInvertModeOnly()
        }

        binding.highwaysConusModeAllButton.setOnClickListener {
            if (maskOptions.mode == StencilMaskCombineMode.ALL) return@setOnClickListener
            maskOptions = maskOptions.copy(mode = StencilMaskCombineMode.ALL)
            refreshModeButtons()
            reapplyInvertModeOnly()
        }
        binding.highwaysConusModeAnyButton.setOnClickListener {
            if (maskOptions.mode == StencilMaskCombineMode.ANY) return@setOnClickListener
            maskOptions = maskOptions.copy(mode = StencilMaskCombineMode.ANY)
            refreshModeButtons()
            reapplyInvertModeOnly()
        }
        refreshModeButtons()

        binding.highwaysConusHighwayMaskCheck.isChecked = maskOptions.highwayMaskEnabled
        binding.highwaysConusConusMaskCheck.isChecked = maskOptions.conusMaskEnabled
        binding.highwaysConusHighwayMaskCheck.setOnCheckedChangeListener(maskLayerToggleListener)
        binding.highwaysConusConusMaskCheck.setOnCheckedChangeListener(maskLayerToggleListener)
        refreshMaskCombineControlsEnabled()

        setControlsEnabled(false)
    }

    private fun refreshModeButtons() {
        styleModeButton(binding.highwaysConusModeAllButton, maskOptions.mode == StencilMaskCombineMode.ALL)
        styleModeButton(binding.highwaysConusModeAnyButton, maskOptions.mode == StencilMaskCombineMode.ANY)
    }

    private fun masksActive(): Boolean =
        maskOptions.highwayMaskEnabled || maskOptions.conusMaskEnabled

    /** Invert / ALL / ANY only apply while at least one mask layer is enabled. */
    private fun refreshMaskCombineControlsEnabled() {
        val active = masksActive()
        binding.highwaysConusInvertButton.isEnabled = controlPanelEnabled && active
        binding.highwaysConusModeAllButton.isEnabled = controlPanelEnabled && active
        binding.highwaysConusModeAnyButton.isEnabled = controlPanelEnabled && active
    }

    /** Visual cue for which of ALL / ANY is the currently active combine mode. */
    private fun styleModeButton(button: Button, active: Boolean) {
        button.alpha = if (active) 1.0f else 0.45f
        button.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun updateWidthLabel(uv: Double) {
        binding.highwaysConusWidthLabel.text = String.format(java.util.Locale.US, "Width: %.2f", uv)
    }

    private val maskLayerToggleListener = CompoundButton.OnCheckedChangeListener { _, _ ->
        val highwayEnabled = binding.highwaysConusHighwayMaskCheck.isChecked
        val conusEnabled = binding.highwaysConusConusMaskCheck.isChecked
        if (highwayEnabled == maskOptions.highwayMaskEnabled &&
            conusEnabled == maskOptions.conusMaskEnabled
        ) {
            return@OnCheckedChangeListener
        }
        maskOptions = maskOptions.copy(
            highwayMaskEnabled = highwayEnabled,
            conusMaskEnabled = conusEnabled,
        )
        refreshMaskCombineControlsEnabled()
        reapplyMaskLayersOnly()
    }

    private fun setControlsEnabled(enabled: Boolean) {
        controlPanelEnabled = enabled
        binding.highwaysConusHighwayMaskCheck.isEnabled = enabled
        binding.highwaysConusConusMaskCheck.isEnabled = enabled
        binding.highwaysConusWidthSeekBar.isEnabled = enabled
        binding.highwaysConusCapSpinner.isEnabled = enabled
        binding.highwaysConusJoinSpinner.isEnabled = enabled
        refreshMaskCombineControlsEnabled()
    }

    private fun reapply() {
        if (!::stencilDemos.isInitialized) return
        stencilDemos.applyHighwaysConusMaskOptions(maskOptions)
    }

    /**
     * Fast path for the invert / combine-mode toggles. The renderer reads these flags every frame
     * from the temperature layer's `stencilLayerMask`, so mutating the spec in place avoids the
     * heavy teardown (re-parsing MVT, rebuilding stencil meshes, re-uploading weather tiles) that
     * the full [reapply] path triggers — that's what was causing the multi-minute GC pause when
     * the user clicked Invert/ALL/ANY.
     *
     * Falls back to the full rebuild only if the live mutation can't be applied (e.g. the
     * temperature layer hasn't been added yet because `mapLoaded` hasn't fired).
     */
    private fun reapplyInvertModeOnly() {
        if (!::stencilDemos.isInitialized) return
        val applied = stencilDemos.updateHighwaysConusMaskInvertAndMode(maskOptions.invert, maskOptions.mode)
        if (!applied) {
            stencilDemos.applyHighwaysConusMaskOptions(maskOptions)
        }
    }

    private fun reapplyMaskLayersOnly() {
        if (!::stencilDemos.isInitialized) return
        val applied = stencilDemos.updateHighwaysConusActiveMaskLayers(maskOptions)
        if (!applied) {
            stencilDemos.applyHighwaysConusMaskOptions(maskOptions)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapLoadedCancelable?.cancel()
        mapLoadedCancelable = null
        mapView.onDestroy()
        super.onDestroy()
    }

    private fun returnToMainActivity() {
        val intent = Intent(this, StencilMaskMenuActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }
}
