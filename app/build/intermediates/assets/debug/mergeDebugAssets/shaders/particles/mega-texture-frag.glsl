//precision highp float;
precision mediump float;
varying vec4 outPos; //in
uniform sampler2D u_Texture;
uniform sampler2D dataTextureNext;
uniform float x_offset;
uniform float y_offset;
uniform float x_offset2;
uniform float y_offset2;
uniform float x_scale;
uniform float x_scale2;
uniform float dataMeld;
float tex_mult=512.0/516.0;
float pad_mult=2.0/516.0;

void main(void) {
    vec2 texCoord;
    vec2 texCoord2;
    vec4 color1;
    vec4 color2;
    if (x_scale < 1.0) { //Partial code. May differ from JS version...
                         texCoord = vec2(outPos.x * x_scale + x_offset, outPos.y * x_scale + y_offset);
                         color1 = texture2D(u_Texture, texCoord);
    }
    else {
        color1 = texture2D(u_Texture, outPos.xy);
    }

    if (x_scale2 < 1.0) { //Partial code. May differ from JS version...
                         texCoord2 = vec2(outPos.x * x_scale2 + x_offset2, outPos.y * x_scale2 + y_offset2);
                         color2 = texture2D(dataTextureNext, texCoord2);
    }
    else {
        color2 = texture2D(dataTextureNext, outPos.xy);
    }


    gl_FragColor = mix(color1, color2, dataMeld);



}
