package com.example.mapsgldemo.customize

import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Anchor icons to the map instead of the screen.**
 *
 * By default a symbol icon is billboarded: it faces the viewer, staying upright and the same size
 * however you tilt or rotate the map. That is right for labels and pins, and wrong for anything
 * representing a direction or lying flat on the ground.
 *
 * - `IconPaint.rotateWithMap` - rotation tracks the map bearing, so a heading arrow keeps pointing
 *   at the real-world heading when you rotate the map.
 * - `SymbolLayerPaint.pitchWithMap` - the icon lies in the map plane and foreshortens as you tilt,
 *   like paint on the ground rather than a sign standing up.
 *
 * Rotate with two fingers and drag down to pitch, then toggle each one. With both off nothing
 * about the icons changes as the camera moves - which is the point of billboarding.
 */
class IconOrientationActivity : CustomizationDemoActivity() {

    override val caption =
        "Two-finger rotate and drag down to tilt the map, then toggle these. With both off the " +
            "icons stay screen-facing however the camera moves."

    override val cameraCenter = Coordinate(32.0, -95.0)
    override val cameraZoom = 5.5

    private lateinit var paint: SymbolLayerPaint
    private lateinit var layerId: String

    override fun customizeLayers(controller: MapboxMapController) {
        val icons = WeatherService.LightningStrikesIcons(controller.service)
        paint = icons.layer.paint
        layerId = icons.layer.id

        paint.icon.rotateWithMap = true
        paint.pitchWithMap = true
        paint.icon.scale = StyleValue.Constant(SCALE_DEFAULT)

        controller.addWeatherLayer(icons)
    }

    override fun buildControls() {
        addToggle("rotateWithMap (follow map bearing)", true) {
            paint.icon.rotateWithMap = it
            refreshSymbolPaint(layerId)
        }
        addToggle("pitchWithMap (lie flat in the map plane)", true) {
            paint.pitchWithMap = it
            refreshSymbolPaint(layerId)
        }
        addSlider("Icon scale", 0.5, 4.0, SCALE_DEFAULT) {
            paint.icon.scale = StyleValue.Constant(it)
            refreshSymbolPaint(layerId)
        }
    }

    private companion object {
        const val SCALE_DEFAULT = 1.8
    }
}
