package com.example.mapsgldemo.moreExamples

import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.MoreExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityDisplayWeatherMapBinding
import com.example.mapsgldemo.helpers.InsetEdges
import com.example.mapsgldemo.helpers.drawBehindCutout
import com.example.mapsgldemo.helpers.loadFlatStyle
import com.example.mapsgldemo.helpers.returnToMenu
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Display a weather map** - the smallest useful MapsGL integration, written out in full.
 *
 * Every other example in [MoreExamplesMenuActivity] is built on
 * [com.example.mapsgldemo.customize.CustomizationDemoActivity], which owns the map, the controller
 * and the lifecycle so that an example file holds only the API it demonstrates. This one does that
 * work itself, so there is one place to read what the scaffold does on the others' behalf.
 *
 * What an integration has to get right, all visible below:
 *
 * - **The controller is built once the [MapView] has been laid out.** It reads the view's size, so
 *   building it in `onCreate` measures a zero-sized map.
 * - **Weather layers are added after the map has loaded.** The controller exists as soon as the
 *   map view does, but the style does not, and a layer added before the style is up is dropped.
 * - **The map view is told about the activity lifecycle**, and the controller is shut down with the
 *   activity - it owns tile work and GL resources that would otherwise outlive the screen.
 *
 * `addWeatherLayer(LayerCode)` builds the layer's source, paint, legend and inspector formatting
 * from the weather spec, so one call per layer is enough. Layers stack in the order they are
 * added: radar first, then the alert outlines over it.
 */
class DisplayWeatherMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDisplayWeatherMapBinding
    private lateinit var mapView: MapView
    private lateinit var controller: MapboxMapController
    private var mapLoadedCancelable: Cancelable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayWeatherMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawBehindCutout(
            binding.root,
            InsetEdges(binding.displayWeatherBackButton, top = true, start = true),
        )

        mapView = binding.displayWeatherMapView
        mapView.loadFlatStyle()

        binding.displayWeatherBackButton.setOnClickListener { goBack() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBack()
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
                controller.setCenter(Coordinate(40.0, -85.5))
                controller.setZoom(3.0)

                mapLoadedCancelable = controller.mapboxMap?.subscribeMapLoaded { addWeatherLayers() }
            }
        })
    }

    private fun addWeatherLayers() {
        // The map-loaded callback can fire again for the same map; adding twice would stack a
        // second copy of each layer.
        mapLoadedCancelable?.cancel()
        mapLoadedCancelable = null

        for (code in listOf(LayerCode.RADAR, LayerCode.ALERTS_OUTLINE)) {
            controller.addWeatherLayer(code)
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
        if (::controller.isInitialized) controller.shutdown()
        mapView.onDestroy()
        super.onDestroy()
    }

    private fun goBack() = returnToMenu(MoreExamplesMenuActivity::class.java)
}
