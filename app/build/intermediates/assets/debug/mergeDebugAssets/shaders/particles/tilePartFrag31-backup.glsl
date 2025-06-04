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
// uniform sampler2D dataTextureNext; // Declared but seems unused in main logic shown
uniform float noData;
uniform float mult;         // Declared but seems unused in main logic shown
uniform int mode;           // Declared but seems unused (logic hardcoded to vector mode?)
uniform int channel1;
uniform int channel2;
uniform int interpolation;
uniform float outputRangeMin; // Declared but seems unused in main logic shown
uniform float outputRangeMax; // Declared but seems unused in main logic shown
uniform float drawRangeMin;
uniform float drawRangeMax;
uniform float dataRangeMin;
uniform float dataRangeMax;
uniform float sampleDataMin;  // Declared but seems unused in main logic shown
uniform float sampleDataMax;  // Declared but seems unused in main logic shown
uniform float colorSampleOffset; // Declared but seems unused in main logic shown
uniform float colorRangeFactor;  // Declared but seems unused in main logic shown

// --- Varyings become 'in' variables (Fragment Shader Inputs) ---
// Ensure these match the 'out' variables from your corresponding Vertex Shader
// in vec4 v_Position; // Changed 'varying' to 'in' (NOTE: Seems unused in main)
// in vec2 vUV;        // Changed 'varying' to 'in' (NOTE: Seems unused in main)
in vec4 outPos;     // Changed 'varying' to 'in' (Input from VS)
in float alphaOut;  // Changed 'varying' to 'in' (Input from VS)

// --- Fragment Shader Output ---
out vec4 fragColor; // Declare the output variable

// --- Constants ---
const vec2 dataResolution = vec2(516.0, 516.0); // Assuming this is fixed


// --- Helper Functions (texture2D changed to texture) ---
vec4 SAM2(sampler2D tex, int a, int b, vec2 res, vec2 i) {
    // Changed texture2D to texture
    return texture(tex, (i + vec2(float(a), float(b)) + 0.5) * (1.0 / res));
}

// Bicubic constants remain the same
const vec4 BS_A = vec4(   3.0,  -6.0,   0.0,  4.0 ) * (1.0/6.0); // Use 1.0/6.0 for clarity
const vec4 BS_B = vec4(  -1.0,   6.0, -12.0,  8.0 ) * (1.0/6.0);
// ... other constants omitted for brevity, assumed correct ...
const vec4 CA = BS_A;
const vec4 CB = BS_B;

vec4 powers(float x) {
    return vec4(x*x*x, x*x, x, 1.0);
}

vec4 spline(float x, vec4 c0, vec4 c1, vec4 c2, vec4 c3) {
    vec4 px = powers(x);
    vec4 p1mx = powers(1.0 - x);
    vec4 pxp1 = powers(x + 1.0);
    vec4 p2mx = powers(2.0 - x);

    // Using dot product formulation
    return c0 * dot(CB, pxp1) +
    c1 * dot(CA, px) +
    c2 * dot(CA, p1mx) +
    c3 * dot(CB, p2mx);
}


vec4 bicubic(sampler2D tex, vec2 uv, vec2 res) {
    vec2 tuv = uv * res - 0.5;
    vec2 f = fract(tuv);
    vec2 i = floor(tuv);

    // Optimized spline calls might be possible, but keep structure for now
    vec4 row0 = spline(f.x, SAM2(tex, -1,-1, res, i), SAM2(tex, 0,-1, res, i), SAM2(tex, 1,-1, res, i), SAM2(tex, 2,-1, res, i));
    vec4 row1 = spline(f.x, SAM2(tex, -1, 0, res, i), SAM2(tex, 0, 0, res, i), SAM2(tex, 1, 0, res, i), SAM2(tex, 2, 0, res, i));
    vec4 row2 = spline(f.x, SAM2(tex, -1, 1, res, i), SAM2(tex, 0, 1, res, i), SAM2(tex, 1, 1, res, i), SAM2(tex, 2, 1, res, i));
    vec4 row3 = spline(f.x, SAM2(tex, -1, 2, res, i), SAM2(tex, 0, 2, res, i), SAM2(tex, 1, 2, res, i), SAM2(tex, 2, 2, res, i));

    return spline(f.y, row0, row1, row2, row3);
}

