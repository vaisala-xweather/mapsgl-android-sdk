package com.example.mapsglmaps.maps.types

class DataSourceConsumer(override val layerId: String,
                         override val sourceLayerId: String,
                         override val sourceLayerType: String,
                         override val type: String,
                         override val style: Pair<String, *>,
                         override val options: Pair<String, *>) : LayerMetadata(layerId, sourceLayerId, sourceLayerType, type, style, options)

open class LayerMetadata (
    override val layerId: String,
    override val sourceLayerId: String,
    override val sourceLayerType: String,
    override val type: String,
    override val style: Pair<String, *>,
    override val options: Pair<String, *>
):ILayerMetadata

interface ILayerMetadata {
    val layerId: String
    val sourceLayerId: String
    val sourceLayerType: String
    val type: String
    val style: Pair<String, *>
    val options: Pair<String, *>
}

enum class LayerType {
    Raster,
    Circle,
    Symbol,
    Fill,
    Line,
    Sample,
    Grid,
    Heatmap,
    Contour,
    Particles
}

