package com.example.mapsgldemo.vector

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.AnimatedVectorLayersMenuActivity
import com.example.mapsgldemo.Qa17MapLayersActivity
import com.xweather.mapsgl.types.LayerType

/** 1.7.0 QA: GLES vector line layers only. */
class VectorLineMapLayersActivity : Qa17MapLayersActivity() {
    override fun vectorLayersOnly(): Boolean = true
    override fun vectorLayerTypeFilter(): LayerType = LayerType.line
    override fun showStencilDemoButtons(): Boolean = false
    override fun backNavigationActivity(): Class<out AppCompatActivity> =
        AnimatedVectorLayersMenuActivity::class.java
}
