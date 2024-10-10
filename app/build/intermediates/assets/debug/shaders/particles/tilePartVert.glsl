uniform sampler2D uTexture;
uniform mat4 projectionMatrix;
uniform mat4 modelViewMatrix;
attribute vec3 a_position;
attribute vec2 a_TexCoord;
varying vec2 vUV;
uniform float time;
uniform float timeStep;
uniform float speedMult;
uniform float vectorOffset;
uniform int numSteps;
uniform int tileMode;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform float particleSize;
uniform float seed;
varying vec4 outPos; //out
varying vec4 pointCenter; //out
varying float alphaOut; //out
/*layout (location = 0) */varying vec3 position;
/*layout (location = 1) */varying vec2 texCoord;
int totalSteps = 500;
int fadeOutSteps = 500-80;
int fadeInSteps = 80;
float fadeInStepsF = 80.0;
float totalStepsF = 500.0;

void main() {
    gl_PointSize = particleSize;
    float mult = 516.0/512.0;
    float offset = 8.0/516.0;

    vec4 vPosition = vec4(a_position.x*mult-offset, a_position.y*mult-offset, 0.0, 1.0);
    //vUV=a_TexCoord;

        vec2 tuv;//this bit handles partial
        if (x_scale < 1.0) {
            //tuv = vec2(vPosition.x * x_scale + x_offset, vPosition.y * x_scale + y_offset); //works, but "pops"
            float tex_mult=511.78/516.0;
            float pad_mult=2.11/516.0;
            tuv= vec2((pad_mult)+(vPosition.x*(tex_mult))*x_scale +(tex_mult)*x_offset,   (pad_mult)+(vPosition.y*(tex_mult))*x_scale+(tex_mult)*y_offset);

            //tuv=vPosition.xy;
        } else {
            //vPosition.xy= (vPosition.xy * (511.78) + 2.11) / 516.0;
            tuv = vPosition.xy;
        }

        vec4 color;// = texture2D(uTexture, tuv); //partial version
        vec2 dir;
        //time offset, used make sure particles don't all reset at once
        int timeOffset=  int((fract(sin(vPosition.x/*+seed*/) * 43758.5453))*totalStepsF);
        int ns = numSteps+timeOffset;
        if (ns>totalSteps) {ns-=totalSteps;}

        float speedCalc= .2 * (timeStep * speedMult);

        //if(ns<fadeInSteps){ alphaOut=  .5; } //fade in
        if(ns<fadeInSteps){ alphaOut=  .17+ ((float(ns)/fadeInStepsF)*.83); } //fade in
        else if(ns>fadeOutSteps){ alphaOut=  1.0- (float(ns-fadeOutSteps)/40.0);  } //fade out
        else {  alphaOut=1.0; }

       // speedCalc*=.2;// for currents
        for (int i = 0; i < ns; i++) {
            color = texture2D(uTexture, tuv);

            /*
            if(vectorOffset==.1 && color.g==0.0 && color.b==0.0) { //temporary mask for ocean current particles.
                vPosition.x=1000.0;
                return;
            }*/

            dir.x =  (color.g - vectorOffset ); //-.1 for currents, -.5 for wind..
            dir.y = -(color.b - vectorOffset);


            vPosition.x += dir.x * speedCalc;
            vPosition.y += dir.y * speedCalc;

            if(vPosition.y < -.00 || vPosition.x > 1.00 || vPosition.x<-.00 || vPosition.y>1.00) {alphaOut-=0.010; }

            if (x_scale < 1.0) {
                tuv = vec2(vPosition.x * x_scale + x_offset, vPosition.y * x_scale + y_offset);
            } else {
                tuv = vPosition.xy;
            }
        }


    if (tileMode == 0) {
        gl_Position = projectionMatrix * modelViewMatrix * vPosition; //Adaptation of TS version
    }
    else if (tileMode == 1) {
        gl_Position = vPosition;
    }

    outPos = vPosition;
    pointCenter = gl_Position;
}
