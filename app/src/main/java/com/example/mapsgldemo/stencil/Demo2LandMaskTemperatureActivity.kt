package com.example.mapsgldemo.stencil

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsgldemo.R
import com.example.mapsgldemo.StencilMaskMenuActivity
import com.example.mapsgldemo.databinding.ActivityDemo2LandMaskTemperatureBinding
import com.example.mapsgldemo.helpers.TimelineTextFormatter
import com.mapbox.maps.MapView
import com.xweather.mapsgl.anim.AnimationEvent
import com.xweather.mapsgl.anim.AnimationState
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.map.mapbox.GlStencilOsm
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate

/**
 * Demo 2: JS `addWeatherLayer('temperatures', { mask: { type: 'land' } })` or
 * `{ mask: { type: 'water' } }` — surface temperature clipped to land or water. Use the mask
 * dropdown to switch. Former Demo 3 is merged here. Launched from [StencilMaskMenuActivity].
 */
class Demo2LandMaskTemperatureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDemo2LandMaskTemperatureBinding
    private lateinit var mapView: MapView
    private lateinit var mapLifecycle: StencilMaskMapLifecycle

    /** Index into [MASK_TYPE_OPTIONS]: 0 = land, 1 = water. */
    private var maskTypeIndex = MASK_LAND

    private var syncingMaskTypeSpinner = false

    private var resumeTimelineAfterBackground = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemo2LandMaskTemperatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.demo2LandMaskTemperatureMapView
        setupTimelineChrome()

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        setupMaskTypeSpinner()

        mapLifecycle = StencilMaskMapLifecycle(
            activity = this,
            mapView = mapView,
            account = xweatherAccount,
            configureGlStencilSource = {
                GlStencilOsm.source =
                    if (maskTypeIndex == MASK_WATER) GlStencilOsm.Source.MAPBOX else GlStencilOsm.Source.MAPSGL
                GlStencilOsm.mapboxAccessToken = getString(R.string.mapbox_access_token)
            },
            onControllerCreated = { controller, _ ->
                // North of Australia's centroid so Tasmania stays above the timeline chrome.
                controller.setCenter(Coordinate(-18.0, 134.0))
                controller.setZoom(2.0)
                with(controller.timeline) {
                    duration = 4.0
                    delay = 0.0
                    endDelay = 1.0
                    repeat = true
                    setStartDateUsingOffset(-3600 * 1000 * 24)
                    setEndDateUsingOffset(0)
                }
                binding.timelineView.timelineControls.setupButtonListeners(
                    controller.timeline,
                    binding.timelineView,
                )
            },
            onMapReady = { controller, stencilDemos, _ ->
                stencilDemos.setupDemo2LandWaterMaskTemperature(waterMask = maskTypeIndex == MASK_WATER)
                binding.demo2LandMaskTemperatureMaskTypeSpinner.isEnabled = true
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
                binding.timelineView.timelineControls.updatePlayButtonImage(false, binding.timelineView)
                setupTimelineListeners(controller)
            },
            teardownDemo = { it.teardownDemo2LandWaterMaskTemperature() },
        )

        binding.demo2LandMaskTemperatureBackButton.setOnClickListener { returnToStencilMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToStencilMenu()
            }
        })
    }

    private fun setupMaskTypeSpinner() {
        val adapter = ArrayAdapter(
            this,
            R.layout.map_overlay_spinner_item,
            MASK_TYPE_OPTIONS,
        ).also { it.setDropDownViewResource(R.layout.map_overlay_spinner_dropdown_item) }
        binding.demo2LandMaskTemperatureMaskTypeSpinner.adapter = adapter
        syncingMaskTypeSpinner = true
        binding.demo2LandMaskTemperatureMaskTypeSpinner.setSelection(maskTypeIndex)
        syncingMaskTypeSpinner = false
        binding.demo2LandMaskTemperatureMaskTypeSpinner.isEnabled = false
        binding.demo2LandMaskTemperatureMaskTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (syncingMaskTypeSpinner) return
                    if (position == maskTypeIndex) return
                    maskTypeIndex = position
                    mapLifecycle.stencilDemos?.setDemo2LandWaterMaskInverted(maskTypeIndex == MASK_WATER)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
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

    private fun setupTimelineListeners(controller: MapboxMapController) {
        controller.onLoadStart.observe(this) {
            binding.timelineView.progressBar.isVisible = true
            binding.timelineView.progressTextView.isVisible = true
        }
        controller.onLoadComplete.observe(this) {
            binding.timelineView.progressBar.isVisible = false
            binding.timelineView.progressTextView.isVisible = false
        }
        controller.onLoadProgress.observe(this) { progress ->
            val percentInt = if (progress.total > 0) {
                ((progress.completed.toFloat() / progress.total.toFloat()) * 100f).toInt()
            } else {
                0
            }
            binding.timelineView.progressTextView.text =
                if (percentInt != 0 && percentInt != 100) "$percentInt%" else ""
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
        mapLifecycle.onStart()
        mapLifecycle.controller?.let { controller ->
            if (resumeTimelineAfterBackground) {
                controller.timeline.resume()
                resumeTimelineAfterBackground = false
            }
        }
    }

    override fun onStop() {
        mapLifecycle.controller?.let { controller ->
            if (controller.timeline.state == AnimationState.playing) {
                controller.timeline.pause()
                resumeTimelineAfterBackground = true
            }
        }
        mapLifecycle.onStop()
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapLifecycle.onDestroy()
        mapView.onDestroy()
        super.onDestroy()
    }

    private fun returnToStencilMenu() {
        mapLifecycle.destroyMapController()
        startActivity(
            Intent(this, StencilMaskMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        private const val MASK_LAND = 0
        private const val MASK_WATER = 1
        private val MASK_TYPE_OPTIONS = arrayOf("Land", "Water")
    }
}
