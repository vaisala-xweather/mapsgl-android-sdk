package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.layers.spec.FillLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Filtering the alerts layer by alert category.**
 *
 * Android port of the JS example
 * [Filter weather alerts by category](https://www.xweather.com/docs/mapsgl/examples/filter-alerts).
 * Each category is a set of VTEC codes, and the filter keeps only the features whose `VTEC`
 * property is in that set. The codes are listed at
 * [Alert types](https://www.xweather.com/docs/maps/reference/alert-types).
 *
 * ### `contains` is the Android form of `["in", …]`
 *
 * The JS filter is a membership test:
 *
 * ```js
 * ['in', ['get', 'VTEC'], ['literal', ['SV.W', 'TO.W', …]]]
 * ```
 *
 * [Expression.contains] is the counterpart, and it serializes to `match` rather than `in`:
 *
 * ```kotlin
 * Expression.contains(Expression.get("VTEC"), listOf("SV.W", "TO.W", …))
 * // ["match", ["get", "VTEC"], "SV.W", true, "TO.W", true, …, false]
 * ```
 *
 * The two are equivalent as a boolean layer filter; `match` is the more widely supported of the
 * pair in the Mapbox GL native engine, which is why the SDK builds that form. [Expression.literal]
 * exists as well, so the `in` shape can be written out by hand, but there is no reason to.
 *
 * ### "All" is no filter at all
 *
 * JS returns `[]` for the unfiltered case, which reads as an empty expression. The Android
 * equivalent is `null` - [FillLayerDescriptor.filter] is nullable, and clearing it is what removes
 * the restriction.
 *
 * ### Changing the filter on a live layer
 *
 * [com.xweather.mapsgl.map.MapController.setLayerFilter] is the counterpart of the JS example's
 * `alertsLayer.setFilter(…)`, and the category picker calls it:
 *
 * ```kotlin
 * controller.setLayerFilter(layerId, filterFor(category))
 * ```
 *
 * Assigning [FillLayerDescriptor.filter] by hand does not do the same thing. The descriptor's
 * filter is copied onto the layer when it is added, so a later write to the descriptor goes
 * nowhere; and writing the layer's own `filter` still leaves the meshes built under the old filter
 * in the source's cache. `setLayerFilter` is the call that handles both.
 */
class FilterAlertsActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "The alerts layer filtered to one category of VTEC codes. Tap a polygon to read which " +
            "alert it is."

    // The JS example's center: [-93, 40], zoom: 3. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(40.0, -93.0)
    override val cameraZoom = 3.0

    /**
     * Tapping a polygon names the alert, which is how you check the filter did what you meant -
     * every feature left on the map should belong to the category the picker has selected.
     */
    override val showDataInspector = true

    /** The caption explains what the picker does, so the two belong together. */
    override val controlsBelowCaption = true

    override fun customizeLayers(controller: MapboxMapController) {
        val config = WeatherService.Alerts(controller.service)
        val layer: FillLayerDescriptor = config.layer

        // JS: addWeatherLayer('alerts', { filter: getFilter() }). Setting it on the descriptor
        // before the layer is added is the same thing.
        layer.filter = filterFor(CATEGORIES[INITIAL_CATEGORY])
        layerId = layer.id

        controller.addWeatherLayer(config)
    }

    override fun buildControls() {
        // JS: alertsLayer.setFilter(getFilter()) from the category <select>.
        addChoice("Category", CATEGORIES.map { it.title }, INITIAL_CATEGORY) { index ->
            controller.setLayerFilter(layerId, filterFor(CATEGORIES[index]))
        }
    }

    /** `null` for "All", matching the JS example's empty filter. */
    private fun filterFor(category: Category): Expression? =
        category.codes?.let { Expression.contains(Expression.get("VTEC"), it) }

    /** One entry in the picker: a title and the VTEC codes it keeps, or `null` for everything. */
    private class Category(val title: String, val codes: List<String>?)

    private var layerId = ""

    private companion object {
        /** "Fire" - the JS example opens on "All"; this opens on a category to show the filter. */
        const val INITIAL_CATEGORY = 2

        val CATEGORIES = listOf(
            Category("All", null),

            Category(
                "Severe",
                listOf(
                    "SV.W", "TO.W", "FF.W", "MA.W", "FF.A", "MA.A", "TO.A", "SV.A",
                    "AW.TS.MN", "AW.TS.MD", "AW.TS.SV", "AW.TS.EX",
                ),
            ),

            Category(
                "Fire",
                listOf(
                    "FW.A", "FRW", "RFD", "FW.W",
                    "AW.FR.MN", "AW.FR.MD", "AW.FR.SV", "AW.FR.EX",
                ),
            ),

            Category(
                "Flood",
                listOf(
                    "FL.Y", "FL.A", "FL.W", "FL.S", "FA.W", "FA.A", "FF.A", "FF.W", "RA.W",
                    // Spelled "AW.RA,MN" in the JS example - a comma where the other codes use a
                    // dot. There is no such VTEC code, so that entry matches nothing.
                    "AW.RA.MN", "AW.RA.MD", "AW.RA.SV", "AW.RA.EX",
                    "AW.FL.MN", "AW.FL.MD", "AW.FL.SV", "AW.FL.EX",
                    "AW.RF.MN", "AW.RF.MD", "AW.RF.SV", "AW.RF.EX",
                ),
            ),

            Category(
                "Heat",
                listOf(
                    "EH.A", "EH.W", "HT.Y", "HT.W",
                    "AW.HT.MN", "AW.HT.MD", "AW.HT.SV", "AW.HT.EX",
                ),
            ),

            Category(
                "Wind",
                listOf(
                    "BW.Y", "EW.W", "HW.W", "HW.A", "HF.W", "HF.A", "LW.Y", "SI.Y", "WI.Y", "WI.W",
                    "AW.WI.MN", "AW.WI.MD", "AW.WI.SV", "AW.WI.EX",
                ),
            ),

            Category(
                "Tropical",
                listOf(
                    "TC.S", "TR.S", "TR.W", "TR.A", "HF.W", "HF.A", "HU.S", "HU.W", "HU.A",
                ),
            ),

            Category(
                "Winter",
                listOf(
                    "AR.W", "AVW", "AVA", "AV.Y", "BZ.W", "BS.Y", "EC.W", "EC.A", "ZL.Y", "ZF.Y",
                    "ZR.W", "ZY.Y", "UP.W", "UP.A", "IS.W", "LE.W", "LW.Y", "SQ.W", "SQ.A", "SB.Y",
                    "SN.W", "WC.Y", "WC.W", "WC.A", "WS.W", "WS.A", "LE.A", "BZ.A", "WW.Y", "LE.Y",
                    "ZR.Y",
                    "AW.SI.MN", "AW.SI.MD", "AW.SI.SV", "AW.SI.EX",
                    "AW.AV.MN", "AW.AV.MD", "AW.AV.SV", "AW.AV.EX",
                    "AW.LT.MN", "AW.LT.MD", "AW.LT.SV", "AW.LT.EX",
                ),
            ),

            Category(
                "Freeze",
                listOf(
                    "FE.W", "FZ.W", "FZ.A", "HZ.W", "HZ.A",
                    "AW.LT.MN", "AW.LT.MD", "AW.LT.SV", "AW.LT.EX",
                ),
            ),
        )
    }
}
