precision highp float;
uniform float u_Alpha;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform bool colorize;
uniform float time;
uniform float fadeMult;
uniform sampler2D u_Texture;
uniform sampler2D colorScale;
varying vec4 v_Position;
//attribute  vec2 a_TexCoord;
varying vec2 vUV;
//new custom below
varying vec4 outPos; //in
varying float alphaOut; //in
//----------------------------------
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
#include <color_lookup>

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
        value = (color.r + color.g) / 2.0;
    } else if (channel == 5) {
        value = (color.g + color.b) / 2.0;
    } else if (channel == 6) {
        value = (color.r + color.b) / 2.0;
    } else if (channel == 7) {
        value = (color.r + color.a) / 2.0;
    } else if (channel == 8) {
        value = (color.g + color.a) / 2.0;
    } else if (channel == 9) {
        value = (color.b + color.a) / 2.0;
    } else if (channel == 10) {
        value = (color.r + color.g + color.b) / 3.0;
    } else if (channel == 11) {
        value = (color.g + color.b + color.a) / 3.0;
    } else if (channel == 12) {
        value = (color.r + color.g + color.b + color.a) / 4.0;
    }

    value = min(1.0, max(0.0, value)); //Nicks code
    //value = clamp(value, 0.0, 1.0); //Test this when all else is working

    return value;
}



void main(void){




    //Partial Code
    vec2 outPart;
    vec2 v_TexCoordP;//this bit handles partial
    if(x_scale<1.0) { //Partial code. May differ from JS version...
                      outPart =  vec2((outPos.x)*x_scale +x_offset,   (outPos.y)*x_scale+y_offset);
    } else{
        outPart = outPos.xy; // .055 extra crop per pixel
        outPart.x=(outPart.x*1.0+1.0)*.5;
        outPart.y=(outPart.y+1.0)*.5;
    }

    float colorScaleRes = 256.0;

    vec4 texColor = texture2D(u_Texture, outPart.xy); // was vUV
    /*
    if ((1.0 - (time)) <.1){
        //texColor.a= (1.0-(time+stepOffset))*10.0;
        texColor.a= (1.0-(time))*10.0;
    } else {
        texColor.a =1.0;
    }
    texColor.a= texColor.a * fadeMult;
    */

    float value;
    vec2 velocity = vec2(0.);
    float angle = 0.;
    float _PI = 3.14159265359;

    float valueU = sampleColorValue(texColor, channel1);
    float valueV = sampleColorValue(texColor, channel2);
    vec2 vector = vec2(valueU, valueV);
    velocity = mix(vec2(dataRangeMin), vec2(dataRangeMax), vector); //real
    value = length(velocity) / dataRangeMax; // / dataRangeMax


    angle = atan(velocity.x, -velocity.y);
    angle -= _PI / 2.0;

    vec4 color;

    float dataValue = sampleDataMin  + (sampleDataMax - sampleDataMin) * value; //good
   // if (dataValue < drawRange.x) discard;
   // if (dataValue > drawRange.y) discard;



    value = (value - drawRangeMin) / (drawRangeMax-drawRangeMin); //redone

    if(value>.995) value = .995;//trying to fix AQI issue with going past max value 240703 MAPSGLAND-103
    if (value<.001) value = .001; //wind chill

    color = colorLookup(value, colorScaleRes, colorScale); //original


    //gl_FragColor = texColor; //use raw underlying texture

    color.a =  alphaOut;
    gl_FragColor = color; //try to do encode-style color band



}

