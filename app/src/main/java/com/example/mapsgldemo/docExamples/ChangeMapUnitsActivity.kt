package com.example.mapsgldemo.docExamples

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityChangeMapUnitsBinding
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.controls.legend.LegendControl
import com.xweather.mapsgl.extensions.UnitSpeed
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * Change map units — the Android port of the MapsGL JS example at
 * https://www.xweather.com/docs/mapsgl/examples/change-map-units
 *
 * Shows wind speeds with a legend, and a selector that switches the speed unit between km/h, mph
 * and m/s at runtime. The legend relabels itself to match; the underlying data is unchanged, so
 * only the formatting moves.
 *
 * The JS example calls `controller.setUnits({ speed: 'km/h' })`. Android has no `setUnits`; units
 * live on [MapboxMapController.units], a [com.xweather.mapsgl.map.MeasurementUnits] data class with
 * one field per measurement category. Changing a single category is a `copy`:
 *
 * ```kotlin
 * controller.units = controller.units.copy(speed = UnitSpeed.kilometersPerHour)
 * ```
 *
 * Two differences from the published example:
 *
 * 1. The JS version also adds `wind-speeds-text`. Those data-query `*-text` layers are not part of
 *    this SDK build, so only `wind-speeds` is added here.
 * 2. The JS page's prose describes units changing automatically after a five-second delay, but that
 *    `setTimeout` is commented out in the published source. Only the selector is live there, and
 *    that is what this screen mirrors.
 *
 * Launched from [DocsExamplesMenuActivity].
 */
class ChangeMapUnitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangeMapUnitsBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private var mapLoadedCancelable: Cancelable? = null
    private val legendControl = LegendControl()

    private var syncingUnitsSpinner = false
    private var speedUnitIndex = DEFAULT_SPEED_UNIT_INDEX

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeMapUnitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.changeMapUnitsMapView
        binding.changeMapUnitsCaption.text =
            "Wind speeds with a legend. The picker changes the speed unit at runtime - the data is " +
            "unchanged, only the formatting."

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.changeMapUnitsBackButton.setOnClickListener { returnToDocsMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToDocsMenu()
            }
        })

        setupUnitsSpinner()

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (mapView.parent == null) return

                controller = MapboxMapController(mapView, xweatherAccount)
                mapboxMap = controller.mapboxMap

                // Set the projection before the first frame is drawn. Doing it inside
                // subscribeMapLoaded (on `style`) is too late — the map paints as a globe and then
                // visibly snaps flat. MapSettings.setMapboxPreferences does the same for the other
                // demo screens, which is why they open flat.
                mapboxMap?.setProjection(projection(ProjectionName.MERCATOR))

                // Matches the JS example's `center: [-93, 40], zoom: 4`. Coordinate is (lat, lon).
                controller.setCenter(Coordinate(40.0, -93.0))
                controller.setZoom(4.0)

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    attachLegend()

                    // Seed the controller from the selector so the two agree before the first draw.
                    applySpeedUnit(speedUnitIndex)

                    controller.addWeatherLayer(LayerCode.WIND_SPEEDS)
                }
            }
        })
    }

    /** Mirrors the JS `addLegendControl('#legend')`, positioned like the app's other map screens. */
    private fun attachLegend() {
        controller.add(legendControl)
        legendControl.setDarkTheme(true)
        val legendView = legendControl.getView()
        legendView.id = View.generateViewId()
        binding.changeMapUnitsRoot.addView(legendView, 1)
        val params = legendView.layoutParams as ConstraintLayout.LayoutParams
        val parentId = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = parentId
        params.bottomToBottom = parentId
        params.bottomMargin = dp(24)
        params.marginEnd = dp(16)
        params.width = dp(300)
        legendView.layoutParams = params
    }

    private fun setupUnitsSpinner() {
        val adapter = ArrayAdapter(
            this,
            R.layout.map_overlay_spinner_item,
            SPEED_UNIT_LABELS,
        ).also { it.setDropDownViewResource(R.layout.map_overlay_spinner_dropdown_item) }
        binding.changeMapUnitsSpeedSpinner.adapter = adapter
        syncingUnitsSpinner = true
        binding.changeMapUnitsSpeedSpinner.setSelection(speedUnitIndex)
        syncingUnitsSpinner = false
        binding.changeMapUnitsSpeedSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (syncingUnitsSpinner) return
                    if (position == speedUnitIndex) return
                    speedUnitIndex = position
                    if (::controller.isInitialized) applySpeedUnit(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    /** The Android equivalent of `controller.setUnits({ speed: … })`. */
    private fun applySpeedUnit(index: Int) {
        val unit = when (index) {
            0 -> UnitSpeed.kilometersPerHour
            2 -> UnitSpeed.metersPerSecond
            else -> UnitSpeed.milesPerHour
        }
        controller.units = controller.units.copy(speed = unit)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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

    private fun returnToDocsMenu() {
        startActivity(
            Intent(this, DocsExamplesMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        /** Same options and order as the JS example's `<select>`, which defaults to mph. */
        private val SPEED_UNIT_LABELS = arrayOf("km/h", "mph", "m/s")
        private const val DEFAULT_SPEED_UNIT_INDEX = 1
    }
}
