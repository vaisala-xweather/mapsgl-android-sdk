package com.example.mapsgldemo

import android.app.Activity
import androidx.annotation.DrawableRes
import com.example.mapsgldemo.docExamples.AddGeoJsonLayerActivity
import com.example.mapsgldemo.docExamples.AddRasterLayerActivity
import com.example.mapsgldemo.docExamples.AddVectorLayerActivity
import com.example.mapsgldemo.docExamples.ChangeMapUnitsActivity
import com.example.mapsgldemo.docExamples.ChangeTimelineRangeActivity
import com.example.mapsgldemo.docExamples.CitiesTextActivity
import com.example.mapsgldemo.docExamples.CustomAlertStylesActivity
import com.example.mapsgldemo.docExamples.CustomEarthquakeShaderActivity
import com.example.mapsgldemo.docExamples.CustomFiresShaderActivity
import com.example.mapsgldemo.docExamples.CustomHeatIndexLegendActivity
import com.example.mapsgldemo.docExamples.CustomLightningShaderActivity
import com.example.mapsgldemo.docExamples.CustomLightningStylesActivity
import com.example.mapsgldemo.docExamples.CustomRadarColorscaleActivity
import com.example.mapsgldemo.docExamples.CustomTempsFillActivity
import com.example.mapsgldemo.docExamples.CustomWindParticlesActivity
import com.example.mapsgldemo.docExamples.FilterAlertsActivity
import com.example.mapsgldemo.docExamples.FrostFreezeLayerActivity
import com.example.mapsgldemo.docExamples.LayerOrderingActivity
import com.example.mapsgldemo.docExamples.ParticleDrawRangeActivity
import com.example.mapsgldemo.docExamples.TemperaturePlacesQueryActivity
import com.example.mapsgldemo.docExamples.WindSpeedCategoriesActivity
import com.example.mapsgldemo.docExamples.WindSpeedGeoJsonQueryActivity
import com.example.mapsgldemo.moreExamples.AdjustDrawRangeActivity
import com.example.mapsgldemo.moreExamples.DisplayWeatherMapActivity
import com.example.mapsgldemo.moreExamples.LayerMaskingActivity
import com.example.mapsgldemo.moreExamples.PreloadAnimationDataActivity
import com.example.mapsgldemo.moreExamples.TimelineControlsActivity

/**
 * One card on [DocsExamplesMenuActivity]. [image] is the screenshot shown above the title.
 */
class DocsExample(
    val title: String,
    val summary: String,
    val activity: Class<out Activity>,
    @DrawableRes val image: Int? = null,
    val imageDescription: String? = null,
)

/** A heading and the examples under it, in the order of the MapsGL examples page. */
class DocsExampleSection(
    val title: String,
    val examples: List<DocsExample>,
)

/**
 * Documentation examples in the same groups and order as
 * https://www.xweather.com/docs/mapsgl/examples.
 *
 * MapLibre, Google Maps, Leaflet and the globe samples are other map libraries. The runtime mask
 * toggle needs `setMask`, which this SDK does not have. Forecast-model temperatures and the
 * day/night overlay need the 1.9 layer codes and `addDayNightOverlay`.
 */
object DocsExamplesCatalog {

