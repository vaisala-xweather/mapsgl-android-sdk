# Changelog

## 1.8.0
*Nov 9, 2026*

### ✨ Features

* Add data-query text layers that show a weather value and the place name at city points, such as `temperatures-text`, `feels-like-text`, `humidity-text`, and wind and air-quality variants. Label units follow the legend's unit system.

    ```kotlin
    mapController.addWeatherLayer(LayerCode.TEMPERATURES_TEXT)
    ```

* Add time-series operation layers that combine a whole timeline per pixel: `precip-accum`, `temperatures-max`, `temperatures-min`, and `wind-gusts-max`. Each frame shows what has happened up to that moment.

    ```kotlin
    mapController.addOperationLayer(OperationLayerCode.TEMPERATURES_MAX)
    ```

* `MapController.setPaintProperty` now accepts sample and particle paint, such as `sample.colorscale`, `sample.drawRange`, `particle.density`, `particle.size`, and `particle.speed`, and applies the change in one call, as in MapsGL JS. Particle draw ranges can change while the layer is on the map.
* `MapController.setLayerFilter` changes a vector layer's filter while it is on the map. Pass `null` to draw everything again.
* `MapController.onLayerError` reports a layer that could not be added or could not draw, with a reason, the layer, and the cause.

    ```kotlin
    mapController.onLayerError.observe(this) { error ->
        Log.w("weather", "MapsGL: $error", error.cause)
    }
    ```

* `BoundedRange.atMost` and `BoundedRange.atLeast` let a draw range leave one end open. The open end comes from the layer's own data range.
* `GeoJSONSourceDescriptor.data` takes inline features, so static GeoJSON can be set before `addSource`.

### 🐞 Bug Fixes

* A failed data load no longer crashes the app. The layer stays on the map, reports the failure through `onLayerError`, and retries.
* Apps with `minifyEnabled true` keep their own `Log` and `println` output. The SDK's ProGuard rules no longer strip logging outside MapsGL.
* Public API that the minified SDK had renamed is reachable again, including `GlCapabilities.supportsParticleLayers`.
* Layer failures are now reported in release builds instead of being dropped.
* Layers that share a source no longer re-download each other's tiles, and the loading indicator no longer sticks at 50% or 99%.
* Polygons no longer show ghost copies from neighboring tiles, and vector tiles draw only inside their own square.
* A fill layer's `filter` decides what it draws, and the data inspector honors it.
* Layers that share a vector source no longer hide each other.
* Particle layers honor their mask and their `drawRange`, and a removed particle layer releases its GPU memory.
* Custom icon shaders written as `#version 300 es` run on every device.
* Stepped color scales no longer blend between stops, and a stop below the data range keeps its color.
* Text labels no longer freeze the map when the label texture runs out of room.
* Severe alerts disappear once the timeline passes their expiry.
* Fire icons are sized by acreage again, and contained fires turn grey.
* Storm cells and sea-level pressure contours match MapsGL JS.
* Wave and swell direction arrows stay on water, and air-quality stations are colored by their real category.
* The surface-pressure legend is labeled in pressure units.

### 📱 Demo App

* Mapbox and Xweather keys now go in `local.properties` instead of `strings.xml`, so they stay out of source control. See the README.

## 1.7.1
*Oct 9, 2026*

