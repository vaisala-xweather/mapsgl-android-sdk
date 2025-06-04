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


void main(void){

    /*
    vec2 uv = gl_FragCoord.xy / 512.0;
    float randomValue = fract(sin(dot(uv * 1000.0, vec2(12.9898, 78.233))) * 43758.5453);
    if (randomValue < 0.01) {
        //fragColor = texture(uTexture, uv);
    } else {    discard;  }
    */

    vec4 oColor = texture2D(u_Texture, vUV);

    float moveX = time * 2.3 *(oColor.g-.5); // Adjust the speed of movement

    float moveY = time * 2.3 *(oColor.b-.5); // Adjust the speed of movement

    vec2 movedUV = vec2(vUV.x * x_scale + x_offset+moveX, vUV.y * y_scale + y_offset + moveY);



    vec4 texColor = texture2D(u_Texture, movedUV);



    //vec4 texColor = texture2D(u_Texture, v_TexCoord); //simple
    //vec4 texColor = texture2D(u_Texture, vec2(vUV.x * x_scale+x_offset, vUV.y * y_scale+y_offset)); //stretch smaller part of texture if needed
    //texColor.rgb = vec3(1.0-time, 1.0-time, 1.0-time); // Set text color to white
   // gl_FragColor = texture2D(colorScale, vUV); // show color band
    gl_FragColor = texColor; // show original image


}




