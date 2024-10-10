uniform sampler2D uTexture;
uniform mat4 projectionMatrix;
uniform mat4 modelViewMatrix;
attribute vec3 a_position;
//attribute vec2 a_TexCoord;
varying  vec2 vUV;
uniform float time;
uniform float timeStep;
uniform float speedMult;
uniform int numSteps;

uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform float pointSize;
varying vec4 outPos; //out
varying vec4 pointCenter; //out
varying float alphaOut; //out
layout(location = 0) varying vec3 position;
layout(location = 1) varying vec2 texCoord;

void main() {

    vec4 vPosition = vec4(a_position.x, a_position.y, 0.0, 1.0);
    //vUV=a_TexCoord;


    //if ( vPosition.y > 1.2 || vPosition.x > 1.2) {vPosition.y =9999.0;    }//temporary hack to get rid of seams consisting of excess points


    vPosition.x= (vPosition.x-1.0)*2.0;
    vPosition.y= (vPosition.y-1.0)*2.0;

    if(true) {
        gl_PointSize = pointSize; //1.5
        //a_position is the new vPosition

        vec2 tuv;//this bit handles partial
        if(x_scale<1.0) {
            tuv =  vec2(vPosition.x * x_scale +x_offset, vPosition.y * x_scale + y_offset);
        } else{
            //TEST 2MESSING WITH THIS, good size, misalignment
            //vPosition.x=vPosition.x*2.0-1.0;
            //vPosition.y=vPosition.y*2.0-1.0;


            //vPosition.x = vPosition.x-.5;


            tuv = vPosition.xy;



        }

        vec4 color;// = texture2D(uTexture, tuv); //partial version
        vec2 dir;
        //time offset, used make sure particles dom't all reset at once
        int timeOffset=  int((fract(sin(vPosition.x) * 43758.5453))*200.0);
        int ns = numSteps+timeOffset;
        if (ns>200) {ns-=200;}

        if(ns<15){ alphaOut=  (float(ns)/15.0); }
        else if(ns>160){ alphaOut=  1.0- (float(ns-160)/40.0);  } //fade out
        else {alphaOut=1.0;}

        float speedCalc= .2 * (timeStep * speedMult);
        //colorTemp.x= (colorTemp.x + 1.0)*.5;
        //TEST===========================================
        //tuv.x=(tuv.x*.5)+1.0;
        //tuv.y=(tuv.y*.5)+1.0;

        //TEST===========================================
        for (int i = 0; i < ns; i++) {
            color = texture2D(uTexture, tuv);
            dir.x = speedCalc * (color.g - .5);
            dir.y = speedCalc * -(color.b - .5);
            vPosition.x += dir.x;
            vPosition.y += dir.y;

            if (x_scale < 1.0) {
                tuv = vec2(vPosition.x * x_scale + x_offset, vPosition.y * x_scale + y_offset);
            } else {

                tuv = vPosition.xy;
                tuv.x= (vPosition.x+1.0)/2.0;
                tuv.y= (vPosition.y+1.0)/2.0;
            }

        }
    }
    //vec4 tempPosition = vPosition;
    //tempPosition.x=(vPosition.x-1.0)*1.0;
    //tempPosition.y=(vPosition.y-1.0)*1.0;
    //gl_Position = /*projectionMatrix * modelViewMatrix * */ tempPosition; //Adaptation of TS version //tempPosition will move from BR to TL

    gl_Position = /*projectionMatrix * modelViewMatrix * */ vPosition; //Adaptation of TS version
    outPos=vPosition;
    pointCenter = gl_Position;
}
