uniform float tx_offset;
uniform float ty_offset;
uniform float tx_scale;
uniform float ty_scale;
attribute vec2 a_position;
varying vec4 outPos; //out


void main() {

    outPos =   vec4(a_position.x, a_position.y, 0.0, 1.0);

    gl_Position = vec4((a_position.x-1.0)*tx_scale+ tx_offset, (a_position.y-1.0)*ty_scale+ ty_offset, 0.0, 1.0);

}