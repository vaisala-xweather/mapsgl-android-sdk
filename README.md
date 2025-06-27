
Xweather MapsGL SDK Demo App
================

![Screenshot](/images/screenshot.png)

The Xweather MapsGL SDK for Android allows a developer to quickly and easily add weather content and functionality to their android applications. It utilizes the Xweather API backend for data loading and is built on top of an object mapping system that efficiently loads requested weather content into third-party Android applications, greatly reducing the amount of code and development needed on the developer end.

## Features
-Visualizing real-time weather and geospatial data
-High-performance layer rendering with OpenGLES
-Customizable presentation and styling of weather and geospatial information client-side

## Getting Started

View the latest installation and implementation details at Xweather under the [Xweather Android SDK toolkit documentation](https://www.xweather.com/docs/android-sdk/getting-started/).


## Running the Demo App
The MapsGL Android SDK includes a demo application that showcases the capabilities of the SDK. To run the demo application, follow these steps:

##### Prerequisites:
- Android Studio Chipmunk|2022.3.1 Patch2 or later
- An Android 9.0+ (sdk 28)
- An [Xweather account](https://signup.xweather.com/) — We offer a free developer account for you to give our weather API a test drive.
- A [Mapbox account](https://www.mapbox.com/)

## Xweather API Configuration for the Xweather Demo Application
Before you can begin using the Xweather MapsGL SDK in your project, you will need to download the latest version of the SDK and ensure that you have the required Xweather API keys for your application.

##### Step 1: Get the files.
Download the latest version of the [Xweather Android SDK demo application](https://github.com/vaisala-xweather/mapsgl-android-sdk)

##### Step 2: Get access to the Xweather API.
To use the Xweather API, you will need to have valid access keys. Access keys are obtained by registering your application/namespace. To register your application, log in to Xweather with your account and look for the "APPS" section. Don't have an Xweather account? You can get one for free [here](https://signup.xweather.com/).

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
            gradlePluginPortal()
        }
    }

    dependencyResolutionManagement {
        repositories {
            google()
            mavenCentral()
            maven { url 'https://jitpack.io' }
            maven { url 'https://maven.ecc.no/releases' }
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


In **AndroidManifest.xml**:

        <uses-permission android:name="android.permission.ACCESS_COURSE_LOCATION"/>
        <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>


In the app-level **build.gradle**

    repositories {
        mavenCentral()
    }
    dependencies {
        implementation 'no.ecc.vectortile:java-vector-tile:1.4.1'
	    implementation "com.github.vaisala-xweather:mapsgl-android-sdk:v1.2.0"
    }




### In your activity, inside the onCreate method:

Create a reference to your account:

	val xweatherAccount = XweatherAccount(
		getString(R.string.xweather_client_id),
        getString(R.string.xweather_client_secret)
    )


Create your MapboxController:

	binding.mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
		override fun onGlobalLayout() {
			binding.mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
			mapController = MapboxMapController(mapView, baseContext, xweatherAccount)
            with(mapController) {
				mapboxMap.loadStyle(Style.DARK)
				//Make sure to set Mapbox to Mercator mode
                mapboxMap.setProjection(projection(ProjectionName.MERCATOR)) 
                mapboxMap.subscribeMapLoaded(mapLoadedCallback)
                mapboxMap.subscribeCameraChanged(cameraChangeCallBack)
            }
		}
	})

Add a MapsGL layer to the map:

	val mapLoadedCallback = MapLoadedCallback {
		val temperatureLayer = 	mapController.addWeatherLayer(LayerCode.temperatures)
    }


### Android Studio
Version: [Chipmunk|2022.3.1 Patch2 or later](https://androidstudio.googleblog.com/2023/09/android-studio-giraffe-patch-2-is-now.html) \
Android Gradle Plugin Version 7.2.2 \
Gradle Version 7.5.1.

## Reference Links

[Xweather API Docs](http://www.xweather.com/support/docs/api/) \
[Xweather API Signup](https://signup.xweather.com/) \
[Xweather Android SDK](http://www.aerisweather.com/support/docs/toolkits/aeris-android-sdk/) \
[Xweather Android Maps](https://www.xweather.com/docs/android-sdk/getting-started/weather-maps) 



