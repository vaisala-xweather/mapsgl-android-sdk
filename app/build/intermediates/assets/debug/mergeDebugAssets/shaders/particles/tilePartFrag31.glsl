#version 310 es
precision highp float; // Keep high precision

// --- Uniforms remain the same ---
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform float x_offset2;
uniform float y_offset2;
uniform float x_scale2;
uniform float tx_scale;
uniform float ty_scale;
uniform float tx_offset;
uniform float ty_offset;
uniform float time;
uniform sampler2D u_Texture;
uniform sampler2D colorScale;
// uniform sampler2D dataTextureNext;
uniform float noData;
uniform float mult;
uniform int mode;
uniform int channel1;
uniform int channel2;
uniform int interpolation;
uniform float outputRangeMin;
uniform float outputRangeMax;
uniform float drawRangeMin;
uniform float drawRangeMax;
uniform float dataRangeMin;
uniform float dataRangeMax;
uniform float sampleDataMin;
uniform float sampleDataMax;
uniform float colorSampleOffset;
uniform float colorRangeFactor;

// --- Varyings become 'in' variables (Fragment Shader Inputs) ---
in vec4 outPos;     // Input from VS
in float alphaOut;  // Input from VS

// --- Fragment Shader Output ---
out vec4 fragColor; // Declare the output variable

// --- Constants ---
const vec2 dataResolution = vec2(516.0, 516.0);


vec4 SAM2(sampler2D tex, int a, int b, vec2 res, vec2 i) {
    return texture(tex, (i + vec2(float(a), float(b)) + 0.5) * (1.0 / res));
}
const vec4 BS_A = vec4(   3.0,  -6.0,   0.0,  4.0 ) * (1.0/6.0);
const vec4 BS_B = vec4(  -1.0,   6.0, -12.0,  8.0 ) * (1.0/6.0);
const vec4 CA = BS_A;
const vec4 CB = BS_B;
vec4 powers(float x) { return vec4(x * x * x, x * x, x, 1.0); }
vec4 spline(float x, vec4 c0, vec4 c1, vec4 c2, vec4 c3) { vec4 px = powers(x); vec4 p1mx = powers(1.0 - x); vec4 pxp1 = powers(x + 1.0); vec4 p2mx = powers(2.0 - x); return c0 * dot(CB, pxp1) + c1 * dot(CA, px) + c2 * dot(CA, p1mx) + c3 * dot(CB, p2mx); }
vec4 bicubic(sampler2D tex, vec2 uv, vec2 res) { vec2 tuv = uv * res - 0.5; vec2 f = fract(tuv); vec2 i = floor(tuv); vec4 r0 = spline(f.x, SAM2(tex, -1, -1, res, i), SAM2(tex, 0, -1, res, i), SAM2(tex, 1, -1, res, i), SAM2(tex, 2, -1, res, i)); vec4 r1 = spline(f.x, SAM2(tex, -1, 0, res, i), SAM2(tex, 0, 0, res, i), SAM2(tex, 1, 0, res, i), SAM2(tex, 2, 0, res, i)); vec4 r2 = spline(f.x, SAM2(tex, -1, 1, res, i), SAM2(tex, 0, 1, res, i), SAM2(tex, 1, 1, res, i), SAM2(tex, 2, 1, res, i)); vec4 r3 = spline(f.x, SAM2(tex, -1, 2, res, i), SAM2(tex, 0, 2, res, i), SAM2(tex, 1, 2, res, i), SAM2(tex, 2, 2, res, i)); return spline(f.y, r0, r1, r2, r3); }
vec4 bicubic_nodata(sampler2D tex, vec2 uv, vec2 res, float channel, float nodata) { vec2 tuv = uv * res - 0.5; vec2 f = fract(tuv); vec2 i = floor(tuv); vec4 p[4][4]; for (int y = -1; y <= 2; ++y) for (int x = -1; x <= 2; ++x) p[y + 1][x + 1] = SAM2(tex, x, y, res, i); if (p[1][1].r == nodata)p[1][1] = p[1][2]; if (p[1][1].r == nodata)p[1][1] = p[1][0]; if (p[1][1].r == nodata)p[1][1] = p[0][1]; if (p[1][1].r == nodata)p[1][1] = p[2][1]; if (p[1][1].r == nodata)p[1][1] = p[2][2]; for (int y = 0; y <= 3; ++y) for (int x = 0; x <= 3; ++x) if (p[y][x].r == nodata) {if (y == 1 && x == 1)continue; p[y][x] = p[1][1];} if (p[1][1].r == nodata)return vec4(0.0); vec4 r0 = spline(f.x, p[0][0], p[0][1], p[0][2], p[0][3]); vec4 r1 = spline(f.x, p[1][0], p[1][1], p[1][2], p[1][3]); vec4 r2 = spline(f.x, p[2][0], p[2][1], p[2][2], p[2][3]); vec4 r3 = spline(f.x, p[3][0], p[3][1], p[3][2], p[3][3]); return spline(f.y, r0, r1, r2, r3); }
vec4 biquadratic(sampler2D tex, vec2 uv, vec2 res) { float texSize = max(res.x, res.y); vec2 p = uv; vec2 f = fract(p * texSize); vec2 c = (f * (f - 1.0) + 0.5) * (1.0 / texSize); vec2 w0 = p - c; vec2 w1 = p + c; return (texture(tex, vec2(w0.x, w0.y)) + texture(tex, vec2(w0.x, w1.y)) + texture(tex, vec2(w1.x, w1.y)) + texture(tex, vec2(w1.x, w0.y))) * 0.25; }
vec4 bilinear(sampler2D tex, vec2 uv, vec2 res) { vec2 px = 1.0 / res; vec2 vc = floor(uv * res) * px; vec2 f = fract(uv * res); vec4 tl = texture(tex, vc); vec4 tr = texture(tex, vc + vec2(px.x, 0.0)); vec4 bl = texture(tex, vc + vec2(0.0, px.y)); vec4 br = texture(tex, vc + px); vec4 tx = mix(tl, tr, f.x); vec4 bx = mix(bl, br, f.x); return mix(tx, bx, f.y); }
vec4 bilinear_nodata(sampler2D tex, vec2 uv, vec2 res, float channel, float nodata) { vec2 px = 1.0 / res; vec2 vc = floor(uv * res) * px; vec2 f = fract(uv * res); vec4 tl = texture(tex, vc); vec4 tr = texture(tex, vc + vec2(px.x, 0.0)); vec4 bl = texture(tex, vc + vec2(0.0, px.y)); vec4 br = texture(tex, vc + px); if (tl.r == nodata)return vec4(0.0); if (tr.r == nodata)tr = tl; if (bl.r == nodata)bl = tl; if (br.r == nodata)br = (bl.r != nodata) ? bl : tr; vec4 tx = mix(tl, tr, f.x); vec4 bx = mix(bl, br, f.x); return mix(tx, bx, f.y); }
vec4 interpolatedTexel(int type, sampler2D tex, vec2 uv, vec2 res) { switch (type) { case 1: return bilinear(tex, uv, res); case 2: return bicubic(tex, uv, res); case 3: return biquadratic(tex, uv, res); default: return texture(tex, uv); } }
vec4 colorLookup(float value, float size, sampler2D ramp) { return texture(ramp, vec2(value + .001, 0.5)); } // Added the missing function body
vec4 interpolatedNoDataTexel(int type, sampler2D tex, vec2 uv, vec2 res, float nodata) { switch (type) { case 1: return bilinear_nodata(tex, uv, res, 0.0, nodata); case 2: return bicubic_nodata(tex, uv, res, 0.0, nodata); case 3: vec4 bq = biquadratic(tex, uv, res); return bq; default: vec4 nn = texture(tex, uv); if (nn.r == nodata)return vec4(0.0); return nn; } }
vec4 interpolatedScaledTexel(int type, sampler2D tex, vec2 uv, vec2 res, float scale) { return interpolatedTexel(type, tex, uv, res * scale); }
vec4 interpolatedScaledNoDataTexel(int type, sampler2D tex, vec2 uv, vec2 res, float scale, float nodata) { return interpolatedNoDataTexel(type, tex, uv, res * scale, nodata); }
float sampleColorValue(vec4 color, int channel) { float value; if (channel == 0)value = color.r; else if (channel == 1)value = color.g; else if (channel == 2)value = color.b; else if (channel == 3)value = color.a; else if (channel == 4)value = (color.r + color.g) * 0.5; else if (channel == 5)value = (color.g + color.b) * 0.5; else if (channel == 6)value = (color.r + color.b) * 0.5; else if (channel == 7)value = (color.r + color.a) * 0.5; else if (channel == 8)value = (color.g + color.a) * 0.5; else if (channel == 9)value = (color.b + color.a) * 0.5; else if (channel == 10)value = (color.r + color.g + color.b) * (1.0 / 3.0); else if (channel == 11)value = (color.g + color.b + color.a) * (1.0 / 3.0); else if (channel == 12)value = (color.r + color.g + color.b + color.a) * 0.25; else value = color.r; return clamp(value, 0.0, 1.0); }

