package com.example.mapsgldemo.docExamples

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityAddGeojsonLayerBinding
import com.mapbox.common.Cancelable
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.layers.spec.FillLayerDescriptor
import com.xweather.mapsgl.layers.spec.LineLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.GeoJSONSourceDescriptor
import com.xweather.mapsgl.style.FillLayerPaint
import com.xweather.mapsgl.style.FillPaint
import com.xweather.mapsgl.style.LineLayerPaint
import com.xweather.mapsgl.style.StrokePaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate

/**
 * Adding custom GeoJSON data — the Android port of the MapsGL JS example at
 * https://www.xweather.com/docs/mapsgl/examples/add-geojson-layer
 *
 * Renders a static polygon (Portugal) from an in-line GeoJSON object, then associates two style
 * layers with that one source: a semi-transparent fill and a thicker outline along the edges.
 *
 * The JS example calls `controller.addSource('country-region', { type: 'geojson', data: … })`.
 * [GeoJSONSourceDescriptor.data] is the Android equivalent, assigned on the descriptor before it is
 * added:
 *
 * ```kotlin
 * controller.addSource(
 *     GeoJSONSourceDescriptor(id = SOURCE_ID).apply {
 *         data = FeatureCollection.fromJson(json)
 *     },
 * )
 * ```
 *
 * Assigning [com.xweather.mapsgl.sources.GeoJSONSource.data] after `addSource` still works and is
 * the right choice when the features arrive later than the source does.
 *
 * Layer setup is inlined in [onCreate] so the whole flow reads top to bottom, matching the JS
 * example's single `controller.on('load')` block.
 *
 * Launched from [DocsExamplesMenuActivity].
 */
class AddGeoJsonLayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddGeojsonLayerBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private var mapLoadedCancelable: Cancelable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddGeojsonLayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.addGeojsonMapView

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.addGeojsonBackButton.setOnClickListener { returnToDocsMenu() }
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

                // Matches the JS example's `center: [-8, 40], zoom: 5`. Coordinate is (lat, lon).
                controller.setCenter(Coordinate(40.0, -8.0))
                controller.setZoom(5.0)

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    // Add the custom polygon as GeoJSON data.
                    if (!controller.hasSource(SOURCE_ID)) {
                        controller.addSource(
                            GeoJSONSourceDescriptor(id = SOURCE_ID).apply {
                                data = FeatureCollection.fromJson(COUNTRY_REGION_GEOJSON)
                            },
                        )
                    }

                    // Fill the polygon region.
                    if (!controller.hasLayer(FILL_LAYER_ID)) {
                        controller.addLayer(
                            FillLayerDescriptor(
                                id = FILL_LAYER_ID,
                                source = SOURCE_ID,
                                paint = FillLayerPaint(
                                    fill = FillPaint(
                                        color = StyleValue.Constant(REGION_COLOR),
                                        opacity = StyleValue.Constant(0.5),
                                    ),
                                ),
                            ),
                            beforeID = null,
                        )
                    }

                    // Add a thicker outline to the region.
                    if (!controller.hasLayer(OUTLINE_LAYER_ID)) {
                        controller.addLayer(
                            LineLayerDescriptor(
                                id = OUTLINE_LAYER_ID,
                                source = SOURCE_ID,
                                paint = LineLayerPaint(
                                    stroke = StrokePaint(
                                        color = StyleValue.Constant(REGION_COLOR),
                                        thickness = StyleValue.Constant(5.0),
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
            if (controller.hasLayer(OUTLINE_LAYER_ID)) controller.removeLayer(OUTLINE_LAYER_ID)
            if (controller.hasLayer(FILL_LAYER_ID)) controller.removeLayer(FILL_LAYER_ID)
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
        private const val SOURCE_ID = "country-region"
        private const val FILL_LAYER_ID = "region-fill"
        private const val OUTLINE_LAYER_ID = "region-outline"

        /** `#ff0000`, as in the JS example. */
        private val REGION_COLOR = Color(0xFFFF0000)

        /**
         * The same single-polygon FeatureCollection the JS example embeds, so the two render
         * identically. Kept verbatim rather than reusing the app's admin-boundary asset.
         */
        private const val COUNTRY_REGION_GEOJSON = """
        {
            "type": "FeatureCollection",
            "features": [
                {
                    "type": "Feature",
                    "properties": {
                        "type": "Sovereign country",
                        "name": "Portugal",
                        "continent": "Europe",
                        "region_un": "Europe",
                        "subregion": "Southern Europe",
                        "region_wb": "Europe & Central Asia"
                    },
                    "geometry": {
                        "type": "Polygon",
                        "coordinates": [
                            [
                                [-9.034817674180246, 41.88057058365967],
                                [-8.67194576662672, 42.13468943945496],
                                [-8.263856980817792, 42.28046865495034],
                                [-8.013174607769912, 41.790886135417125],
                                [-7.422512986673795, 41.79207469335983],
                                [-7.251308966490824, 41.91834605566505],
                                [-6.668605515967656, 41.883386949219584],
                                [-6.389087693700915, 41.381815497394655],
                                [-6.851126674822552, 41.11108266861753],
                                [-6.864019944679385, 40.33087189387483],
                                [-7.026413133156595, 40.184524237624245],
                                [-7.066591559263529, 39.71189158788277],
                                [-7.498632371439725, 39.62957103124181],
                                [-7.098036668313128, 39.03007274022378],
                                [-7.374092169616318, 38.37305858006492],
                                [-7.029281175148796, 38.07576406508977],
                                [-7.166507941099865, 37.803894354802225],
                                [-7.537105475281024, 37.42890432387623],
                                [-7.453725551778092, 37.09778758396607],
                                [-7.855613165711985, 36.83826854099627],
                                [-8.382816127953689, 36.97888011326246],
                                [-8.898856980820327, 36.86880931248078],
                                [-8.746101446965554, 37.65134552667661],
                                [-8.839997524439879, 38.26624339451761],
                                [-9.287463751655224, 38.3584858261586],
                                [-9.526570603869715, 38.73742910415491],
                                [-9.446988898140232, 39.39206614842837],
                                [-9.048305223008427, 39.75509308527877],
                                [-8.977353481471681, 40.15930613866581],
                                [-8.768684047877102, 40.76063894303019],
                                [-8.79085323733031, 41.18433401139126],
                                [-8.99078935386757, 41.54345937760364],
                                [-9.034817674180246, 41.88057058365967]
                            ]
                        ]
                    }
                }
            ]
        }
        """
    }
}
