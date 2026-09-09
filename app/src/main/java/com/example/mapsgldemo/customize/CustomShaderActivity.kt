package com.example.mapsgldemo.customize

import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.WeatherService

/**
 * **Run your own GLSL fragment shader on symbol icons.**
 *
 * `IconPaint.shader` replaces the SDK's symbol fragment shader outright, so icons can pulse, fade
 * or recolour on the GPU instead of being redrawn from Kotlin.
 *
 * Because it replaces the whole shader, yours has to honour the same contract. The interface the
 * symbol vertex stage hands you is:
 *
 * ```glsl
 * #version 310 es
 * precision highp float;                  // required - the vertex stage uses highp
 * uniform sampler2D u_sdfTex;             // glyph / SDF atlas
 * uniform sampler2D u_rasterTex;          // sprite atlas
 * uniform vec4  u_haloColor;
 * uniform float u_haloWidth;
 * uniform int   u_raster_mode;            // != 0 for sprite icons, 0 for SDF
 * uniform float u_time;                   // seconds since process start
 * in vec2  v_tex;
 * in vec4  v_color;                       // .a carries the collision fade - always respect it
 * in float v_factor;                      // per-feature IconPaint.factor, default 1.0
 * out vec4 fragColor;
 * ```
 *
 * Two things people get wrong:
 *
 * 1. Writing GLSL ES 1.00 (`varying`, `texture2D`, `gl_FragColor`). This is ES 3.10.
 * 2. Changing icon paint *after* the layer is on the map without calling
 *    [MapboxMapController.refreshGlVectorLayerPaint]. Setting it before `addWeatherLayer`, as
 *    below, needs no refresh.
 *
 * If your shader fails to compile the SDK logs the driver's message and falls back to the built-in
 * icon shader rather than taking the app down, so a typo shows up as "icons look normal".
 */
class CustomShaderActivity : CustomizationDemoActivity() {

    override val caption =
        "Lightning icons run a custom GLSL fragment shader: every icon pulses on u_time, and " +
            "v_factor (fed from the \"age\" property) makes older strikes pulse dimmer."

    override val cameraCenter = Coordinate(32.0, -95.0)
    override val cameraZoom = 5.0

    private lateinit var paint: SymbolLayerPaint
    private lateinit var layerId: String

    override fun customizeLayers(controller: MapboxMapController) {
        val icons = WeatherService.LightningStrikesIcons(controller.service)
        paint = icons.layer.paint
        layerId = icons.layer.id

        paint.icon.shader = PULSE_SHADER
        applyFactor(true)

        controller.addWeatherLayer(icons)
    }

    override fun buildControls() {
        // Setting shader back to null restores the SDK's own icon shader, so this is an honest
        // A/B rather than a claim.
        addToggle("Custom shader on", true) { on ->
            paint.icon.shader = if (on) PULSE_SHADER else null
            refreshSymbolPaint(layerId)
        }
        addToggle("Dim older strikes (drive v_factor from age)", true) { byAge ->
            applyFactor(byAge)
            refreshSymbolPaint(layerId)
        }
        addSlider("Icon scale", 0.5, 4.0, 1.0) {
            paint.icon.scale = StyleValue.Constant(it)
            refreshSymbolPaint(layerId)
        }
    }

    /**
     * `factor` is an ordinary data-driven value, so it can be an expression. Driven by age it maps
     * 0s → 1.0 and 1800s → 0.15; flat, every icon gets 1.0 and the pulse is uniform.
     */
    private fun applyFactor(byAge: Boolean) {
        paint.icon.factor = if (byAge) {
            StyleValue.Expression(
                Expression.interpolate(Expression.get("age"), listOf(0.0, 1.0, 1800.0, 0.15)),
            )
        } else {
            StyleValue.Constant(1.0)
        }
    }

    private companion object {
        /**
         * Samples the icon's sprite, then scales its brightness by a sine wave on `u_time` so the
         * icon pulses; `v_factor` fades that pulse out for older strikes.
         *
         * The `u_raster_mode` branch is kept because the same shader has to work whether the icon
         * came from the sprite atlas or the SDF atlas.
         */
        const val PULSE_SHADER = """#version 310 es
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

void main() {
    // 0..1, twice a second. v_factor dims the pulse for older strikes, but both terms keep a
    // floor: multiplying them raw sent old icons to ~15% brightness, which read as dirty brown
    // rather than "older", and on a light basemap you could barely find them.
    float pulse = 0.5 + 0.5 * sin(u_time * 6.2831853);
    float brightness = mix(0.55, 1.0, pulse) * mix(0.6, 1.0, v_factor);

    if (u_raster_mode != 0) {
        vec4 sprite = texture(u_rasterTex, v_tex);
        // v_color.a is the collision fade - multiply it in or icons stop fading in and out.
        fragColor = vec4(sprite.rgb * v_color.rgb * brightness, sprite.a * v_color.a);
        if (fragColor.a < 0.05) discard;
        return;
    }

    // SDF path: same coverage maths as the built-in shader, brightness applied to the fill.
    float dist = texture(u_sdfTex, v_tex).r;
    float aa = max(fwidth(dist), 0.001);
    float coverage = smoothstep(0.8 - aa, 0.8 + aa, dist);
    fragColor = vec4(v_color.rgb * brightness, coverage * v_color.a);
    if (fragColor.a < 0.05) discard;
}
"""
    }
}