### 🐞 Bug Fixes
An encoded weather layer whose metadata cannot load no longer closes the app. as seen in (issue #40). The layer stays on the map without data. The first failure is reported on MapController.onLayerError as MapsGLLayerError.Reason.RENDER_FAILED, and the SDK retries the fetch. A later pan or zoom tries again.

## 1.7.0
*Sep 25, 2026*

### ✨ Features

* Vector weather layers — alerts, lightning, hail, tropical cyclones, fires, earthquakes, and the rest — now animate with the map timeline and draw alongside encoded and particle layers. Existing `addWeatherLayer` and `addLayer` calls stay the same.
* Filter vector features by the timeline with `Expression.mapTime`. Built-in tropical tracks and convective outlooks already do this.

    ```kotlin
    layer.filter = Expression.and(
        listOf(
            Expression.equals(Expression.get("featureType"), "trackPoint"),
            Expression.lessThanOrEqual(Expression.get("timestamp"), Expression.mapTime),
        )
    )
    ```

* Add place-name layers: `place-city`, `place-country`, `place-state`, `place-neighborhood`, and `places`.
* Custom symbol icons can use a GLSL fragment shader on `IconPaint.shader`. Declare `precision highp float`. After changing icon paint on a layer that is already on the map, call `MapController.refreshGlVectorLayerPaint`.
* More of the JavaScript paint and expression set evaluates on Android, including fill and circle sort order, heatmap blur, color interpolation, and `Expression.has`.

### 🐞 Bug Fixes

* A shader that fails to compile no longer closes the app. The layer is turned off and the driver's message is logged. Particle layers require OpenGL ES 3.1 ([issue #39](https://github.com/vaisala-xweather/mapsgl-android-sdk/issues/39)).
* The HTML API reference is no longer packaged inside the SDK, so it is no longer copied into your app ([issue #38](https://github.com/vaisala-xweather/mapsgl-android-sdk/issues/38)).
* Animated symbol shaders and icon spin stay smooth on devices that have been running for a long time.
* Tropical tracks, forecast cones, and icons wrap correctly across the dateline, and forecast-error cones are filled.
* Place labels no longer collide, flicker, drift, or disappear while panning and zooming.
* Vector animation no longer freezes, flashes, or leaves gaps while panning, zooming, or looping.
* Hail-threat polygons stay in the right category as the timeline moves.
* Circle outlines draw in the right order and no longer hide neighboring fills.
* Encoded layers cover both sides of the antimeridian, and `sample.meld` is honored during playback.
* The loading indicator tracks vector and raster loads, and playback no longer freezes while a vector layer is on the map.
* Radar colors for rain, snow, and mixed precipitation are updated.
* Tropical icons and outlines are closer to the JavaScript SDK.

### ⚠️ Breaking Changes

* `no.ecc.vectortile:java-vector-tile` is no longer required. Remove that dependency and the `https://maven.ecc.no/releases` repository if you added them. Also remove any `protobuf-java` exclusion or `-dontwarn` rules you added for it.
* If you read raw vector-tile features from `VectorData.rawFeatures` or `MapboxVectorFeature.from`, import `com.xweather.mapsgl.mvt` instead of `no.ecc.vectortile` and JTS. Property names such as `coordinates` and `exteriorRing` are unchanged. `addWeatherLayer` and `addLayer` are unaffected.

## 1.6.1
*Jul 30, 2026*

### ✨ Features

* Mask a layer to land, water, or to other layers on the map. `LayerMasks.land()` and `LayerMasks.water()` are the shorthands. The `useGlStencilMask` switch has been removed; a mask is applied whenever the layer sets one.

    ```kotlin
    val temperature = mapController.getConfigForCode(LayerCode.TEMPERATURES)
    temperature.layer.layerMask = LayerMasks.land()
    mapController.addWeatherLayer(temperature)
    ```

* On a long timeline, playback starts once about half the frames are ready, then switches to the full set at the next loop. The loading indicator covers the whole download.
* Weather layers have data ready sooner after you pan or zoom.
* The data inspector closes when its point scrolls off the map.

### 🐞 Bug Fixes

* Fix a crash when adding a weather layer ([issue #35](https://github.com/vaisala-xweather/mapsgl-android-sdk/issues/35)).
* Adding and removing layers no longer leaves the previous tiles on screen.
* Timeline playback resumes after you extend the range, holds the current frame when paused, and does not download extra tiles while paused or scrubbing.
* Wind barbs and arrows no longer jitter between timeline frames.
* Maritime water masks and marine land masks draw correctly.
* The data inspector hides zero precipitation, drops duplicate rows, and stays on screen at the top and bottom edges of the map.
* Vector wind draws again.
* Dark legends, contour spacing, and `drawRange` match the expected scale.
* Precipitation and particle layers stay stable while you pan.

## 1.6.0
*Apr 23, 2026*

### ✨ Features

* Add maritime direction layers such as `wave-dir` and `swell-dir`, with arrows that stay on the water and play smoothly on the timeline.

    ```kotlin
    mapController.addWeatherLayer(mapController.getConfigForCode(LayerCode.WAVE_DIR))
    ```

* `MapController.onLoadProgress` reports `total`, `completed`, `failed`, and `cancelled` in release builds.

    ```kotlin
    mapController.onLoadProgress.observe(this) { progress ->
        val label = "${progress.completed} / ${progress.total}"
    }
    ```

### 🐞 Bug Fixes

* Tapping empty map clears the data inspector, and repeated taps no longer pile up old results ([issue #31](https://github.com/vaisala-xweather/mapsgl-android-sdk/issues/31)).
* Precipitation values in the data inspector and legend match the JavaScript SDK.
* The loading indicator matches the JavaScript and iOS SDKs, including while textures are still binding.
* Alerts keep updating after the layer is removed and added again.
* Removing a gridded layer while it is still downloading no longer leaves the loading indicator stuck.
* Encoded tiles and the radar legend handle missing values and bar labels correctly.
* Wind barbs and arrows scale with screen density.
* The camera stays aligned at low pitch, and gridded animation stays correct across the dateline and across multiple world copies.

## 1.5.0
*Mar 27, 2026*

### ✨ Features

* Add contour layers, including mean sea level pressure, temperature, and wind speed.
* Call `MapController.preloadAnimationData()` to load the visible animation before playback starts. `animationOptions.shouldPreloadData` does this automatically.

### 🐞 Bug Fixes

* Encoded tiles meet cleanly at their edges, including at zoom level 1.
* The data inspector works on contour layers before playback has started.
* Newly active fires use the correct colors.
* Wind-speed contours and their legends use the right range.

## 1.4.0
*Mar 3, 2026*

### ✨ Features

* Add a legend control. Supported weather layers show their legend when they are on the map. See [Legends](/mapsgl-android-sdk/getting-started/legends).

### 🐞 Bug Fixes

* The loading indicator no longer stays on after loading has finished ([issue #27](https://github.com/vaisala-xweather/mapsgl-android-sdk/issues/27)).

## 1.3.1
*Jan 15, 2026*

### 🐞 Bug Fixes

* Particle layers no longer glitch after you move the timeline ([issue #24](https://github.com/vaisala-xweather/mapsgl-apple-sdk/issues/24)).
* Precipitation colors are corrected.

## 1.3.0
*Dec 10, 2025*

### ✨ Features

* Add the data inspector. Tap the map to read the weather value at that point. Supported layers include a formatted presentation. See [Data inspector](/mapsgl-android-sdk/controls/data-inspector).
* Query features with `MapController.query`.

### 🐞 Bug Fixes

* Particle layers no longer disappear at some zoom levels.
* The fires layer includes its data.

## 1.2.5
*Nov 7, 2025*

### 🐞 Bug Fixes

* Layers appear reliably when added.
* The map no longer gets stuck in a loading state.

## 1.2.4
*Sep 24, 2025*

### ✨ Features

* Maritime layers include a land mask. The mask uses the Mapbox style's background color.

### 🐞 Bug Fixes

* Timeline animation is more stable.
* Vector layers stay on the map after you change the Mapbox style.
* Radar keeps its snow colors after the layer is removed and added again.

## 1.2.3
*Jul 31, 2025*

### ✨ Features

* Wave and swell particles can be drawn as rectangles.

### 🐞 Bug Fixes

* Fix a crash from repeatedly adding and removing layers.

### ⚠️ Breaking Changes

* Particle `size` is a `Size`, not a `Double`. A square particle takes one number: `size = Size(4)`.

## 1.2.2
*Jul 11, 2025*

### 🐞 Bug Fixes

* Fix a crash when the device has no network at startup.
* Time zones with a positive offset no longer produce errors.

## 1.2.1
*Jul 10, 2025*

### 🐞 Bug Fixes

* Fix a crash when adding layers during startup.

## 1.2.0
*Jun 27, 2025*

### ✨ Features

* Add vector layers: fill, line, circle, heatmap, symbol, and text, including admin boundaries.
* Style those layers from feature properties.
* A GeoJSON source can be created from a GeoJSON string.

### 🐞 Bug Fixes

* Radar shows mixed precipitation as well as rain.

## 1.2.0-beta.1
*Jun 18, 2025*

### ✨ Features

* First support for vector-tile and GeoJSON layers, and for data-driven vector paint.
* More than 80 additional weather layers.
* Encoded layers are closer in appearance to the JavaScript SDK.

## 1.1.0
*May 23, 2025*

### ✨ Features

* Add timeline animation for the supported weather layers.

### 🐞 Bug Fixes

* Session tokens refresh when they expire during a session.
* Particle layers keep a consistent density as you zoom, and large particles are round.

## 1.1.0-beta.2
*Apr 30, 2025*

### 🐞 Bug Fixes

* Panning, zooming, and timeline playback are smoother.
* Snow depth and air quality show the correct historical intervals.
* Particle layers cover multiple world copies and keep a consistent density as you zoom.

## 1.1.0-beta.1
*Apr 16, 2025*

### ✨ Features

* First support for the map timeline. Supported weather layers can animate.

## 1.0.1
*Nov 22, 2024*

### 🐞 Bug Fixes

* Radar uses separate colors for rain, snow, and mixed precipitation.
* `WeatherService.WavePeriods`, `WeatherService.AirQualityIndexCategories`, and `WeatherService.SwellPeriods3` return the layer you asked for.

## 1.0.0
*Oct 29, 2024*

### ✨ Features

* Particle layers can be styled.
* The map handles screen rotation.

### 🐞 Bug Fixes

* Scrolling and zooming are smoother, including across the antimeridian and in landscape with several world copies.
* Use Mapbox Maps SDK 11.3.0 or newer.
* Colors for precipitation, ocean currents, and snow depth are updated.

## 1.0.0-beta.2
*Aug 20, 2024*

### ✨ Features

* Add wind particles, maritime and air-quality layers, snow depth, and wind gusts.
* The map handles screen rotation.

### 🐞 Bug Fixes

* Tiles load when the map opens past the maximum data zoom.
* Color scales are closer to the JavaScript SDK.

## 1.0.0-beta.1
*Jul 1, 2024*

* First beta. Raster and encoded weather layers on Mapbox.
