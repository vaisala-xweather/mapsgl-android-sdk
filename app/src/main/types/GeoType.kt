package com.example.mapsglmaps

data class CoordinateBounds(
    val north: Double,
    val west: Double,
    val south: Double,
    val east: Double
)

data class Coordinate(
    var lat: Double,
    var lon: Double
)

data class Anchor(
    var x: Float,
    var y: Float,
    var z: Float,
    var angle: Float
)

/*
 * A `PlotCoordinate` represents a projected three-dimensional position where `{ x: 0, y: 0, z: 0 }`
 * represents the top-left corner of the viewport or render region, and `{ x: 1, y: 1, z: 0 }` represents
 * the bottom-right corner. The coordinate `z` value is only used for spherical and three-dimensional
 * map projections as altitude.
 */
data class PlotCoordinate(
    var x: Float,
    var y: Float,
    var z: Float
)

data class ElevationScale(

    // Converts meters to units used to describe elevation in tile space.
    // Default units in mercator space are x & y: [0, 8192] and z: meters
    var metersToTile: Float,

    // Converts meters to units used for elevation for map aligned labels.
    // Default unit in mercator space is meter.
    var metersToLabelSpace: Float
)