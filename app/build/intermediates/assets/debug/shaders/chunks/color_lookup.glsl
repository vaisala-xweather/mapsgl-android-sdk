vec4 colorLookup(float value, float size, sampler2D ramp) {
    // vec2 pos = vec2(fract(size * value), floor(size * value) / size);
    // snap position to nearest pixel to avoid artifacts if value happens to fall at or
    // close to pixel boundaries
    //float x = value+.001;
    //float s = 1.0 / size;
    //x = floor((value / s) + 0.5) * s;
    return texture2D(ramp, vec2(value+.001, 0.5));
}