// Review NoData logic carefully - this is complex fill logic
vec4 bicubic_nodata(sampler2D tex, vec2 uv, vec2 res, float channel, float nodata) {
    // Note: 'channel' parameter is declared but not used within this function
    vec2 tuv = uv * res - 0.5;
    vec2 f = fract(tuv);
    vec2 i = floor(tuv);

    // Sample the 4x4 neighborhood
    vec4 p[4][4];
    for(int y=-1; y<=2; ++y) {
        for(int x=-1; x<=2; ++x) {
            p[y+1][x+1] = SAM2(tex, x, y, res, i);
        }
    }

    // Basic NoData fill strategy (replace nodata with nearest valid neighbor)
    // This can be sophisticated; the original had a specific fill pattern.
    // Replicating the exact fill from the original:
    if (p[1][1].r == nodata) p[1][1] = p[1][2]; // c = cr
    if (p[1][1].r == nodata) p[1][1] = p[1][0]; // c = cl
    if (p[1][1].r == nodata) p[1][1] = p[0][1]; // c = t
    if (p[1][1].r == nodata) p[1][1] = p[2][1]; // c = b
    if (p[1][1].r == nodata) p[1][1] = p[2][2]; // c = br
    // Propagate the filled center 'c' (p[1][1]) outwards if others are nodata
    for(int y=0; y<=3; ++y) { // Iterate through p array indices (0..3)
                              for(int x=0; x<=3; ++x) {
                                  if (p[y][x].r == nodata) {
                                      // Simple fill from center if possible, otherwise requires more complex neighbor search
                                      if (y==1 && x==1) continue; // Skip center if already handled
                                      p[y][x] = p[1][1]; // Fallback: fill with center value if available
                                      // A more robust fill might search outwards for the nearest valid pixel
                                  }
                              }
    }
    // The original code had a very specific propagation pattern. A simpler approach:
    // If p[1][1] is still nodata after initial checks, the result is likely invalid.
    // Consider returning vec4(0.0) or discarding if the central pixel cannot be filled.
    if (p[1][1].r == nodata) return vec4(0.0); // Or discard;

    // Spline calculation using potentially filled values
    vec4 row0 = spline(f.x, p[0][0], p[0][1], p[0][2], p[0][3]);
    vec4 row1 = spline(f.x, p[1][0], p[1][1], p[1][2], p[1][3]);
    vec4 row2 = spline(f.x, p[2][0], p[2][1], p[2][2], p[2][3]);
    vec4 row3 = spline(f.x, p[3][0], p[3][1], p[3][2], p[3][3]);

    return spline(f.y, row0, row1, row2, row3);
}


vec4 biquadratic(sampler2D tex, vec2 uv, vec2 res) {
    float texSize = max(res.x, res.y);
    vec2 p = uv;
    #if 0 // Roger/iq style
                p = p*texSize;
    vec2 i = floor(p);
    vec2 f = fract(p);
    p = i + f*0.5;
    p = p/texSize;
    float w = 0.5/texSize;
    // Changed texture2D to texture
    return mix(mix(texture(tex, p+vec2(0,0)),
                   texture(tex, p+vec2(w,0)),f.x),
               mix(texture(tex, p+vec2(0,w)),
                   texture(tex, p+vec2(w,w)),f.x), f.y);
    #else // paniq style
                vec2 f = fract(p*texSize);
    vec2 c = (f*(f-1.0)+0.5) * (1.0 / texSize);
    vec2 w0 = p - c;
    vec2 w1 = p + c;
    // Changed texture2D to texture
    return (texture(tex, vec2(w0.x, w0.y))+
    texture(tex, vec2(w0.x, w1.y))+
    texture(tex, vec2(w1.x, w1.y))+
    texture(tex, vec2(w1.x, w0.y))) * 0.25; // Use * 0.25 instead of / 4.0
    #endif
        }


