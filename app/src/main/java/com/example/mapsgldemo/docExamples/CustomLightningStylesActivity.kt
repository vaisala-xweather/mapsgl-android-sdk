package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.mapbox.maps.Style
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.CircleLayerPaint
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Lightning strike circles styled by strike age.**
 *
 * Android port of the JS example
 * [Customizing lightning point styles](https://www.xweather.com/docs/mapsgl/examples/custom-lightning-styles).
 * No shader here - this is the plain paint-override pattern: take the built-in
 * `lightning-strikes` circle layer and replace its fill, stroke and radius so a fresh strike is
 * solid white and older ones fade out in bands.
 *
 * ### Overriding paint on a built-in configuration
 *
 * [WeatherService.LightningStrikes] hands back the same configuration `addWeatherLayer` would build
 * from [LayerCode.LIGHTNING_STRIKES]. Mutate its paint before adding it and the layer arrives
 * styled, which is the Android counterpart of the JS `addWeatherLayer('lightning-strikes', { paint })`
 * options argument.
 *
 * ### The alpha ladder
 *
 * The JS example fades with age through two `step` expressions - one on `fill.color`, one on
 * `fill.opacity`. Only the colour is data-driven here, so the two are folded into one ladder whose
 * alphas are the products of the JS pairs: `1.0`, `0.56`, `0.30`, `0.12`. The rendered result is
 * the same.
 *
 * ### `step` wants the base output first
 *
 * [Expression.step] takes [Expression.Step] objects where the **first** one is the output used
 * below the first real threshold and its `value` is ignored - pass `null` for it. That mirrors the
 * JS form, where the output immediately after the input is the base case.
 *
 * `coalesce` guards strikes that arrive without an `age`, which would otherwise fall through to the
 * base output by accident rather than by intent.
 *
 * ### The basemap
 *
 * The JS example loads `dark-v9`. White circles on the default light style would be invisible, so
 * this screen loads [Style.DARK] and adds the layer in the load callback - replacing a style
 * discards a layer added before it.
 *
 * The colour ladder is set once, before the layer is added, exactly as the JS example does. The
 * radius slider is the demo's own addition.
 *
 * See also [com.example.mapsgldemo.customize.DataDrivenColorActivity], which colours the same layer
 * by the same property as a tour of `interpolate` versus `step`; this screen is the published
 * example, with its specific thresholds and white fade.
 */
class CustomLightningStylesActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Lightning circles styled by age: solid white under a minute, then fading in bands at " +
            "1, 5 and 10 minutes. Plain paint overrides - no shader."

    // The JS example's center: [-84.5, 30], zoom: 5. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(30.0, -84.5)
    override val cameraZoom = 5.0

    private lateinit var paint: CircleLayerPaint
    private lateinit var layerId: String
    private var radius = JS_RADIUS

    override fun customizeLayers(controller: MapboxMapController) {
        // Dark first: white circles are invisible on the default light style. The layer goes on
        // inside the callback because loading a style discards layers added beforehand.
        controller.mapView.mapboxMap.loadStyle(Style.DARK) { addStrikes() }
    }

    private fun addStrikes() {
        val lightning = WeatherService.LightningStrikes(controller.service)
        paint = lightning.layer.paint
        layerId = lightning.layer.id

        applyFill()

        // JS `stroke: { color: '#fff', thickness: 1 }` and `circle: { radius: 4 }`.
        paint.stroke.color = StyleValue.Constant(Color.White)
        paint.stroke.thickness = StyleValue.Constant(1.0)
        paint.circle.radius = StyleValue.Constant(radius)

        controller.addWeatherLayer(lightning)
    }

    /**
     * Applies a paint change to the layer in place.
     *
     * [MapboxMapController.refreshGlVectorLayerPaint] handles every vector layer type despite the
     * "symbol" naming on the base class helper: symbols through their instance cache, everything
     * else - circles included - through VectorGeometryCoordinator.refreshPaint.
     */
    private fun refreshPaint() = controller.refreshGlVectorLayerPaint(layerId)

    override fun buildControls() {
        addSlider("Circle radius", 2.0, 12.0, JS_RADIUS, { "%.0f".format(it) }) {
            radius = it
            paint.circle.radius = StyleValue.Constant(radius)
            refreshPaint()
        }
    }

    /**
     * The single folded ladder. `null` on the first [Expression.Step] marks it as the base output,
     * used for anything below the first threshold.
     */
    private fun applyFill() {
        paint.fill.color = StyleValue.Expression(
            Expression.step(
                Expression.coalesce(listOf(Expression.get("age"), 0.0)),
                listOf(
                    Expression.Step(null, "rgba(255, 255, 255, 1)"),
                    Expression.Step(61.0, "rgba(255, 255, 255, 0.56)"),
                    Expression.Step(301.0, "rgba(255, 255, 255, 0.30)"),
                    Expression.Step(601.0, "rgba(255, 255, 255, 0.12)"),
                ),
            ),
        )
    }

    private companion object {
        /** JS `circle: { radius: 4 }`. */
        const val JS_RADIUS = 4.0
    }
}
