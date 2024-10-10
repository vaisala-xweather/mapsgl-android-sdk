precision highp float;

uniform int interpolation;
uniform float smoothing;
uniform vec2 resolution;
uniform float dataChannel[2];
uniform vec2 dataResolution;
uniform vec4 dataCoordOffset;
uniform mat4 dataMatrix;
uniform float dataMeld;
uniform vec2 dataMin;
uniform vec2 dataMax;
uniform vec2 dataRange;
uniform vec2 drawRange; 
uniform vec2 outputRange;
uniform float noData;
uniform float colorScaleRes;

uniform bool initialize;
uniform float speedFactor;
uniform float fadeInIncrement;
uniform float randomSeed;
uniform float dropRate;
uniform float dropRateBump;
uniform int positionOutCoord;

uniform sampler2D dataTexture;
uniform sampler2D dataTextureNext;
uniform sampler2D tPositionX;
uniform sampler2D tPositionY;
uniform sampler2D tAlpha;

varying vec2 vUv;


//#include <texture_lookup_3x3>
    uniform sampler2D tDataTL;
    uniform sampler2D tDataTC;
    uniform sampler2D tDataTR;
    uniform sampler2D tDataML;
    uniform sampler2D tDataMC;
    uniform sampler2D tDataMR;
    uniform sampler2D tDataBL;
    uniform sampler2D tDataBC;
    uniform sampler2D tDataBR;

    vec4 textureLookup(const vec2 uv) {
        if (uv.x > 1.0 && uv.y > 1.0) {
            return texture2D(tDataBR, uv - vec2(1.0, 1.0));
        } else if (uv.x > 0.0 && uv.y > 1.0) {
            return texture2D(tDataBC, uv - vec2(0.0, 1.0));
        } else if (uv.y > 1.0) {
            return texture2D(tDataBL, uv - vec2(-1.0, 1.0));
        } else if (uv.x > 1.0 && uv.y > 0.0) {
            return texture2D(tDataMR, uv - vec2(1.0, 0.0));
        } else if (uv.x > 0.0 && uv.y > 0.0) {
            return texture2D(tDataMC, uv - vec2(0.0, 0.0));
        } else if (uv.y > 0.0) {
            return texture2D(tDataML, uv - vec2(-1.0, 0.0));
        } else if (uv.x > 1.0) {
            return texture2D(tDataTR, uv - vec2(1.0, -1.0));
        } else if (uv.x > 0.0) {
            return texture2D(tDataTC, uv - vec2(0.0, -1.0));
        } else {
            return texture2D(tDataTL, uv - vec2(-1.0, -1.0));
        }
    }
//end #include <texture_lookup_3x3>

//#include <color_lookup>
    vec4 colorLookup(float value, float size, sampler2D ramp) {
        // vec2 pos = vec2(fract(size * value), floor(size * value) / size);
        // snap position to nearest pixel to avoid artifacts if value happens to fall at or
        // close to pixel boundaries
        float x = value;
        float s = 1.0 / size;
        // x = floor((value / s) + 0.5) * s;
        x = value+0.001;

        return texture2D(ramp, vec2(x, 0.5));
    }
//end #include <color_lookup>

