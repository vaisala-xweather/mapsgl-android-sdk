package com.example.mapsgldemo

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.maplayers.LayerMenu
import com.example.mapsgldemo.maplayers.LayerMenuEntry
import com.example.mapsgldemo.maplayers.LayerMenuSection
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.weather.LayerCode

/** Showcase entry for tropical map-time playback; back returns to [ShowcaseMenuActivity]. */
class ShowcaseTropicalActivity : MapTimeFilterActivity() {

    override fun backNavigationActivity(): Class<out AppCompatActivity> = ShowcaseMenuActivity::class.java

    override fun layerMenuSections(): List<LayerMenuSection> = listOf(
        LayerMenuSection(
            heading = "Tropical cyclones",
            entries = listOf(
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_ICONS, "Storm icons"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_LINES, "Track lines"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_FORECAST_LINES, "Forecast lines"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_NAMES, "Storm names"),
            ),
        ),
        LayerMenuSection(
            heading = "Severe",
            entries = listOf(
                LayerMenuEntry(LayerCode.CONVECTIVE, "Convective outlook"),
            ),
        ),
    )

    override fun onLayerMenuReady(layerMenu: LayerMenu, controller: MapboxMapController) {
        super.onLayerMenuReady(layerMenu, controller)
        controller.timeline.play()
        binding.timelineView.timelineControls.updatePlayButtonImage(true, binding.timelineView)
    }
}
