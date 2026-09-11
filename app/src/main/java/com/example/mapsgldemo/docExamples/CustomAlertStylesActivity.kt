package com.example.mapsgldemo.docExamples

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.helpers.InsetEdges
import com.example.mapsgldemo.helpers.drawBehindCutout
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.R
import com.example.mapsgldemo.databinding.ActivityCustomAlertStylesBinding
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.projection.generated.setProjection
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.FillLayerPaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.WeatherLayerConfiguration
import com.xweather.mapsgl.weather.WeatherService

/**
 * Customizing alert polygon styles — the Android port of the MapsGL JS example at
 * https://www.xweather.com/docs/mapsgl/examples/custom-alert-styles
 *
 * Recolours the `alerts` layer by severity so warnings, watches, advisories and statements read
 * apart at a glance, instead of the per-product colours the tiles ship with.
 *
 * **How the severity is derived differs from the JS example.** The JS version runs a case-insensitive
 * `regex` over the `ADVISORY` text (`warning|extreme`, `watch|severe`, and so on). Android has no
 * `regex` expression, and `Expression.contains` is an exact match against a list rather than a
 * substring test, so that approach does not carry over.
 *
 * The alert tiles also carry `VTEC`, the NWS product code, whose last character is the official
 * significance: `W` warning, `A` watch, `Y` advisory, `S` statement. Slicing that one character and
 * matching on it is both simpler and more precise than matching words in a display string — "SEVERE
 * THUNDERSTORM WARNING" is `SV.W`, "GALE WATCH" is `GL.A`, "HEAT ADVISORY" is `HT.Y`.
 *
 * Products issued without a VTEC code — an air quality alert, for one — match nothing and fall
 * through to the tile's own `COLOR`, which is what the JS example's
 * `coalesce(get('COLOR'), '#000')` fallback does.
 *
 * Launched from [DocsExamplesMenuActivity].
 */
class CustomAlertStylesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomAlertStylesBinding
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private lateinit var controller: MapboxMapController
    private var mapLoadedCancelable: Cancelable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomAlertStylesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawBehindCutout(
            binding.root,
            InsetEdges(binding.customAlertStylesBackButton, top = true, start = true),
        )

        mapView = binding.customAlertStylesMapView

        val xweatherAccount = XweatherAccount(
            getString(R.string.xweather_client_id),
            getString(R.string.xweather_client_secret),
        )

        binding.customAlertStylesBackButton.setOnClickListener { returnToDocsMenu() }
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

                // Set the projection before the first frame is drawn so the map does not paint as
                // a globe and then snap flat.
                mapboxMap?.setProjection(projection(ProjectionName.MERCATOR))

                // Matches the JS example's `center: [-96.33207, 40.60621], zoom: 3`.
                // Coordinate is (lat, lon).
                controller.setCenter(Coordinate(40.60621, -96.33207))
                controller.setZoom(3.0)

                mapLoadedCancelable = mapboxMap?.subscribeMapLoaded {
                    controller.addWeatherLayer(alertsWithSeverityColors())
                }
            }
        })
    }

    /**
     * The built-in alerts configuration with both fill and stroke recoloured by severity.
     *
     * Overriding the paint on a [WeatherLayerConfiguration] is the same pattern as any other
     * weather-layer customization — see "Adding weather layers" in the documentation.
     */
    private fun alertsWithSeverityColors(): WeatherLayerConfiguration<*, *> {
        val config = WeatherService.Alerts(controller.service) as WeatherLayerConfiguration<*, *>
        val paint = config.layer.paint as FillLayerPaint

        paint.fill?.color = StyleValue.Expression(ALERT_COLOR)
        paint.fill?.opacity = StyleValue.Constant(0.5)
        paint.stroke?.color = StyleValue.Expression(ALERT_COLOR)
        paint.stroke?.opacity = StyleValue.Constant(1.0)

        return config
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
        /** Same four colours as the JS example. */
        private const val WARNING = "#df1616"
        private const val WATCH = "#ff9600"
        private const val ADVISORY = "#009ac8"
        private const val STATEMENT = "#808080"

        /**
         * `VTEC` is a code such as `SV.W`; its fourth character is the NWS significance. Anything
         * without a VTEC code falls back to the tile's own `COLOR`, which arrives without a leading
         * `#` — the same fallback the built-in alerts layer uses.
         */
        private val ALERT_COLOR: Expression = Expression.match(
            Expression.slice(Expression.get("VTEC"), 3),
            listOf(
                Expression.Step("W", WARNING),
                Expression.Step("A", WATCH),
                Expression.Step("Y", ADVISORY),
                Expression.Step("S", STATEMENT),
            ),
            Expression.concat(listOf("#", Expression.get("COLOR"))),
        )
    }
}
