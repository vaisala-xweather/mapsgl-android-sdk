# Changelog

## v1.4.0
*March 3rd, 2026*

### ✨ Features

* Add legends support for MapController, allowing a configurable legend view to be displayed along with the MapsGL map content. See the Legend documentation for more information.

* Add preconfigured legend configurations for all supported weather layers so that active layers automatically display an associated legend when active.

### 🐞 Bug Fixes

 * Fix issue where loading indicator would show indefinitely. (fixes [#27](https://github.com/vaisala-xweather/mapsgl-android-sdk/issues/27))

## v1.3.1
January 15th, 2026

### 🐞 Bug Fixes

* Fix glitchy display of particle layers after adjusting the timeline. (fixes [#24](https://github.com/vaisala-xweather/mapsgl-apple-sdk/issues/24))
* Fix incorrect colors for Precipitation layer.

## v1.3.0
December 10th, 2025

### ✨ Features

* Add data inspector control support for MapController, allowing users to tap on the map and view detailed information about the weather data at that location. See the data inspector control documentation for more information.
* Add preconfigured data presentations for all supported weather layers to use when formatting their data values shown in a data inspector control.
* Add map feature querying support via MapController#query(coord:layerIds:).

### 🐞 Bug Fixes

* Fix particle layers sometimes disappearing at certain zoom levels
* Fix missing data in Fire layer

## v1.2.5
November 7th, 2025

### 🐞 Bug Fixes

* Fix layers sometimes not appearing when added to MapController
* Fix for issue where MapController could get stuck in a loading state.

## v1.2.4
September 24th, 2025

### 🛠 Improvements

* Improve memory management
* Add land mask to Maritime layers. This land mask layer will automatically inherit the background color from the current Mapbox map style being used, which currently has limitations when using one of the "satellite" Mapbox styles.

### 🐞 Bug Fixes

* Increased Stability during timeline animation
* Vector Layers no longer dissapear after Mapbox style changes at runtime.
* Fixed issue where radar could lose snow colors after being removed and added again.

### ⚠️ Known Issues

* To accommodate rectangular particles for wave and swell layers, particle size is now represented by
  ```size: Size(width: Int, height: Int)```
  instead of
  ```size: Double```
  For round particles, for example wind particles, you only need to specify one parameter, for example ```size = Size(4)```
  Please make the necessary changes in your code if necessary.

## v1.2.3
July 31st, 2025

### 🛠 Improvements

* Better texture memory management.
* Rectangular particles for wave and swell particle layers.

### 🐞 Bug Fixes

* Fix for a crash that can occur when repeatedly adding and removing layers. 

### ⚠️ Known Issues

* To accommodate rectangular particles for wave and swell layers, particle size is now represented by 
```size: Size(width: Int, height: Int)``` 
instead of
```size: Double```
Please make the necessary changes in your code if necessary.

<Alert variant="warn">**Note:** The MapsGL SDK for Android does not currently support all features provided by the core [MapsGL Javascript SDK](https://www.xweather.com/docs/mapsgl) and are currently in development. Refer to the [development roadmap](./roadmap) for information regarding which versions of the MapsGL SDK for Android support various MapsGL features as they become available.</Alert>

## v1.2.2
*July 11, 2025*

### 🐞 Bug Fixes

* Fix for crash that can occurr with no internet connection at startup.
* Fix for errors when using a timezone with a positive offset.

## v1.2.1
*July 10, 2025*

### 🐞 Bug Fixes

* Fix for WorkerThreadException that can occur when adding layers at startup.

## v1.2.0
*June 27, 2025*

### ✨ Features

* All vector layers are now supported, including most fill, line, circle, heatmap, symbol, and text weather layers.
* Add support for vector paint types and style expressions for data-driven styling of vector layers.
* Add support for Admin layers.

### 🛠 Improvements

* Support providing a GeoJSON string as the data for a GeoJSON data source in addition to the existing remote URL or Turf feature collection methods.

### ⚠️ Known Issues

* Built-in layers under the ["Roads"](https://www.xweather.com/docs/mapsgl/weather-layers#roads) categories are not yet supported.
  Road weather layers are planned for a future release.

### 🐞 Bug Fixes

* Radar now displays rain and mix precipitation, in addition to rain.

## v1.2.0-beta.1
June 18, 2025

✨ Features
- Add initial support for vector layers from vector tile and Geo JSON data sources. This includes support for most fill, line, circle, heatmap, symbol, and text weather layers.

- Add support for vector paint types and style expressions for data-driven styling of vector layers.

🛠 Improvements
- Added over 80 new layers.
- Improved rendering quality and appearance of encoded layers to better align with the MapsGL Javascript SDK.

⚠️ Known Issues
Built-in layers under the "Admin", "Roads", and "Lightning-Density" categories are not yet supported. Admin layers and "Lightning-Density" are planned for the final release of 1.2.0, while road weather layers are planned for a future release. 

## v1.1.0
May 23, 2025

✨ Features
- Added animation and timeline support to MapController. All currently supported weather layers support animation and timeline functionality.

🛠 Improvements
- Better multithreading for faster tile downloads.
- Improved rendering quality and appearance of particle layers to better align with the MapsGL Javascript SDK.
- Particle visualizations (like wind or currents) now maintain a natural and consistent density whether you're zoomed way in or zoomed far out.
- Large particles now have a rounder, more refined appearance.

🐞 Bug Fixes
- Fix an issue where session tokens were not being re-requested when they expired during an active session.

## 1.1.0-beta.2
* April 30th, 2025**

### 🛠 Improvements
- Optimized graphics rendering for smoother map panning, zooming, and overall responsiveness, especially with complex data layers.
- Enhanced particle rendering to only process visible particles, leading to smoother performance and reduced battery usage when viewing particle data.
- Fixed an issue that could cause choppiness during timeline playback for animated data layers.
- Particle visualizations (like wind or currents) now maintain a natural and consistent density whether you're zoomed way in or zoomed far out.
- Removed limitations on how much particle data can be shown at once, allowing for more complete visuals across multiple world copies.
- Particles in visualizations now have a rounder, more refined appearance.

### 🐞 Bug Fixes
- Fixed an issue preventing historical data intervals from displaying correctly for Snow Depth and Air Quality layers.
- Fixed an issue preventing historical data intervals from displaying correctly for Snow Depth and Air Quality layers.

## 1.1.0-beta.1
*April 16, 2025*

### ✨ Features
- Add support for map timeline and layer animation. All supported weather layers are now animatable.

### 🛠 Improvements
- Better multithreading for faster tile downloads.
- Particle layers now use compute shaders for improved performance.
- Particle layers now more closely match the JavaScript version.


## 1.0.1
*November 22, 2024*

### 🛠 Improvements

* Update `WeatherService.Radar` to use separate color bands for rain, snow, and mixed precipitation.

### 🐞 Bug Fixes

* Fix an issue where `WeatherService.WavePeriods` returned `WeatherService.WaveHeights` instead.
* Fix an issue where `WeatherService.AirQualityIndexCategories` returned `WeatherService.AirQualityIndex` instead.
* Fix an issue where `WeatherService.SwellPeriods3` returned `WeatherService.SwellHeights3` instead.

### ⚠️ Known Issues

* Due to the way that earlier versions of Mapbox handle mutliple world copies, please use Mapbox version 11.3.0+.

## 1.0.0
*October 29, 2024*

### ✨ Features

* Add support for fully stylable particle layers.
* Improve handling for screen and device rotation.

### 🛠 Improvements

* Improve rendering performance when scrolling and zooming.
* Update colors scales for `WeatherService.Precipitation`, `WeatherService.OceanCurrents`, and `WeatherService.SnowDepth`.
* Fix particle transitions across tile boundaries.

### 🐞 Bug Fixes

* Fix an issue where the maximum number of world copies being rendered when zoomed out in landscape mode was clamped at 2.
* Fix issue with sporadic screen flashing at high zoom levels.
* Fix an issue where tiles across the anti-meridian from the center would be rendered at lower detail levels.
* Fix render lag for encoded raster data rendering relative to the underlying map layer during fast scrolling.

### ⚠️ Known Issues

* Due to the way that earlier versions of Mapbox handle mutliple world copies, please use Mapbox version 11.3.0+.
* `WeatherService.Radar` layer does not currently depict precipitation types (mix, snow).

## 1.0.0-beta.2
*Aug 20, 2024*

### ✨ Features

* Add particle WindParticles layer.
* Add Maritime and Air Quality layer categories as well as Snow Depth and Wind Gust layers.
* Better support in-app screen rotation.

### 🛠 Improvements

* Improve image download and caching and overall performance.
* Update color scales to better align with the MapsGL Javascript SDK.

### 🐞 Bug Fixes

* Fix an issue where tiles would not load until map movement when the map was initialized at a zoom level past the max data zoom.

### ⚠️ Known Issues

* Particle trail rendering and size configuration are not yet supported.
* Tile partials currently make use of ancestors, but not decendants.
* Displays a maximum of 2 world copies. This can cause visual issues when zoomed out in landscape mode and three world copies should be visible.

## 1.0.0-beta.1
*Jul 1, 2024*

* Initial beta release with support for raster and encoded raster weather layers using Mapbox GL.



`## 0.2.005 (2024-10-01) -------------------------------------------------------------------------
### 🛠 Improvements
* MapsGL Layers are now synced well with the underlying Mapbox map.


`## 0.2.004 (2024-09-26) -------------------------------------------------------------------------
### 🛠 Improvements
* Particle Trails now working very well. 

`## 0.2.002 (2024-09-04) -------------------------------------------------------------------------
### ✨ Features
* Particle Trails working (per tile basis). Will need to convert to per-screen.


`## 0.2.001 (2024-08-27) -------------------------------------------------------------------------
### ✨ Features
* beforeID now works in MapController.addWeatherLayer

### 🛠 Improvements
* Tile partials now work with particles. This fixes zooming and display at levels beyond maxZoom
* Clumping of particles along tile edges eliminated.

`## 0.1.004 (2024-08-26) -------------------------------------------------------------------------
First commit after beta.2
### ✨ Features
* Added Snow Depth, Wind Gust layers.

### 🐞 Bug Fixes
* Now can display 3+ world copies in landscape mode.

### 🛠 Improvements
* Particle speed now consistent across zoom levels (appears faster zoomed in)

`## 0.1.003 (2024-08-14) -------------------------------------------------------------------------
### 🛠 Improvements
* Toggling a layer on and off now puts it on top
* Wind Particles now fade in and out
* Wind Particles have offset lifespans
* Wind particles use proper colors

`## 0.1.002 (2024-08-09) -------------------------------------------------------------------------
### ✨ Features
* Particles partially working. Movement and color based on underlying texture.

### 🐞 Bug Fixes
* Fixed padded tile offset when copying tile to padded tile in tile that caused 1px vertical black line in texture

`## 0.1.001 (2024-07-29) -------------------------------------------------------------------------
### ✨ Features
* Now handles screen rotation. Tiles remain square when switching aspect ratio.

`## 0.0.335 (2024-07-26) -------------------------------------------------------------------------

### 🛠 Improvements
* Raster Tile Layers now use mapZoom properly and look more like the JS version. Set maxZoom to 6.
* Layers that use the same source now share tile cache, for quicker performance and less downloads.

`## 0.0.334 (2024-07-19) -------------------------------------------------------------------------

### 🐞 Bug Fixes
* Now Downloads the correct tiles when a layer is activated at any zoom level.

`## 0.0.333 (2024-07-17) -------------------------------------------------------------------------
### ✨ Features

### 🐞 Bug Fixes
* AQI Common now working
* Fixed bug where adding/activating layer at zooms above 0, tiles wouldn't show until map movement
* Fixed bug where layer tiles  would not download when activated when zoomed past maxZoom

`## 0.0.332 (2024-07-12) -------------------------------------------------------------------------

### 🐞 Bug Fixes
* Fixed issue in Ocean Currents and Visibility where transparent areas where too transparent (alpha was ^2 in shader)
* 

### 🛠 Improvements
* More accurate 24 hour / 1 hour temp changes.
* ImageTileSource credentials now dynamic
* updateColorRangeFactor and updateColorSampleOffset work, no longer hardcoded in shader

`## 0.0.331 (2024-07-10) -------------------------------------------------------------------------
### ✨ Features
* AQI Layers now working.

### 🐞 Bug Fixes
* Fixed wrong color mapping in Air Quality datasets by changing ColorLookupTable delta 
  from colorstop range to data range based on drawRange.
 * Fixed colorstops in Air quality where the rightmost color was showing as black 
  due to missing "F" at the front of the value

### 🛠 Improvements
* Adding layer to map now matches other versions (mapController.addWeatherLayer("temperatures"))

`## 0.0.330 (2024-07-03) -------------------------------------------------------------------------
### ✨ Features
* Added missing maritime layers.

### 🐞 Bug Fixes
* Possibly fixed https://medialogicgroup.atlassian.net/browse/MAPSGLAND-103

### 🛠 Improvements
* Removed legacy layers

`## 0.0.329 (2024-07-01) -------------------------------------------------------------------------

### ✨ Features
* First commit after v1.0.0-beta.1
* Started work on color scale legend

### 🐞 Bug Fixes
* Fixed performance issue in demo app where touching map ran a lot of extra ui functions and slowed map navigation.

### 🛠 Improvements
* No longer create all layers on startup.
* Temperature 1 hour /24 hour, Precipitation, Windspeed now accurate
* NoData now shows "blank" spots instead of the leftmost color of the color scale

## 0.0.328 (2024-06-11) -------------------------------------------------------------------------

### ✨ Features
* UI style looks more like javascript demo
* About 30 datasets implemented

### 🐞 Bug Fixes
* ColorStops with alpha values are now handled properly when creating color band texture
* Fixed partials across dateline being stretched along x axis.

### 🛠 Improvements
* Now request metadata only once for each dataset


## 0.0.327 (2024-05-31) -------------------------------------------------------------------------

### 🐞 Bug Fixes
* Fixed seams on blended tiles by truncating texture coords by 2.1 pixels instead of 2.0 pixels.
* Fixed tile blending across anti-meridian with added logic in TileCoord.getNeighbor
* MaxZoom in TileLayer getVisibleCoords() now refers to source.metadata.data.maxZoom instead of hardcoded
  value in Camera
* Fixed segFault when changing datasets. Was caused by setting colorband uniform more than once.

### 🛠 Improvements
* Tiles now blend at corners. No more "rainbow" points.
* padding crop now done in shader instead of in uv geometry.
* If neighbor tiles are unavailable, tiles are padded with edge pixels.

## 0.0.326 (2024-05-21) -------------------------------------------------------------------------

### ✨ Features
* Edge blending partially works. Seams visible in places around 9+ zoom. Doesn't integrate well with partials.

## 0.0.325 (2024-05-14) -------------------------------------------------------------------------

### 🐞 Bug Fixes
* Fixed crosshatch artifacts that were noticeable around the data pixels at zoom levels above 5.
  The issue was caused texture mipmaps.

* Fixed one-frame flicker on tiles when crossing anti meridian

### 🛠 Improvements
* Started implementing tile-edge expansion (not functional yet)

## 0.0.324 (2024-05-10) -------------------------------------------------------------------------

### ✨ Features
* Tile Partial ancestors now working. If a tile is not loaded yet, it will check for a lower zoom tile.
* MaxZoom used to avoid downloading redundant tiles at higher zoom levels.

### 🐞 Bug Fixes
* Fixed tile flicker when zooming between data zoom levels.

### 🛠 Improvements
* Now using same fragment shader as WebGL version. Some data inputs are incorrect, so tile seams are visible.

## 0.0.323 (2024-05-02) -------------------------------------------------------------------------

### 🐞 Bug Fixes
* Fixed issue where zoom level 1 would not load 
  (related to persistent / tile partials in TileCache.updateTile)

*  Fixed issue with map-scaling on large displays with low denisty.
*  Fixed issue when crossing back and forth over anti-meridian, was caused by optimization in getRenderables()

### 🛠 Improvements
* Starting implementing maxZoom to avoid using pixel doubled textures
* Generates interpolated colorband texture
* Uses colorband texture as lookup table within shader
* "beforeID" in MapboxMapController now works, so admin labels are visible over our drawn tile maps.

## 0.0.322 (2024-04-18) -------------------------------------------------------------------------

### ✨ Features
* Downloads tiles from the center outward.
* Shader coloring for Encoded Tiles works at basic level (In progress)

### 🐞 Bug Fixes
* All Datasets now working (Conditions and Air Quality)

### 🛠 Improvements
* Changed base urls from https://dev.v1.api.mapsgl.aerisapi.com to https://prod.v1.api.mapsgl.aerisapi.com

## 0.0.321 (2024-04-12) -------------------------------------------------------------------------

### ✨ Features
* Supports all Weather Conditions Datasets (No Air Quality yet)

### 🛠 Improvements
* More work towards colorful encoded tile display: EncodedDataRenderer, EncodedTileLayer... WIP

## 0.0.320 (2024-04-01) -------------------------------------------------------------------------

### ✨ Features
* Requesting and downloading Encoded Tiles
* Displaying Encoded Tiles for 3 datasets in Temperature group

### 🐞 Bug Fixes
* Fixed tile scale being wrong at tablet resolutions 
  by using a density multiplier in in MapBoxMapController.updateCamera

### 🛠 Improvements

* Transparency now works in the shader.
* Moved some GL handles from Renderpass to Program class

## 0.0.31 (2024-03-18) ------------------------------------------------------------------------

### 🐞 Bug Fixes
* Fixed zooming 'popping' glitch on zoom levels above 1

### 🛠 Improvements
* Upgraded Mapbox SDK to 11.0.0
* Removed wraparound offset from mesh and render path in general, moved to Renderable creation.
* Updated listeners into callbacks in MainActivity for Mapbox 11 compliance

## 0.0.3 (2024-03-29) -------------------------------------------------------------------------

### 🛠 Improvements
* GL overlay now tracks underlying map properly with flinging and tilt

## 0.0.2 (2024-02-28) -------------------------------------------------------------------------

### ✨ Features
* Multi-layer support

### 🐞 Bug Fixes
* Fixed alignment when zooming at levels 0 and 1, still need work on higher levels

### 🛠 Improvements
* One mesh used for all renderables.
* Moved all shader code out of MapBoxLayerHost, most went to RenderPass
* Started using ModelView matrix
* Can now continuously scroll around International Data Line with no flicker
* Now uses Projection, View, and Model matrices
* Visible map tiles now load as needed, without user intervention

## 0.0.1 (2024-02-16) -------------------------------------------------------------------------

### 🐞 Bug Fixes
* Fixed concurrent modification crash in tileLayer bindTextures()
* Improvements to zoom code, Still need to correctly incorporate world and view matrices.




