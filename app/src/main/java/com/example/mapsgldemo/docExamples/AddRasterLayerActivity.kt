package com.example.mapsgldemo.docExamples

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityAddRasterLayerBinding
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.layers.spec.RasterLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.ImageSourceDescriptor
import com.xweather.mapsgl.style.RasterLayerPaint
import com.xweather.mapsgl.style.RasterPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.types.TileSize

/**
 * Adding a custom raster layer — the Android port of the MapsGL JS example at
 * https://www.xweather.com/docs/mapsgl/examples/add-raster-layer
 *
 * Renders `satellite-geocolor` from a hand-built raster tile source rather than from
 * `WeatherService.SatelliteGeocolor`, and drops its opacity to 0.7. Building the source by hand is
 * the point of the example: it shows the tile-URL template a custom raster product would use.
 *
 * Two things differ from the JS example:
 *
 * 1. There is no `RasterSourceDescriptor` on Android. A raster tile source is an
 *    [ImageSourceDescriptor], whose `kind` is `DataSourceKind.RASTER`.
 * 2. Opacity is not a raster paint property. [RasterPaint] carries no fields; the JS
 *    `paint: { raster: { opacity: 0.7 } }` maps to [RasterLayerPaint.opacity].
 *
 * The tile URL also uses the current `maps.api.xweather.com` host rather than the example's older
 * sharded `maps{s}.aerisapi.com`, matching what `WeatherService.makeRasterSourceDescriptor` builds.
 *
 * Launched from [DocsExamplesMenuActivity].
 */
class AddRasterLayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRasterLayerBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private var mapLoadedCancelable: Cancelable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRasterLayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.addRasterMapView

        val clientId = getString(R.string.xweather_client_id)
        val clientSecret = getString(R.string.xweather_client_secret)
        val xweatherAccount = XweatherAccount(clientId, clientSecret)

        binding.addRasterBackButton.setOnClickListener { returnToDocsMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToDocsMenu()
            }
        })

        mapView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (mapView.parent == null) return

                controller = MapboxMapController(mapView, xweatherAccount)
                mapboxMap = controller.mapboxMap

                // Set the projection before the first frame is drawn. Doing it inside
                // subscribeMapLoaded (on `style`) is too late — the map paints as a globe and then
                // visibly snaps flat. MapSettings.setMapboxPreferences does the same for the other
                // demo screens, which is why they open flat.
                mapboxMap?.setProjection(projection(ProjectionName.MERCATOR))

                // Matches the JS example's `center: [12.35666, 45.34880], zoom: 3`.
                // Coordinate is (lat, lon), the reverse of the GeoJSON-style pair above.
                controller.setCenter(Coordinate(45.34880, 12.35666))
                controller.setZoom(3.0)

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    // Add the raster data tile source. `{z}/{x}/{y}` are filled in per tile; the
                    // access key and product code are baked into the template up front, which is
                    // what WeatherService does for the built-in raster products.
                    if (!controller.hasSource(SOURCE_ID)) {
                        controller.addSource(
                            ImageSourceDescriptor(
                                id = SOURCE_ID,
                                url = "$AMP_SERVER/${clientId}_$clientSecret/$PRODUCT_CODE/{z}/{x}/{y}/0@2x.png",
                                tileSize = TileSize(512),
                            ),
                        )
                    }

                    // Add the raster layer and associate it with the data source.
                    if (!controller.hasLayer(LAYER_ID)) {
                        controller.addLayer(
                            RasterLayerDescriptor(
                                id = LAYER_ID,
                                source = SOURCE_ID,
                                paint = RasterLayerPaint(
                                    opacity = 0.7f,
                                    raster = RasterPaint(),
                                ),
                            ),
                            beforeID = null,
                        )
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        if (::controller.isInitialized) {
            if (controller.hasLayer(LAYER_ID)) controller.removeLayer(LAYER_ID)
            if (controller.hasSource(SOURCE_ID)) controller.removeSource(SOURCE_ID)
        }
        mapLoadedCancelable?.cancel()
        mapLoadedCancelable = null
        mapView.onDestroy()
        super.onDestroy()
    }

    private fun returnToDocsMenu() {
        startActivity(
            Intent(this, DocsExamplesMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        private const val SOURCE_ID = "satellite-geocolor"
        private const val LAYER_ID = "satellite"
        private const val PRODUCT_CODE = "satellite-geocolor"
        private const val AMP_SERVER = "https://maps.api.xweather.com"
    }
}
