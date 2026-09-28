package com.example.mapsgldemo

import androidx.appcompat.app.AppCompatActivity
import com.xweather.mapsgl.types.Coordinate

/**
 * Layer browser limited to the data-query (`*-text`) catalog: conditions value labels
 * (temperatures-text, feels-like-text, and the rest) and air-quality text variants.
 */
class DataQueryTextActivity : Qa17MapLayersActivity() {

    override fun dataQueryTextLayersOnly(): Boolean = true

    override fun showStencilDemoButtons(): Boolean = false

    override fun hideMapboxPlaceNameLabels(): Boolean = true

    override fun backNavigationActivity(): Class<out AppCompatActivity> =
        DataQueryMenuActivity::class.java

    override fun initialCameraPosition(): Pair<Coordinate, Double> =
        Coordinate(39.0, -98.0) to 5.0
}
