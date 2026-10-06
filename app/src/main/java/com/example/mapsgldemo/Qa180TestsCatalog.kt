package com.example.mapsgldemo

import com.example.mapsgldemo.docExamples.AddGeoJsonLayerActivity
import com.example.mapsgldemo.docExamples.AddRasterLayerActivity
import com.example.mapsgldemo.docExamples.AddVectorLayerActivity
import com.example.mapsgldemo.docExamples.ChangeMapUnitsActivity
import com.example.mapsgldemo.docExamples.ChangeTimelineRangeActivity
import com.example.mapsgldemo.docExamples.CustomAlertStylesActivity
import com.example.mapsgldemo.docExamples.CustomHeatIndexLegendActivity
import com.example.mapsgldemo.docExamples.CustomLightningStylesActivity
import com.example.mapsgldemo.docExamples.CustomRadarColorscaleActivity
import com.example.mapsgldemo.docExamples.CustomTempsFillActivity
import com.example.mapsgldemo.docExamples.CustomWindParticlesActivity
import com.example.mapsgldemo.docExamples.FilterAlertsActivity
import com.example.mapsgldemo.docExamples.FrostFreezeLayerActivity
import com.example.mapsgldemo.maplayers.MapLayersActivity
import com.example.mapsgldemo.moreExamples.AdjustDrawRangeActivity
import com.example.mapsgldemo.moreExamples.DisplayWeatherMapActivity
import com.example.mapsgldemo.moreExamples.LayerMaskingActivity
import com.example.mapsgldemo.moreExamples.LegendAndInspectorActivity
import com.example.mapsgldemo.moreExamples.PreloadAnimationDataActivity
import com.example.mapsgldemo.moreExamples.TimelineControlsActivity
import com.example.mapsgldemo.moreExamples.ValuesAtOwnPointsActivity
import com.example.mapsgldemo.moreExamples.ValuesAtPlacesActivity
import com.example.mapsgldemo.docExamples.WindSpeedCategoriesActivity

/**
 * The 1.8.0 test notes as screens, for [Qa180MenuActivity]: one row per step the demo can run, in
 * the notes' own order, each naming its section and what to check.
 *
 * Every row opens a screen the app already has - the More Examples screens line up with the notes'
 * examples one for one - so a screen fixed in one menu is fixed here too. [Example.summary] carries
 * the check rather than a description of the screen.
 */
object Qa180TestsCatalog {

