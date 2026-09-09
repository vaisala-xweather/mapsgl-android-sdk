package com.example.mapsgldemo

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.mapsgldemo.maplayers.LayerMenu
import com.example.mapsgldemo.maplayers.LayerMenuEntry
import com.example.mapsgldemo.maplayers.LayerMenuSection
import com.example.mapsgldemo.maplayers.MapLayersActivity
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Showcase: 7-day high/low at a point, with temperatures on the map.
 * Daily max/min come from the Xweather `/forecasts` endpoint. The pin starts at
 * [FORECAST_LAT] / [FORECAST_LON]; a map tap moves the pin and reloads that point.
 */
class ShowcaseForecastActivity : MapLayersActivity() {

    private var selectedDayOffset = 0
    private val dayChipViews = mutableListOf<View>()
    private var locationTitleView: TextView? = null
    private var dailyForecast: PointDailyForecast? = null
    private var forecastLat = FORECAST_LAT
    private var forecastLon = FORECAST_LON
    private var forecastRequestId = 0
    private var pinManager: CircleAnnotationManager? = null
    private var pinAnnotation: CircleAnnotation? = null

    override fun backNavigationActivity(): Class<out AppCompatActivity> = ShowcaseMenuActivity::class.java

    override fun initialCameraPosition(): Pair<Coordinate, Double> =
        Coordinate(FORECAST_LAT, FORECAST_LON) to FORECAST_ZOOM

    override fun applyMapLayersTimelineRange() {
        applyDayOffset(0, playAfter = false)
    }

    override fun layerMenuSections(): List<LayerMenuSection> = listOf(
        LayerMenuSection(
            heading = "Forecast",
            entries = listOf(
                LayerMenuEntry(LayerCode.TEMPERATURES, "Temperatures"),
                LayerMenuEntry(LayerCode.FEELS_LIKE, "Feels like"),
                LayerMenuEntry(LayerCode.PRECIPITATION, "Precipitation"),
                LayerMenuEntry(LayerCode.CLOUD_COVER, "Cloud cover"),
                LayerMenuEntry(LayerCode.HUMIDITY, "Humidity"),
                LayerMenuEntry(LayerCode.WIND_SPEEDS, "Wind speeds"),
                LayerMenuEntry(LayerCode.WIND_PARTICLES, "Wind particles"),
            ),
        ),
    )

    override fun onLayerMenuReady(layerMenu: LayerMenu, controller: MapboxMapController) {
        placeForecastPin()
        installDayChips()
        loadPointForecast(forecastLat, forecastLon)
        layerMenu.activateLayerIfPresent(controller, LayerCode.TEMPERATURES)
        controller.timeline.play()
        binding.timelineView.timelineControls.updatePlayButtonImage(true, binding.timelineView)
    }

    override fun onMapClick(point: Point): Boolean {
        selectForecastPoint(point.latitude(), point.longitude())
        return super.onMapClick(point)
    }

    private fun selectForecastPoint(lat: Double, lon: Double) {
        forecastLat = lat
        forecastLon = lon
        moveForecastPin(lat, lon)
        locationTitleView?.text = "Loading…"
        dailyForecast = null
        updateChipSelection()
        loadPointForecast(lat, lon)
    }

    private fun placeForecastPin() {
        pinManager = mapView.annotations.createCircleAnnotationManager()
        pinAnnotation = pinManager?.create(forecastPinOptions(forecastLat, forecastLon))
    }

    private fun moveForecastPin(lat: Double, lon: Double) {
        val manager = pinManager ?: return
        val existing = pinAnnotation
        if (existing != null) {
            existing.point = Point.fromLngLat(lon, lat)
            manager.update(existing)
        } else {
            pinAnnotation = manager.create(forecastPinOptions(lat, lon))
        }
    }

    private fun forecastPinOptions(lat: Double, lon: Double): CircleAnnotationOptions {
        return CircleAnnotationOptions()
            .withPoint(Point.fromLngLat(lon, lat))
            .withCircleRadius(4.0)
            .withCircleColor("#E53935")
            .withCircleStrokeWidth(1.5)
            .withCircleStrokeColor("#FFFFFF")
    }

