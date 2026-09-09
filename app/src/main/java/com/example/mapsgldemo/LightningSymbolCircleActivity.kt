package com.example.mapsgldemo

import androidx.appcompat.app.AppCompatActivity
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.types.LayerType

/**
 * 1.7.0 QA: GLES circle/symbol vector playback. Lightning products listed first.
 * CONUS at zoom 4.
 */
class LightningSymbolCircleActivity : Qa17MapLayersActivity() {

    override fun vectorLayersOnly(): Boolean = true

    override fun vectorLayerTypeFilterSet(): Set<LayerType> =
        setOf(LayerType.circle, LayerType.symbol)

    override fun layerMenuLightningSectionFirst(): Boolean = true

    override fun showStencilDemoButtons(): Boolean = false

    override fun backNavigationActivity(): Class<out AppCompatActivity> =
        MoreExamplesMenuActivity::class.java

    override fun initialCameraPosition(): Pair<Coordinate, Double> =
        Coordinate(39.0, -98.0) to 4.0
}
