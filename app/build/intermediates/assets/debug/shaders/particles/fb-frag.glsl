//#version 300 es
precision highp float;
uniform sampler2D texture;
uniform float transparency;
varying vec2 texCoord;
varying vec4 fragColor;
void main() {
    vec4 tex = texture2D(texture, texCoord);
    if (tex.a == 0.0) discard;
    tex.a = transparency;
    gl_FragColor = tex;
}