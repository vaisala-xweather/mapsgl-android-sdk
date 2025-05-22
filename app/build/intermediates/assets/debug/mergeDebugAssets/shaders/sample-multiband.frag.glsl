precision highp float; //from WebGL
uniform float opacity;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform float noData;
uniform float mult;
uniform float add;
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
uniform float dataMeld;
uniform sampler2D dataTexture;
uniform sampler2D dataTextureNext;
uniform sampler2D colorScale;
uniform sampler2D colorScale1;
uniform sampler2D colorScale2;
varying vec2 vUV;

float smoothing = .5; //.4
float colorScaleRes = 256.0;

#include <color_lookup>

//sample_color_value

struct ValueRange {
    float min;
    float max;
};

/**  0 = R  1 = G  2 = B  3 = A
 * 4 = R+G  5 = G+B  6 = R+B  7 = R+A 8 = G+A 9 = B+A
 * 10 = R+G+B   11 = G+B+A  12 = R+G+B+A  */
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

    return clamp(value, 0.0, 1.0);
}

float sampleDataValue(ValueRange dataRange, float p) {
    return dataRange.min + (dataRange.max - dataRange.min) * p;
}
//end sample_color_value


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

vec4 op_not(vec4 a) {
    return 1.0 - a;
}
//END #include <operators>

//#include <sample_color_value>
float sampleColorValue(vec4 color, int channel) {
    float value;
    if (channel == 0)
        value = color.r; //add case for 0=r as it is a common case.
    if (channel == 1) {
        value = color.g;
    } else if (channel == 2) {
        value = color.b;
    } else if (channel == 3) {
        value = color.a;
    } else if (channel == 4) {
        value = (color.r + color.g) * .5;
    } else if (channel == 5) {
        value = (color.g + color.b) * .5;
    } else if (channel == 6) {
        value = (color.r + color.b) * .5;
    } else if (channel == 7) {
        value = (color.r + color.a) * .5;
    } else if (channel == 8) {
        value = (color.g + color.a) * .5;
    } else if (channel == 9) {
        value = (color.b + color.a) * .5;
    } else if (channel == 10) {
        value = (color.r + color.g + color.b) * .33333;
    } else if (channel == 11) {
        value = (color.g + color.b + color.a) * .33333;
    } else if (channel == 12) {
        value = (color.r + color.g + color.b + color.a) * .25;
    }

    value = min(1.0, max(0.0, value)); //Nicks code
    //value = clamp(value, 0.0, 1.0); //Test this when all else is working

    return value;
}

float sampleDataValue(vec2 dataRange, float p) {  //converts 0 to 1 value to value in range(0 to 1 -> min to max )
                                                  //discard;
                                                  return dataRange.x + (dataRange.y - dataRange.x) * p;
}
//END #include <sample_color_value>

//#include <interpolate_bilinear>
// texture filtering methods: https://www.shadertoy.com/view/MllSzX
vec4 bilinear(sampler2D tex, vec2 uv, vec2 res) {
    // texel size
    vec2 px = 1.0 / res;

    // interpolation factor for x and y direction
    vec2 vc = (floor(uv * res)) * px;
    vec2 f = fract(uv * res);

    // the corners
    vec4 tl = texture2D(tex, vc);
    vec4 tr = texture2D(tex, vc + vec2(px.x, 0));
    vec4 bl = texture2D(tex, vc + vec2(0, px.y));
    vec4 br = texture2D(tex, vc + px);

    // interpolate top and bottom rows in x direction, then interpolate in y direction
    vec4 tx = mix(tl, tr, f.x);
    vec4 bx = mix(bl, br, f.x);

    return mix(tx, bx, f.y);
}

