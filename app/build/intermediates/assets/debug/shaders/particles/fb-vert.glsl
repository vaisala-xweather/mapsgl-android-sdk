//#version 300 es
layout(location = 0) varying vec4 a_position;
varying vec2 texCoord;
void main() {
    gl_Position = a_position;
    texCoord = a_position.xy * 0.5 + 0.5; // Map to [0,1]
}