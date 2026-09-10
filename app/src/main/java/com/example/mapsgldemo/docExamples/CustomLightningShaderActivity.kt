package com.example.mapsgldemo.docExamples

import androidx.appcompat.app.AppCompatActivity
import com.mapbox.maps.Style
import com.example.mapsgldemo.DocsExamplesMenuActivity
import com.example.mapsgldemo.customize.CustomizationDemoActivity
import com.xweather.mapsgl.controls.legend.Legend
import com.xweather.mapsgl.layers.spec.SymbolLayerDescriptor
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.sources.source.spec.VectorSourceDescriptor
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.IconPaint
import com.xweather.mapsgl.style.IconSize
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.types.Coordinate
import com.xweather.mapsgl.weather.LayerCode
import com.xweather.mapsgl.weather.WeatherLayerConfiguration
import com.xweather.mapsgl.weather.WeatherService
import com.xweather.mapsgl.weather.WeatherSource
import com.xweather.mapsgl.weather.common.Presentation

/**
 * **Lightning strikes that flash, driven by strike age.**
 *
 * Android port of the JS example
 * [Custom lightning symbols using a shader](https://www.xweather.com/docs/mapsgl/examples/custom-lightning-shader).
 * Each strike draws as a procedural glow with no sprite; a noise function gates when it flashes,
 * and `v_factor` - fed from the strike's `age` - controls how often and how brightly.
 *
 * Read [CustomEarthquakeShaderActivity] first for the icon-less shader path, the `v_tex` /
 * `u_time` interface and the `spin` trick that keeps frames arriving.
 *
 * ### Age drives everything
 *
 * The only paint expression is the factor, straight from the JS example:
 *
 * ```
 * factor: ['/', ['-', 200, ['get', 'age']], 200]
 * ```
 *
 * A fresh strike is near `1.0` and a 200-second-old one reaches `0.0`. In the shader `v_factor`
 * appears four times - flash duration, flash frequency, flash intensity and overall alpha - so a
 * recent strike flashes often and brightly while an old one barely registers. That single
 * expression is what makes the layer feel alive.
 *
 * ### The one setting that does not port: `blending: 2`
 *
 * The JS paint asks for a non-default blend mode and its shader premultiplies to suit
 * (`gl_FragColor.rgb *= gl_FragColor.a`). Android's symbol pass has no paint-level blend option -
 * [com.xweather.mapsgl.renderers.VectorSymbolLayerRenderer] fixes it at
 * `GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA` for every symbol layer - so this port drops both the
 * blending request and the premultiply, and composites with straight alpha instead.
 *
 * An isolated flash looks the same either way. The visible difference is where two flashes
 * overlap: additive blending accumulates into a brighter core, whereas straight alpha just lets
 * the nearer one win. Additive blending does exist in this SDK - the heatmap renderer uses
 * `GL_ONE, GL_ONE` - it simply is not exposed for symbols.
 *
 * ### The JS listing carries a lot of dead code
 *
 * Worth knowing before comparing the two shaders side by side. The published shader declares
 * `rand3d`, `noise3d`, `perlin`, `perlin3d`, `vPosition`, `vRandom`, `resolution`, `dpr` and the
 * `SIZE`, `RADIUS`, `SPEED` and (empty) `SEED` defines, and uses **none** of them. Only `rand`,
 * `noise` and `FLASH_POWER` do any work. This port keeps what runs and drops the rest, so the
 * flash maths is unchanged while the shader is a third of the length.
 *
 * ### The basemap matters here
 *
 * The JS example loads `dark-v9`, and that is not decoration: the flash colour is very nearly
 * white, so against the default light style there is almost nothing to see. This screen loads
 * [Style.DARK] and adds the layer in the load callback, because replacing a style discards a
 * custom layer added before it.
 *
 * ### A note on the layer id
 *
 * [WeatherService.LightningStrikes] pairs this source with a circle layer, and
 * [WeatherService.LightningStrikesIcons] gives its symbols a sprite - which would turn `v_tex`
 * into atlas coordinates and break the distance maths. This layer therefore uses its own id and
 * sets no icon image.
 */
class CustomLightningShaderActivity : CustomizationDemoActivity() {

    override fun menuActivity(): Class<out AppCompatActivity> =
        DocsExamplesMenuActivity::class.java

    override val caption =
        "Lightning strikes drawn with no sprite: a noise function gates each flash, and v_factor " +
            "from the strike's age sets how often and how brightly it fires."

    // The JS example's center: [-73.961, 31.984], zoom: 2. Coordinate is (lat, lon).
    override val cameraCenter = Coordinate(31.984, -73.961)
    override val cameraZoom = 3.0

    private lateinit var paint: SymbolLayerPaint
    private lateinit var layerId: String