vec4 bilinear_nodata(sampler2D tex, vec2 uv, vec2 res, float channel, float nodata) {
    // texel size
    vec2 px = 1.0 / res;

    // interpolation factor for x and y direction
    vec2 vc = (floor(uv * res)) * px;
    vec2 f = fract(uv * res);

    // the corners
    vec4 tl = texture2D(tex, vc);
    vec4 tr = texture2D(tex, vc + vec2(px.x, 0));
    vec4 bl = texture2D(tex, vc + vec2(0, px.y));
    vec4 br = texture2D(tex, vc + px);

    if (tr.r == nodata) tr = tl;
    if (bl.r == nodata) bl = tl;
    if (br.r == nodata) br = tl;

    // interpolate top and bottom rows in x direction, then interpolate in y direction
    vec4 tx = mix(tl, tr, f.x);
    vec4 bx = mix(bl, br, f.x);

    return mix(tx, bx, f.y);
}
//END #include <interpolate_bilinear>

//#include <interpolate_bicubic>
// [iq: https://www.shadertoy.com/view/XsSXDy]
const vec4 BS_A = vec4(3.0, -6.0, 0.0, 4.0) * .166666;
const vec4 BS_B = vec4(-1.0, 6.0, -12.0, 8.0) * .166666;
const vec4 RE_A = vec4(21.0, -36.0, 0.0, 16.0) * .055555;
const vec4 RE_B = vec4(-7.0, 36.0, -60.0, 32.0) * .055555;
const vec4 CR_A = vec4(3.0, -5.0, 0.0, 2.0) * .5;
const vec4 CR_B = vec4(-1.0, 5.0, -8.0, 4.0) * .5;

const vec4 CA = BS_A;
const vec4 CB = BS_B;

vec4 powers(float x) {
    return vec4(x * x * x, x * x, x, 1.0);
}

vec4 spline(float x, vec4 c0, vec4 c1, vec4 c2, vec4 c3) {
    return c0 * dot(CB, powers(x + 1.)) +
    c1 * dot(CA, powers(x)) +
    c2 * dot(CA, powers(1. - x)) +
    c3 * dot(CB, powers(2. - x));
}

// #define SAM2(a, b) texture((i + vec2(a,b) + 0.5) / res, -99.0)

vec4 SAM2(sampler2D tex, int a, int b, vec2 res, vec2 i) {
    return texture2D(tex, (i + vec2(a, b) + 0.5) / res);
}

vec4 bicubic(sampler2D tex, vec2 uv, vec2 res) {
    vec2 tuv = uv * res - 0.5;
    vec2 f = fract(tuv);
    vec2 i = floor(tuv);

    return spline(f.y,
                  spline(f.x, SAM2(tex, -1, -1, res, i), SAM2(tex, 0, -1, res, i), SAM2(tex, 1, -1, res, i), SAM2(tex, 2, -1, res, i)),
                  spline(f.x, SAM2(tex, -1, 0, res, i), SAM2(tex, 0, 0, res, i), SAM2(tex, 1, 0, res, i), SAM2(tex, 2, 0, res, i)),
                  spline(f.x, SAM2(tex, -1, 1, res, i), SAM2(tex, 0, 1, res, i), SAM2(tex, 1, 1, res, i), SAM2(tex, 2, 1, res, i)),
                  spline(f.x, SAM2(tex, -1, 2, res, i), SAM2(tex, 0, 2, res, i), SAM2(tex, 1, 2, res, i), SAM2(tex, 2, 2, res, i))
    );
}

