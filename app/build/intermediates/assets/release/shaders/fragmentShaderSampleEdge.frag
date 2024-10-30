precision highp float;
uniform float u_Alpha;
uniform sampler2D u_Texture;
uniform sampler2D color_band;
varying vec2 v_TexCoord;

const float edgeThreshold = 0.2; // Adjust this value to control edge detection sensitivity
//const float edgeThreshold = 0.02; // Adjust this value to control edge detection sensitivity

void main(void) {
    vec4 texColor = texture2D(u_Texture, v_TexCoord);
    vec2 myVec2 = vec2(texColor.r, 0.0);
    vec4 color2 = texture2D(color_band, myVec2);

    // Sample neighboring pixels
    vec4 left = texture2D(u_Texture, v_TexCoord + vec2(-1.0 / 512.0, 0.0));
    vec4 right = texture2D(u_Texture, v_TexCoord + vec2(1.0 / 512.0, 0.0));
    vec4 top = texture2D(u_Texture, v_TexCoord + vec2(0.0, 1.0 / 512.0));
    vec4 bottom = texture2D(u_Texture, v_TexCoord + vec2(0.0, -1.0 / 512.0));

    // Calculate color differences with neighboring pixels
    float leftDiff = abs(texColor.r - left.r);
    float rightDiff = abs(texColor.r - right.r);
    float topDiff = abs(texColor.r - top.r);
    float bottomDiff = abs(texColor.r - bottom.r);

    // Check if any neighboring pixel exceeds the edge threshold
    bool isEdge = leftDiff > edgeThreshold || rightDiff > edgeThreshold || topDiff > edgeThreshold || bottomDiff > edgeThreshold;

    // Smooth out edges by blending with neighboring pixels
    if (isEdge) {
        vec4 blendedColor = (texColor + left + right + top + bottom) / 5.0;
        gl_FragColor = texture2D(color_band, vec2(blendedColor.r, 0.0));
    } else {
        gl_FragColor = texture2D(color_band, myVec2);
    }
}