    private fun installDayChips() {
        val chipsBar = layoutInflater.inflate(R.layout.widget_forecast_day_chips, binding.outerConstraint, false)
        locationTitleView = chipsBar.findViewById(R.id.forecastLocationTitle)
        val row = chipsBar.findViewById<LinearLayout>(R.id.forecastDayChipsRow)
        val weekdayFormat = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE")
        val weekdayFormatter = java.text.SimpleDateFormat(weekdayFormat, Locale.getDefault())

        for (offset in 0 until FORECAST_DAYS) {
            val chip = layoutInflater.inflate(R.layout.widget_forecast_day_chip, row, false)
            val dayDate = Date(startOfLocalDayMillis(offset))
            chip.findViewById<TextView>(R.id.forecastDayName).text =
                if (offset == 0) "Today" else weekdayFormatter.format(dayDate)
            chip.setOnClickListener { onDayChipTapped(offset) }
            row.addView(chip)
            dayChipViews.add(chip)
        }

        val lp = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            // Clears the circular back control instead of covering it.
            topMargin = resources.getDimensionPixelSize(R.dimen.map_overlay_panel_margin_top)
            marginStart = resources.getDimensionPixelSize(R.dimen.xw_space_3)
            marginEnd = resources.getDimensionPixelSize(R.dimen.xw_space_3)
        }
        binding.outerConstraint.addView(chipsBar, lp)
        updateChipSelection()
    }

    private fun loadPointForecast(lat: Double, lon: Double) {
        val clientId = getString(R.string.xweather_client_id)
        val clientSecret = getString(R.string.xweather_client_secret)
        val requestId = ++forecastRequestId
        Thread({
            val result = runCatching {
                fetchPointDailyForecast(clientId, clientSecret, lat, lon)
            }.getOrNull()
            runOnUiThread {
                if (isFinishing || isDestroyed || requestId != forecastRequestId) return@runOnUiThread
                if (result == null) {
                    locationTitleView?.text = "Forecast unavailable"
                    dailyForecast = null
                    updateChipSelection()
                    return@runOnUiThread
                }
                dailyForecast = result
                locationTitleView?.text = result.placeTitle
                updateChipSelection()
            }
        }, "showcase-point-forecast").start()
    }

    private fun onDayChipTapped(offset: Int) {
        if (!isMapControllerReady()) return
        applyDayOffset(offset, playAfter = true)
    }

    private fun applyDayOffset(offset: Int, playAfter: Boolean) {
        selectedDayOffset = offset
        val (start, end) = rangeForDayOffset(offset)
        applyMapLayersTimelineStartEnd(start, end)
        controller.timeline.duration = DAY_PLAYBACK_SECONDS
        updateChipSelection()
        if (playAfter) {
            controller.timeline.play()
            binding.timelineView.timelineControls.updatePlayButtonImage(true, binding.timelineView)
        }
    }

    private fun updateChipSelection() {
        val selectedBg = R.drawable.forecast_chip_selected
        val unselectedBg = R.drawable.forecast_chip_unselected
        val selectedName = ContextCompat.getColor(this, R.color.xw_text_inverse)
        val unselectedName = ContextCompat.getColor(this, R.color.xw_text_primary)
        val selectedLow = ContextCompat.getColor(this, R.color.xw_text_inverse)
        val unselectedLow = ContextCompat.getColor(this, R.color.xw_text_secondary)
        dayChipViews.forEachIndexed { index, chip ->
            val selected = index == selectedDayOffset
            chip.setBackgroundResource(if (selected) selectedBg else unselectedBg)
            chip.findViewById<TextView>(R.id.forecastDayName).setTextColor(
                if (selected) selectedName else unselectedName,
            )
            val highView = chip.findViewById<TextView>(R.id.forecastDayHigh)
            val lowView = chip.findViewById<TextView>(R.id.forecastDayLow)
            val day = dayForOffset(index)
            highView.text = day?.highF?.let { "$it°" } ?: "--"
            lowView.text = day?.lowF?.let { "$it°" } ?: "--"
            highView.setTextColor(if (selected) selectedName else unselectedName)
            lowView.setTextColor(if (selected) selectedLow else unselectedLow)
        }
    }

    private fun dayForOffset(offset: Int): DailyForecastDay? {
        val days = dailyForecast?.days ?: return null
        val dayStart = startOfLocalDayMillis(offset)
        val dayEnd = startOfLocalDayMillis(offset + 1)
        return days.firstOrNull { it.timestampMs in dayStart until dayEnd } ?: days.getOrNull(offset)
    }

    private fun rangeForDayOffset(offset: Int): Pair<Date, Date> {
        val endOfTargetDayMillis = startOfLocalDayMillis(offset + 1)
        val (startMs, endMs) = if (offset == 0) {
            System.currentTimeMillis() to endOfTargetDayMillis
        } else {
            val start = millisLocalHourOnDay(offset, 8, 0)
            var end = endOfTargetDayMillis
            if (end < start + MIN_SPAN_MS) {
                end = start + MIN_SPAN_MS
            }
            start to end
        }
        return Date(startMs) to Date(endMs)
    }

    private fun startOfLocalDayMillis(dayOffsetFromToday: Int): Long {
        return Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, dayOffsetFromToday)
            timeInMillis
        }
    }

    private fun millisLocalHourOnDay(dayOffsetFromToday: Int, hourOfDay: Int, minute: Int): Long {
        return Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, dayOffsetFromToday)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            timeInMillis
        }
    }

    companion object {
        /** Same fixed pin as [LocalActivity] fallback (Lakewood / Cleveland, OH). */
        private const val FORECAST_LAT = 41.355701
        private const val FORECAST_LON = -81.806944
        private const val FORECAST_ZOOM = 8.5
        private const val FORECAST_DAYS = 7
        private const val DAY_PLAYBACK_SECONDS = 6.0
        private const val MIN_SPAN_MS = 60L * 60 * 1000
    }
}

