package com.example.mapsgldemo

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.maplayers.LayerMenu
import com.example.mapsgldemo.maplayers.LayerMenuEntry
import com.example.mapsgldemo.maplayers.LayerMenuSection
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * 1.7.0 QA: vector **mapTime filter** animation (JS/iOS parity).
 *
 * Tropical tracks/points scrub with `timestamp <= map-time`; current position markers stay
 * visible. Convective outlook fill/outline is gated by `details.range.min/maxTimestamp`.
 */
open class MapTimeFilterActivity : Qa17MapLayersActivity() {

    override fun showStencilDemoButtons(): Boolean = false

    override fun backNavigationActivity(): Class<out AppCompatActivity> =
        MoreExamplesMenuActivity::class.java

    override fun initialCameraPosition(): Pair<Coordinate, Double> =
        Coordinate(25.0, -65.0) to 3.5

    override fun layerMenuSections(): List<LayerMenuSection> = listOf(
        LayerMenuSection(
            heading = "Tropical",
            entries = listOf(
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES, "Active All"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_ICONS, "Active All (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_POINTS, "Active Track Points"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_POINT_ICONS, "Active Track Points (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_FORECAST_POINTS, "Active Forecast Points"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_FORECAST_POINT_ICONS, "Active Forecast Points (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_LINES, "Active Track Lines"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_FORECAST_LINES, "Active Forecast Lines"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_POSITIONS, "Active Positions"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_POSITION_ICONS, "Active Positions (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_NAMES, "Active Names"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_BREAK_POINTS, "Active Breakpoints"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_ARCHIVE, "Archive All"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_ARCHIVE_ICONS, "Archive All (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_POINTS_ARCHIVE, "Archive Track Points"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_POINT_ICONS_ARCHIVE, "Archive Track Points (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_LINES_ARCHIVE, "Archive Track Lines"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_INVESTS, "Invests"),
            ),
        ),
        LayerMenuSection(
            heading = "Severe",
            entries = listOf(
                LayerMenuEntry(LayerCode.CONVECTIVE, "Convective"),
                LayerMenuEntry(LayerCode.CONVECTIVE_OUTLINE, "Convective Outlook (Outline)"),
            ),
        ),
    )

    override fun onLayerMenuReady(layerMenu: LayerMenu, controller: MapboxMapController) {
        layerMenu.activateLayerIfPresent(controller, LayerCode.TROPICAL_CYCLONES_ICONS)
    }
}