vec4 bicubic_nodata(sampler2D tex, vec2 uv, vec2 res, float channel, float nodata) {
    vec2 tuv = uv * res - 0.5;
    vec2 f = fract(tuv);
    vec2 i = floor(tuv);

    vec4 tl = SAM2(tex, -1, -1, res, i);
    vec4 t = SAM2(tex, 0, -1, res, i);
    vec4 tr = SAM2(tex, 1, -1, res, i);
    vec4 tr2 = SAM2(tex, 2, -1, res, i);

    vec4 cl = SAM2(tex, -1, 0, res, i);
    vec4 c = SAM2(tex, 0, 0, res, i);
    vec4 cr = SAM2(tex, 1, 0, res, i);
    vec4 cr2 = SAM2(tex, 2, 0, res, i);

    vec4 bl = SAM2(tex, -1, 1, res, i);
    vec4 b = SAM2(tex, 0, 1, res, i);
    vec4 br = SAM2(tex, 1, 1, res, i);
    vec4 br2 = SAM2(tex, 2, 1, res, i);

    vec4 b2l = SAM2(tex, -1, 2, res, i);
    vec4 b2 = SAM2(tex, 0, 2, res, i);
    vec4 b2r = SAM2(tex, 1, 2, res, i);
    vec4 b2r2 = SAM2(tex, 2, 2, res, i);

    if (c.r == nodata) c = cr;
    if (c.r == nodata) c = cl;
    if (c.r == nodata) c = t;
    if (c.r == nodata) c = b;
    if (c.r == nodata) c = br;

    if (t.r == nodata) t = c;
    if (tl.r == nodata) tl = c;
    if (tr.r == nodata) tr = c;
    if (tr2.r == nodata) tr2 = c;

    if (cl.r == nodata) cl = c;
    if (cr.r == nodata) cr = c;
    if (cr2.r == nodata) cr2 = c;

    if (b.r == nodata) b = c;
    if (bl.r == nodata) bl = c;
    if (br.r == nodata) br = c;
    if (br2.r == nodata) br2 = c;

    if (b2.r == nodata) b2 = b;
    if (b2l.r == nodata) b2l = b2;
    if (b2r.r == nodata) b2r = b2;
    if (b2r2.r == nodata) b2r2 = b2r;

    return spline(f.y,
                  spline(f.x, tl, t, tr, tr2),
                  spline(f.x, cl, c, cr, cr2),
                  spline(f.x, bl, b, br, br2),
                  spline(f.x, b2l, b2, b2r, b2r2)
    );
}
//END  #include <interpolate_bicubic>

//#include <interpolate_biquadratic>
// https://www.shadertoy.com/view/wtXXDl
vec4 biquadratic(sampler2D tex, vec2 uv, vec2 res) {
    float texSize = max(res.x, res.y);
    vec2 p = uv;
    #if 0
                //Roger/iq style
    p = p * texSize;
    vec2 i = floor(p);
    vec2 f = fract(p);
    p = i + f * 0.5;
    p = p / texSize;
    //f = f*f*(3.0-2.0*f); // optional for extra sweet
    float w = 0.5 / texSize;
    return mix(mix(texture2D(tex, p + vec2(0, 0)),
                   texture2D(tex, p + vec2(w, 0)), f.x),
               mix(texture2D(tex, p + vec2(0, w)),
                   texture2D(tex, p + vec2(w, w)), f.x), f.y);
    #else
                // paniq style (https://www.shadertoy.com/view/wtXXDl)
    vec2 f = fract(p * texSize);
    vec2 c = (f * (f - 1.0) + 0.5) / texSize;
    vec2 w0 = p - c;
    vec2 w1 = p + c;
    return (texture2D(tex, vec2(w0.x, w0.y)) +
    texture2D(tex, vec2(w0.x, w1.y)) +
    texture2D(tex, vec2(w1.x, w1.y)) +
    texture2D(tex, vec2(w1.x, w0.y))) * .25;
    #endif
        }
//END#include <interpolate_biquadratic>


//END #include <interpolated_texel>

