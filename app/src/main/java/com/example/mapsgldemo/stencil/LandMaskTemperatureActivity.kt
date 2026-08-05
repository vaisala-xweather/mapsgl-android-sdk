package com.example.mapsgldemo.stencil

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsgldemo.R
import com.example.mapsgldemo.StencilMaskMenuActivity
import com.example.mapsgldemo.databinding.ActivityLandMaskTemperatureBinding
import com.example.mapsgldemo.helpers.StencilMaskDemos
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
import com.xweather.mapsgl.map.mapbox.GlStencilOsm
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate

/**
 * Map screen showing surface temperature with the SDK built-in [com.xweather.mapsgl.layers.spec.MaskLayerKind.LAND]
 * mask (temperature over land, clipped from oceans). Launched from [StencilMaskMenuActivity].
 */
class LandMaskTemperatureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLandMaskTemperatureBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private lateinit var stencilDemos: StencilMaskDemos
    private var mapLoadedCancelable: Cancelable? = null

    private var resumeTimelineAfterBackground = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandMaskTemperatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.landMaskTemperatureMapView

        setupTimelineChrome()

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.landMaskTemperatureBackButton.setOnClickListener { returnToMainActivity() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainActivity()
            }
        })

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (mapView.parent == null) return

                GlStencilOsm.source = GlStencilOsm.Source.MAPSGL
                GlStencilOsm.mapboxAccessToken = getString(R.string.mapbox_access_token)

                controller = MapboxMapController(mapView, xweatherAccount)
                mapboxMap = controller.mapboxMap
                stencilDemos = StencilMaskDemos(this@LandMaskTemperatureActivity, controller)

                controller.setCenter(Coordinate(20.0, 0.0))
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
                setupTimelineListeners()

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    onMapLoaded()
                }
            }
        })
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

    private fun onMapLoaded() {
        mapboxMap?.style?.setProjection(projection(ProjectionName.MERCATOR))
        stencilDemos.setupLandMaskTemperature()

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
        if (::stencilDemos.isInitialized) {
            stencilDemos.teardownLandMaskTemperature()
        }
        mapLoadedCancelable?.cancel()
        mapLoadedCancelable = null
        mapView.onDestroy()
        super.onDestroy()
    }

    private fun returnToMainActivity() {
        startActivity(
            Intent(this, StencilMaskMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