    override fun customizeLayers(controller: MapboxMapController) {
        val config = LightningFlashConfiguration(controller.service)
        paint = config.layer.paint
        layerId = config.layer.id

        paint.icon.shader = FLASH_SHADER

        // JS `size: { width: 60, height: 60 }`. Constant and square, so iconSize carries it
        // directly - no need for the per-axis iconWidth / iconHeight.
        paint.icon.iconSize = IconSize(60f, 60f)

        // JS parity: both are already the Android defaults, set here to mirror the example.
        paint.pitchWithMap = true
        paint.icon.allowOverlap = StyleValue.Constant(true)

        applyFactor(byAge = true)

        // Keeps frames coming so u_time advances; rotation stays at zero.
        paint.icon.spin = true
        paint.icon.spinDegreesPerSecond = 0f

        // The JS example uses the dark basemap, and it is not cosmetic: the flash colour is very
        // nearly white, so on the default light style there is almost nothing to see. Loading a
        // style replaces it wholesale, which would drop a custom layer added beforehand, so the
        // layer goes on inside the callback once the new style is in place.
        controller.mapView.mapboxMap.loadStyle(Style.DARK) {
            controller.addWeatherLayer(config)
        }
    }

    override fun buildControls() {
        addToggle("Custom shader on", true) { on ->
            paint.icon.shader = if (on) FLASH_SHADER else null
            refreshSymbolPaint(layerId)
        }
        // Pinning the factor high makes every strike flash like a fresh one, which is the clearest
        // way to see what age is actually doing.
        addToggle("Flash rate from age (v_factor)", true) { byAge ->
            applyFactor(byAge)
            refreshSymbolPaint(layerId)
        }
        addSlider("Glow size", 0.4, 2.5, 1.0) { multiplier ->
            paint.icon.size = StyleValue.Constant(multiplier)
            refreshSymbolPaint(layerId)
        }
    }

    private fun applyFactor(byAge: Boolean) {
        paint.icon.factor = if (byAge) {
            StyleValue.Expression(AGE_FACTOR)
        } else {
            StyleValue.Constant(0.9)
        }
    }

    /**
     * Lightning vector tiles with a shader-only symbol layer over them.
     *
     * Reuses [WeatherSource.severe_lightning] so the features and their `age` property are the
     * same ones the built-in products read.
     */
    private class LightningFlashConfiguration(
        service: WeatherService,
    ) : WeatherLayerConfiguration<VectorSourceDescriptor, SymbolLayerDescriptor> {
        override val code: LayerCode = LayerCode.LIGHTNING_STRIKES
        override val source: VectorSourceDescriptor = WeatherSource.severe_lightning(service)

        // Deliberately neither "severe.lightning.circle" nor the icons layer id.
        override val layer: SymbolLayerDescriptor = SymbolLayerDescriptor(
            id = "severe.lightning.flash",
            source = source.id,
            paint = SymbolLayerPaint(icon = IconPaint(), text = emptyList()),
        )
        override var presentation: Presentation? = null
        override var legend: Legend? = null
    }

    private companion object {

        /** JS `factor: ['/', ['-', 200, ['get', 'age']], 200]` - 1.0 fresh, 0.0 at 200s. */
        val AGE_FACTOR: Expression = Expression.divide(
            Expression.subtract(200.0, Expression.get("age")),
            200.0,
        )

        /**
         * Ported from the JS example, with its unused noise helpers, defines and varyings removed.
         * The flash maths is untouched; only the interface moved to GLSL ES 3.x and the
         * premultiply at the end was dropped - see the class doc on `blending: 2`.
         */
        const val FLASH_SHADER = """#version 300 es
precision highp float;

uniform float u_time;

in vec2  v_tex;
in vec4  v_color;
in float v_factor;

out vec4 fragColor;

float rand(float x) {
    return fract(sin(x) * 75154.32912);
}

float noise(float x) {
    float i = floor(x);
    float a = rand(i), b = rand(i + 1.0);
    float f = x - i;
    return mix(a, b, f);
}

#define COL1 vec4(0, 0, 0, 0) / 255.0
#define COL2 vec4(235, 241, 245, 255) / 255.0
#define FLASH_POWER 0.8

void main() {
    // Radially symmetric about the quad centre, so v_tex needs no Y flip here.
    vec2 pos = v_tex;

    float dist = length(2.0 * pos - 1.0) * 2.0;
    float x = u_time + 0.1;

    float m = 0.2 + 0.2 * v_factor;   // max duration of strike
    float i = floor(x / m);
    float f = x / m - i;
    float k = v_factor;               // frequency of strikes
    float n = noise(i);
    float t = ceil(n - k);            // occurrence
    float d = max(0.0, n - k) / (1.0 - k); // duration
    float o = ceil(t - f - (1.0 - d)); // occurrence with duration

    float fx = 4.0;
    if (o == 1.0) {
        fx += 10.0 * v_factor;
    }

    fx = max(4.0, fx);
    float g = fx / (dist * (10.0 + 20.0)) * FLASH_POWER;

    // smooth out edges to avoid fading extending beyond the symbol's bounds
    float edgeFadeFactor = smoothstep(0.5, 1.0, dist);
    float invertedEdgeFadeFactor = 1.0 - edgeFadeFactor;

    vec4 color = mix(COL1, COL2, g);
    color.a *= min(1.0, 0.5 + v_factor) * invertedEdgeFadeFactor;

    // v_color.a carries the collision fade. No premultiply: this renderer blends
    // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA.
    fragColor = vec4(color.rgb, clamp(color.a, 0.0, 1.0) * v_color.a);
    if (fragColor.a < 0.01) discard;
}
"""
    }
}