    val sections: List<DocsExampleSection> = listOf(
        DocsExampleSection(
            "Getting Started",
            listOf(
                DocsExample(
                    "Using MapsGL with Mapbox",
                    "Wire up a map, a controller and two weather layers.",
                    DisplayWeatherMapActivity::class.java,
                    R.drawable.example_display_weather_map,
                    "Radar and weather alerts over the Gulf of Mexico",
                ),
                DocsExample(
                    "Controlling layer ordering",
                    "Stack weather layers and pin an underlay band below the basemap.",
                    LayerOrderingActivity::class.java,
                    R.drawable.example_layer_ordering,
                    "Weather layers stacked below basemap boundaries",
                ),
            ),
        ),
        DocsExampleSection(
            "Styling Weather Layers",
            listOf(
                DocsExample(
                    "Customizing radar color scale",
                    "Swap the precipitation-type radar palette, and watch the legend follow.",
                    CustomRadarColorscaleActivity::class.java,
                    R.drawable.example_custom_radar_colorscale,
                    "Radar recoloured with a custom scale",
                ),
                DocsExample(
                    "Customizing temperature colors",
                    "Replace the temperature colour scale, as a gradient or in fixed bands.",
                    CustomTempsFillActivity::class.java,
                    R.drawable.example_custom_temps_fill,
                    "Temperatures painted with a custom colour scale",
                ),
                DocsExample(
                    "Adjusting temperature draw range in real time",
                    "Clip temperatures to a band and change that band live.",
                    AdjustDrawRangeActivity::class.java,
                    R.drawable.example_adjust_draw_range,
                    "Temperatures drawn only between the minimum and maximum sliders",
                ),
                DocsExample(
                    "Create a freeze layer",
                    "Clip and band temperatures into agricultural frost categories.",
                    FrostFreezeLayerActivity::class.java,
                    R.drawable.example_frost_freeze_layer,
                    "Temperatures banded into frost and freeze categories",
                ),
                DocsExample(
                    "Create a wind speed categories layer",
                    "Show only notable wind, painted as named categories.",
                    WindSpeedCategoriesActivity::class.java,
                    R.drawable.example_wind_speed_categories,
                    "Wind of 15 mph and up, painted as named categories",
                ),
                DocsExample(
                    "Customizing wind particles",
                    "Tune particle density, speed, trails and colour on the wind field.",
                    CustomWindParticlesActivity::class.java,
                    R.drawable.example_custom_wind_particles,
                    "Wind particles with a custom colour scale and density",
                ),
                DocsExample(
                    "Adjusting particle draw range in real time",
                    "Draw wind particles only inside a speed band, and move that band live.",
                    ParticleDrawRangeActivity::class.java,
                    R.drawable.example_particle_draw_range,
                    "Wind particles drawn only between 0 and 11 mph",
                ),
                DocsExample(
                    "Custom earthquake symbols using a shader",
                    "Draw earthquake icons with a custom fragment shader.",
                    CustomEarthquakeShaderActivity::class.java,
                    R.drawable.example_custom_earthquake_shader,
                    "Earthquake icons drawn by a custom shader",
                ),
                DocsExample(
                    "Custom fire symbols using a shader",
                    "Draw wildfire icons with a custom fragment shader.",
                    CustomFiresShaderActivity::class.java,
                    R.drawable.example_custom_fires_shader,
                    "Wildfire icons drawn by a custom shader",
                ),
                DocsExample(
                    "Custom lightning symbols using a shader",
                    "Draw lightning strikes with a custom fragment shader.",
                    CustomLightningShaderActivity::class.java,
                    R.drawable.example_custom_lightning_shader,
                    "Lightning strikes drawn by a custom shader",
                ),
            ),
        ),
        DocsExampleSection(
            "Features & Filtering",
            listOf(
                DocsExample(
                    "Custom alert styles",
                    "Colour alert polygons by severity instead of by alert type.",
                    CustomAlertStylesActivity::class.java,
                    R.drawable.example_custom_alert_styles,
                    "Alert polygons coloured by severity",
                ),
                DocsExample(
                    "Custom lightning strike styling",
                    "Fade lightning strikes out as they age, using a step expression.",
                    CustomLightningStylesActivity::class.java,
                    R.drawable.example_custom_lightning_styles,
                    "Lightning strikes fading with age",
                ),
                DocsExample(
                    "Filter weather alerts by category",
                    "Draw a subset of a layer's features with a filter expression.",
                    FilterAlertsActivity::class.java,
                    R.drawable.example_filter_alerts,
                    "Weather alerts filtered to one category",
                ),
            ),
        ),
        DocsExampleSection(
            "Masking Data",
            listOf(
                DocsExample(
                    "Adding a land mask to a raster layer",
                    "Clip Blue Marble imagery to land or to water.",
                    LayerMaskingActivity::class.java,
                    R.drawable.example_land_mask,
                    "Blue Marble imagery clipped to land, with the sea left clear",
                ),
            ),
        ),
        DocsExampleSection(
            "Animation",
            listOf(
                DocsExample(
                    "Set up timeline animation controls",
                    "Play, pause, resume and scrub the timeline from your own controls.",
                    TimelineControlsActivity::class.java,
                    R.drawable.example_timeline_controls,
                    "Play and pause controls on the temperature timeline",
                ),
                DocsExample(
                    "Change timeline range",
                    "Move the animation window between history and forecast at runtime.",
                    ChangeTimelineRangeActivity::class.java,
                    R.drawable.example_change_timeline_range,
                    "Timeline range switched between history and forecast",
                ),
                DocsExample(
                    "Preload animated weather data",
                    "Fetch every frame up front so the first loop plays without stalling.",
                    PreloadAnimationDataActivity::class.java,
                    R.drawable.example_preload_animation,
                    "Wind speeds with every animation frame already loaded",
                ),
            ),
        ),
        DocsExampleSection(
            "Legends, Units & Readouts",
            listOf(
                DocsExample(
                    "Customizing the heat index legend",
                    "Relabel a legend with categories instead of values.",
                    CustomHeatIndexLegendActivity::class.java,
                    R.drawable.example_custom_heat_index_legend,
                    "Heat index legend with custom category labels",
                ),
                DocsExample(
                    "Change map units",
                    "Change one quantity's units and watch every layer, legend and readout follow.",
                    ChangeMapUnitsActivity::class.java,
                    R.drawable.example_change_map_units,
                    "Map units changed and the readout updated",
                ),
                DocsExample(
                    "Temperature text labels using GeoJSON places",
                    "Label the 100 largest US places with the temperature sampled there.",
                    TemperaturePlacesQueryActivity::class.java,
                    R.drawable.example_temperature_places,
                    "Temperatures labelled on the largest US places",
                ),
                DocsExample(
                    "Wind speed text labels using custom GeoJSON",
                    "Label cities around Seattle with the wind speed sampled there.",
                    WindSpeedGeoJsonQueryActivity::class.java,
                    R.drawable.example_wind_speed_geojson,
                    "Wind speed labelled on cities around Seattle",
                ),
            ),
        ),
        DocsExampleSection(
            "Your Own Data",
            listOf(
                DocsExample(
                    "Adding a custom raster layer",
                    "Draw your own {z}/{x}/{y} image tiles.",
                    AddRasterLayerActivity::class.java,
                    R.drawable.example_add_raster_layer,
                    "Satellite geocolor imagery over Europe at partial opacity",
                ),
                DocsExample(
                    "Adding a custom vector tile layer",
                    "Render a vector tile source, coloured from each feature's own properties.",
                    AddVectorLayerActivity::class.java,
                    R.drawable.example_add_vector_layer,
                    "North Atlantic coastlines outlined in dark grey lines",
                ),
                DocsExample(
                    "Add a custom GeoJSON layer",
                    "Draw your own GeoJSON geometry, filled and outlined from one source.",
                    AddGeoJsonLayerActivity::class.java,
                    R.drawable.example_add_geojson_layer,
                    "A polygon of Portugal filled in red with a thicker red outline",
                ),
                DocsExample(
                    "Cities text scaled by population",
                    "Place labels sized by importance. The published example uses Nextzen; this uses the SDK cities tiles.",
                    CitiesTextActivity::class.java,
                    R.drawable.example_cities_text,
                    "City names sized by importance",
                ),
            ),
        ),
    )
}
