precision mediump float;
uniform float u_Alpha;
uniform sampler2D u_Texture;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
uniform float y_scale;
uniform float noData;
uniform float mult;
uniform float add;
uniform int mode;
uniform int channel1;
uniform int channel2;
uniform int interpolation;
uniform float outputRangeMin;
uniform float outputRangeMax;
uniform float drawRangeMin;
uniform float drawRangeMax;
uniform float dataRangeMin;
uniform float dataRangeMax;
uniform float sampleDataMin;
uniform float sampleDataMax;
uniform sampler2D dataTexture;
uniform sampler2D dataTextureNext;
uniform sampler2D colorScale;
varying vec2 vUV;

void main(void){
 vec4 texColor = texture2D(u_Texture, vUV);
 gl_FragColor = texColor * u_Alpha;

}



