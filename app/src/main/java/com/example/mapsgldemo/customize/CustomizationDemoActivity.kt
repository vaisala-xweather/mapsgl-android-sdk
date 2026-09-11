package com.example.mapsgldemo.customize

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.example.mapsgldemo.LayerCustomizationMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityCustomizationDemoBinding
import com.example.mapsgldemo.helpers.InsetEdges
import com.example.mapsgldemo.helpers.TimelineTextFormatter
import com.example.mapsgldemo.helpers.drawBehindCutout
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.controls.legend.LegendControl
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * Boilerplate for the layer-customization demos: creates the map, wires the timeline chrome, and
 * handles lifecycle and back navigation.
 *
 * You do not need to read this class to understand the demos. Each demo subclass overrides
 * [customizeLayers] and nothing else of substance, so the whole point of that screen is the few
 * lines in that one function. Start there:
 *
 * - [DataDrivenColorActivity] - colour a layer from a feature property with an expression
 * - [CustomShaderActivity] - run your own GLSL fragment shader on symbol icons
 * - [FillSortOrderActivity] - control which overlapping polygon draws on top
 * - [HeatmapTuningActivity] - tune heatmap radius, intensity and blur
 * - [IconOrientationActivity] - tilt and rotate icons with the map
 */
abstract class CustomizationDemoActivity : AppCompatActivity() {

    /** One or two sentences shown over the map describing what this demo changed. */
    protected abstract val caption: String

    /** Where the demo's data actually is, so the screen is not empty on launch. */
    protected open val cameraCenter: Coordinate = Coordinate(39.0, -98.0)

    protected open val cameraZoom: Double = 4.0

    /** Hours of timeline history. Vector products carry far less history than raster ones. */
    protected open val timelineHoursBack: Long = 6

    /**
     * Set `true` to attach the SDK's data inspector, so tapping the map reads the value or the
     * feature under the finger.
     *
     * Off by default: it is only useful on a demo whose layer carries data worth inspecting.
     */
    protected open val showDataInspector: Boolean = false

    /**
     * Set `true` to show the SDK's legend for whatever layers this demo adds.
     *
     * Off by default: most of these screens style a layer that has no legend, or one whose legend
     * says nothing about what the demo changed. The control is registered before [customizeLayers]
     * runs, because a legend is created as its layer is added.
     */
    protected open val showLegend: Boolean = false

    /**
     * Add and style the layers for this demo.
     *
     * Called once, after the map and its style are ready. This is the only function a demo needs
     * to implement, and it is the code worth copying into your own app.
     */
    protected abstract fun customizeLayers(controller: MapboxMapController)

    /**
     * Menu this demo returns to.
     *
     * Defaults to the layer-customization menu, which is where most of these demos are listed.
     * A demo published under a different menu overrides this so the back control and the system
     * back gesture both land where the user came from.
     */
    protected open fun menuActivity(): Class<out AppCompatActivity> =
        LayerCustomizationMenuActivity::class.java

    /**
     * Add the demo's interactive controls with [addSlider], [addChoice] and [addToggle].
     *
     * Called straight after [customizeLayers]. Optional - a demo with nothing to tweak can leave
     * it out and the control panel stays hidden.
     */
    protected open fun buildControls() = Unit

    private lateinit var binding: ActivityCustomizationDemoBinding
    private lateinit var mapView: MapView
    protected lateinit var controller: MapboxMapController
        private set
    private var mapboxMap: MapboxMap? = null
    private var mapLoadedCancelable: Cancelable? = null
    /**
     * The legend control, for a demo that wants to reach into the legend itself.
     *
     * Only registered with the controller when [showLegend] is set; until then nothing here is on
     * screen and [LegendControl.getLegend] has nothing to return.
     */
    protected val legendControl by lazy { LegendControl() }
    private var layersReady = false
    private var resumeTimelineAfterBackground = false

