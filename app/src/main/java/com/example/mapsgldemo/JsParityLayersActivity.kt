package com.example.mapsgldemo

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.maplayers.LayerMenuEntry
import com.example.mapsgldemo.maplayers.LayerMenuSection
import com.example.mapsgldemo.maplayers.MapLayersActivity
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode

/**
 * Layer browser whose menu mirrors the MapsGL JavaScript demo: same section headings, same order
 * within each section, and the same row labels.
 *
 * Only the 152 rows that resolve to an Android [LayerCode] are listed. The JS menu has 275; the
 * remainder have no Android product and so cannot be added to a map here:
 *
 * - **ROAD WEATHER** (80 rows) — the SDK has no `road-weather-*` codes at all, so the whole section
 *   is absent rather than empty.
 * - **FORECAST** — empty in the JS demo itself, so there is nothing to mirror.
 * - **CONDITIONS** (29 rows) — max/min temps, accumulation variants (precip / snow / ice / sleet),
 *   ice and sleet themselves, wind categories, and a few `*-text` partners with no sampled product.
 * - **BASE** (4 rows) — power lines / plants / generators and the day-night overlay.
 * - **SEVERE** (7 rows) — short-fuse alerts, lightning-density accumulations, and max hail variants.
 * - **OTHER / SEVERE** — `Earthquakes: Custom`, `Fires: Custom` and `Lightning: Pulse` are the JS
 *   demo’s own custom-shader configurations rather than weather products. The Android equivalents
 *   live in [CustomShaderActivity].
 * - **AIR QUALITY** (1 row) — `AQI: Common Text`; the SDK has `caqi-categories` but no `caqi-text`.
 *
 */
class JsParityLayersActivity : MapLayersActivity() {

    override fun showStencilDemoButtons(): Boolean = false


    override fun backNavigationActivity(): Class<out AppCompatActivity> = MainActivity::class.java

    /** Mid-Atlantic at z6, matching the JS demo’s default view. */
    override fun initialCameraPosition(): Pair<Coordinate, Double> =
        Coordinate(38.423, -78.671) to 6.15

