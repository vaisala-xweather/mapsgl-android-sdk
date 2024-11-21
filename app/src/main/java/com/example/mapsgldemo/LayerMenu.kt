package com.example.mapsgldemo

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.example.mapsgldemo.LayerButtonView.Companion.createHeadingTextView
import com.xweather.mapsgl.config.weather.WeatherService

class LayerMenu {
    companion object {
        /**  Create menu buttons for all the available layers **/
        fun createLayerButtons(
            context: Context,
            buttonList: MutableList<View>,
            service: WeatherService,
            layout: LinearLayout
        ) {
            buttonList.add(createHeadingTextView("Conditions", context))
            buttonList.add(LayerButtonView(context, "Temperatures", WeatherService.Temperatures(service)))
            buttonList.add( LayerButtonView(context, "24hr Temp Change", WeatherService.Temperatures24HourChange(service)))
            buttonList.add(LayerButtonView(context,"1hr Temp Change", WeatherService.Temperatures1HourChange(service)))
            buttonList.add(LayerButtonView(context, "Feels Like", WeatherService.FeelsLike(service)))
            buttonList.add(LayerButtonView(context, "Heat Index", WeatherService.HeatIndex(service)))
            buttonList.add(LayerButtonView(context, "Wind Chill", WeatherService.WindChill(service)))
            buttonList.add(LayerButtonView(context, "Winds", WeatherService.WindSpeeds(service)))
            buttonList.add(LayerButtonView(context, "Wind Particles", WeatherService.WindParticles(service)))
            buttonList.add(LayerButtonView(context, "Wind Gusts", WeatherService.WindGusts(service)))
            buttonList.add(LayerButtonView(context, "Dew Point", WeatherService.Dewpoint(service)))
            buttonList.add(LayerButtonView(context, "Humidity", WeatherService.Humidity(service)))
            buttonList.add(LayerButtonView(context, "MSLP", WeatherService.PressureMeanSeaLevel(service)))
            buttonList.add(LayerButtonView(context,"Cloud Cover", WeatherService.CloudCover(service)))
            buttonList.add(LayerButtonView(context,"Precipitation", WeatherService.Precipitation(service)))
            buttonList.add(LayerButtonView(context, "Snow Depth", WeatherService.SnowDepth(service)))
            buttonList.add(LayerButtonView(context, "Visibility", WeatherService.Visibility(service)))
            buttonList.add(LayerButtonView(context, "UV Index", WeatherService.UVIndex(service)))
            buttonList.add(LayerButtonView(context, "Radar", WeatherService.Radar(service)))

            buttonList.add(createHeadingTextView("Raster", context))
            buttonList.add(LayerButtonView(context, "Satellite", WeatherService.Satellite(service)))
            buttonList.add(LayerButtonView(context,"Satellite GeoColor", WeatherService.SatelliteGeoColor(service)))
            buttonList.add(LayerButtonView(context,"Satellite Visible", WeatherService.SatelliteVisible(service)))
            buttonList.add(LayerButtonView(context,"Satellite Infrared Color", WeatherService.SatelliteInfraredColor(service)))
            buttonList.add(LayerButtonView(context,"Satellite Water Vapor", WeatherService.SatelliteWaterVapor(service)))
            buttonList.add(createHeadingTextView("Maritime", context))
            buttonList.add(LayerButtonView(context,"Sea Surface Temps", WeatherService.SST(service)))
            buttonList.add(LayerButtonView(context,"Currents: Fill", WeatherService.OceanCurrents(service)))
            buttonList.add(LayerButtonView(context,"Currents: Particles", WeatherService.OceanCurrentsParticles(service)))
            buttonList.add(LayerButtonView(context,  "Wave Heights", WeatherService.WaveHeights(service)))
            buttonList.add(LayerButtonView(context, "Wave Periods", WeatherService.WavePeriods(service)))
            buttonList.add(LayerButtonView(context,"Swell 1 Heights", WeatherService.SwellHeights(service)))
            buttonList.add(LayerButtonView(context,"Swell Periods", WeatherService.SwellPeriods(service)))
            buttonList.add(LayerButtonView(context,"Swell 2 Heights", WeatherService.SwellHeights2(service)))
            buttonList.add(LayerButtonView(context,"Swell 2 Periods", WeatherService.SwellPeriods2(service)))
            buttonList.add(LayerButtonView(context,"Swell 3 Heights", WeatherService.SwellHeights3(service)))
            buttonList.add(LayerButtonView(context,"Swell 3 Periods", WeatherService.SwellPeriods3(service)))
            buttonList.add(LayerButtonView(context,"Storm Surge",WeatherService.StormSurge(service)))
            buttonList.add(LayerButtonView(context,"Tide Heights", WeatherService.TideHeights(service)))

            buttonList.add(createHeadingTextView("Air Quality", context))
            buttonList.add(LayerButtonView(context,"Air Quality Index", WeatherService.AirQualityIndex(service)))
            buttonList.add(LayerButtonView(context, "AQI Categories", WeatherService.AirQualityIndexCategories(service)))
            buttonList.add(LayerButtonView(context,"Health Index", WeatherService.HealthIndex(service)))
            buttonList.add(LayerButtonView(context,"AQI: China", WeatherService.AirQualityIndexChina(service)))
            buttonList.add(LayerButtonView(context,"AQI: India", WeatherService.AirQualityIndexIndia(service)))
            buttonList.add(LayerButtonView(context,"AQI: Common", WeatherService.AirQualityIndexCommon(service)))
            buttonList.add(LayerButtonView(context,"AQI: European", WeatherService.AirQualityIndexEuropean(service)))
            buttonList.add(LayerButtonView(context,"AQI: UK", WeatherService.AirQualityIndexUK(service)))
            buttonList.add( LayerButtonView(context,"AQI: Germany", WeatherService.AirQualityIndexGermany(service)))
            buttonList.add(LayerButtonView(context,"AQI: Korea", WeatherService.AirQualityIndexKorea(service)))
            buttonList.add(LayerButtonView(context,"Carbon Monoxide (CO)", WeatherService.CarbonMonoxide(service)))
            buttonList.add(LayerButtonView(context,"Nitrogen Monoxide (NO)", WeatherService.NitricOxide(service)))
            buttonList.add(LayerButtonView(context,"Nitrogen Dioxide (NO2)", WeatherService.NitrogenDioxide(service)))
            buttonList.add(LayerButtonView(context, "Ozone (O3)", WeatherService.Ozone(service)))
            buttonList.add(LayerButtonView(context,"Sulfur Dioxide (SO2)", WeatherService.SulfurDioxide(service)))
            buttonList.add(LayerButtonView(context,"Particle Pollution (PM 2.5)", WeatherService.ParticulateMatter25Micron(service)))
            buttonList.add(LayerButtonView(context,"Particle Pollution (PM 10)", WeatherService.ParticulateMatter10Micron(service)))

            for (customView in buttonList) {
                if (customView is LayerButtonView) {
                    layout.addView(customView.outerView) // Add layer button
                } else {
                    layout.addView(customView) //Add category label
                }
            }
        }
    }
}