//#include <interpolated_texel>
    //#include <operators>
            // https://theorangeduck.com/page/avoiding-shader-conditionals
            vec4 op_when_eq(vec4 x, vec4 y) {
                return 1. - abs(sign(x - y));
            }

            float op_when_eq_f(float x, float y) {
                return 1. - abs(sign(x - y));
            }

            vec4 op_when_neq(vec4 x, vec4 y) {
                return abs(sign(x - y));
            }

            float op_when_neq_f(float x, float y) {
                return abs(sign(x - y));
            }

            vec4 op_when_gt(vec4 x, vec4 y) {
                return max(sign(x - y), 0.);
            }

            float op_when_gt_f(float x, float y) {
                return max(sign(x - y), 0.);
            }

            vec4 op_when_lt(vec4 x, vec4 y) {
                return max(sign(y - x), 0.);
            }

            vec4 op_when_ge(vec4 x, vec4 y) {
                return 1. - op_when_lt(x, y);
            }

            vec4 op_when_le(vec4 x, vec4 y) {
                return 1. - op_when_gt(x, y);
            }

            vec4 op_and(vec4 a, vec4 b) {
                return a * b;
            }

            // vec4 op_or(vec4 a, vec4 b) {
            //     return min(a + b, 1.0);
            // }

            // vec4 op_xor(vec4 a, vec4 b) {
            //     return (a + b) % 2.0;
            // }

            vec4 op_not(vec4 a) {
                return 1.0 - a;
            }
    //end #include <operators>
    //#include <sample_color_value>
        struct ValueRange {
            float min;
            float max;
        };

        /**
             * 0 = R
             * 1 = G
             * 2 = B
             * 3 = A
             * 4 = R+G
             * 5 = G+B
             * 6 = R+B
             * 7 = R+A
             * 8 = G+A
             * 9 = B+A
             * 10 = R+G+B
             * 11 = G+B+A
             * 12 = R+G+B+A
             */
        float sampleColorValue(vec4 color, float channel) {
            float value = color.r;

            if (channel == 1.0) {
                value = color.g;
            } else if (channel == 2.0) {
                value = color.b;
            } else if (channel == 3.0) {
                value = color.a;
            } else if (channel == 4.0) {
                value = (color.r + color.g) / 2.0;
            } else if (channel == 5.0) {
                value = (color.g + color.b) / 2.0;
            } else if (channel == 6.0) {
                value = (color.r + color.b) / 2.0;
            } else if (channel == 7.0) {
                value = (color.r + color.a) / 2.0;
            } else if (channel == 8.0) {
                value = (color.g + color.a) / 2.0;
            } else if (channel == 9.0) {
                value = (color.b + color.a) / 2.0;
            } else if (channel == 10.0) {
                value = (color.r + color.g + color.b) / 3.0;
            } else if (channel == 11.0) {
                value = (color.g + color.b + color.a) / 3.0;
            } else if (channel == 12.0) {
                value = (color.r + color.g + color.b + color.a) / 4.0;
            }

            value = min(1.0, max(0.0, value));

            return value;
        }

        float sampleDataValue(ValueRange dataRange, float p) {
            return dataRange.min + (dataRange.max - dataRange.min) * p;
        }
    //end #include <sample_color_value>

    float isEdge(sampler2D tex, vec2 uv, vec2 res, float nodata, float channel) {
        vec2 px = 1.0 / res;
        vec2 vc = (floor(uv * res)) * px;

        vec4 t = texture2D(tex, vc + vec2(0., -px.y));
        vec4 b = texture2D(tex, vc + vec2(0., px.y));
        vec4 l = texture2D(tex, vc + vec2(-px.x, 0.));
        vec4 r = texture2D(tex, vc + vec2(px.x, 0.));

        float a = op_when_eq_f(nodata, sampleColorValue(t, channel));
        a += op_when_eq_f(nodata, sampleColorValue(b, channel));
        a += op_when_eq_f(nodata, sampleColorValue(l, channel));
        a += op_when_eq_f(nodata, sampleColorValue(r, channel));

        // return 1.0 * abs(sign(4. - a));
        // return a * 0.2;
        // return when_gt(0., a);
        return a > 0. && a < 4. ? 1. : 0.;
        // return a > 0. ? 1. : 0.;
    }

    //#include <interpolate_bilinear>
    //#include <interpolate_bicubic>
    //#include <interpolate_biquadratic>
        // https://www.shadertoy.com/view/wtXXDl
        vec4 biquadratic(sampler2D tex, vec2 uv, vec2 res) {
            // vec2 q = fract(uv * res);
            // vec2 c = (q*(q - 1.0) + 0.5) / res;
            // vec2 w0 = uv - c;
            // vec2 w1 = uv + c;
            // vec4 s = textureLookup(vec2(w0.x, w0.y))
            //    + textureLookup(vec2(w0.x, w1.y))
            //    + textureLookup(vec2(w1.x, w1.y))
            //    + textureLookup(vec2(w1.x, w0.y));
            // return s / 4.0;

            float texSize = max(res.x, res.y);
            vec2 p = uv;

            #if 0
                //Roger/iq style
            p = p*texSize;
            vec2 i = floor(p);
            vec2 f = fract(p);
            p = i + f*0.5;
            p = p/texSize;
            //f = f*f*(3.0-2.0*f); // optional for extra sweet
            float w = 0.5/texSize;
            return mix(mix(texture2D(tex, p+vec2(0,0)),
                           texture2D(tex, p+vec2(w,0)),f.x),
                       mix(texture2D(tex, p+vec2(0,w)),
                           texture2D(tex, p+vec2(w,w)),f.x), f.y);

            #else
                // paniq style (https://www.shadertoy.com/view/wtXXDl)
            vec2 f = fract(p*texSize);
            vec2 c = (f*(f-1.0)+0.5) / texSize;
            vec2 w0 = p - c;
            vec2 w1 = p + c;
            return (texture2D(tex, vec2(w0.x, w0.y))+
            texture2D(tex, vec2(w0.x, w1.y))+
            texture2D(tex, vec2(w1.x, w1.y))+
            texture2D(tex, vec2(w1.x, w0.y)))/4.0;
            #endif
            }

    //end #include <interpolate_biquadratic>
    vec4 interpolatedTexel(int type, sampler2D tex, vec2 uv, vec2 res) {
        vec4 texel;

        if (type == 1) {
            texel = bilinear(tex, uv, res);
        } else if (type == 2) {
            texel = bicubic(tex, uv, res);
        } else if (type == 3) {
            texel = biquadratic(tex, uv, res);
        } else {
            texel = texture2D(tex, uv);
        }

        return texel;
    }

    vec4 interpolatedNoDataTexel(int type, sampler2D tex, vec2 uv, vec2 res, float nodata) {
        vec4 texel;

        if (type == 1) {
            texel = bilinear_nodata(tex, uv, res, 0., nodata);
        } else if (type == 2) {
            texel = bicubic_nodata(tex, uv, res, 0., nodata);
        } else if (type == 3) {
            texel = biquadratic(tex, uv, res);
        } else {
            texel = texture2D(tex, uv);
        }

        return texel;
    }

    vec4 interpolatedScaledTexel(int type, sampler2D tex, vec2 uv, vec2 res, float scale) {
        return interpolatedTexel(type, tex, uv, res * scale);
    }

    vec4 interpolatedScaledNoDataTexel(int type, sampler2D tex, vec2 uv, vec2 res, float scale, float nodata) {
        return interpolatedNoDataTexel(type, tex, uv, res * scale, nodata);
    }
