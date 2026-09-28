package com.example.mapsgldemo

import android.app.Activity
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
import com.example.mapsgldemo.moreExamples.ValuesAtPlacesActivity
import com.example.mapsgldemo.moreExamples.WindSpeedCategoriesActivity

/** One row in [MoreExamplesMenuActivity]: what the example is called, what it shows, and where it is. */
class Example(
    val title: String,
    val summary: String,
    val activity: Class<out Activity>,
)

/** A titled group of [examples], drawn as an overline and the rows under it. */
class ExampleCategory(
    val title: String,
    val examples: List<Example>,
)

/**
 * Every example in [MoreExamplesMenuActivity], in the order it lists them.
 *
 * Where the app already had a screen for an example (most of the documentation examples) the row
 * opens that screen rather than a copy of it.
 *
 * Sampling a weather layer at GeoJSON points supplied inline has no row yet: the data-query
 * coordinator only collects points from a vector-tile source.
 */
object MoreExamplesCatalog {

    val categories: List<ExampleCategory> = listOf(
        ExampleCategory(
            "Explore",
            listOf(
                Example(
                    "Weather layer explorer",
                    "Browse every weather layer, with timeline playback, legends, and data inspection.",
                    MapLayersActivity::class.java,
                ),
            ),
        ),
        ExampleCategory(
            "Getting Started",
            listOf(
                Example(
                    "Display a weather map",
                    "Wire up a map, a controller and two weather layers, with no demo scaffolding.",
                    DisplayWeatherMapActivity::class.java,
                ),
            ),
        ),
        ExampleCategory(
            "Styling Weather Layers",
            listOf(
                Example(
                    "Custom radar colors",
                    "Swap between per-precipitation-type radar masks and generic single-value palettes.",
                    CustomRadarColorscaleActivity::class.java,
                ),
                Example(
                    "Custom temperature colors",
                    "Replace a sample layer's color scale, as a gradient or in fixed bands.",
                    CustomTempsFillActivity::class.java,
                ),
                Example(
                    "Adjust the draw range",
                    "Clip a layer to part of its data range and change it live.",
                    AdjustDrawRangeActivity::class.java,
                ),
                Example(
                    "Frost and freeze map",
                    "Clip and band the temperatures layer into agricultural frost categories.",
                    FrostFreezeLayerActivity::class.java,
                ),
                Example(
                    "Wind speed categories",
                    "Show only notable wind, painted as named categories rather than a gradient.",
                    WindSpeedCategoriesActivity::class.java,
                ),
                Example(
                    "Custom wind particles",
                    "Tune particle density, speed, trails and color on the wind field.",
                    CustomWindParticlesActivity::class.java,
                ),
            ),
        ),
        ExampleCategory(
            "Features & Filtering",
            listOf(
                Example(
                    "Custom alert styles",
                    "Color alert polygons by severity instead of by alert type.",
                    CustomAlertStylesActivity::class.java,
                ),
                Example(
                    "Custom lightning styles",
                    "Fade lightning strikes out as they age, using a step expression.",
                    CustomLightningStylesActivity::class.java,
                ),
                Example(
                    "Filter weather alerts",
                    "Draw a subset of a layer's features with a filter expression.",
                    FilterAlertsActivity::class.java,
                ),
            ),
        ),
        ExampleCategory(
            "Animation",
            listOf(
                Example(
                    "Timeline controls",
                    "Play, pause, resume and scrub the timeline, keeping your own UI in step.",
                    TimelineControlsActivity::class.java,
                ),
                Example(
                    "Change the timeline range",
                    "Move the animation window between history and forecast at runtime.",
                    ChangeTimelineRangeActivity::class.java,
                ),
                Example(
                    "Preload animation data",
                    "Fetch every frame up front so the first loop plays without stalling.",
                    PreloadAnimationDataActivity::class.java,
                ),
            ),
        ),
        ExampleCategory(
            "Legends, Units & Readouts",
            listOf(
                Example(
                    "Legend and data inspector",
                    "Add the SDK's legend and tap-to-inspect controls.",
                    LegendAndInspectorActivity::class.java,
                ),
                Example(
                    "Custom legend labels",
                    "Relabel a layer's legend with categories instead of values.",
                    CustomHeatIndexLegendActivity::class.java,
                ),
                Example(
                    "Change map units",
                    "Change one quantity's units and watch every layer, legend and readout follow.",
                    ChangeMapUnitsActivity::class.java,
                ),
                Example(
                    "Values at API places",
                    "Label major cities with temperatures sampled from a weather layer.",
                    ValuesAtPlacesActivity::class.java,
                ),
            ),
        ),
        ExampleCategory(
            "Your Own Data",
            listOf(
                Example(
                    "Custom raster layer",
                    "Draw your own {z}/{x}/{y} image tiles with an ImageSourceDescriptor.",
                    AddRasterLayerActivity::class.java,
                ),
                Example(
                    "Custom vector layer",
                    "Render a vector tile source, colored from each feature's own properties.",
                    AddVectorLayerActivity::class.java,
                ),
                Example(
                    "Custom GeoJSON layer",
                    "Draw your own GeoJSON geometry, filled and outlined from one source.",
                    AddGeoJsonLayerActivity::class.java,
                ),
                Example(
                    "Layer masking",
                    "Clip a raster layer to land or to water with a mask.",
                    LayerMaskingActivity::class.java,
                ),
            ),
        ),
    )
}
