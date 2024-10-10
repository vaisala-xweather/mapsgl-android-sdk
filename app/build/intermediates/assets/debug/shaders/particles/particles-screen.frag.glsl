precision highp float;

uniform float opacity;
uniform sampler2D tOutput;

varying vec2 vUv;

void main() {
    vec4 color = texture2D(tOutput, vUv);
    gl_FragColor = vec4(floor(255.0 * color * opacity) / 255.0);
}