//end #include <interpolated_texel>

//#include <decode_float_rgba>
    highp float decodeFloatRGBA(vec4 v) {
        float a = floor(v.r * 255.0 + 0.5);
        float b = floor(v.g * 255.0 + 0.5);
        float c = floor(v.b * 255.0 + 0.5);
        float d = floor(v.a * 255.0 + 0.5);

        float exponent = a - 127.0;
        float sign = 1.0 - mod(d, 2.0)*2.0;
        float mantissa = float(a > 0.0)
        + b / 256.0
        + c / 65536.0
        + floor(d / 2.0) / 8388608.0;

        return sign * mantissa * exp2(exponent);
    }
//end #include <decode_float_rgba>

//#include <encode_float_rgba>
    vec4 encodeFloatRGBA(highp float val) {
        if (val == 0.0) {
            return vec4(0.0, 0.0, 0.0, 0.0);
        }

        float mag = abs(val);
        float exponent = floor(log2(mag));
        // Correct log2 approximation errors.
        exponent += float(exp2(exponent) <= mag / 2.0);
        exponent -= float(exp2(exponent) > mag);

        float mantissa;
        if (exponent > 100.0) {
            // Not sure why this needs to be done in two steps for the largest float to work.
            // Best guess is the optimizer rewriting '/ exp2(e)' into '* exp2(-e)',
            // but exp2(-128.0) is too small to represent.
            mantissa = mag / 1024.0 / exp2(exponent - 10.0) - 1.0;
        } else {
            mantissa = mag / float(exp2(exponent)) - 1.0;
        }

        float a = exponent + 127.0;
        mantissa *= 256.0;

        float b = floor(mantissa);
        mantissa -= b;
        mantissa *= 256.0;

        float c = floor(mantissa);
        mantissa -= c;
        mantissa *= 128.0;

        float d = floor(mantissa) * 2.0 + float(val < 0.0);

        return vec4(a, b, c, d) / 255.0;
    }
