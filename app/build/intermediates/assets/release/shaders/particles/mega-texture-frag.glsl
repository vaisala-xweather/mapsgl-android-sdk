//precision highp float;
precision mediump float;
varying vec4 outPos; //in
uniform sampler2D u_Texture;
uniform float x_offset;
uniform float y_offset;
uniform float x_scale;
float tex_mult=511.78/516.0;
float pad_mult=2.11/516.0;
void main(void) {


    if(x_scale<1.0) { //Partial code. May differ from JS version...


                      vec2 texCoord;

                      //float tex_mult=512.0/516.0;        float pad_mult=2.0/516.0;

                      //float tex_mult=510.78/516.0;    float pad_mult=2.51/516.0;
                      //texCoord =  vec2((pad_mult)+(outPos.x*(tex_mult))*x_scale +(tex_mult)*x_offset,   (pad_mult)+(outPos.y*(tex_mult))*x_scale+(tex_mult)*y_offset); //padding
                      //texCoord =  vec2((outPos.x)*x_scale +x_offset,   .1+((outPos.y)*x_scale+ y_offset)*.8); //dumb test
                      //texCoord =  vec2((outPos.x)*x_scale +x_offset,   ((outPos.y)*x_scale+ y_offset)); //no padding
                      texCoord =  vec2((outPos.x)*x_scale +x_offset,   (pad_mult)+((outPos.y )*(tex_mult))*x_scale+(tex_mult)*y_offset);


                      gl_FragColor =  texture2D(u_Texture, texCoord);



    } else{

        gl_FragColor=  texture2D(u_Texture, outPos.xy);

    }


}

