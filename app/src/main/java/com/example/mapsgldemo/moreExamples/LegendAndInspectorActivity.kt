package com.example.mapsgldemo.moreExamples

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.mapsgldemo.MoreExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Legend and data inspector** - the two controls the SDK ships for reading values off a map.
 *
 * - [com.xweather.mapsgl.controls.legend.LegendControl] renders a legend for every weather layer on
 *   the map, restyling itself as layers are added or removed and as units change. Register it with
 *   `controller.add(legendControl)` before adding layers, then place `legendControl.getView()` in
 *   your own layout.
 * - `controller.addDataInspectorControl(mapView)` adds the tap-to-inspect callout.
 *
 * Both are switched on through [showLegend] and [showDataInspector]; the base class shows the
 * handful of lines each takes.
 */
class LegendAndInspectorActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> = MoreExamplesMenuActivity::class.java

    override val caption = "Temperatures and wind particles with the SDK legend. Tap the map to inspect values at that point."

    override val showLegend = true
    override val showDataInspector = true

    override fun customizeLayers(controller: MapboxMapController) {
        for (code in listOf(LayerCode.TEMPERATURES, LayerCode.WIND_PARTICLES)) {
            controller.addWeatherLayer(code)
        }
    }

    override fun buildControls() {
        addToggle("Legend", true) { visible -> legendControl.getView().isVisible = visible }
    }
}
