package com.example.mapsgldemo

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.maplayers.LayerMenu
import com.example.mapsgldemo.maplayers.LayerMenuEntry
import com.example.mapsgldemo.maplayers.LayerMenuSection
import com.example.mapsgldemo.maplayers.MapLayersActivity
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import java.util.Date

/**
 * Flagship SDK showcase: a short weather catalog, temperatures already on the map, timeline playing.
 */
class ShowcaseWeatherActivity : MapLayersActivity() {

    override fun backNavigationActivity(): Class<out AppCompatActivity> = ShowcaseMenuActivity::class.java

    override fun initialCameraPosition(): Pair<Coordinate, Double> =
        Coordinate(39.0, -98.0) to 3.6

    override fun applyMapLayersTimelineRange() {
        val end = Date()
        val start = Date(end.time - 24L * 60 * 60 * 1000)
        applyMapLayersTimelineStartEnd(start, end)
    }

    override fun layerMenuSections(): List<LayerMenuSection> = listOf(
        LayerMenuSection(
            heading = "Conditions",
            entries = listOf(
                LayerMenuEntry(LayerCode.TEMPERATURES, "Temperatures"),
                LayerMenuEntry(LayerCode.FEELS_LIKE, "Feels like"),
                LayerMenuEntry(LayerCode.HUMIDITY, "Humidity"),
                LayerMenuEntry(LayerCode.PRECIPITATION, "Precipitation"),
            ),
        ),
        LayerMenuSection(
            heading = "Radar and satellite",
            entries = listOf(
                LayerMenuEntry(LayerCode.RADAR, "Radar"),
                LayerMenuEntry(LayerCode.SATELLITE_GEOCOLOR, "Satellite"),
            ),
        ),
        LayerMenuSection(
            heading = "Wind",
            entries = listOf(
                LayerMenuEntry(LayerCode.WIND_SPEEDS, "Wind speeds"),
                LayerMenuEntry(LayerCode.WIND_PARTICLES, "Wind particles"),
                LayerMenuEntry(LayerCode.WIND_BARBS, "Wind barbs"),
            ),
        ),
        LayerMenuSection(
            heading = "Severe",
            entries = listOf(
                LayerMenuEntry(LayerCode.ALERTS, "Alerts"),
                LayerMenuEntry(LayerCode.LIGHTNING_STRIKES, "Lightning"),
                LayerMenuEntry(LayerCode.LIGHTNING_STRIKES_HEAT, "Lightning heatmap"),
            ),
        ),
    )

    override fun onLayerMenuReady(layerMenu: LayerMenu, controller: MapboxMapController) {
        layerMenu.activateLayerIfPresent(controller, LayerCode.TEMPERATURES)
        controller.timeline.play()
        binding.timelineView.timelineControls.updatePlayButtonImage(true, binding.timelineView)
    }
}
