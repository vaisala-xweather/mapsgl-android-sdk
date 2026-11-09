package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.bindgen.Value
import com.xweather.mapsgl.extensions.UnitLength
import com.xweather.mapsgl.extensions.UnitPressure
import com.xweather.mapsgl.extensions.UnitSpeed
import com.xweather.mapsgl.extensions.UnitTemperature
import com.xweather.mapsgl.map.MeasurementUnits
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * **Temperature labels in Celsius, because the map's units say so.**
 *
 * Android port of the JS example
 * [Setting map units](https://www.xweather.com/docs/mapsgl/examples/map-units).
 *
 * ### Units are set once, on the controller
 *
 * JS passes them to the controller's constructor:
 *
 * ```js
 * new MapboxMapController(map, {
 *     account,
 *     units: { temperature: 'C', speed: 'km/h', distance: 'km', pressure: 'mb' }
 * });
 * ```
 *
 * On Android they are the controller's [MapboxMapController.units], a [MeasurementUnits] value:
 *
 * ```kotlin
 * controller.units = MeasurementUnits.IMPERIAL.copy(
 *     temperature = UnitTemperature.celsius,
 *     speed = UnitSpeed.kilometersPerHour,
 *     distance = UnitLength.kilometers,
 *     pressure = UnitPressure.millibars,
 * )
 * ```
 *
 * Starting from `IMPERIAL` and copying in the four units is what the JS object does: it names only
 * the units it changes and leaves the rest at the default. [MeasurementUnits.METRIC] would switch
 * every category at once, which is a different example.
 *
 * Every layer, legend and label formats against these units, so they are set before the layer is
 * added. Changing them later works too — the labels reformat without re-sampling — which is the
 * "Changing map units" example.
 *
 * ### Labels are numbers, as in JS
 *
 * MapsGL JS renders the value alone — `14`, not `14°C` — and leaves the unit to the legend. Android
 * does the same by default. The "Show units" switch sets
 * [MapboxMapController.showTextLabelUnits], which puts the unit back on every label (`14°C`) and
 * reformats them in place, without re-sampling.
 *
 * ### The basemap's own labels are hidden
 *
 * The JS example turns off every `symbol` layer in the Mapbox style so the temperature labels are
 * the only text on the map. That works for a classic style like the JS example's `light-v9`, whose
 * labels are top-level layers — and the same loop is here for that case.
 *
 * This app's basemap is Mapbox Standard, where the labels are not top-level layers at all: they sit
 * inside the style's `basemap` import and are switched off through its configuration
 * (`showPlaceLabels`, `showRoadLabels`, …). Without that, the loop finds nothing to hide.
 */
class SetMapUnitsActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Temperatures as text, in Celsius: the map's units are set to C, km/h, km and mb."

    // The JS example's center: [-122.33207, 47.60621], zoom: 8. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(47.60621, -122.33207)
    override val cameraZoom = 8.0

    override fun customizeLayers(controller: MapboxMapController) {
        // JS: the controller's `units` option.
        controller.units = MeasurementUnits.IMPERIAL.copy(
            temperature = UnitTemperature.celsius,
            speed = UnitSpeed.kilometersPerHour,
            distance = UnitLength.kilometers,
            pressure = UnitPressure.millibars,
        )

        // JS: hide every symbol layer in the basemap style, so only the temperature labels show.
        controller.mapboxMap?.style?.let { style ->
            // Classic styles (the JS example's light-v9): labels are top-level symbol layers.
            style.styleLayers
                .filter { it.type == "symbol" }
                .forEach { style.setStyleLayerProperty(it.id, "visibility", Value("none")) }
            // Mapbox Standard: labels live in the `basemap` import and are toggled by its config.
            // On a classic style there is no such import, and the call returns an error, unused.
            STANDARD_LABEL_CONFIG.forEach { key ->
                style.setStyleImportConfigProperty(BASEMAP_IMPORT, key, Value.valueOf(false))
            }
        }

        // JS: controller.addWeatherLayer('temperatures-text')
        controller.addWeatherLayer(LayerCode.TEMPERATURES_TEXT)
    }

    override fun buildControls() {
        // Not in the JS example, whose labels never carry a unit.
        addToggle("Show units", controller.showTextLabelUnits) { on ->
            controller.showTextLabelUnits = on
        }
    }

    private companion object {
        /** The import Mapbox Standard's own layers live in. */
        const val BASEMAP_IMPORT = "basemap"

        /** Standard's label toggles: places, roads, points of interest and transit. */
        val STANDARD_LABEL_CONFIG = listOf(
            "showPlaceLabels",
            "showRoadLabels",
            "showPointOfInterestLabels",
            "showTransitLabels",
        )
    }
}