    val categories: List<ExampleCategory> = listOf(
        ExampleCategory(
            "Layer selector (§2)",
            listOf(
                Example(
                    "Weather layer explorer",
                    "Open the layer menu and toggle layers on and off. Active layers are highlighted.",
                    MapLayersActivity::class.java,
                ),
                Example(
                    "Layers in the JS menu order",
                    "Categories, order and row titles match the JS demo menu.",
                    JsParityLayersActivity::class.java,
                ),
            ),
        ),
        ExampleCategory(
            "Built-in text layers (§3–4)",
            listOf(
                Example(
                    "All built-in text layers",
                    "Each *-text layer draws a value over the city name and matches JS in UAT, " +
                        "including cloud-cover, temperatures-1hr-change, precip-rate, snow-depth, " +
                        "air-quality-index-caqi and weather-text (a condition name, not a number). " +
                        "Turn on temperatures-text, then wind-speeds-text: only wind speeds draw.",
                    DataQueryTextActivity::class.java,
                ),
                Example(
                    "Units, animation and ordering",
                    "Add Temperatures (Text), then Temperatures, then remove Temperatures: the text " +
                        "keeps drawing and animating. Then add Temperatures, then the text, and remove " +
                        "the text: temperatures still draw. Tap the temperature legend: the labels " +
                        "switch units with it. Play, pause and scrub: labels follow the timeline.",
                    MapLayersActivity::class.java,
                ),
                Example(
                    "Text label size",
                    "The value and name sliders resize the labels live, each on its own.",
                    DataQueryTextSizeActivity::class.java,
                ),
            ),
        ),
        ExampleCategory(
            "Radar sampling (§5)",
            listOf(
                Example(
                    "Radar and the data inspector",
                    "Turn on Radar and tap the map. A value appears only where it is precipitating, " +
                        "none in clear areas. Active cells have no grey fringe.",
                    MapLayersActivity::class.java,
                ),
            ),
        ),
        ExampleCategory(
            "Examples (§6)",
            listOf(
                Example(
                    "Display a weather map",
                    "The map loads with its weather layers and nothing but the back control.",
                    DisplayWeatherMapActivity::class.java,
                ),
                Example(
                    "Custom raster layer",
                    "Hand-built satellite tiles stay aligned while panning and zooming.",
                    AddRasterLayerActivity::class.java,
                ),
                Example(
                    "Custom vector layer",
                    "Water boundaries drawn from a vector tileset.",
                    AddVectorLayerActivity::class.java,
                ),
                Example(
                    "Custom GeoJSON layer",
                    "A region drawn with both a fill and an outline from one source.",
                    AddGeoJsonLayerActivity::class.java,
                ),
                Example(
                    "Layer masking",
                    "The mask clips the layer to land or to water and switches live.",
                    LayerMaskingActivity::class.java,
                ),
                Example(
                    "Timeline controls",
                    "Play advances the time, pause stops it, scrubbing jumps frames, and the " +
                        "timestamp tracks the slider.",
                    TimelineControlsActivity::class.java,
                ),
                Example(
                    "Change the timeline range",
                    "The range moves between history and forecast; playback covers the new range.",
                    ChangeTimelineRangeActivity::class.java,
                ),
                Example(
                    "Preload animation data",
                    "With preloading on, the first loop plays without stalling; the loading " +
                        "indicator and Preload now behave.",
                    PreloadAnimationDataActivity::class.java,
                ),
                Example(
                    "Custom radar colors",
                    "The picker swaps per-type masks and single palettes; map and legend follow each.",
                    CustomRadarColorscaleActivity::class.java,
                ),
                Example(
                    "Custom temperature colors",
                    "The custom ramp and its 2 °F bands show on both the map and the legend.",
                    CustomTempsFillActivity::class.java,
                ),
                Example(
                    "Adjust the draw range",
                    "Moving the control clips the layer live.",
                    AdjustDrawRangeActivity::class.java,
                ),
                Example(
                    "Custom wind particles",
                    "Density, trails and colour visibly change the particle field.",
                    CustomWindParticlesActivity::class.java,
                ),
                Example(
                    "Custom alert styles",
                    "Alerts are coloured by severity rather than by alert type.",
                    CustomAlertStylesActivity::class.java,
                ),
                Example(
                    "Custom lightning styles",
                    "Strikes fade out as they age.",
                    CustomLightningStylesActivity::class.java,
                ),
                Example(
                    "Frost and freeze map",
                    "Temperatures are clipped and banded into frost categories; nothing draws outside them.",
                    FrostFreezeLayerActivity::class.java,
                ),
                Example(
                    "Wind speed categories",
                    "Only notable wind draws, painted as named categories, with a matching legend.",
                    WindSpeedCategoriesActivity::class.java,
                ),
                Example(
                    "Change map units",
                    "The speed unit changes the wind values and the legend together and leaves " +
                        "temperature, distance and pressure alone.",
                    ChangeMapUnitsActivity::class.java,
                ),
                Example(
                    "Legend and data inspector",
                    "The legend shows and hides; tapping the map opens a callout under the finger.",
                    LegendAndInspectorActivity::class.java,
                ),
                Example(
                    "Filter weather alerts",
                    "Each category draws only the matching subset of alerts.",
                    FilterAlertsActivity::class.java,
                ),
                Example(
                    "Custom legend labels",
                    "The heat index legend relabels with categories instead of values.",
                    CustomHeatIndexLegendActivity::class.java,
                ),
                Example(
                    "Values at API places",
                    "The largest US cities are labelled with sampled temperatures.",
                    ValuesAtPlacesActivity::class.java,
                ),
                Example(
                    "Values at your own points",
                    "Your own points are labelled with wind speed and the place name beneath.",
                    ValuesAtOwnPointsActivity::class.java,
                ),
            ),
        ),
    )

    /** Checks with no screen of their own, shown as plain text under their section. */
    val notes: List<Pair<String, String>> = listOf(
        "App-wide (§7)" to
            "Open a screen, go back and open it again: the map rebuilds cleanly each time, and " +
            "back always returns to this menu. Every legend title is styled the same way.",
        "Not in this app" to
            "Examples browser search and the UIKit integration example are platform-specific. This " +
            "demo has no dark/light switch and hides the map logo and attribution, so those checks " +
            "do not apply. Text layers have no legend of their own here, so use the sampled " +
            "layer's legend for units.",
    )
}
