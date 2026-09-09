package com.example.mapsgldemo

import androidx.appcompat.app.AppCompatActivity
import com.xweather.mapsgl.types.Coordinate

/**
 * 1.7.0 QA: GLES place labels (city / state / country). Not data-query `*-text` layers.
 * Mapbox Dark settlement / state / country labels are hidden so only MapsGL place text shows.
 */
class PlacesTextActivity : Qa17MapLayersActivity() {

    override fun textLayersOnly(): Boolean = true

    override fun layerMenuPlacesSectionFirst(): Boolean = true

    override fun showStencilDemoButtons(): Boolean = false

    override fun hideMapboxPlaceNameLabels(): Boolean = true

    override fun backNavigationActivity(): Class<out AppCompatActivity> =
        MoreExamplesMenuActivity::class.java

    override fun initialCameraPosition(): Pair<Coordinate, Double> =
        Coordinate(39.0, -98.0) to 5.0
}
