precision highp float; //from WebGL
uniform float dataMeld;
uniform float opacity;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float x_offset2;
uniform float y_offset2;
uniform float x_scale2;
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

uniform sampler2D dataTexture;
uniform sampler2D dataTextureNext;
uniform sampler2D colorScale;
varying vec2 vUV;

#include <color_lookup>

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
    if (channel == 0 )
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
        value = (color.r + color.g + color.b + color.a) *.25;
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


float isEdge(sampler2D tex, vec2 uv, vec2 res, float nodata, int channel) { //Is this ever used?
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
const vec4 BS_A = vec4(   3.0,  -6.0,   0.0,  4.0 ) /  6.0;
const vec4 BS_B = vec4(  -1.0,   6.0, -12.0,  8.0 ) /  6.0;
const vec4 RE_A = vec4(  21.0, -36.0,   0.0, 16.0 ) / 18.0;
const vec4 RE_B = vec4(  -7.0,  36.0, -60.0, 32.0 ) / 18.0;
const vec4 CR_A = vec4(   3.0,  -5.0,   0.0,  2.0 ) * .5;
const vec4 CR_B = vec4(  -1.0,   5.0,  -8.0,  4.0 ) * .5;

const vec4 CA = BS_A;
const vec4 CB = BS_B;

vec4 powers(float x) {
    return vec4(x*x*x, x*x, x, 1.0);
}

vec4 spline(float x, vec4 c0, vec4 c1, vec4 c2, vec4 c3) {
    return c0 * dot(CB, powers(x + 1.)) +
    c1 * dot(CA, powers(x     )) +
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
                  spline(f.x, SAM2(tex, -1,-1, res, i), SAM2(tex, 0,-1, res, i), SAM2(tex, 1,-1, res, i), SAM2(tex, 2,-1, res, i)),
                  spline(f.x, SAM2(tex, -1, 0, res, i), SAM2(tex, 0, 0, res, i), SAM2(tex, 1, 0, res, i), SAM2(tex, 2, 0, res, i)),
                  spline(f.x, SAM2(tex, -1, 1, res, i), SAM2(tex, 0, 1, res, i), SAM2(tex, 1, 1, res, i), SAM2(tex, 2, 1, res, i)),
                  spline(f.x, SAM2(tex, -1, 2, res, i), SAM2(tex, 0, 2, res, i), SAM2(tex, 1, 2, res, i), SAM2(tex, 2, 2, res, i))
    );
}

vec4 bicubic_nodata(sampler2D tex, vec2 uv, vec2 res, float channel, float nodata) {
    vec2 tuv = uv * res - 0.5;
    vec2 f = fract(tuv);
    vec2 i = floor(tuv);

    vec4 tl = SAM2(tex, -1,-1, res, i);
    vec4 t = SAM2(tex, 0,-1, res, i);
    vec4 tr = SAM2(tex, 1,-1, res, i);
    vec4 tr2 = SAM2(tex, 2,-1, res, i);

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
    texture2D(tex, vec2(w1.x, w0.y))) * .25;
    #endif
        }
//END#include <interpolate_biquadratic>


//END #include <interpolated_texel>

