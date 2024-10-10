precision highp float;

attribute vec2 position;
attribute vec2 uv;
attribute float particleIndex;
attribute vec2 reference;

uniform sampler2D particles;
uniform sampler2D tPositionX;
uniform sampler2D tPositionY;
uniform sampler2D tAlpha;
uniform vec2 particleSize;
uniform mat4 dataMatrix;

uniform vec2 resolution;
uniform float dpr;
uniform float pitch;
uniform float cameraToCenterDistance;
uniform mat4 modelViewMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform mat4 projectionMatrix;
uniform mat4 labelPlaneMatrix;
uniform mat4 glCoordMatrix;

varying vec2 vUv;
varying vec2 vParticlePos;
varying vec4 vColor;
varying float vAlpha;
varying float vRotation;
varying float vDegrees;

const float PI = 3.14159265359;

vec2 transformData(vec2 pos, mat4 matrix) {
    vec4 transformed = matrix * vec4(pos.xy, 1.0, 1.0);
    return transformed.xy / transformed.w;
}

//#include <decode_float_rgba>
    highp float decodeFloatRGBA(vec4 v) {
    float a = floor(v.r * 255.0 + 0.5);
    float b = floor(v.g * 255.0 + 0.5);
    float c = floor(v.b * 255.0 + 0.5);
    float d = floor(v.a * 255.0 + 0.5);

    float exponent = a - 127.0;
    float sign = 1.0 - mod(d, 2.0)*2.0;
    float mantissa = float(a > 0.0)
    + b / 256.0
    + c / 65536.0
    + floor(d / 2.0) / 8388608.0;

    return sign * mantissa * exp2(exponent);
    }
//end #include <decode_float_rgba>

//#include <transform>
    mat3 scale(vec2 v) {
    return mat3(
    1.0 / v.x, 0.0, 0.0,
    0.0, 1.0 / v.y, 0.0,
    0.0, 0.0, 1.0
    );
    }

    mat3 translate(vec2 v) {
    return mat3(
    1.0, 0.0, v.x,
    0.0, 1.0, v.y,
    0.0, 0.0, 1.0
    );
    }

    vec2 rotate(vec2 v, float a) {
    float s = sin(a);
    float c = cos(a);
    mat2 m = mat2(c, -s, s, c);
    return m * v;
    }

    vec3 rotateX(vec3 v, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    mat3 m = mat3(
    1.0, 0.0, 0.0,
    0.0, c, s,
    0.0, -s, c
    );
    return m * v;
    }
//end #include <transform>


void main() {
    // get particle texture position based on index
    vec2 fuv = reference;
    vec2 anchor = vec2(
        decodeFloatRGBA(texture2D(tPositionX, fuv)),
        decodeFloatRGBA(texture2D(tPositionY, fuv))
    );
    anchor = transformData(anchor, dataMatrix);

    // get particle state from texture (r = alpha, g = angle in degrees from 0..1)
    vec4 state = texture2D(tAlpha, fuv);

    highp vec4 projectedAnchor = projectionMatrix * modelViewMatrix * vec4(anchor, 0.0, 1.0);

    vec2 size = particleSize;
    vec4 glyph = vec4(-size.x*0.5, -size.y*0.5, size.x, size.y);
    float symbolScale = 1.0;

    float angle = 0.0;
    float deg = state.g * 360.;
    float rad = deg * (PI / 180.);
    float valueU = sin(rad);
    float valueV = cos(rad);
    vec2 vector = vec2(valueU, valueV);
    vec2 velocity = vector;

#ifdef EXPRESSION_ANGLE
    angle = atan(velocity.x, -velocity.y);
#else
    angle = atan(velocity.x, -velocity.y);
#endif
    angle -= PI / 2.0;
    
#ifdef STYLE_ARROW
    angle -= PI / 2.0;
#endif

    if (angle < 0.) angle += PI * 2.;

    //#include <symbol_vert_position>
        #ifdef SIZE_ATTENUATION
            float cameraToAnchorDistance = projectedAnchor.w;
        float distanceRatio = cameraToCenterDistance / cameraToAnchorDistance;
        // distanceRatio = cameraToAnchorDistance / cameraToCenterDistance;
        highp float perspectiveRatio = clamp(0.5 + 0.5 * distanceRatio, 0.0, 4.0);
        symbolScale *= perspectiveRatio;
        #endif

            // vec4 scaledGlyph = glyph * (1.0 / EXTENT);
        vec2 glyphOffset = (glyph.xy + position.xy * glyph.zw) * symbolScale;

        highp float symbolRotation = 0.0;
        #ifdef ROTATE_WITH_MAP
            vec4 offsetProjectedAnchor = projectionMatrix * modelViewMatrix * vec4(adjustedAnchor.xy + vec2(1.0, 0.0), 0., 1.);
        vec2 a = projectedAnchor.xy / projectedAnchor.w;
        vec2 b = offsetProjectedAnchor.xy / offsetProjectedAnchor.w;
        float aspectRatio = resolution.x / resolution.y;
        // Hack to make the aspect ratio closer to 1.0 when pitch is high so that symbols remain tile-aligned
        aspectRatio = mix(1.0, aspectRatio, PI * 0.333 - pitch);
        symbolRotation = atan((b.y - a.y) / aspectRatio, b.x - a.x) * -1.0;
        #endif

        highp float angleSin = sin(angle + symbolRotation);
        highp float angleCos = cos(angle + symbolRotation);
        mat2 rotationMatrix = mat2(angleCos, -angleSin, angleSin, angleCos);

        highp vec4 projectedPosition = labelPlaneMatrix * projectedAnchor;
        float z = 0.0;
        vec3 pointPosition = vec3(glyphOffset, 0.0);
        pointPosition.xy *= rotationMatrix;
        #ifdef PITCH_WITH_MAP
            z = projectedAnchor.z / projectedAnchor.w;
        pointPosition = rotateX(pointPosition, pitch);
        #endif
            gl_Position = glCoordMatrix * vec4((projectedPosition.xy / projectedPosition.w + pointPosition.xy), z, 1.0);
    //end #include <symbol_vert_position>

    vUv = uv;
    vAlpha = state.r;
    vRotation = state.g;
    vParticlePos = anchor;
    vDegrees = deg;
}