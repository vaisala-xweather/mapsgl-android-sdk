#version 310 es
precision highp float;
uniform sampler2D uTexture;
uniform mat4 projectionMatrix;
uniform mat4 modelViewMatrix;
uniform float speedMult;
uniform float vectorOffset;
uniform int numSteps;
uniform float tx_scale;
uniform float ty_scale;
uniform float tx_offset;
uniform float ty_offset;
uniform float particleSize;
uniform int bufferItemsPerTile;

layout (location = 2) in vec4 a_particleRenderData; // was 0

// Varyings bacome 'out' variables (in Vertex Shader) ---
out vec4 outPos;    // Changed 'varying' to 'out'
out float alphaOut; // Changed 'varying' to 'out'

// Simple pseudo-random function
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

void main() {
    gl_PointSize = particleSize;
    //gl_PointSize = 50.0;

    vec4 vPosition = vec4(a_particleRenderData.x, a_particleRenderData.y, 0.0, 1.0);
    vec2 tuv = vec2((vPosition.x * tx_scale) + tx_offset,  (vPosition.y * ty_scale) + ty_offset);
    alphaOut = a_particleRenderData.z;
    gl_Position = projectionMatrix * modelViewMatrix * vPosition; //Adaptation of TS version
    outPos.xy = tuv; // This is passed to the fragment shader
}