precision highp float;
uniform float u_Alpha;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform bool colorize;
uniform float time;
uniform sampler2D u_Texture;
uniform sampler2D colorScale;
varying vec2 vUV;
//new custom below
varying vec4 outPos; //in

void main(void){


    vec2 coord = vec2(outPos.x, outPos.y);


    vec2 movingPos = vUV;
    //vec2 movingPos = vec2(0.0, 0.0);


    //movingPos.x= movingPos.x+=time*512.0;

    //vec4 oColor = texture2D(u_Texture, vUV);
    vec2 tempVec = vec2(outPos.x, 1.0-outPos.y);

   // vec2 movedUV = vec2(vUV.x * x_scale + x_offset -(time*4.1), vUV.y * y_scale + y_offset );
    //movingPos.x = movingPos.x +time*10.0;
    vec4 texColor = texture2D(u_Texture, vUV); // was vUV
    //vec4 texColor = texture2D(u_Texture, tempVec); // was vUV
    //texColor.r = vUV.x;
    //texColor.g = vUV.x;
    //texColor.b = vUV.x;


    gl_FragColor = texColor;
    //gl_FragColor = texture2D(u_Texture, movedUV);
    //gl_FragColor = texture2D(u_Texture, vUV); // color directly under.... doesnt change because uv moving....
    //gl_FragColor = texture2D(u_Texture, coord); // color updates when particle moves //glGetUniformLocation -> error in gl: GL_INVALID_VALUE



}
/*
uniform sampler2D uTexture;
uniform float time;

uniform float u_Alpha;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform bool colorize;

uniform sampler2D u_Texture;
uniform sampler2D colorScale;
varying vec2 vUV;

void main() {
    vec2 coord = vec2(outPos.x, outPos.y);
    //gl_FragColor = vec4((outPos.x*.5)+.5, 0.0, .1, 1.0);
    gl_FragColor = texture2D(uTexture, coord); // color updates when particle moves

}
*/