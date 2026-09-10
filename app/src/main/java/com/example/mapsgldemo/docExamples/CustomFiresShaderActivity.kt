package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.controls.legend.Legend
import com.xweather.mapsgl.layers.spec.SymbolLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.VectorSourceDescriptor
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.IconPaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.types.Anchor
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherLayerConfiguration
import com.xweather.mapsgl.weather.WeatherService
import com.xweather.mapsgl.weather.WeatherSource
import com.xweather.mapsgl.weather.common.Presentation

/**
 * **Animated wildfire flames from a custom GLSL fragment shader.**
 *
 * Android port of the JS example
 * [Custom fire symbols using a shader](https://www.xweather.com/docs/mapsgl/examples/custom-fires-shader).
 * Every fire observation draws as a procedural flame - no sprite - with fractal noise driving the
 * flicker, sized from the fire's acreage and blackened to smoke once it is fully contained.
 *
 * Read [CustomEarthquakeShaderActivity] first: it covers the icon-less shader path, the
 * `v_tex`/`u_time` interface and the `spin` trick for keeping frames coming. This example adds
 * five things that one does not need.
 *
 * ### 1. Reusable noise via `#include <fbm>`
 *
 * The flame needs fractal Brownian motion, and the SDK ships the same chunk registry the web SDK
 * uses, so `#include <fbm>` resolves to the identical value-noise implementation rather than
 * pasting it in by hand. An unresolvable name is left as a GLSL comment, so a typo surfaces as a
 * compile error at the reference site instead of vanishing.
 *
 * ### 2. `v_random` and `v_factor`
 *
 * `u_time` alone would make every fire on the map flicker in lockstep. The JS shader offsets each
 * instance with `vRandom`; on Android that is `v_random`, a stable per-instance value the vertex
 * stage provides for free.
 *
 * `v_factor` carries [IconPaint.factor], set here to `report.perContained / 100`. The shader tests
 * `v_factor == 1.0` and swaps flame colour for smoke, so a fully contained fire reads differently
 * without a second layer.
 *
 * ### 3. Orientation: keep the JS flip
 *
 * `v_tex.y` grows *downward* on screen - the vertex stage sets `v_tex = (a_quad + 1) / 2`, and
 * billboard mode maps `a_quad.y = +1` to a negative NDC offset, so `v_tex.y = 1` is the quad's
 * bottom edge. That matches the JS `vUv` orientation, so the example's
 * `uv = vec2(vUv.x, 1.0 - vUv.y)` flip is ported verbatim. Drop it and the flame body lands at the
 * top with the smoke falling downward, which is the one bug in this port that still looks
 * plausible at a glance.
 *
 * ### 4. `fwidth` needs no extension
 *
 * The JS shader requests `GL_OES_standard_derivatives` for `fwidth`. In GLSL ES 3.x `fwidth` is
 * core, so that `#extension` line is a compile error rather than a no-op and is simply dropped.
 *
 * ### 5. Size: one expression per axis
 *
 * The JS example sizes width and height *independently* per feature:
 *
 * ```
 * width  = min(80,  round(30 + 40 * f))
 * height = min(130, round(40 + 80 * f))
 * ```
 *
 * so a fire's aspect ratio changes with its size - 30x40 when small, 80x130 when large.
 * [IconPaint.iconSize] carries constants only and [IconPaint.size] is a single multiplier applied
 * to both axes, so neither can express that on its own. [IconPaint.iconWidth] and
 * [IconPaint.iconHeight] can: each resolves from its own expression per feature, so both JS
 * formulas port across exactly.
 *
 * [IconPaint.size] still multiplies whatever those resolve to, which is what the demo's flame-size
 * slider drives.
 *
 * ### A note on the layer id
 *
 * The renderer special-cases the id `climate.fire_obs.icons`, forcing an 18-40px square from
 * `report.areaAC` to match the JS `fires-obs-icons` product. Reusing that id here would silently
 * override everything above, so this layer uses its own id.
 */
class CustomFiresShaderActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Wildfire observations drawn with no sprite: fbm noise drives a flame that flickers on " +
            "u_time, offset per fire by v_random, sized by acreage and turned to smoke once " +
            "fully contained."

    // The JS example's center: [-94, 42], zoom 2 - CONUS, where fires-obs is densest.
    override val cameraCenter = Coordinate(42.0, -94.0)
    override val cameraZoom = 3.0

    private lateinit var paint: SymbolLayerPaint
    private lateinit var layerId: String

    override fun customizeLayers(controller: MapboxMapController) {
        val config = FireFlameConfiguration(controller.service)
        paint = config.layer.paint
        layerId = config.layer.id

        paint.icon.shader = FLAME_SHADER

        // Flames are screen-upright: they should not tilt into the map plane or swing with
        // bearing. JS parity - the example sets both false.
        paint.pitchWithMap = false
        paint.rotateWithMap = false

        // The flame's base sits on the fire, so the quad anchors at its bottom edge.
        paint.icon.anchor = StyleValue.Constant(Anchor.BOTTOM)

        // One expression per axis, straight from the JS example.
        paint.icon.iconWidth = StyleValue.Expression(JS_WIDTH_PX)
        paint.icon.iconHeight = StyleValue.Expression(JS_HEIGHT_PX)
        applySize(1.0)
        paint.icon.factor = StyleValue.Expression(CONTAINED_FRACTION)

        // Registers the layer as continuously animated without rotating it; see
        // CustomEarthquakeShaderActivity for why this is needed. Must precede addWeatherLayer.
        paint.icon.spin = true
        paint.icon.spinDegreesPerSecond = 0f

        controller.addWeatherLayer(config)
    }

    override fun buildControls() {
        addToggle("Custom shader on", true) { on ->
            paint.icon.shader = if (on) FLAME_SHADER else null
            refreshSymbolPaint(layerId)
        }
        addToggle("Smoke when contained (drive v_factor)", true) { byContainment ->
            paint.icon.factor = if (byContainment) {
                StyleValue.Expression(CONTAINED_FRACTION)
            } else {
                StyleValue.Constant(0.0)
            }
            refreshSymbolPaint(layerId)
        }
        addSlider("Flame size", 0.25, 2.5, 1.0) { multiplier ->
            applySize(multiplier)
            refreshSymbolPaint(layerId)
        }
    }

    /**
     * [IconPaint.size] multiplies whatever [IconPaint.iconWidth] / [IconPaint.iconHeight] resolve
     * to, so the demo's slider rides on top of the JS pixel formulas without disturbing them.
     */
    private fun applySize(multiplier: Double) {
        paint.icon.size = StyleValue.Constant(multiplier)
    }

    /**
     * `fires-obs` vector tiles with a symbol layer over them.
     *
     * [WeatherService.FiresObs] pairs this source with a circle layer, so it cannot carry an icon
     * shader; reusing [WeatherSource.fires_obs] keeps the same tiles and `report.*` properties.
     * The Android equivalent of the JS example's `type: 'symbol'` override.
     */
    private class FireFlameConfiguration(
        service: WeatherService,
    ) : WeatherLayerConfiguration<VectorSourceDescriptor, SymbolLayerDescriptor> {
        override val code: LayerCode = LayerCode.FIRES_OBS
        override val source: VectorSourceDescriptor = WeatherSource.fires_obs(service)

        // Deliberately not "climate.fire_obs.icons" - see the class doc.
        override val layer: SymbolLayerDescriptor = SymbolLayerDescriptor(
            id = "climate.fire_obs.flames",
            source = source.id,
            paint = SymbolLayerPaint(icon = IconPaint(), text = emptyList()),
        )
        override var presentation: Presentation? = null
        override var legend: Legend? = null
    }

    private companion object {

        /** JS `['/', ['coalesce', ['get', 'report.perContained'], 0], 100]`. */
        val CONTAINED_FRACTION: Expression = Expression.divide(
            Expression.coalesce(listOf(Expression.get("report.perContained"), 0.0)),
            100.0,
        )

        /**
         * JS:
         * ```
         * area      = ['max', 1, ['coalesce', ['get', 'report.areaAC'], 0]]
         * contained = ['coalesce', ['get', 'report.perContained'], 0]
         * f         = ['/', ['*', area, ['case', ['==', contained, 100], 0.75, 1]], 10000]
         * ```
         * A contained fire is scaled to three quarters before sizing, so it settles rather than
         * disappearing.
         */
        val JS_SIZE_F: Expression = Expression.divide(
            Expression.multiply(
                Expression.max(
                    1.0,
                    Expression.coalesce(listOf(Expression.get("report.areaAC"), 0.0)),
                ),
                Expression.switchCase(
                    listOf(
                        Expression.Case(
                            condition = Expression.equals(
                                Expression.coalesce(
                                    listOf(Expression.get("report.perContained"), 0.0),
                                ),
                                100.0,
                            ),
                            result = 0.75,
                        ),
                    ),
                    fallback = 1.0,
                ),
            ),
            10000.0,
        )

        /** JS `width: ['min', 80, ['round', ['+', 30, ['*', 40, ['var', 'f']]]]]`. */
        val JS_WIDTH_PX: Expression = Expression.min(
            80.0,
            Expression.round(Expression.add(30.0, Expression.multiply(40.0, JS_SIZE_F))),
        )

        /** JS `height: ['min', 130, ['round', ['+', 40, ['*', 80, ['var', 'f']]]]]`. */
        val JS_HEIGHT_PX: Expression = Expression.min(
            130.0,
            Expression.round(Expression.add(40.0, Expression.multiply(80.0, JS_SIZE_F))),
        )

        /**
         * Ported from the JS example, itself adapted from https://www.shadertoy.com/view/Xtf3DX.
         *
         * The flame maths is unchanged. The interface moved to GLSL ES 3.x, the premultiply at the
         * end was dropped because this renderer blends non-premultiplied, and the unused
         * `tDiffuse`, `resolution` and `dpr` uniforms are gone - Android exposes no counterpart and
         * the effect never read them.
         */
        const val FLAME_SHADER = """#version 300 es
precision highp float;

uniform float u_time;

in vec2  v_tex;
in vec4  v_color;
in float v_factor;
in float v_random;

out vec4 fragColor;

#include <fbm>

void main() {
    // v_tex is the quad's 0..1 coordinate (no icon image bound), and v_tex.y grows *downward*
    // on screen: the vertex stage sets v_tex = (a_quad + 1) / 2, and billboard mode maps
    // a_quad.y = +1 to a negative NDC offset, so v_tex.y = 1 is the quad's bottom edge.
    //
    // That is the same orientation as the JS example's vUv, so its flip is kept verbatim.
    // Dropping it puts the flame body at the top with the smoke falling downward.
    vec2 uv = vec2(v_tex.x, 1.0 - v_tex.y);

    // v_random de-syncs the fires; without it every flame on the map flickers in lockstep.
    float t = (u_time + v_random * 10.0) * 0.5;

    vec2 q = uv;
    q.x *= 1.0;
    q.y *= 2.0;
    float strength = floor(q.x + 2.0);
    float T3 = max(3.0, 1.25 * strength) * t;
    q.x = mod(q.x, 1.0) - 0.5;
    q.y -= 0.25;

    float n = fbm(strength * q - vec2(0, T3));
    float c = 1.0 - 16.0 * pow(max(0.0, length(q * vec2(1.8 + q.y * 1.5, 0.75)) - n * max(0.0, q.y + 0.25)), 1.2);
    float c1 = n * c * (1.5 - pow(1.25 * uv.y, 2.0));
    c1 = clamp(c1, 0.0, 1.0);

    // flame colour
    vec3 col = vec3(3.5 * c1, 1.5 * c1 * c1 * c1, c1 * c1 * c1 * c1 * c1 * c1);
    float alpha = 1.0;

    // v_factor is percent contained; a fully contained fire goes to smoke.
    if (v_factor == 1.0) {
        col *= vec3(0.0);
        alpha = 0.7;
    }

    // smoke intensity: a lower number gives more smoke
    float a = c * (1.0 - pow(uv.y, 1.0));
    col = mix(vec3(0.3), col, a);

    if (col.r == col.g && col.g == col.b) {
        alpha *= 1.0 - col.r;
    }

    if (uv.y > 0.5) {
        float dist = length(2.0 * uv - 1.0);
        float delta = fwidth(dist);
        alpha *= 1.0 - smoothstep(1.0 - delta - 0.5, 1.0, dist);
    }

    // v_color.a carries the collision fade. No premultiply: this renderer blends
    // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, so the JS `rgb *= a` would darken twice.
    fragColor = vec4(col, clamp(alpha, 0.0, 1.0) * v_color.a);
    if (fragColor.a < 0.01) discard;
}
"""
    }
}
