package com.example.mapsgldemo.customize

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsgldemo.LayerCustomizationMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityCustomizationDemoBinding
import com.example.mapsgldemo.helpers.TimelineTextFormatter
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.config.weather.account.XweatherAccount
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
     * Add and style the layers for this demo.
     *
     * Called once, after the map and its style are ready. This is the only function a demo needs
     * to implement, and it is the code worth copying into your own app.
     */
    protected abstract fun customizeLayers(controller: MapboxMapController)

    /**
     * Add the demo's interactive controls with [addSlider] and [addToggle].
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
    private var layersReady = false
    private var resumeTimelineAfterBackground = false

    @SuppressLint("ClickableViewAccessibility")
    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomizationDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            setTextColor(getColor(R.color.bright_text))
            textSize = 12f
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

    /** Adds a labelled on/off switch. */
    protected fun addToggle(label: String, initial: Boolean, onChange: (Boolean) -> Unit) {
        @Suppress("UseSwitchCompatOrMaterialCode")
        val toggle = Switch(this).apply {
            text = label
            isChecked = initial
            setTextColor(getColor(R.color.bright_text))
            textSize = 13f
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
            Intent(this, LayerCustomizationMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
