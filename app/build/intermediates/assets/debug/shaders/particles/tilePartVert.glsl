uniform sampler2D uTexture;
uniform mat4 projectionMatrix;
uniform mat4 modelViewMatrix;
attribute vec3 a_position;
attribute vec2 a_TexCoord;
varying vec2 vUV;
uniform float speedMult;
uniform float vectorOffset;
uniform int numSteps;
uniform int tileMode;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform float tx_scale;
uniform float ty_scale;
uniform float tx_offset;
uniform float ty_offset;
uniform float seed;
uniform int tile_test;
uniform float particleSize;
varying vec4 outPos; //out
varying float alphaOut; //out
// /*layout (location = 0) */varying vec3 position;
// /*layout (location = 1) */varying vec2 texCoord;
int totalSteps = 500;
int fadeOutSteps = 500-80;
int fadeInSteps = 80;
float fadeInStepsF = 80.0;
float totalStepsF = 500.0;


float random(float t_seed) {
    return fract(sin(t_seed) * 43758.5453);
}

vec2 transformPos(vec2 inputVec) {
    // Scale the vector by a factor of 2.0
    inputVec.x *= tx_scale;
    inputVec.y *= ty_scale;
    inputVec.x += tx_offset;
    inputVec.y += ty_offset;
    return inputVec;
}


void main() {

        gl_PointSize = particleSize;
        vec4 vPosition = vec4(a_position.x, 1.0-a_position.y, 0.0, 1.0); //without padding
        vec2 tuv = transformPos(vPosition.xy);

        vec2 dir;
        //time offset, used make sure particles don't all reset at once
        int timeOffset=  int((fract(sin(vPosition.x/*+seed*/) * 43758.5453))*totalStepsF);
        int ns = numSteps+timeOffset;
        if (ns>totalSteps) {ns-=totalSteps;}

        float speedCalc=  (.002 * speedMult) * 9.0;

        if(ns<fadeInSteps){ alphaOut=  .17+ ((float(ns)/fadeInStepsF)*.83); } //fade in
        else if(ns>fadeOutSteps){ alphaOut=  1.0- (float(ns-fadeOutSteps)/40.0);  } //fade out
        else {  alphaOut=1.0; }

        vec4 color;// = texture2D(uTexture, tuv); //partial version

        float dropCount = 0.0;

        for (int i = 0; i < ns; i++) {

            //float randNum =  random(seed+tuv.x+tuv.y );
            if(dropCount >.25){
                vPosition.x+=.1;
                //vPosition.y=0.5;
                tuv = transformPos(vPosition.xy);
                color = texture2D(uTexture, tuv);
                dropCount=0.0;
            } else{
                color = texture2D(uTexture, tuv);
                dir.x = (color.g - vectorOffset);
                dir.y = - (color.b - vectorOffset);
                vPosition.x += dir.x * speedCalc;
                vPosition.y += dir.y * speedCalc;

                tuv.x = vPosition.x * tx_scale;
                tuv.y = vPosition.y * ty_scale;
                tuv.x += tx_offset;
                tuv.y += ty_offset;

                dropCount+=.001;
            }

        }

        gl_Position = projectionMatrix * modelViewMatrix * vPosition; //Adaptation of TS version
        outPos.xy = tuv; //241021
        //pointCenter = gl_Position;

}