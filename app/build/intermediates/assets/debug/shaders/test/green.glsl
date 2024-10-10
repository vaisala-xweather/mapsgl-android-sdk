precision highp float;
uniform float u_Alpha;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform bool colorize;

uniform sampler2D u_Texture;
uniform sampler2D colorScale;
varying vec2 vUV;

void main(void) {
    vec4 texColor = texture2D(u_Texture, vec2(vUV.x * x_scale + x_offset, vUV.y * y_scale + y_offset));
    gl_FragColor = vec4(texColor.g, texColor.g, texColor.g, 1.0);
}