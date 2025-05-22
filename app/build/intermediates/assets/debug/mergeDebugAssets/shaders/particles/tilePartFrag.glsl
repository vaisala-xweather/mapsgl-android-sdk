precision highp float;
//precision mediump float;
//uniform float dataMeld;
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

//uniform float tx_scale2;
//uniform float ty_scale2;
//uniform float tx_offset2;
//uniform float ty_offset2;

uniform float time;
uniform sampler2D u_Texture;
uniform sampler2D colorScale;
//uniform sampler2D dataTextureNext;
varying vec4 v_Position;
varying vec2 vUV;
varying vec4 outPos; //in
varying float alphaOut; //in
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
vec2 dataResolution = vec2(516.0, 516.0);
#include <color_lookup>

vec4 SAM2(sampler2D tex, int a, int b, vec2 res, vec2 i) {
    return texture2D(tex, (i + vec2(a, b) + 0.5) * (1.0 / res));
}

//#include <interpolate_bicubic>
// [iq: https://www.shadertoy.com/view/XsSXDy]
const vec4 BS_A = vec4(   3.0,  -6.0,   0.0,  4.0 ) * .1666666;
const vec4 BS_B = vec4(  -1.0,   6.0, -12.0,  8.0 ) * .1666666;
const vec4 RE_A = vec4(  21.0, -36.0,   0.0, 16.0 ) * .0555555;
const vec4 RE_B = vec4(  -7.0,  36.0, -60.0, 32.0 ) * .0555555;
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
    vec2 c = (f*(f-1.0)+0.5) * (1.0 / texSize);
    vec2 w0 = p - c;
    vec2 w1 = p + c;
    return (texture2D(tex, vec2(w0.x, w0.y))+
    texture2D(tex, vec2(w0.x, w1.y))+
    texture2D(tex, vec2(w1.x, w1.y))+
    texture2D(tex, vec2(w1.x, w0.y)))/4.0;
    #endif
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

float sampleColorValue(vec4 color, int channel) {

    float value;
    if (channel == 0)
    value = color.r; //add case for 0=r as it is a common case.
    else if (channel == 1) {
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
        value = (color.r + color.g + color.b) * .333333;
    } else if (channel == 11) {
        value = (color.g + color.b + color.a) * .333333;
    } else if (channel == 12) {
        value = (color.r + color.g + color.b + color.a) * .25;
    }

    value = min(1.0, max(0.0, value)); //Nicks code
    //value = clamp(value, 0.0, 1.0); //Test this when all else is working
    return value;
}

//END#include <interpolate_biquadratic>

//END #include <interpolated_texel>

#ifdef EXPRESSION_VECTOR
    #define NO_DATA //for currents, see if impacts wind
#endif


void main(void) {

        vec2 outPart;
        vec2 outPart2;
        //vec2 v_TexCoordP;//this bit handles partial
        if (x_scale < 1.0) { //Partial code. May differ from JS version...
                             outPart = vec2((outPos.x) * x_scale + x_offset, (outPos.y) * x_scale + y_offset);

        } else {
             outPart = outPos.xy;
        }

        if (x_scale2 < 1.0) { //Partial code. May differ from JS version...
                             outPart2 = vec2((outPos.x) * x_scale + x_offset, (outPos.y) * x_scale + y_offset);

        } else {
            outPart2 = outPos.xy;
        }

        float colorScaleRes = 256.0;
        vec4 texColor = texture2D(u_Texture, outPart.xy);

        //#include <skip_nodata>
        #ifdef NO_DATA
        vec4 tex = texture2D(u_Texture, outPart.xy); //u_Texture
        float dd = sampleColorValue(tex, channel1);
        if (dd == noData) discard;
        #endif
    //END #include <skip_nodata>


        float scale = 1.0 - (0.8 * .5); //.5 is smoothing
        #ifdef NO_DATA
        //float ndVal = 0.0;
        vec4 texel1 = interpolatedScaledNoDataTexel(interpolation, u_Texture,  outPart.xy, dataResolution, scale, noData); // u_Texture
        //vec4 texel2 = interpolatedScaledNoDataTexel(interpolation, dataTextureNext, outPart.xy, dataResolution, scale, noData);
        #else
        vec4 texel1 = interpolatedScaledTexel(interpolation, u_Texture,  outPart.xy, dataResolution, scale);
        //vec4 texel2 = interpolatedScaledTexel(interpolation, dataTextureNext, outPart.xy, dataResolution, scale);
        #endif

        //if(texColor.a<0.01||alphaOut<0.01) discard;
        //if(texColor.g == 0.0 && texColor.r == 0.0 && texColor.b == 0.0) discard;
        //if(texColor.a==0.0 && texColor.b ==0.0 && texColor.g == 0.0) discard;
        vec4 color;

        if(true){ //vector
                  float valueU = sampleColorValue(texel1, channel1);
                  float valueV = sampleColorValue(texel1, channel2);
                  vec2 vector = vec2(valueU, valueV);
                  vec2 velocity = mix(vec2(dataRangeMin), vec2(dataRangeMax), vector); //real

                  float value = length(velocity) / dataRangeMax; // / dataRangeMax
                  float angle = atan(velocity.x, -velocity.y);
                  float _PI = 3.14159265359;
                  angle -= _PI / 2.0;
                  //float dataValue = sampleDataMin + (sampleDataMax - sampleDataMin) * value; //good
                  // if (dataValue < drawRange.x) discard;
                  // if (dataValue > drawRange.y) discard;
                  value = (value - drawRangeMin) / (drawRangeMax - drawRangeMin); //redone
                  //if (value > .995) value = .995;//trying to fix AQI issue with going past max value 240703 MAPSGLAND-103
                  //if (value<.001) value = .001; //wind chill
                  color = colorLookup(value, colorScaleRes, colorScale); //original
                  color.a=alphaOut;

        } else{
            color = texColor;
        }

        //if(color.a<0.1) discard;
        //if(color.r+color.b+color.g<0.1) discard;
        //color.a =  alphaOut; //this handles fade in and fade out, re-enable
        //color.a = .2;
        gl_FragColor = color; //try to do encode-style color band
        //fragColor=color;
        //gl_FragColor.rgb*=gl_FragColor.a;




}

