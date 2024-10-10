
precision highp float;
uniform float u_Alpha;
uniform sampler2D u_Texture;
uniform sampler2D color_band;
varying vec2 v_TexCoord;
uniform float blur_size;
uniform float blur_radius;



void main(void){


    float blurSize =  blur_size/512.0;
    float blurRadius = blur_radius;

    vec4 texColor = texture2D(u_Texture, v_TexCoord);

    vec2 myVec2 = vec2(texColor.r, 0.0);
    vec4 color2 = texture2D(color_band, myVec2);

    vec4 blurColor = vec4(0.0);
    float totalWeight = 0.0;
    for(float x = -blurRadius; x <= blurRadius; x += 1.0) {
        for(float y = -blurRadius; y <= blurRadius; y += 1.0) {
            vec2 offset = vec2(x * blurSize, y * blurSize);
            float weight = 1.0 - sqrt(dot(offset, offset)) / blurRadius;
            blurColor += weight * texture2D(color_band, myVec2 + offset);
            totalWeight += weight;
        }
    }
    blurColor /= totalWeight;

    gl_FragColor = blurColor;
}