private data class PointDailyForecast(
    val placeTitle: String,
    val days: List<DailyForecastDay>,
)

private data class DailyForecastDay(
    val timestampMs: Long,
    val highF: Int?,
    val lowF: Int?,
)

private fun fetchPointDailyForecast(
    clientId: String,
    clientSecret: String,
    lat: Double,
    lon: Double,
): PointDailyForecast {
    val loc = "$lat,$lon"
    val query = buildString {
        append("filter=day&limit=7")
        append("&fields=place.name,place.state,place.country,periods.timestamp,periods.maxTempF,periods.minTempF")
        append("&client_id=").append(URLEncoder.encode(clientId, "UTF-8"))
        append("&client_secret=").append(URLEncoder.encode(clientSecret, "UTF-8"))
    }
    val url = URL("https://data.api.xweather.com/forecasts/$loc?$query")
    val connection = url.openConnection() as HttpURLConnection
    connection.connectTimeout = 15_000
    connection.readTimeout = 15_000
    connection.requestMethod = "GET"
    try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) {
            throw IllegalStateException("Forecast request failed ($code)")
        }
        return parsePointDailyForecast(JSONObject(body))
    } finally {
        connection.disconnect()
    }
}

private fun parsePointDailyForecast(root: JSONObject): PointDailyForecast {
    if (!root.optBoolean("success", true)) {
        throw IllegalStateException("Forecast response was not successful")
    }
    val record = when (val response = root.opt("response")) {
        is JSONObject -> response
        is JSONArray -> response.optJSONObject(0)
        else -> null
    } ?: throw IllegalStateException("Forecast response was empty")
    val place = record.optJSONObject("place")
    val placeTitle = formatForecastPlace(
        place?.optString("name").orEmpty(),
        place?.optString("state"),
        place?.optString("country"),
    )
    val periods = record.optJSONArray("periods") ?: JSONArray()
    val days = buildList {
        for (i in 0 until periods.length()) {
            val period = periods.optJSONObject(i) ?: continue
            add(
                DailyForecastDay(
                    timestampMs = period.optLong("timestamp") * 1000L,
                    highF = period.optRoundedInt("maxTempF"),
                    lowF = period.optRoundedInt("minTempF"),
                ),
            )
        }
    }
    if (days.isEmpty()) throw IllegalStateException("Forecast periods were empty")
    return PointDailyForecast(placeTitle.ifBlank { "7-day forecast" }, days)
}

private fun formatForecastPlace(name: String, state: String?, country: String?): String {
    if (name.isBlank()) return "7-day forecast"
    val pretty = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    val region = state?.takeIf { it.isNotBlank() } ?: country?.takeIf { it.isNotBlank() }
    return if (region.isNullOrBlank()) pretty else "$pretty, ${region.uppercase(Locale.US)}"
}

private fun JSONObject.optRoundedInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return when (val value = opt(key)) {
        is Number -> kotlin.math.round(value.toDouble()).toInt()
        else -> null
    }
}
