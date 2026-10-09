
Xweather MapsGL SDK Demo App
================

The Xweather MapsGL SDK for Android allows a developer to quickly and easily add weather content and functionality to their Android applications. It utilizes the Xweather API backend for data loading and is built on top of an object mapping system that efficiently loads requested weather content into third-party Android applications, greatly reducing the amount of code and development needed on the developer end.

## Features
-Visualizing real-time weather and geospatial data
-High-performance layer rendering with OpenGLES
-Customizable presentation and styling of weather and geospatial information client-side

## Getting Started

View the latest installation and implementation details in the [MapsGL Android SDK documentation](https://www.xweather.com/docs/mapsgl-android-sdk/getting-started/).


## Running the Demo App
The MapsGL Android SDK includes a demo application that showcases the capabilities of the SDK. To run the demo application, follow these steps:

##### Prerequisites:
- Android Studio with Android Gradle Plugin 8.11 and Gradle 8.14. This project uses AGP 8.11.2, Gradle 8.14.3, and Kotlin 2.0.20.
- Android 9.0 or newer (minSdk 28). This project compiles against SDK 36.
- Mapbox Maps SDK for Android 11.x, configured to use the Mercator projection. This project uses `com.mapbox.maps:android-ndk27:11.15.3`.
- An [Xweather account](https://signup.xweather.com/developer) — We offer a free developer account for you to give our weather API a test drive.
- A [Mapbox account](https://www.mapbox.com/)

## Xweather API Configuration for the Xweather Demo Application
Before you can begin using the Xweather MapsGL SDK in your project, you will need to download the latest version of the SDK and ensure that you have the required Xweather API keys for your application.

##### Step 1: Get the files.
Download the latest version of the [Xweather Android SDK demo application](https://github.com/vaisala-xweather/mapsgl-android-sdk)

##### Step 2: Get access to the Xweather API.
To use the Xweather API, you will need to have valid access keys. Access keys are obtained by registering your application/namespace. To register your application, log in to Xweather with your account and look for the "APPS" section. Don't have an Xweather account? You can get one for free [here](https://signup.xweather.com/developer).

##### Step 3: Get a Mapbox API key
If you don't have a Mapbox account, you can create one for free at [www.mapbox.com](https://www.mapbox.com/). Follow the instructions to get a Mapbox API key.

## Mapbox Configuration for the Xweather Demo Applications

Once you have your Mapbox Maps API account and Xweather client id / client secret:

In **strings.xml**:

    <string name="mapbox_access_token">mapbox_secret_token</string>
	<string name="xweather_client_id">your_xweather_client_id</string>    
	<string name="xweather_client_secret">your_xweather_client_secret</string>

You are now set to run the demo application.

### To add the MapsGL SDK to your own application:

Add the string resources above to your strings.xml. In addition:

In **settings.gradle**:

    pluginManagement {
        repositories {
            google()
            mavenCentral()
        }
    }

    dependencyResolutionManagement {
        repositories {
            google()
            mavenCentral()
            maven { url 'https://jitpack.io' }
            maven {
                url 'https://api.mapbox.com/downloads/v2/releases/maven'
                authentication {
                    basic(BasicAuthentication)
                }
                credentials {
                    username = "mapbox"
                    // Use the secret token you stored in gradle.properties as the password
                    password = MAPBOX_DOWNLOADS_TOKEN
                }
            }
        }
    }

Mapbox's Maven repository is required because this demo, and the Mapbox quick start below, use the Mapbox Maps SDK. Set `MAPBOX_DOWNLOADS_TOKEN` in `gradle.properties`.

In **AndroidManifest.xml**:

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

In the app-level **build.gradle**:

    dependencies {
        implementation "com.github.vaisala-xweather:mapsgl-android-sdk:v1.7.0"
    }

#### Upgrading from 1.6.1 or earlier — remove `java-vector-tile`

As of **1.7.0** MapsGL decodes Mapbox Vector Tiles with its own reader
(`com.xweather.mapsgl.mvt.MvtReader`). **`no.ecc.vectortile:java-vector-tile` is no longer needed**
and the published POM no longer references it, nor `com.google.protobuf:protobuf-java` or
`org.locationtech.jts:jts-core`.

**Please remove it from your app.** Delete all of the following if you have them:

    dependencies {
        implementation 'no.ecc.vectortile:java-vector-tile:1.4.1'   // <- delete
    }

    repositories {
        maven { url 'https://maven.ecc.no/releases' }               // <- delete
    }

Also remove anything you added to work around that dependency, such as `exclude group: 'com.google.protobuf', module: 'protobuf-java'` exclusion rules and `-dontwarn no.ecc.vectortile.**` or `-dontwarn org.locationtech.**` ProGuard lines. Leaving `java-vector-tile` in place puts `com.google.protobuf:protobuf-java` back on the classpath, where it declares the same classes as `protobuf-javalite` and fails dexing with duplicate-class errors.

If you read raw vector-tile features from `VectorData.rawFeatures` or `MapboxVectorFeature.from`, import `com.xweather.mapsgl.mvt` instead of `no.ecc.vectortile` and JTS. Property names such as `coordinates` and `exteriorRing` are unchanged. `addWeatherLayer` and `addLayer` are unaffected.

### In your activity

Create the account, wait until the map view has been laid out, then create a `MapboxMapController`. Mapbox's latest styles default to the globe projection, which MapsGL cannot use, so set Mercator before subscribing to the map-loaded event. `mapboxMap` is nullable.

    val xweatherAccount = XweatherAccount(
        getString(R.string.xweather_client_id),
        getString(R.string.xweather_client_secret)
    )

    val mapLoadedCallback = MapLoadedCallback {
        mapController.addWeatherLayer(WeatherService.Temperatures(mapController.service))
        mapController.addWeatherLayer(WeatherService.WindParticles(mapController.service))
        // Or, with no customizations, add a layer by code:
        mapController.addWeatherLayer(LayerCode.TEMPERATURES)
    }

    binding.mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            binding.mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            mapController = MapboxMapController(binding.mapView, xweatherAccount)
            with(mapController) {
                mapboxMap?.loadStyle(Style.LIGHT)
                mapboxMap?.setProjection(projection(ProjectionName.MERCATOR))
                mapboxMap?.subscribeMapLoaded(mapLoadedCallback)
            }
        }
    })

## Reference Links

[MapsGL Android SDK](https://www.xweather.com/docs/mapsgl-android-sdk/)

## Worked Examples

Ports of the MapsGL JavaScript examples, each with a runnable screen in this app and notes on
what changes between the two SDKs:

- [Custom earthquake symbols using a shader](docs/examples/custom-earthquake-shader.md) — a
  procedural GLSL fragment shader on symbol icons, with no sprite at all
  (**Documentation Examples → Custom earthquake symbols using a shader**)
- [Custom fire symbols using a shader](docs/examples/custom-fires-shader.md) — fbm noise,
  `v_random` and `v_factor` driving an animated flame, with no sprite
  (**Documentation Examples → Custom fire symbols using a shader**)
- [Customizing the heat index legend](docs/examples/custom-heat-index-legend.md) — replacing a
  bar legend's labels with normalized, qualitative stops
  (**Documentation Examples → Customizing the heat index legend**)
- [Custom lightning symbols using a shader](docs/examples/custom-lightning-shader.md) — strike
  age driving a noise-gated flash, on a dark basemap
  (**Documentation Examples → Custom lightning symbols using a shader**)
- [Customizing lightning point styles](docs/examples/custom-lightning-styles.md) — a step
  ladder on circle fill, with the JS opacity expression folded into the colour alpha
  (**Documentation Examples → Customizing lightning point styles**)
- [Customizing radar color scale](docs/examples/custom-radar-colorscale.md) — swapping a sample
  layer's colour scale at runtime, why the assignment alone only moves the legend, and folding the
  three per-type legend bars into one
  (**Documentation Examples → Customizing radar color scale**)
- [Customizing temperature colors](docs/examples/custom-temps-fill.md) — a custom ramp banded at a
  fixed interval, and why `interval` needs `interpolate = false` to do anything
  (**Documentation Examples → Customizing temperature colors**)
- [Customizing wind particles](docs/examples/custom-wind-particles.md) — a normalized scale and
  heavier particle settings, and why replacing a paint sub-object is not the same as JS's merge
  (**Documentation Examples → Customizing wind particles**)
- [Create a freeze layer](docs/examples/frost-freeze-layer.md) — the temperatures layer as a frost
  map: a stepped three-colour scale, a draw range hiding everything warmer, and a point legend
  (**Documentation Examples → Create a freeze layer**)
- [Filter weather alerts by category](docs/examples/filter-alerts.md) — a VTEC membership filter on
  the alerts layer, switched at runtime, and the data inspector confirming what survived it
  (**Documentation Examples → Filter weather alerts by category**)

Documentation Examples also includes screens for adding custom GeoJSON, raster, and vector-tile layers, changing map units, changing the timeline range, and customizing alert polygon styles.

The launcher lists **All Map Layers**, **Sorted Map Layers**, **Documentation Examples**, **Local Weather**, and **Stencil masks**.