    @SuppressLint("ClickableViewAccessibility")
    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomizationDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawBehindCutout(
            binding.customizationRoot,
            InsetEdges(binding.customizationBackButton, top = true, start = true),
            InsetEdges(binding.customizationCaption, top = true, end = true),
        )

        mapView = binding.customizationMapView
        binding.customizationCaption.text = caption

        setupTimelineChrome()

        binding.customizationBackButton.setOnClickListener { returnToMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = returnToMenu()
        })

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

                controller.setCenter(cameraCenter)
                controller.setZoom(cameraZoom)

                with(controller.timeline) {
                    duration = 4.0
                    delay = 0.0
                    endDelay = 1.0
                    repeat = true
                    setStartDateUsingOffset(-3600L * 1000L * timelineHoursBack)
                    setEndDateUsingOffset(0)
                }

                binding.timelineView.timelineControls.setupButtonListeners(
                    controller.timeline,
                    binding.timelineView,
                )
                setupTimelineListeners()

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded { onMapLoaded() }
            }
        })
    }

    private fun onMapLoaded() {
        if (layersReady) return
        layersReady = true
        mapboxMap?.style?.setProjection(projection(ProjectionName.MERCATOR))

        // Before customizeLayers: the control has to be registered to pick up the legend that
        // comes with a layer as that layer is added.
        if (showLegend) attachLegend() else if (controlsBelowCaption) moveControlsBelowCaption()
        if (showDataInspector) controller.addDataInspectorControl(mapView)

        // The demo's own code runs here.
        customizeLayers(controller)
        buildControls()
        binding.customizationControlsScroll.visibility =
            if (binding.customizationControls.childCount == 0) View.GONE else View.VISIBLE

        binding.timelineView.timelineControls.setupSeekBarChangeListener(
            binding.timelineView,
            controller.timeline,
        ) {}
        binding.timelineView.timelineControls.setPosition(controller.timeline.position)
        TimelineTextFormatter.setTimeTextViews(
            binding.timelineView,
            controller.timeline,
            startEndLabels = binding.timelineSettingsPanel,
        )
    }

    /**
     * Adds the legend view into the root - the Android counterpart of the JS
     * `addLegendControl('#legend')` - and swaps it with the controls panel.
     *
     * The legend takes the bottom, above the timeline, and the controls move up under the caption.
     * That is the right way round for a screen whose control changes what the legend describes:
     * the legend reads as part of the map, and the thing you touch sits near the thing that tells
     * you what you did. The swap is scoped to legend-showing demos, so the other demos keep their
     * controls at the bottom.
     */
    private fun attachLegend() {
        controller.add(legendControl)
        legendControl.setDarkTheme(true)
        val legendView = legendControl.getView()
        legendView.id = View.generateViewId()
        binding.customizationRoot.addView(legendView)
        legendView.layoutParams = (legendView.layoutParams as ConstraintLayout.LayoutParams).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.timeline_settings_panel
            bottomMargin = dpToPx(8)
            marginEnd = dpToPx(12)
            width = dpToPx(240)
        }

        moveControlsBelowCaption()
    }

    /**
     * Puts the controls panel directly under the caption instead of at the bottom of the map.
     *
     * [showLegend] implies this - the legend takes the bottom and the controls move out of its way.
     * A screen with no legend can ask for it on its own when the caption is what explains the
     * control, so the two read together rather than sitting at opposite ends of the screen.
     */
    protected open val controlsBelowCaption: Boolean = false

    private fun moveControlsBelowCaption() {
        // bottomToTop has to be cleared explicitly, or the panel keeps its XML anchor to the
        // timeline and stretches the whole height of the map.
        binding.customizationControlsScroll.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToTop = ConstraintLayout.LayoutParams.UNSET
            topToBottom = binding.customizationHeaderBottom.id
            topMargin = dpToPx(8)
            bottomMargin = 0
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun setupTimelineChrome() {
        binding.timelineView.timelineControls.attachSettingsPanel(binding.timelineSettingsPanel)
        binding.timelineView.settingsCS.root.visibility = View.GONE
        binding.timelineView.timelineControls.adjustPaddingForNavigation(binding.timelineView.playControlsCS)
        binding.timelineView.timelineControls.setAnimations(this, binding.timelineView)
        binding.timelineView.timelineControls.setConfigAnimations(this, binding.timelineView)
        binding.timelineView.timelineControls.installDismissSettingsOnMapTap(mapView, binding.timelineView)
        binding.timelineView.layerMenuButton.visibility = View.GONE
        binding.timelineView.locationButton.visibility = View.GONE
        binding.timelineView.localControlStrip.visibility = View.GONE
    }

    private fun setupTimelineListeners() {
        controller.onLoadStart.observe(this) {
            binding.timelineView.progressBar.isVisible = true
            binding.timelineView.progressTextView.isVisible = true
        }
        controller.onLoadComplete.observe(this) {
            binding.timelineView.progressBar.isVisible = false
            binding.timelineView.progressTextView.isVisible = false
        }
        controller.onLoadProgress.observe(this) { progress ->
            val percent = if (progress.total > 0) {
                ((progress.completed.toFloat() / progress.total.toFloat()) * 100f).toInt()
            } else {
                0
            }
            binding.timelineView.progressTextView.text =
                if (percent != 0 && percent != 100) "$percent%" else ""
        }
        controller.timeline.on(AnimationEvent.play) {
            val playing = controller.timeline.state == AnimationState.playing
            binding.timelineView.timelineControls.updatePlayButtonImage(playing, binding.timelineView)
        }
        controller.timeline.on(AnimationEvent.stop) {
            binding.timelineView.timelineControls.updatePlayButtonImage(false, binding.timelineView)
        }
        controller.timeline.on(AnimationEvent.advance) {
            binding.timelineView.timelineControls.setPosition(controller.timeline.position)
        }
        controller.timeline.on(AnimationEvent.range_change) {
            TimelineTextFormatter.setTimeTextViews(
                binding.timelineView,
                controller.timeline,
                position = controller.timeline.position,
                startEndLabels = binding.timelineSettingsPanel,
            )
            binding.timelineView.timelineControls.updatePlayButtonImage(false, binding.timelineView)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        if (::controller.isInitialized && resumeTimelineAfterBackground) {
            controller.timeline.resume()
            resumeTimelineAfterBackground = false
        }
    }

    override fun onStop() {
        if (::controller.isInitialized && controller.timeline.state == AnimationState.playing) {
            controller.timeline.pause()
            resumeTimelineAfterBackground = true
        }
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
        if (::controller.isInitialized) controller.shutdown()
        mapView.onDestroy()
        super.onDestroy()
    }

    // --- Controls a demo can add ------------------------------------------------------------

    /**
     * Adds a labelled slider. [onChange] fires as it moves, with the value already mapped into
     * [min]..[max], so a demo only ever sees the number it cares about.
     *
     * Remember to call [refreshPaint] from [onChange] - changing a paint value after the layer is
     * on the map does nothing on its own.
     */
    protected fun addSlider(
        label: String,
        min: Double,
        max: Double,
        initial: Double,
        format: (Double) -> String = { "%.2f".format(it) },
        onChange: (Double) -> Unit,
    ) {
        val caption = TextView(this).apply {
            // A label with a live value: monospaced and tabular so digits do not jitter.
            setTextColor(getColor(R.color.xw_text_primary))
            typeface = Typeface.MONOSPACE
            fontFeatureSettings = "tnum"
            letterSpacing = 0.02f
            textSize = 13f
            text = "$label: ${format(initial)}"
        }
        val steps = 100
        val bar = SeekBar(this).apply {
            this.max = steps
            progress = (((initial - min) / (max - min)) * steps).toInt().coerceIn(0, steps)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                private fun valueOf(progress: Int) = min + (max - min) * (progress.toDouble() / steps)

                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    // Label tracks the finger; the layer is only rebuilt on release, because
                    // re-adding a layer per touch event would thrash the tile pipeline.
                    caption.text = "$label: ${format(valueOf(progress))}"
                }

                override fun onStartTrackingTouch(sb: SeekBar?) = Unit

                override fun onStopTrackingTouch(sb: SeekBar?) {
                    onChange(valueOf(sb?.progress ?: 0))
                }
            })
        }
        binding.customizationControls.addView(caption)
        binding.customizationControls.addView(
            bar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    /**
     * Adds a labelled drop-down over [options] - the counterpart of a `<select>` in the JS
     * examples.
     *
     * A [Spinner] rather than a slider on purpose. [addSlider] maps its track onto 100 steps, so
     * picking an exact one of a handful of named choices means landing inside the right 1/100th of
     * the track; a drag that stops short reads back as the neighbouring option. A drop-down names
     * every choice and cannot be off by one.
     *
     * [onChange] fires only when the selection actually changed, which matters when the handler is
     * expensive - re-adding a layer, say.
     */
    protected fun addChoice(
        label: String,
        options: List<String>,
        initialIndex: Int = 0,
        onChange: (Int) -> Unit,
    ) {
        require(options.isNotEmpty()) { "addChoice needs at least one option" }
        var current = initialIndex.coerceIn(options.indices)

        val caption = TextView(this).apply {
            setTextColor(getColor(R.color.xw_text_secondary))
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.02f
            textSize = 13f
            text = label
        }

        // Spinner rows are inflated from the platform layouts, which assume a light background,
        // so each one gets retinted as it is bound.
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            options,
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).also(::style)

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getDropDownView(position, convertView, parent).also(::style)

            private fun style(row: View) {
                (row as? TextView)?.apply {
                    setTextColor(getColor(R.color.xw_text_primary))
                    typeface = Typeface.MONOSPACE
                    textSize = 14f
                }
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinner = Spinner(this).apply {
            this.adapter = adapter
            setPopupBackgroundResource(R.drawable.xw_dropdown_background)
            setSelection(current, false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) {
                    if (position == current) return
                    current = position
                    onChange(position)
                }

                override fun onNothingSelected(p: AdapterView<*>?) = Unit
            }
        }

        binding.customizationControls.addView(caption)
        binding.customizationControls.addView(
            spinner,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    /** Adds a labelled on/off switch. */
    protected fun addToggle(label: String, initial: Boolean, onChange: (Boolean) -> Unit) {
        @Suppress("UseSwitchCompatOrMaterialCode")
        val toggle = Switch(this).apply {
            text = label
            isChecked = initial
            setTextColor(getColor(R.color.xw_text_primary))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.04f
            textSize = 14f
            setOnCheckedChangeListener { _, checked -> onChange(checked) }
        }
        binding.customizationControls.addView(
            toggle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    /**
     * Re-applies a layer after its paint or filter changed.
     *
     * Paint is read when a layer's meshes are built, so mutating it afterwards has no effect on
     * its own. [MapboxMapController.refreshGlVectorLayerPaint] handles that for **symbol** layers,
     * but for circle, fill, line and heatmap layers it falls through to the geometry coordinator,
     * which does not re-read paint like `heatmap.radius` - a slider bound to it appears to do
     * nothing. Removing and re-adding always works, so that is what these demos do.
     *
     * The cost is real (the layer re-registers and re-reads cached tiles), which is why sliders
     * commit on release rather than on every touch event.
     */
    protected fun reAddLayer(code: LayerCode, add: () -> Unit) {
        controller.removeWeatherLayer(code)
        add()
        controller.mapboxMap?.triggerRepaint()
    }

    /**
     * Symbol-layer paint changes only. See [reAddLayer] for why other layer types cannot use this.
     */
    protected fun refreshSymbolPaint(layerId: String) {
        controller.refreshGlVectorLayerPaint(layerId)
        controller.mapboxMap?.triggerRepaint()
    }

    private fun returnToMenu() {
        startActivity(
            Intent(this, menuActivity())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
