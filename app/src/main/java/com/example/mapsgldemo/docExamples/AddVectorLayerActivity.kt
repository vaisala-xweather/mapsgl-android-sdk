package com.example.mapsgldemo.docExamples

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.example.mapsgldemo.helpers.InsetEdges
import com.example.mapsgldemo.helpers.drawBehindCutout
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityAddVectorLayerBinding
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.layers.spec.LineLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.VectorSourceDescriptor
import com.xweather.mapsgl.style.LineCap
import com.xweather.mapsgl.style.LineJoin
import com.xweather.mapsgl.style.LineLayerPaint
import com.xweather.mapsgl.style.StrokePaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.WeatherSource

/**
 * Adding a custom vector tile layer — the Android port of the MapsGL JS example at
 * https://www.xweather.com/docs/mapsgl/examples/add-vector-layer
 *
 * Draws water boundaries from vector tiles: one [VectorSourceDescriptor] for the tile endpoint,
 * and a [LineLayerDescriptor] that styles the source's `water` layer.
 *
 * The published JS example pulls its tiles from [Nextzen](https://nextzen.org), which needs an API
 * key of its own. This uses Xweather's OpenStreetMap-derived vector source instead —
 * [WeatherSource.base_osm] — which carries the same `water` source layer and authenticates with the
 * Xweather credentials the app already has, so the example runs with no extra sign-up. Everything
 * downstream of the source is unchanged from the JS version, including the `#333333` stroke.
 *
 * Building the source and layer by hand is the point of the example. The same water boundaries are
 * available prewired as `LayerCode.WATERWAY_OCEAN_BOUNDARIES` via `addWeatherLayer`; what is shown
 * here is the manual wiring you would use for any vector tile endpoint.
 *
 * The Android mapping is close to the JS: `sourceLayer` selects the layer inside the vector tile,
 * and the JS `paint.stroke` fields map one-to-one onto [StrokePaint]. The JS `sourceType: 'line'`
 * has no Android counterpart — the descriptor type already determines how the geometry is drawn.
 *
 * Launched from [DocsExamplesMenuActivity].
 */
class AddVectorLayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddVectorLayerBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private var mapLoadedCancelable: Cancelable? = null

    /** Id of the vector source, taken from the descriptor rather than hard-coded. */
    private var sourceId: String? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVectorLayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawBehindCutout(
            binding.root,
            InsetEdges(binding.addVectorBackButton, top = true, start = true),
            InsetEdges(binding.addVectorCaption, start = true, end = true),
        )

        mapView = binding.addVectorMapView
        binding.addVectorCaption.text =
            "Water boundaries from a custom vector tile source, styled by a line layer on the " +
            "source's water layer."

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.addVectorBackButton.setOnClickListener { returnToDocsMenu() }
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

                // Matches the JS example's `center: [-30.33207, 40.60621], zoom: 2`.
                // Coordinate is (lat, lon), the reverse of the GeoJSON-style pair above.
                controller.setCenter(Coordinate(40.60621, -30.33207))
                controller.setZoom(2.0)

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    // Add the vector tile source. `base_osm` is Xweather's OpenStreetMap-derived
                    // vector product; the descriptor it returns already carries the tile URL and
                    // the authenticator, so no separate key is needed.
                    val waterSource: VectorSourceDescriptor =
                        WeatherSource.base_osm(controller.service)
                    sourceId = waterSource.id
                    if (!controller.hasSource(waterSource.id)) {
                        controller.addSource(waterSource)
                    }

                    // Style the source's `water` layer as lines.
                    if (!controller.hasLayer(LAYER_ID)) {
                        controller.addLayer(
                            LineLayerDescriptor(
                                id = LAYER_ID,
                                source = waterSource.id,
                                sourceLayer = SOURCE_LAYER,
                                paint = LineLayerPaint(
                                    stroke = StrokePaint(
                                        color = StyleValue.Constant(BOUNDARY_COLOR),
                                        opacity = StyleValue.Constant(1.0),
                                        thickness = StyleValue.Constant(2.0),
                                        lineCap = StyleValue.Constant(LineCap.ROUND),
                                        lineJoin = StyleValue.Constant(LineJoin.ROUND),
                                    ),
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
            sourceId?.let { if (controller.hasSource(it)) controller.removeSource(it) }
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
        private const val LAYER_ID = "boundaries"

        /** The layer inside the vector tile to draw — the same name Nextzen uses. */
        private const val SOURCE_LAYER = "water"

        /** `#333333`, as in the JS example. */
        private val BOUNDARY_COLOR = Color(0xFF333333)
    }
}
