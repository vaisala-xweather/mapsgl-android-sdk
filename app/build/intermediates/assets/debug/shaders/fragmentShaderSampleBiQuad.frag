precision highp float;
uniform float u_Alpha;
uniform sampler2D u_Texture;
uniform sampler2D color_band;
varying vec2 v_TexCoord;

vec4 biquadFilter(sampler2D tex, vec2 texCoord) {
    vec2 texSize = vec2(512.0,512.0);
    vec2 texelSize = 1.0 / texSize;

    vec4 color = vec4(0.0);
    float totalWeight = 0.0;


    for (int x = -5; x <= 5; x++) {
        for (int y = -5; y <= 5; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            vec2 coord = texCoord + offset;
            float weight = (1.0 - abs(offset.x)) * (1.0 - abs(offset.y));
            color += texture2D(tex, coord) * weight;
            totalWeight += weight;
        }
    }

    return color / totalWeight;
}

void main(void) {
    vec4 texColor = biquadFilter(u_Texture, v_TexCoord);

    vec2 myVec2 = vec2(texColor.r, 0.0);
    //vec4 color2 = texture2D(color_band, myVec2);

    gl_FragColor = texture2D(color_band, myVec2); // final image
}