vec4 interpolatedTexel(int type, sampler2D tex, vec2 uv, vec2 res) {

    if (type == 2) { // This one is used.
        return bicubic(tex, uv, res);
    }else if (type == 1) {
        return bilinear(tex, uv, res);
    } else if (type == 3) {
        return biquadratic(tex, uv, res);
    } else {
        return texture2D(tex, uv);
    }
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


void main() {


/*
  vec4 texel = texture2D(dataTexture, vUv);
    float value = sampleColorValue(texel, dataChannel[0]);
    float mask1 = sampleColorValue(texel, colorMaskChannel[0]);
    float mask2 = sampleColorValue(texel, colorMaskChannel[1]);
    float mask3 = sampleColorValue(texel, colorMaskChannel[2]);

    vec4 color = vec4(0.0, 0.0, 0.0, value);

    if (mask1 == colorMaskValue[0]) {
        color.r = 1.0;
    } else if (mask2 == colorMaskValue[1]) {
        color.g = 1.0;
    } else if (mask3 == colorMaskValue[2]) {
        color.b = 1.0;
    }

    gl_FragColor = color;


                vec4 tex1 = texture2D(dataTexture, vUV);
                //float value = sampleColorValue(tex1, dataChannel[0]);
                float value1 = sampleColorValue(tex1, 0.0);
                // get value data from specific channel?
                //float mask1 = sampleColorValue(tex1, colorMaskChannel[0]);
                //float mask2 = sampleColorValue(tex1, colorMaskChannel[1]);
                //float mask3 = sampleColorValue(tex1, colorMaskChannel[2]);
                float mask1 = sampleColorValue(tex1, 0.0);
                float mask2 = sampleColorValue(tex1, 1.0);
                float mask3 = sampleColorValue(tex1, 2.0);

                vec4 color1 = vec4(0.0, 0.0, 0.0, value1);

                if (mask1 == 1.0/255.0) {  // x/255
                    color1.r = 1.0;
                } else if (mask2 == 2.0/255.0) {  // x/255
                    color1.g = 1.0;
                } else if (mask3 == 3.0/255.0) {  // x/255
                    color1.b = 1.0;
                }

*/
/*
 * - Rain: G channel, value 1 / 255
 * - Snow: G channel, value 2 / 255
 * - Mixed: G channel, value 3 / 255
*/

//=============================================================

    vec2 drawRange = vec2(drawRangeMin, drawRangeMax);
    vec2 dataRange = vec2(dataRangeMin, dataRangeMax);
    vec2 dataResolution = vec2(516.0, 516.0);
    vec2 v_TexCoordP;//this bit handles partial

    if (x_scale < 1.0) { //Partial code. May differ from JS version...
        float tex_mult = 511.78 / 516.0;
        float pad_mult = 2.11 / 516.0;
        v_TexCoordP = vec2((pad_mult) + (vUV.x * (tex_mult)) * x_scale + (tex_mult) * x_offset, (pad_mult) + (vUV.y * (tex_mult)) * x_scale + (tex_mult) * y_offset);
    } else {
        v_TexCoordP = (vUV * (511.78) + 2.11) / 516.0; // .055 extra crop per pixel
    }
    vec2 tuv = v_TexCoordP;
    //vec2 tuv2 = v_TexCoordP;

    //#include <skip_nodata>
    #ifdef NO_DATA
    //float ndVal = noData;
    vec4 tex = texture2D(dataTexture, tuv);
    float dd = sampleColorValue(tex, channel1);
    if (dd == noData) discard;
    #endif
    //END #include <skip_nodata>

    //#include <get_texel>
    float scale = 1.0 - (0.8 * smoothing);
    vec4 texel1 = interpolatedScaledTexel(interpolation, dataTexture, tuv, dataResolution, scale);
    vec4 texel2 = interpolatedScaledTexel(interpolation, dataTextureNext, tuv, dataResolution, scale);
    vec4 texel = mix(texel1, texel2, dataMeld);
    //END #include <get_texel>

    //#include <get_data_value>
    //END #include <get_data_value>

    texel.a = clamp(texel.a, 0.01, .995);
    vec4 color;
    vec4 testColor =  vec4(texel.r*50.0, texel.g*50.0, texel.b*50.0, 1.0);

    if (testColor.r > .04) {
        color = colorLookup(texel.a , colorScaleRes, colorScale1);  //red
    } else if (testColor.g > .04) {
        color = colorLookup(texel.a, colorScaleRes, colorScale); //green
    } else {
        color = colorLookup(texel.a, colorScaleRes, colorScale2);  //blue
    }

    if (opacity < 1.0) {
        color.a *= opacity;
        color.rgb *= opacity;
    }
    gl_FragColor = color; //correct

    //gl_FragColor = vec4(texel.r*50.0, texel.g*50.0, texel.b*50.0, 1.0);
    //gl_FragColor =   texture2D(dataTexture, tuv); //raw test

}
