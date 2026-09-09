package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.controls.legend.Legend
import com.xweather.mapsgl.layers.spec.SymbolLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.GeoJSONSourceDescriptor
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.IconPaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherLayerConfiguration
import com.xweather.mapsgl.weather.WeatherService
import com.xweather.mapsgl.weather.WeatherSource
import com.xweather.mapsgl.weather.common.Presentation

/**
 * **Animated earthquake ripples from a custom GLSL fragment shader.**
 *
 * Android port of the JS example
 * [Custom earthquake symbols using a shader](https://www.xweather.com/docs/mapsgl/examples/custom-earthquake-shader).
 * Each quake draws as a procedural symbol - there is no sprite at all - and the shader turns the
 * quad into concentric waves that travel outward on `u_time`. Quad size comes from the quake's
 * magnitude, so a magnitude 7 ripples across roughly three times the area of a magnitude 3.
 *
 * ### Drawing with no icon image
 *
 * [IconPaint.shader] normally recolours a sprite. Set it with **no** [IconPaint.source] and the
 * renderer takes its icon-less path instead: it still lays the quad out from
 * [IconPaint.size] / [IconPaint.iconSize], but hands the fragment stage a clean `0..1` quad
 * coordinate in `v_tex` rather than an atlas rectangle. That is what makes `v_tex` usable as the
 * JS example's `vUv`, and it only holds while no image is set - give the layer an icon and `v_tex`
 * goes back to being atlas coordinates and the ripple maths breaks.
 *
 * ### Shader language version
 *
 * This shader declares `#version 300 es`, while the SDK's symbol **vertex** stage is fixed at
 * `#version 310 es` and is compiled against your source verbatim - the renderer does not rewrite
 * the version directive. The program is therefore mixed-version, which links only where the
 * driver's shading language is ESSL 3.20 or newer, since ESSL 3.20 is what permits 300 es, 310 es
 * and 320 es shaders to be linked together.
 *
 * Verified on an Adreno 650 reporting `OpenGL ES GLSL ES 3.20`: the ripples render identically to
 * the 310 es version and the renderer logs no fallback. Where the driver's top shading language is
 * ESSL 3.10, the link is rejected instead, and the SDK's never-a-hard-failure rule substitutes the
 * built-in icon shader - which, for a layer with no icon image, draws nothing at all. `310 es`
 * matches the vertex stage on every ES 3.1+ device and is the safer default for shipping code.
 *
 * Nothing in this effect needs ES 3.1: no `fwidth`, no compute, no 3.1-only builtins. Dropping to
 * `300 es` does **not** widen device support either, because the vertex stage still requires
 * ES 3.1, so the whole GL symbol path does too.
 *
 * ### Porting the shader from the web SDK
 *
 * The web SDK compiles GLSL ES 1.00 against a WebGL1 context. Translating this example needed
 * five changes, and nothing else:
 *
 * | JS (`paint.symbol.shader`) | Android ([IconPaint.shader]) |
 * |---|---|
 * | `varying vec2 vUv` | `in vec2 v_tex` (0..1 only with no icon image) |
 * | `varying float vFactor` | `in float v_factor`, from [IconPaint.factor] |
 * | `uniform float time` | `uniform float u_time` |
 * | `gl_FragColor = ...` | `out vec4 fragColor` |
 * | `precision mediump float` | `precision highp float` - the vertex stage uses highp |
 *
 * Two web-only details are dropped rather than translated:
 *
 * 1. `#extension GL_OES_standard_derivatives` - `fwidth` is core in ES 3.x, so requesting the
 *    extension is a compile error rather than a no-op.
 * 2. `gl_FragColor.rgb *= gl_FragColor.a` - the web layer blends premultiplied. Android's symbol
 *    renderer blends `GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA`, so premultiplying here would darken
 *    the ripples twice. This is the one difference that looks like a bug rather than a syntax
 *    error if you copy the web shader across unchanged.
 *
 * The JS `resolution` and `dpr` uniforms have no Android counterpart. This effect never reads
 * them, so nothing is lost; an effect that needs pixel size should derive it from the quad instead.
 *
 * ### Keeping it moving
 *
 * `animated: true` in the JS paint has no direct Android equivalent, and a shader that samples
 * `u_time` does **not** by itself keep the map painting - once the camera settles and the timeline
 * is paused, Mapbox stops calling the renderer and the ripples freeze. The renderer treats a
 * symbol layer as continuously animated when [IconPaint.spin] is set, so this demo sets `spin`
 * with [IconPaint.spinDegreesPerSecond] at `0`: frames keep arriving and `u_time` keeps advancing,
 * while the rotation term stays zero. `spin` is read when the layer is added, so it has to be set
 * before [MapboxMapController.addWeatherLayer].
 *
 * ### Sizing from magnitude
 *
 * The JS example sets `size: { width: s, height: s }` where
 * `s = 20 + 100 * (report.mag / 10)`, in JS icon pixels. Android splits that in two:
 * [IconPaint.iconSize] is the pixel box but takes constants only, while [IconPaint.size] is a
 * *multiplier* and accepts an expression. Dividing the JS pixel expression by
 * [com.xweather.mapsgl.style.IconSize.JS_REFERENCE_WIDTH_PX] (40, the width Android measures
 * multipliers against) turns one into the other and lands on the same pixel size per feature.
 */
class CustomEarthquakeShaderActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Earthquakes drawn with no sprite at all: a custom GLSL shader turns each symbol into " +
            "ripples travelling outward on u_time, sized by the quake's magnitude."

    // The JS example opens on the Ryukyu / Philippine Sea trench, which is reliably busy.
    override val cameraCenter = Coordinate(26.232, 124.091)
    override val cameraZoom = 2.0

    private lateinit var paint: SymbolLayerPaint
    private lateinit var layerId: String

    override fun customizeLayers(controller: MapboxMapController) {
        val config = EarthquakeRippleConfiguration(controller.service)
        paint = config.layer.paint
        layerId = config.layer.id

        paint.icon.shader = RIPPLE_SHADER
        paint.pitchWithMap = true
        applySize(MAGNITUDE_SIZE_SCALE)

        // Registers the layer as continuously animated without actually rotating it - see the
        // class doc. Must be set before the layer is added.
        paint.icon.spin = true
        paint.icon.spinDegreesPerSecond = 0f

        controller.addWeatherLayer(config)
    }

    override fun buildControls() {
        // Clearing shader falls back to the SDK's own icon shader. With no icon image to draw,
        // that leaves flat quads - which is a blunt but honest way to see how much of this screen
        // is the shader's work.
        addToggle("Custom shader on", true) { on ->
            paint.icon.shader = if (on) RIPPLE_SHADER else null
            refreshSymbolPaint(layerId)
        }
        addToggle("Size from magnitude", true) { byMagnitude ->
            applySize(if (byMagnitude) MAGNITUDE_SIZE_SCALE else null)
            refreshSymbolPaint(layerId)
        }
        addSlider("Ripple size", 0.10, 3.0, 1.0) { multiplier ->
            sizeMultiplier = multiplier
            applySize(if (sizeFromMagnitude) MAGNITUDE_SIZE_SCALE else null)
            refreshSymbolPaint(layerId)
        }
    }

    private var sizeMultiplier: Double = 1.0
    private var sizeFromMagnitude: Boolean = true

    /**
     * [IconPaint.size] is a multiplier over a 40 JS-pixel reference box, so the JS pixel
     * expression is reproduced by dividing it by that reference width.
     *
     * Passing `null` swaps the expression for a flat multiplier, which is what the "size from
     * magnitude" toggle turns off.
     */
    private fun applySize(magnitudeScale: Expression?) {
        sizeFromMagnitude = magnitudeScale != null
        paint.icon.size = if (magnitudeScale != null) {
            StyleValue.Expression(Expression.multiply(magnitudeScale, sizeMultiplier))
        } else {
            StyleValue.Constant(FLAT_SIZE_SCALE * sizeMultiplier)
        }
    }

    /**
     * Earthquakes GeoJSON with a symbol layer over it.
     *
     * [WeatherService.Earthquakes] pairs this source with a *circle* layer, matching the default
     * product, so it cannot carry an icon shader. Reusing [WeatherSource.earthquakes] keeps the
     * same data and the same `report.*` feature properties while swapping in a symbol layer -
     * the Android equivalent of the JS example's `type: 'symbol', source: 'earthquakes'`.
     */
    private class EarthquakeRippleConfiguration(
        service: WeatherService,
    ) : WeatherLayerConfiguration<GeoJSONSourceDescriptor, SymbolLayerDescriptor> {
        override val code: LayerCode = LayerCode.EARTHQUAKES
        override val source: GeoJSONSourceDescriptor = WeatherSource.earthquakes(service)
        override val layer: SymbolLayerDescriptor = SymbolLayerDescriptor(
            id = "climate.earthquakes.ripple",
            source = source.id,
            paint = SymbolLayerPaint(icon = IconPaint(), text = emptyList()),
        )
        override var presentation: Presentation? = null
        override var legend: Legend? = null
    }

    private companion object {

        /**
         * JS `['+', 20, ['*', 100, ['/', ['coalesce', ['get', 'report.mag'], 1], 10]]]`, divided
         * by the 40px reference width to become an Android size multiplier. `coalesce` covers
         * quakes that report no magnitude, which would otherwise size to nothing.
         */
        val MAGNITUDE_SIZE_SCALE: Expression = Expression.divide(
            Expression.add(
                20.0,
                Expression.multiply(
                    100.0,
                    Expression.divide(
                        Expression.coalesce(listOf(Expression.get("report.mag"), 1.0)),
                        10.0,
                    ),
                ),
            ),
            40.0,
        )

        /** Mid-scale stand-in when magnitude sizing is switched off (JS `s` at magnitude 5). */
        const val FLAT_SIZE_SCALE = 70.0 / 40.0

        /**
         * Ported from the JS example, itself adapted from https://www.shadertoy.com/view/ldycR3.
         *
         * The wave maths is unchanged; only the shader interface moved to GLSL ES 3.x. The full
         * uniform set the symbol vertex stage exposes is declared even though this effect only
         * reads `u_time`, so the block can be copied as a starting point for other effects.
         */
        const val RIPPLE_SHADER = """#version 300 es
precision highp float;

uniform sampler2D u_sdfTex;
uniform sampler2D u_rasterTex;
uniform vec4  u_haloColor;
uniform float u_haloWidth;
uniform int   u_raster_mode;
uniform float u_time;

in vec2  v_tex;
in vec4  v_color;
in float v_factor;

out vec4 fragColor;

vec3 hsb2rgb(in vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    rgb = rgb * rgb * (3.0 - 2.0 * rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

void main() {
    // With no icon image bound, v_tex is the quad's own 0..1 coordinate - the JS example's vUv.
    vec2 pos = v_tex;
    float t = u_time;

    float dist = length(2.0 * pos - 1.0) * 2.0;
    vec2 p = vec2(dist);

    float r = length(p) * 0.9;
    vec3 color = hsb2rgb(vec3(0.03, 0.7, 0.6));

    float a = pow(r, 3.0);
    float b = sin(r * 1.8 - 1.6);
    float c = sin(r - 0.010);
    float s = sin(a - t * 2.0 + b) * c;

    color *= abs(1.0 / (s * 1.8)) - 0.01;

    // Fade to nothing at the quad's inscribed circle. max() rather than the JS example's bare
    // 1.0 - d: outside that circle the term goes negative, and a negative alpha reaching
    // GL_ONE_MINUS_SRC_ALPHA brightens the basemap instead of leaving it alone.
    float d = length(2.0 * pos - 1.0);
    float alpha = max(0.0, 1.0 - d);

    // v_color.a carries the collision fade, so it has to scale the result or symbols stop
    // fading in and out. No premultiply here: this renderer does not blend premultiplied.
    fragColor = vec4(color, alpha * v_color.a);
    if (fragColor.a < 0.01) discard;
}
"""
    }
}
