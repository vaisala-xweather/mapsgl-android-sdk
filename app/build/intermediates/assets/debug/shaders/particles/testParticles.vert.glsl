uniform sampler2D uTexture;
uniform mat4 projectionMatrix;
uniform mat4 modelViewMatrix;
attribute vec2 a_position;
attribute vec2 a_TexCoord;
varying  vec2 vUV;
uniform float time;
varying vec2 vTexCoord;
varying vec4 aTexCoord;
varying vec4 outPos; //out
layout(location = 0) varying vec3 position;
layout(location = 1) varying vec2 texCoord;

void main() {

    //a_position is the new vPosition
    vec4 vPosition = vec4(a_position.x, a_position.y, 0.0, 1.0);

    outPos.x = vPosition.x;
    outPos.y = vPosition.y;
    vec2 coord = vec2(outPos.x, outPos.y);
    vec4 color = texture2D(uTexture, coord);

    //vPosition.x+=time*(color.b-.5)*.1;
    //vPosition.y+=time*(color.g-.5)*.1;
    gl_Position = projectionMatrix * modelViewMatrix * vPosition; //Adaptation of TS version
    //gl_Position = vPosition ;
    vUV = vec2(a_TexCoord.x, (1.0 - (a_TexCoord.y)));
    gl_PointSize = 40.0;
    outPos.x=a_TexCoord.x;
    outPos.y = a_TexCoord.y;
   // outPos=gl_Position;

}


/*
void main() {
    vec4 prePosition =  vPosition + vec4(angleX, angleY, 0.0, 0.0);  // Position of lats frame
    vec2 coord2 = vec2(prePosition.x, prePosition.y);
    vec4 color2 = texture2D(uTexture, coord2);  //color of last frame
    angleX=color2.b * time;
    angleY=color2.r * time;

   gl_Position = vPosition + vec4(angleX, angleY, 0.0, 0.0);

    outPos = gl_Position;   // color changes as moves color
    //outPos = vPosition;   //keep original color
}*/