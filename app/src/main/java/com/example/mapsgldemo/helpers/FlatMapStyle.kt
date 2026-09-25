package com.example.mapsgldemo.helpers

import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.StyleContract
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.style

/**
 * [styleUri] with the Mercator projection built into the style itself.
 *
 * Mapbox Maps v11 defaults to the globe projection, so a map that only becomes flat after the
 * style has loaded - `mapboxMap.setProjection(...)` from `subscribeMapLoaded`, or straight after
 * [com.xweather.mapsgl.map.mapbox.MapboxMapController] is constructed - paints a globe for the
 * first frames and then visibly snaps flat.
 *
 * Loading the projection as part of the style avoids that: [com.mapbox.maps.MapboxMap.loadStyle]
 * binds the extension on `StyleDataLoaded(STYLE)`, which is before the map draws anything from the
 * new style. A projection set on a style is also discarded by the next [loadStyle], so a screen
 * that swaps the basemap has to load the replacement flat too.
 */
fun flatStyle(styleUri: String = Style.STANDARD): StyleContract.StyleExtension =
    style(styleUri) { +projection(ProjectionName.MERCATOR) }

/**
 * Loads [styleUri] flat - see [flatStyle].
 *
 * Call this from `onCreate`, before the view's `onStart`: [MapView] loads the style from its
 * `MapInitOptions` (the globe-projected [Style.STANDARD] unless the layout says otherwise) on
 * `onStart` only if no style load has been started yet, so claiming the load here keeps the
 * default style - and its globe - off screen entirely.
 */
@JvmOverloads
fun MapView.loadFlatStyle(
    styleUri: String = Style.STANDARD,
    onStyleLoaded: Style.OnStyleLoaded? = null,
) = mapboxMap.loadStyle(flatStyle(styleUri), onStyleLoaded)