//end #include <encode_float_rgba>

// pseudo-random generator
float rand(const vec2 co) {
    float t = dot(vec2(12.9898, 78.233), co);
    return fract(sin(t) * (4375.85453 + t));
}

vec2 transformData(vec2 pos, mat4 matrix) {
    vec4 transformed = matrix * vec4(pos.xy, 1.0, 1.0);
    return transformed.xy / transformed.w;
}

const float PI = 3.14159265359;

// update the position of a particle
vec4 update(vec4 data) {
    vec2 pos = data.xy;
    float alpha = data.z;
    float rotation = data.w;

    vec2 uv = pos;
    uv = transformData(uv, dataMatrix);

    // set UV range to account for portion of texture we're rendering
    uv = vec2(
        dataCoordOffset.x + (uv.x * dataCoordOffset.z),
        dataCoordOffset.y + (uv.y * dataCoordOffset.w)
    );

    vec2 tuv = uv;
    vec2 tuv2 = uv;

    // lookup the velocity from the data texture at this particle position
    //#include <get_texel>
        float scale = 1.0 - (0.8 * smoothing);
        #ifdef NODATA
        vec4 texel1 = interpolatedScaledNoDataTexel(interpolation, dataTexture, tuv, dataResolution, scale, noData);
        vec4 texel2 = interpolatedScaledNoDataTexel(interpolation, dataTextureNext, tuv2, dataResolution, scale, noData);
        #else
        vec4 texel1 = interpolatedScaledTexel(interpolation, dataTexture, tuv, dataResolution, scale);
        vec4 texel2 = interpolatedScaledTexel(interpolation, dataTextureNext, tuv2, dataResolution, scale);
        #endif
        vec4 texel = mix(texel1, texel2, dataMeld);
    //end #include <get_texel>

    //#include <get_data_value>
            float value;
            vec2 velocity = vec2(0.);
            float angle = 0.;

            #ifdef EXPRESSION_VECTOR
            // determine velocity from vectors
            float valueU = sampleColorValue(texel, dataChannel[0]);
            float valueV = sampleColorValue(texel, dataChannel[1]);
            vec2 vector = vec2(valueU, valueV);
            velocity = mix(vec2(dataRange.x), vec2(dataRange.y), vector);
            value = length(velocity) / dataRange.y;

            #ifdef IS_SYMBOL
            // Symbols need to be flipped in the y direction so they point in the right direction
            angle = atan(velocity.x, velocity.y);
            #else
            angle = atan(velocity.x, -velocity.y);
            #endif
            // Rotate -90 degrees so that 0 degrees is at the top (north)
            angle -= PI / 2.0;

            if (angle < 0.) angle += PI * 2.;

            #else
            #ifdef EXPRESSION_ANGLE
                float deg = sampleColorValue(texel, dataChannel[0]) * 360. - 180.0;
            if (deg < 0.) deg += 360.;

            // Calculate the velocity vector from the wind direction in degrees
            // Since we don't have speed data, this just assumes a constant speed of 1 for the vector
            // https://www.eol.ucar.edu/content/wind-direction-quick-reference
            float rad = deg * (PI / 180.);
            float valueU = sin(rad);
            float valueV = cos(rad);
            vec2 vector = vec2(valueU, valueV);
            velocity = vector;

            // Set a static speed value since we don't have speed data
            value = 0.1;

            #ifdef IS_SYMBOL
                    // Symbols need to be flipped in the y direction so they point in the right direction
            angle = atan(velocity.x, velocity.y);
            #else
                    angle = atan(velocity.x, -velocity.y);
            #endif
                angle -= PI / 2.0;

            if (angle < 0.) angle += PI * 2.;
            #else
                value = sampleColorValue(texel, dataChannel[0]);
            #endif
        #endif

        #ifdef EXPRESSION_SUM
            float value2 = sampleColorValue(texel, dataChannel[1]);
            float delta = (value + value2) * colorRangeFactor;
            value = delta + colorSampleOffset;
            #endif
        #ifdef EXPRESSION_DIFF
            float value2 = sampleColorValue(texel, dataChannel[1]);
            float delta = (value - value2) * colorRangeFactor;
            value = delta + colorSampleOffset;
            #endif

        #ifdef NODATA
            // scale value to 0...1 range for color LUT to account for noData
            vec2 mapFrom = outputRange;
            vec2 mapTo = vec2(0., 1.);
            // value = (value - mapFrom.x) * (mapTo.y - mapTo.x) / (mapFrom.y - mapFrom.x) + mapTo.x;
            #endif
    //end #include <get_data_value>

    float speed = value;
    rotation = angle;

    // take EPSG:4236 distortion into account for calculating where the particle moved
    // float distortion = cos(radians(pos.y * 180.0 - 90.0));

    // move particle relative to the data's velocity and direction
    // vec2 offset = vec2(velocity.x / distortion, -velocity.y) * 0.0001 * speedFactor;
    vec2 offset = vec2(velocity.x, -velocity.y) * 0.0001 * speedFactor;

    // update particle position, wrapping around tile edges
    // pos = fract(1.0 + pos + offset);
    pos += offset;

    // a random seed to use for the particle drop
    vec2 seed = (pos + vUv) * randomSeed;

    // drop rate is the chance a particle will restart at a random position to avoid degeneration
    float rate = dropRate + speed * dropRateBump;

    // Using the position to determine the drop rate fixes the issue of particles often collecting near tile edges initially
    rate += smoothstep(0.24, 0.5, length(pos - vec2(0.5, 0.5)) * 0.7);

    float drop = step(1.0 - rate, rand(seed));

    // Scale the new position by half and move to the center of the tile to avoid particles getting stuck on the 
    // edges. Doing so also requires the tile mesh containing the particles to be scaled by 2 and origin 
    // translated by -0.5 (via dataMatrix).
    vec2 randomPos = vec2(
        0.5 * rand(seed + 1.3) + 0.25,
        0.5 * rand(seed + 2.1) + 0.25
    );

    // Fade in newly placed particles
    alpha += fadeInIncrement;
    alpha = mix(min(alpha, 1.), 0., drop);

    // New position is either the updated position or a new random position if dropped
    vec2 nextPos = mix(pos, randomPos, drop);

    return vec4(nextPos, alpha, rotation);
}

void main() {
    vec4 state = texture2D(tAlpha, vUv);
    vec4 data = vec4(
        decodeFloatRGBA(texture2D(tPositionX, vUv)),
        decodeFloatRGBA(texture2D(tPositionY, vUv)),
        state.r,
        state.g
    );

    // move the particle position
    data = update(data);
    if (initialize) {
        for (int i = 0; i < 100; i++) {
            data = update(data);
        }
    }

    if (positionOutCoord == 0) {
        gl_FragColor = vec4(encodeFloatRGBA(data.x));
    } else if (positionOutCoord == 1) {
        gl_FragColor = vec4(encodeFloatRGBA(data.y));
    } else if (positionOutCoord == 2) {
        gl_FragColor = vec4(data.z, data.w / (PI * 2.), 0., 1.);
    }
}