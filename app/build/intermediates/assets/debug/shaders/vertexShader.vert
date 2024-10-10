uniform mat4 projectionMatrix;
uniform mat4 modelViewMatrix;
attribute vec2 a_position;
attribute vec2 a_TexCoord;
varying  vec2 vUV;


void main() {

  gl_Position = projectionMatrix * modelViewMatrix * vec4(a_position, 0.0, 1.0); //Adaptation of TS version
  vUV = vec2(a_TexCoord.x, (1.0 - (a_TexCoord.y)));

}