    override fun layerMenuSections(): List<LayerMenuSection> = listOf(
        LayerMenuSection(
            heading = "BASE",
            entries = listOf(
                LayerMenuEntry(LayerCode.PLACES, "Places"),
                LayerMenuEntry(LayerCode.ROADS, "Roads"),
                LayerMenuEntry(LayerCode.BOUNDARIES, "Boundaries"),
                LayerMenuEntry(LayerCode.WATER, "Water"),
            ),
        ),
        LayerMenuSection(
            heading = "CONDITIONS",
            entries = listOf(
                LayerMenuEntry(LayerCode.TEMPERATURES, "Temperatures"),
                LayerMenuEntry(LayerCode.TEMPERATURES_CONTOUR, "Temps: Contours"),
                LayerMenuEntry(LayerCode.TEMPERATURES_24_HOUR_CHANGE, "24hr Temp Change"),
                LayerMenuEntry(LayerCode.TEMPERATURES_1_HOUR_CHANGE, "1hr Temp Change"),
                LayerMenuEntry(LayerCode.FEELS_LIKE, "Feels Like"),
                LayerMenuEntry(LayerCode.HEAT_INDEX, "Heat Index"),
                LayerMenuEntry(LayerCode.WIND_CHILL, "Wind Chill"),
                LayerMenuEntry(LayerCode.WIND_SPEEDS, "Winds"),
                LayerMenuEntry(LayerCode.WIND_PARTICLES, "Winds: Particles"),
                LayerMenuEntry(LayerCode.WIND_SPEEDS_CONTOUR, "Winds: Contours"),
                LayerMenuEntry(LayerCode.WIND_DIR, "Winds: Grid"),
                LayerMenuEntry(LayerCode.WIND_BARBS, "Winds: Barbs"),
                LayerMenuEntry(LayerCode.WIND_GUSTS, "Wind Gusts"),
                LayerMenuEntry(LayerCode.DEW_POINTS, "Dew Point"),
                LayerMenuEntry(LayerCode.HUMIDITY, "Humidity"),
                LayerMenuEntry(LayerCode.PRESSURE_MEAN_SEA_LEVEL, "MSLP"),
                LayerMenuEntry(LayerCode.PRESSURE_MEAN_SEA_LEVEL_CONTOUR, "MSLP: Contours"),
                LayerMenuEntry(LayerCode.CLOUD_COVER, "Cloud Cover"),
                LayerMenuEntry(LayerCode.PRECIPITATION, "Precip"),
                LayerMenuEntry(LayerCode.SNOW, "Snowfall"),
                LayerMenuEntry(LayerCode.SNOW_DEPTH, "Snow Depth"),
                LayerMenuEntry(LayerCode.VISIBILITY, "Visibility"),
                LayerMenuEntry(LayerCode.ULTRAVIOLET_INDEX, "UV Index"),
                LayerMenuEntry(LayerCode.RADAR, "Radar"),
            ),
        ),
        LayerMenuSection(
            heading = "RASTER",
            entries = listOf(
                LayerMenuEntry(LayerCode.RADAR, "Radar"),
                LayerMenuEntry(LayerCode.SATELLITE, "Satellite"),
                LayerMenuEntry(LayerCode.SATELLITE_GEOCOLOR, "Satellite Geocolor"),
                LayerMenuEntry(LayerCode.SATELLITE_VISIBLE, "Satellite Visible"),
                LayerMenuEntry(LayerCode.SATELLITE_INFRARED_COLOR, "Satellite Infrared Color"),
                LayerMenuEntry(LayerCode.SATELLITE_WATER_VAPOR, "Satellite Water Vapor"),
            ),
        ),
        LayerMenuSection(
            heading = "MARITIME",
            entries = listOf(
                LayerMenuEntry(LayerCode.SEA_SURFACE_TEMPERATURES, "Sea Surface Temps"),
                LayerMenuEntry(LayerCode.OCEAN_CURRENTS, "Currents: Fill"),
                LayerMenuEntry(LayerCode.OCEAN_CURRENTS_PARTICLES, "Currents: Particles"),
                LayerMenuEntry(LayerCode.WAVE_HEIGHTS, "Wave Heights"),
                LayerMenuEntry(LayerCode.WAVE_PERIODS, "Wave Periods"),
                LayerMenuEntry(LayerCode.WAVE_DIR, "Wave Dir: Grid"),
                LayerMenuEntry(LayerCode.WAVE_PARTICLES, "Wave Dir: Particle"),
                LayerMenuEntry(LayerCode.SWELL_HEIGHTS, "Swell Heights"),
                LayerMenuEntry(LayerCode.SWELL_PERIODS, "Swell Periods"),
                LayerMenuEntry(LayerCode.SWELL_DIR, "Swell Dir: Grid"),
                LayerMenuEntry(LayerCode.SWELL_PARTICLES, "Swell Dir: Particle"),
                LayerMenuEntry(LayerCode.SWELL2_HEIGHTS, "Swell 2 Heights"),
                LayerMenuEntry(LayerCode.SWELL2_PERIODS, "Swell 2 Periods"),
                LayerMenuEntry(LayerCode.SWELL2_DIR, "Swell 2 Dir: Grid"),
                LayerMenuEntry(LayerCode.SWELL2_PARTICLES, "Swell 2 Dir: Particle"),
                LayerMenuEntry(LayerCode.SWELL3_HEIGHTS, "Swell 3 Heights"),
                LayerMenuEntry(LayerCode.SWELL3_PERIODS, "Swell 3 Periods"),
                LayerMenuEntry(LayerCode.SWELL3_DIR, "Swell 3 Dir: Grid"),
                LayerMenuEntry(LayerCode.SWELL3_PARTICLES, "Swell 3 Dir: Particle"),
                LayerMenuEntry(LayerCode.STORM_SURGE, "Storm Surge"),
                LayerMenuEntry(LayerCode.TIDE_HEIGHTS, "Tide Heights"),
            ),
        ),
        LayerMenuSection(
            heading = "AIR QUALITY",
            entries = listOf(
                LayerMenuEntry(LayerCode.AIR_QUALITY_INDEX, "AQI"),
                LayerMenuEntry(LayerCode.AIR_QUALITY_INDEX_CATEGORIES, "AQI: Categories"),
                LayerMenuEntry(LayerCode.AIR_QUALITY_HEALTH_INDEX_CATEGORIES, "Health Index"),
                LayerMenuEntry(LayerCode.AIR_QUALITY_INDEX_CHINA_CATEGORIES, "AQI: China"),
                LayerMenuEntry(LayerCode.AIR_QUALITY_INDEX_INDIA_CATEGORIES, "AQI: India"),
                LayerMenuEntry(LayerCode.AIR_QUALITY_INDEX_CAQI_CATEGORIES, "AQI: Common"),
                LayerMenuEntry(LayerCode.AIR_QUALITY_INDEX_EAQI_CATEGORIES, "AQI: European"),
                LayerMenuEntry(LayerCode.AIR_QUALITY_INDEX_UK_DAQI_CATEGORIES, "AQI: UK"),
                LayerMenuEntry(LayerCode.AIR_QUALITY_INDEX_CAI_CATEGORIES, "AQI: Korea"),
                LayerMenuEntry(LayerCode.PARTICULATE_MATTER_2P5_MICRON, "Particle Pollution (PM2.5)"),
                LayerMenuEntry(LayerCode.PARTICULATE_MATTER_10_MICRON, "Particle Pollution (PM10)"),
                LayerMenuEntry(LayerCode.CARBON_MONOXIDE, "Carbon Monoxide (CO)"),
                LayerMenuEntry(LayerCode.NITRIC_OXIDE, "Nitrogen Monoxide (NO)"),
                LayerMenuEntry(LayerCode.NITROGEN_DIOXIDE, "Nitrogen Dioxide (NO2)"),
                LayerMenuEntry(LayerCode.OZONE, "Ozone (O3)"),
                LayerMenuEntry(LayerCode.SULFUR_DIOXIDE, "Sulfur Dioxide (SO2)"),

                ),
        ),
        LayerMenuSection(
            heading = "SEVERE",
            entries = listOf(
                LayerMenuEntry(LayerCode.ALERTS, "Alerts"),
                LayerMenuEntry(LayerCode.ALERTS_OUTLINE, "Alerts: Outline"),
                LayerMenuEntry(LayerCode.STORMCELLS, "Storm Cells"),
                LayerMenuEntry(LayerCode.STORMCELLS_HEAT, "Storm Cells: Heatmap"),
                LayerMenuEntry(LayerCode.STORMCELLS_POSITIONS, "Storm Cells: Points"),
                LayerMenuEntry(LayerCode.STORMCELLS_TRACKS, "Storm Cells: Tracks"),
                LayerMenuEntry(LayerCode.STORMCELLS_CONES, "Storm Cells: Cones"),
                LayerMenuEntry(LayerCode.STORMREPORTS, "Storm Reports"),
                LayerMenuEntry(LayerCode.STORMREPORTS_HEAT, "Storm Reports: Heat"),
                LayerMenuEntry(LayerCode.LIGHTNING_STRIKES, "Lightning"),
                LayerMenuEntry(LayerCode.LIGHTNING_STRIKES_ICONS, "Lightning (Icons)"),
                LayerMenuEntry(LayerCode.LIGHTNING_FLASH, "Lightning: Flash"),
                LayerMenuEntry(LayerCode.LIGHTNING_ALL, "Lightning: All"),
                LayerMenuEntry(LayerCode.LIGHTNING_ALL_ICONS, "Lightning: All (Icons)"),
                LayerMenuEntry(LayerCode.LIGHTNING_STRIKES_HEAT, "Lightning: Heatmap"),
                LayerMenuEntry(LayerCode.LIGHTNING_THREATS, "Lightning Threats"),
                LayerMenuEntry(LayerCode.LIGHTNING_DENSITY, "Lightning Density"),
                LayerMenuEntry(LayerCode.LIGHTNING_DENSITY_CLOUD_TO_GROUND, "Lightning Density (CG)"),
                LayerMenuEntry(LayerCode.LIGHTNING_DENSITY_INTRACLOUD, "Lightning Density (IC)"),
                LayerMenuEntry(LayerCode.HAIL_THREATS, "Hail Threats"),
                LayerMenuEntry(LayerCode.HAIL_SIZE, "Hail Size"),
                LayerMenuEntry(LayerCode.HAIL_SEVERE_PROBABILITY, "Severe Hail Prob"),
                LayerMenuEntry(LayerCode.CONVECTIVE, "Convective"),
                LayerMenuEntry(LayerCode.CONVECTIVE_OUTLINE, "Convective: Outline"),
            ),
        ),
        LayerMenuSection(
            heading = "OTHER",
            entries = listOf(
                LayerMenuEntry(LayerCode.EARTHQUAKES, "Earthquakes"),
                LayerMenuEntry(LayerCode.EARTHQUAKES_HEAT, "Earthquakes: Heatmap"),
                LayerMenuEntry(LayerCode.FIRES_OBS, "Fires"),
                LayerMenuEntry(LayerCode.FIRES_OBS_ICONS, "Fires (Icons)"),
                LayerMenuEntry(LayerCode.FIRES_OBS_HEAT, "Fires: Heatmap"),
                LayerMenuEntry(LayerCode.FIRES_PERIMETER, "Fire Perimeters"),
                LayerMenuEntry(LayerCode.FIRES_OUTLOOK, "Fire Outlook"),
                LayerMenuEntry(LayerCode.DROUGHT_MONITOR, "Drought"),
                LayerMenuEntry(LayerCode.DROUGHT_MONITOR_OUTLINE, "Drought: Outline"),
                LayerMenuEntry(LayerCode.RIVER_OBSERVATIONS, "Rivers"),
                LayerMenuEntry(LayerCode.AIR_QUALITY, "Air Quality"),
            ),
        ),
        LayerMenuSection(
            heading = "TROPICAL",
            entries = listOf(
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES, "Active All"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_ICONS, "Active All (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_POINTS, "Active Track Points"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_POINT_ICONS, "Active Track Points (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_FORECAST_POINTS, "Active Forecast Points"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_FORECAST_POINT_ICONS, "Active Forecast Points (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_LINES, "Active Track Lines"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_FORECAST_LINES, "Active Forecast Lines"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_POSITIONS, "Active Positions"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_POSITION_ICONS, "Active Positions (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_NAMES, "Active Names"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_BREAK_POINTS, "Active Breakpoints"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_ARCHIVE, "Archive All"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_ARCHIVE_ICONS, "Archive All (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_POINTS_ARCHIVE, "Archive Track Points"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_POINT_ICONS_ARCHIVE, "Archive Track Points (Icons)"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_TRACK_LINES_ARCHIVE, "Archive Track Lines"),
                LayerMenuEntry(LayerCode.TROPICAL_CYCLONES_INVESTS, "Invests"),
            ),
        ),
    )
}
