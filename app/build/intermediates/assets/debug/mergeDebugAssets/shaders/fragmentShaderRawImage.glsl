precision highp float;
uniform float opacity;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
//uniform float y_scale;
uniform sampler2D u_Texture;
uniform sampler2D colorScale;
varying vec2 vUV;
/*
uniform float dataMeld;
uniform float x_offset2;
uniform float y_offset2;
uniform float x_scale2;
*/

void main(void){
    vec4 texColor;



    if (x_scale > .9) {
        texColor = texture2D(u_Texture, vUV);
    } else {
        //if (dataMeld != 1.0) {
            texColor = texture2D(u_Texture, vec2(vUV.x * x_scale + x_offset, vUV.y * x_scale + y_offset)); //stretch smaller part of texture if needed
        //} else { // Texture 2
        //    texColor = texture2D(u_Texture, vec2(vUV.x * x_scale2 + x_offset2, vUV.y * x_scale2 + y_offset2)); //stretch smaller part of texture if needed
       // }
    }

    //if (vUV.x > 0.99 || vUV.y > 0.99 ) {       discard;   } // Grid lines for testing.
    gl_FragColor = texColor; // show original image
    gl_FragColor*= opacity;
}