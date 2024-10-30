precision highp float;
//precision mediump float;
uniform float u_Alpha;
uniform sampler2D u_Texture;
uniform sampler2D trail_texture;
varying vec4 v_Position;
varying vec2 vUV;
varying vec4 outPos; //in
varying float alphaOut; //in
uniform int preMult;
uniform int mode;
uniform int channel1;
uniform int channel2;
uniform float alpha;
#include <color_lookup>

void main(void) {

    vec4 texColor = texture2D(u_Texture, outPos.xy); // was vUV



    texColor.a=texColor.a*alpha;


    if(preMult==1){
            if(texColor.a<.20) discard; //.21
            texColor.rgb*=texColor.a;
        } else {
           // if(texColor.a<.08) discard; //.38
            //texColor.rgb*=texColor.a;
        }

    gl_FragColor=texColor;




}

