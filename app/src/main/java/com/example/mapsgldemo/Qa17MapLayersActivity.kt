package com.example.mapsgldemo

import com.example.mapsgldemo.maplayers.MapLayersActivity
import java.util.Date

/** Shared 1.7.0 QA defaults: timeline from 5 days ago through now. */
open class Qa17MapLayersActivity : MapLayersActivity() {

    override fun applyMapLayersTimelineRange() {
        val end = Date()
        val start = Date(end.time - 5L * 24 * 60 * 60 * 1000)
        applyMapLayersTimelineStartEnd(start, end)
    }
}