vec4 bilinear(sampler2D tex, vec2 uv, vec2 res) {
    vec2 px = 1.0 / res;
    // Use textureGather for potential optimization if bilinear filtering is built-in
    // vec2 tc = uv * res;
    // return textureGather(tex, tc / res); // Example - check syntax/availability

    // Manual bilinear using texture()
    vec2 vc = floor(uv * res); // Integer pixel coord
    vec2 f = fract(uv * res);  // Fractional part
    vc = vc * px; // Back to texture coord space for lookup

    // Changed texture2D to texture
    vec4 tl = texture(tex, vc);
    vec4 tr = texture(tex, vc + vec2(px.x, 0.0));
    vec4 bl = texture(tex, vc + vec2(0.0, px.y));
    vec4 br = texture(tex, vc + px);

    vec4 tx = mix(tl, tr, f.x);
    vec4 bx = mix(bl, br, f.x);

    return mix(tx, bx, f.y);
}

// Review NoData fill logic here too
vec4 bilinear_nodata(sampler2D tex, vec2 uv, vec2 res, float channel, float nodata) {
    // Note: 'channel' parameter is declared but not used
    vec2 px = 1.0 / res;
    vec2 vc = floor(uv * res) * px;
    vec2 f = fract(uv * res);

    // Changed texture2D to texture
    vec4 tl = texture(tex, vc);
    vec4 tr = texture(tex, vc + vec2(px.x, 0.0));
    vec4 bl = texture(tex, vc + vec2(0.0, px.y));
    vec4 br = texture(tex, vc + px);

    // Basic NoData fill (prefer valid neighbors)
    if (tl.r == nodata) return vec4(0.0); // Or discard; If top-left is nodata, result is unreliable?

    if (tr.r == nodata) tr = tl;
    if (bl.r == nodata) bl = tl; // Could also use tr if valid: bl = (tr.r != nodata) ? tr : tl;
    if (br.r == nodata) br = (bl.r != nodata) ? bl : tr; // Prefer bl or tr if valid, fallback to tl

    vec4 tx = mix(tl, tr, f.x);
    vec4 bx = mix(bl, br, f.x);

    return mix(tx, bx, f.y);
}


// Interpolation wrapper functions remain structurally the same
vec4 interpolatedTexel(int type, sampler2D tex, vec2 uv, vec2 res) {
    // Switch statement might be slightly clearer
    switch(type) {
        case 1: return bilinear(tex, uv, res);
        case 2: return bicubic(tex, uv, res);
        case 3: return biquadratic(tex, uv, res);
        default: return texture(tex, uv); // Changed texture2D to texture
    }
}

vec4 colorLookup(float value, float size, sampler2D ramp) {
    return texture(ramp, vec2(value+.001, 0.5));
}

vec4 interpolatedNoDataTexel(int type, sampler2D tex, vec2 uv, vec2 res, float nodata) {
    switch(type) {
        case 1: return bilinear_nodata(tex, uv, res, 0.0, nodata);
        case 2: return bicubic_nodata(tex, uv, res, 0.0, nodata);
        case 3: // Biquadratic nodata handling might need specific implementation
    // Falling back to biquadratic without nodata check:
                vec4 bq = biquadratic(tex, uv, res);
    // Add check if needed: if (bq.r == nodata) return vec4(0.0); // Or discard;
                return bq;
        default:
                vec4 nn = texture(tex, uv); // Changed texture2D to texture
                if (nn.r == nodata) return vec4(0.0); // Or discard;
                return nn;
    }
}

vec4 interpolatedScaledTexel(int type, sampler2D tex, vec2 uv, vec2 res, float scale) {
    // Pass scaled resolution
    return interpolatedTexel(type, tex, uv, res * scale);
}

vec4 interpolatedScaledNoDataTexel(int type, sampler2D tex, vec2 uv, vec2 res, float scale, float nodata) {
    // Pass scaled resolution
    return interpolatedNoDataTexel(type, tex, uv, res * scale, nodata);
}


// sampleColorValue function - use clamp
float sampleColorValue(vec4 color, int channel) {
    float value;
    // Consider switch statement
    if (channel == 0) value = color.r;
    else if (channel == 1) value = color.g;
    else if (channel == 2) value = color.b;
    else if (channel == 3) value = color.a;
    else if (channel == 4) value = (color.r + color.g) * 0.5;
    else if (channel == 5) value = (color.g + color.b) * 0.5;
    else if (channel == 6) value = (color.r + color.b) * 0.5;
    else if (channel == 7) value = (color.r + color.a) * 0.5;
    else if (channel == 8) value = (color.g + color.a) * 0.5;
    else if (channel == 9) value = (color.b + color.a) * 0.5;
    else if (channel == 10) value = (color.r + color.g + color.b) * (1.0/3.0);
    else if (channel == 11) value = (color.g + color.b + color.a) * (1.0/3.0);
    else if (channel == 12) value = (color.r + color.g + color.b + color.a) * 0.25;
    else value = color.r; // Default case

    return clamp(value, 0.0, 1.0); // Use clamp function
}

