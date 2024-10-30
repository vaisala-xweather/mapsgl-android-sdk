precision highp float;
uniform float opacity;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform bool colorize;

uniform sampler2D u_Texture;
uniform sampler2D colorScale;
varying vec2 vUV;


void main(void){

    //vec4 texColor = texture2D(u_Texture, v_TexCoord); //simple
    vec4 texColor = texture2D(u_Texture, vec2(vUV.x * x_scale+x_offset, vUV.y * y_scale+y_offset)); //stretch smaller part of texture if needed

    //if (v_TexCoord.x > 0.99 || v_TexCoord.y > 0.99 ) {       discard;   } //lines
    //if (texColor.r>.6) {     discard;  } //punch-out

    vec2 myVec2 = vec2(texColor.r, 0.0);

    //gl_FragColor = texture2D(colorScale, vUV); // show color band
    gl_FragColor = texColor; // show original image
    gl_FragColor*= opacity;
    //gl_FragColor = texture2D(colorScale, myVec2); // final colorized image

}