#ifdef EXPRESSION_VECTOR
    #define NO_DATA
#endif

void main(void) {

    vec2 coord = gl_PointCoord - vec2(0.5); // Center coordinates at (0,0) [-0.5, 0.5]
    float distSquared = dot(coord, coord);  // Calculate distance squared from center
    if (distSquared >= 0.25) { // Radius squared is 0.5 * 0.5 = 0.25
        discard; // Discard fragments outside the circle radius EARLY
    }

    // --- Calculate finalColor (Keep your existing logic) ---
    vec2 lookupUV;
    if (x_scale < 1.0) {
        lookupUV = outPos.xy * vec2(x_scale, y_scale) + vec2(x_offset, y_offset);
    } else {
        lookupUV = outPos.xy;
    }

    #ifdef NO_DATA
    vec4 nodataTex = texture(u_Texture, lookupUV);
    float dataValueCheck = sampleColorValue(nodataTex, channel1);
    if (dataValueCheck == noData) {
        discard;
    }
    #endif

    float interpScale = 0.6; // Or 1.0
    vec4 texel1;
    #ifdef NO_DATA
        texel1 = interpolatedScaledNoDataTexel(interpolation, u_Texture, lookupUV, dataResolution, interpScale, noData);
    #else
        texel1 = interpolatedScaledTexel(interpolation, u_Texture, lookupUV, dataResolution, interpScale);
    #endif

    if (texel1 == vec4(0.0)) { discard; } // Handle potential NoData return

    vec4 finalColor;
    float valueU = sampleColorValue(texel1, channel1);
    float valueV = sampleColorValue(texel1, channel2);
    vec2 velocity = mix(vec2(dataRangeMin), vec2(dataRangeMax), vec2(valueU, valueV));
    float magnitude = length(velocity);
    float normalizedMagnitude = (abs(dataRangeMax) > 0.00001) ? (magnitude / dataRangeMax) : 0.0;
    float drawRange = max(0.00001, drawRangeMax - drawRangeMin);
    float drawValue = (normalizedMagnitude - drawRangeMin) / drawRange;
    drawValue = clamp(drawValue, 0.0, 1.0);
    float colorScaleRes = 256.0;
    finalColor = colorLookup(drawValue, colorScaleRes, colorScale);
    finalColor.a = alphaOut;

    fragColor = finalColor;
}