package com.example.mapsgldemo.vector

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.AnimatedVectorLayersMenuActivity
import com.example.mapsgldemo.Qa17MapLayersActivity
import com.xweather.mapsgl.types.LayerType

/** 1.7.0 QA: GLES vector fill layers only. */
class VectorFillMapLayersActivity : Qa17MapLayersActivity() {
    override fun vectorLayersOnly(): Boolean = true
    override fun vectorLayerTypeFilter(): LayerType = LayerType.fill
    override fun showStencilDemoButtons(): Boolean = false
    override fun backNavigationActivity(): Class<out AppCompatActivity> =
        AnimatedVectorLayersMenuActivity::class.java
}