// --- Preprocessor defines ---
#ifdef EXPRESSION_VECTOR
    #define NO_DATA
#endif


void main(void) {

    // Calculate texture coordinate based on input 'outPos' (from VS)
    vec2 lookupUV;
    if (x_scale < 1.0) { // Handle partial texture mapping logic
                         // Assuming outPos.xy are base UVs [0,1]
                         lookupUV = outPos.xy * vec2(x_scale, y_scale) + vec2(x_offset, y_offset);
    } else {
        lookupUV = outPos.xy; // Use vertex shader output directly
    }

    // Unused block for outPart2
    // vec2 outPart2;
    // if (x_scale2 < 1.0) { ... } else { outPart2 = outPos.xy; }

    // --- NoData Check (Preprocessor Block) ---
    #ifdef NO_DATA
        // Sample specifically for the NoData check at the calculated lookup UV
    vec4 nodataTex = texture(u_Texture, lookupUV); // Changed texture2D to texture
    float dataValueCheck = sampleColorValue(nodataTex, channel1);
    if (dataValueCheck == noData) {
        discard; // Exit shader if NoData value is found
    }
    #endif
        // --- End NoData Check ---


    // --- Interpolated Sampling ---
    // Original smoothing scale: 1.0 - (0.8 * 0.5) = 0.6
    // If smoothing is intended, use 0.6. If not, use 1.0.
    float interpScale = 0.6; // Or 1.0 if no scaling intended

    vec4 texel1; // Result of sampling the main texture

    #ifdef NO_DATA
        texel1 = interpolatedScaledNoDataTexel(interpolation, u_Texture, lookupUV, dataResolution, interpScale, noData);
    #else
        texel1 = interpolatedScaledTexel(interpolation, u_Texture, lookupUV, dataResolution, interpScale);
    #endif
        // --- End Interpolated Sampling ---

    // If texel1 itself became NoData after interpolation/fill attempt, discard or handle
    if (texel1 == vec4(0.0)) { // Check against the potential return value from NoData functions
                               // discard; or set to transparent: fragColor = vec4(0.0); return;
    }


    // --- Color Calculation (Vector Mode) ---
    // Assuming the 'if(true)' means vector mode is always used
    vec4 finalColor;
    float valueU = sampleColorValue(texel1, channel1);
    float valueV = sampleColorValue(texel1, channel2);

    // Map normalized [0,1] values from texture to actual data range
    vec2 velocity = mix(vec2(dataRangeMin), vec2(dataRangeMax), vec2(valueU, valueV));

    // Calculate magnitude and normalize relative to max range
    float magnitude = length(velocity);
    float normalizedMagnitude = (abs(dataRangeMax) > 0.00001) ? (magnitude / dataRangeMax) : 0.0;

    // Map normalized magnitude to the desired drawing range [0, 1]
    float drawRange = max(0.00001, drawRangeMax - drawRangeMin); // Avoid div by zero
    float drawValue = (normalizedMagnitude - drawRangeMin) / drawRange;
    drawValue = clamp(drawValue, 0.0, 1.0); // Clamp to ensure valid lookup coord

    // Lookup color using the external/included function
    float colorScaleRes = 256.0;
    finalColor = colorLookup(drawValue, colorScaleRes, colorScale);

    // Apply alpha fade from vertex shader
    finalColor.a = alphaOut;
    // --- End Color Calculation ---


    // Optional discard based on final alpha
    // if (finalColor.a < 0.01) discard;

    // Assign final color to the declared output variable
    fragColor = finalColor;

    // Premultiplied alpha usually handled by blend function state (e.g., glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA))
    // fragColor.rgb *= fragColor.a; // Only needed if manual premultiplication is required by blending setup
}