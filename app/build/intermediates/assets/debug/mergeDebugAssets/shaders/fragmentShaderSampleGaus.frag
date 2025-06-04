precision highp float;
uniform float u_Alpha;
uniform sampler2D u_Texture;
uniform sampler2D color_band;
uniform vec2 u_TexelSize; // Texel size (1.0 / texture_width, 1.0 / texture_height)
varying vec2 v_TexCoord;

void main(void) {
    float gk[25];
    gk[0] = 1.0 / 256.0; gk[1] = 4.0 / 256.0; gk[2] = 6.0 / 256.0; gk[3] = 4.0 / 256.0; gk[4] = 1.0 / 256.0;
    gk[5] = 4.0 / 256.0; gk[6] = 16.0 / 256.0; gk[7] = 24.0 / 256.0; gk[8] = 16.0 / 256.0; gk[9] = 4.0 / 256.0;
    gk[10] = 6.0 / 256.0; gk[11] = 24.0 / 256.0; gk[12] = 36.0 / 256.0; gk[13] = 24.0 / 256.0; gk[14] = 6.0 / 256.0;
    gk[15] = 4.0 / 256.0; gk[16] = 16.0 / 256.0; gk[17] = 24.0 / 256.0; gk[18] = 16.0 / 256.0; gk[19] = 4.0 / 256.0;
    gk[20] = 1.0 / 256.0; gk[21] = 4.0 / 256.0; gk[22] = 6.0 / 256.0; gk[23] = 4.0 / 256.0; gk[24] = 1.0 / 256.0;

    vec4 blurredColor = vec4(0.0);
    for (int i = -2; i <= 2; i++) {
        for (int j = -2; j <= 2; j++) {
            vec2 offset = vec2(float(i), float(j)) * u_TexelSize;
            vec4 texColor = texture2D(u_Texture, v_TexCoord + offset);
            vec2 myVec2 = vec2(texColor.r, 0.0);
            blurredColor += texture2D(color_band, myVec2) * gk[(i + 2) * 5 + (j + 2)];
        }
    }
    gl_FragColor = blurredColor;
}