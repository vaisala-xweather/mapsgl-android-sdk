precision highp float; //from WebGL

uniform sampler2D dataTexture;
uniform sampler2D dataTextureNext;
uniform sampler2D colorScale;
varying vec2 vUV;


void main() {
    float u_borderWidth = .5;
    vec2 uv = vUV;
    vec2 pixelSize = 1.0 / vec2(512.0, 512.0);



    vec4 texColor = texture2D(dataTexture, uv);
    //texColor.r=0.0;
    vec4 borderColor = vec4(0.0, 0.0, 1.0, .50);
    float borderDistance = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float borderAlpha = smoothstep(0.0, u_borderWidth * pixelSize.x, borderDistance);
    gl_FragColor = mix(borderColor, texColor, borderAlpha);
}