vec4 interpolatedTexel(int type, sampler2D tex, vec2 uv, vec2 res) {
    vec4 texel;
    //type = 4; //force raw pixels

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
//END #include <interpolated_texel>

// #ifndef RELATIVE_TOLERANCE
// #define RELATIVE_TOLERANCE 0.0001
// #endif

// #define TRANSPARENT vec4(0.0)

// bool isCloseEnough(float a, float b) {
//   return abs(a - b) <= max(abs(a), abs(b)) * RELATIVE_TOLERANCE;
// }

//#define NO_DATA
//#define EXPRESSION_VECTOR
//#define EXPRESSION_ANGLE
//#define EXPRESSION_SUM
//#define EXPRESSION_DIFF
//#define DEBUG_EDGE
//#define RAW_IMAGE

void main() {   //TODO: MAIN START

    vec2 drawRange = vec2(drawRangeMin, drawRangeMax);
    //vec2 dataRange = vec2(153.0, 155.0);
    //vec2 dataRange = vec2(0.0, 1.0);
    vec2 dataRange = vec2(dataRangeMin, dataRangeMax);
    float smoothing=.5; //.4
    float colorScaleRes = 256.0;
    // The resolution of the overall padded image since you need that to properly do interpolation based on the texture size
    vec2 dataResolution = vec2(516.0, 516.0);
    //float colorRangeFactor;

    //end constants

    //vec2 v_TexCoordP;//this bit handles partial
    //vec2 v_TexCoordP2;//this bit handles partial for 2nd texture in animation
    vec2 tuv; //= v_TexCoordP;
    vec2 tuv2;// = v_TexCoordP2;

    #ifdef RAW_IMAGE //raster tiles. Not Encoded
        if(x_scale<1.0) { //Partial code. May differ from JS version...
            float tex_mult=1.0;
            tuv =  vec2((vUV.x*(tex_mult))*x_scale +(tex_mult)*x_offset,  (vUV.y*(tex_mult))*x_scale+(tex_mult)*y_offset);
        } else{
            tuv = (vUV ); // .055 extra crop per pixel
        }
    #else //Encoded
        float tex_mult=512.00/516.0;        float pad_mult=2.0/516.0;
       //float tex_mult=511.78/516.0;      //float pad_mult=2.11/516.0;

        //Texture 1 in animation
                if (x_scale < 1.0) { //Partial code. May differ from JS version...
                    tuv = vec2(((pad_mult)+(vUV.x*(tex_mult))*x_scale +(tex_mult)*x_offset), ((pad_mult)+(vUV.y*(tex_mult))*x_scale+(tex_mult)*y_offset) );
                    //tuv = vec2((vUV.x * x_scale + x_offset), (vUV.y * x_scale + y_offset));  //show padding
                } else {
                    tuv = (((vUV * (512.00) + 2.00 )) / 516.0);
                    //tuv = (vUV); //show padding
        }

        //Texture 2 in animation
        if(x_scale2<1.0) { //Partial code. May differ from JS version...
            tuv2 =  vec2((pad_mult)+(vUV.x*(tex_mult))*x_scale2 +(tex_mult)*x_offset2,   (pad_mult)+(vUV.y*(tex_mult))*x_scale2+(tex_mult)*y_offset2);
            //tuv2 =  vec2((vUV.x * x_scale2) +x_offset2,   (vUV.y)*x_scale2+y_offset2);  //show padding
        } else{
            tuv2 = (vUV * (512.0) + 2.0) / 516.0;
            //tuv2 = (vUV);  //show padding
        }
    #endif
    //vec2 px = 1.0 / dataResolution; //Does this get used?

    //#include <skip_nodata>
    #ifdef NO_DATA
        //float ndVal = 0.0;
        //float ndVal = noData;
        vec4 tex = texture2D(dataTexture, tuv);
        float dd = sampleColorValue(tex, channel1);
        if (dd == noData) discard; //real
            //if (dd > .6) discard; //windchill test
            //if (dd < .5)  opacity=dd; //test
    #endif
    //END #include <skip_nodata>

    //#include <get_texel>
    float scale = 1.0 - (0.8 * smoothing);

    #ifdef EXPRESSION_VECTOR
        #define NO_DATA //for currents, see if impacts wind
    #endif

    #ifdef NO_DATA
        //float ndVal = 0.0;
        vec4 texel1 = interpolatedScaledNoDataTexel(interpolation, dataTexture, tuv, dataResolution, scale, noData);
        vec4 texel2 = interpolatedScaledNoDataTexel(interpolation, dataTextureNext, tuv2, dataResolution, scale, noData);
    #else
        vec4 texel1 = interpolatedScaledTexel(interpolation, dataTexture, tuv, dataResolution, scale);
        vec4 texel2 = interpolatedScaledTexel(interpolation, dataTextureNext, tuv2, dataResolution, scale);
    #endif


    vec4 texel = mix(texel1, texel2, dataMeld); // Timeline animation blending
    //vec4 texel = texel1; //test just current texture

    //END #include <get_texel>

           //#include <get_data_value>
           float value;
           vec2 velocity = vec2(0.);
           float angle = 0.;
           float _PI = 3.14159265359;

        #ifdef EXPRESSION_VECTOR
        // determine velocity from vectors
           float valueU = sampleColorValue(texel, channel1);
           float valueV = sampleColorValue(texel, channel2);
           vec2 vector = vec2(valueU, valueV);
           velocity = mix(vec2(dataRangeMin), vec2(dataRangeMax), vector); //real
           value = length(velocity) / dataRangeMax; // / dataRangeMax

        #ifdef IS_SYMBOL
            angle = atan(velocity.x, velocity.y);
        #else
        angle = atan(velocity.x, -velocity.y);
        #endif
        angle -= _PI * .5;

     #else
        #ifdef EXPRESSION_ANGLE
            float deg = sampleColorValue(texel, channel1) * 360. - 180.0;
                if (deg < 0.) deg += 360.;
                // deg += 90.;
                // https://www.eol.ucar.edu/content/wind-direction-quick-reference
                float rad = deg * (_PI / 180.);
                float valueU = sin(rad);
                float valueV = cos(rad);
                vec2 vector = vec2(valueU, valueV);
                // velocity *= vec2(-1.0, 1.0);
                velocity = vector;
                value = length(velocity) / dataRange.y;
                // angle = rad;
                angle = atan(velocity.x, -velocity.y);
                angle -= PI / 2.0;
                if (angle < 0.) angle += PI * 2.;
        #else
           // int channel = 5;
            //was channel1!!
            value = sampleColorValue(texel, channel1); //THIS IS EXECUTED FOR ENCODED TILES
        #endif
    #endif



    #ifdef NO_DATA
        // scale value to 0...1 range for color LUT to account for noData
         vec2 mapFrom = vec2(outputRangeMin, outputRangeMax);
          vec2 mapTo = vec2(0., 1.);
          value = (value - mapFrom.x) * (mapTo.y - mapTo.x) / (mapFrom.y - mapFrom.x) + mapTo.x; //this was commented out
          // float low_data = .2;
          // float high_data = .6f;
          // value = (value - low_data) * (mapTo.y - mapTo.x) / (high_data - low_data) + mapTo.x; //this was commented out
    #endif
    //END #include <get_data_value>

    #ifdef EXPRESSION_SUM
        float value2 = sampleColorValue(texel, channel2);
        float delta = (value + value2) * colorRangeFactor;
        value = delta + colorSampleOffset;
    #endif

    #ifdef EXPRESSION_DIFF
        float deltaMult;
        float value1 = sampleColorValue(texel, channel1);
        float value2 = sampleColorValue(texel, channel2);

        deltaMult = colorRangeFactor*1.0;

        value = (value1 - value2)* deltaMult + colorSampleOffset;
    #endif

    vec4 color;
    #ifdef COLOR_LOOKUP
        float dataValue = sampleDataMin  + (sampleDataMax - sampleDataMin) * value; //good
        if (dataValue < drawRange.x) discard;
        if (dataValue > drawRange.y) discard;

        value = (value - drawRangeMin) / (drawRangeMax-drawRangeMin); //redone
        if(value>.995) value = .995;//trying to fix AQI issue with going past max value 240703 MAPSGLAND-103
        if (value<.001) value = .001; //wind chill
        color = colorLookup(value, colorScaleRes, colorScale); //original

    #else
        color = vec4(value, 0., 0., 1.);
    #endif

    // color = isCloseEnough(value, noData) ? TRANSPARENT : color;
    #ifdef DEBUG_EDGE
        float edgePad=1.0;
        //color = texture2D(dataTexture, vUV); //from webGL, not needed, assigned above
        float pad = edgePad;
        /*
                if (tuv.x < px.x * pad || tuv.x > 1.0 - px.x * pad || tuv.y < px.y * pad || tuv.y > 1.0 - px.y * pad) {
            color.a *= 0.9; //webGL
            //color.a *= 0.0; //my test
            color.r=0.0;            color.b=0.0;            color.g=0.0;
        }*/
    #endif


    #ifdef RASTER_IMAGE //raster tiles
    color.r = sampleColorValue(texel, 0);
    color.g = sampleColorValue(texel, 1);
    color.b = sampleColorValue(texel, 2);
    if(color.b < 0.001&& color.g< 0.001 && color.r<0.001) {
       discard;
    }
    #endif

    //Correct
    if(opacity<1.0){
        color.a*=opacity;
        color.rgb*=opacity;
    }

    //color = texture2D(colorScale, tuv); //raw colorScale
    //color = texture2D(dataTextureNext, tuv); //raw next texture
    //color = texture2D(dataTexture, tuv);  //raw current texture

    //color = mix(texture2D(dataTexture, tuv), texture2D(dataTextureNext, tuv), dataMeld);

    gl_FragColor = color;

    //BoarderTest------------------------------------
    /*
    vec4 borderColor = vec4(0.0, 0.0, 1.0, .50);
    float borderDistance = min(min(vUV.x, 1.0 - vUV.x), min(vUV.y, 1.0 - vUV.y));
    float borderAlpha = smoothstep(0.0, 1.5 * (1.0/516.0), borderDistance); //1/516 is pixelsize, 1.5 is boarderwidth
    gl_FragColor = mix(borderColor, color, borderAlpha);
    */

    //gl_FragColor.rgb *= gl_FragColor.a; //commented out 240710, to fix Vis and Ocean currents.



}
