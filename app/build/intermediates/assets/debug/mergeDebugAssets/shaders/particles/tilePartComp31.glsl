#version 310 es
precision highp float;

// Define work group size (adjust based on performance/device capabilities)
// Common sizes are 64, 128, 256. Check GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS
layout (local_size_x = 128, local_size_y = 1, local_size_z = 1) in;

// --- Uniforms ---
uniform sampler2D uTexture; // Renamed for clarity
uniform float speedMultC;
uniform float vectorOffsetC;
uniform float tx_scaleC; // Texture transform for velocity lookup
uniform float ty_scaleC;
uniform float tx_offsetC;
uniform float ty_offsetC;
uniform int bufferItemsPerTileC; // Particles per mesh/tile
uniform int currentTileBindingPointC; // Index for the current tile being processed
uniform int drawNumC; // the number of particles actually being drawn per tile
//uniform int totalParticleCountC; // Total particles across all tiles

// --- SSBOs ---
// Input: Initial static positions (where particles spawn)
layout(std430, binding = 0) readonly buffer StaticBuffer {
    vec2 initialPositions[];
} staticData;

// Input/Output: Dynamic particle state (current position, lifetime)
layout(std430, binding = 1) buffer DynamicBuffer {
    // x, y = current position [0,1] range
    // z = current lifetime/dropCount [0, dropMax] range
    // w = potential alpha (calculated here) or random seed
    vec4 particleState[];
} dynamicData;

// Output: Data ready for rendering (final position, alpha)
layout(std430, binding = 2) writeonly buffer RenderBuffer {
    // x, y = render position [0,1] range (usually same as dynamicData.xy)
    // z = alpha [0,1] range
    // w = unused (or could store texture coords if needed)
    vec4 renderInfo[];
} renderData;


// --- Global constants (matching original VS) ---
const float speedFactor = (.002 * 1.6); // Pre-calculate part of speedCalc
// Respawn / Lifetime constants
// const float dropIncrement = 0.001; // How much lifetime increases per step
// const float respawnThreshold = 0.05; // Fade-in/out duration relative to dropMax


// Simple pseudo-random function (can refine if needed)
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}


void main() {
    // Get the unique index for this compute shader invocation
    uint globalInvocationID = gl_GlobalInvocationID.x;

    if(int(globalInvocationID) > drawNumC ) return;

    // Calculate the absolute index into the SSBOs for the current tile
    // Assumes dispatch is done per-tile
    int bufferOffset = bufferItemsPerTileC * currentTileBindingPointC;
    uint absoluteIndex = uint(bufferOffset) + globalInvocationID;
    uint staticIndex = globalInvocationID;

    // Bounds check: Ensure we don't process more particles than exist for this tile
    // OR more than total particles if doing single dispatch (adjust logic if single dispatch)
    //if (globalInvocationID >= uint(bufferItemsPerTileC) || absoluteIndex >= uint(totalParticleCountC)) {
    //    return; // Exit if out of bounds for this dispatch/tile or total
   // }

    // --- Read Data ---
    vec2 initialPos = staticData.initialPositions[staticIndex]; // Read initial spawn pos, was using absoluteIndex
    vec4 currentState = dynamicData.particleState[absoluteIndex]; // Read current pos/lifetime

    // --- Simulation Logic (ported from original VS) ---
    vec2 currentPos = currentState.xy;
    float dropCount = currentState.z;
    float particleRand = currentState.w; // Use w component to store particle's random seed

    // Initialize state on first run (or if reset)
    if (currentState.x == 0.0 && currentState.y == 0.0 && currentState.z == 0.0) {
        currentPos = initialPos;
        dropCount = 0.0;
        // Store a random value per particle for consistent behavior
        particleRand = random(initialPos);
        currentState.w = particleRand; // Store it back
    }

    float speedCalc = speedFactor * speedMultC *2.0;
    float dropMax = 0.25 + particleRand * 0.3; // Use stored random value

    // Code to drop faster particles faster (optional, ported)
    vec2 startTUV = vec2((initialPos.x * tx_scaleC) + tx_offsetC, (initialPos.y * ty_scaleC) + ty_offsetC);
    vec4 startColor = texture(uTexture, startTUV); // Sample initial velocity field
    float velocityMagSq = (startColor.g * startColor.g) + (startColor.b * startColor.b);
    if (velocityMagSq > 0.4) {
        dropMax -= (velocityMagSq - 0.4); // Faster particles have shorter lifetime before respawn
        dropMax = max(0.05, dropMax); // Ensure dropMax doesn't become too small/negative
    }

    // Decide whether to move or respawn
    if (dropCount < dropMax) { // Move particle
                               vec2 tuv = vec2((currentPos.x * tx_scaleC) + tx_offsetC, (currentPos.y * ty_scaleC) + ty_offsetC);
                               vec4 velocityColor = texture(uTexture, tuv); // Sample velocity field at current pos
                               vec2 dir;
                               dir.x = (velocityColor.g - vectorOffsetC);
                               dir.y = -(velocityColor.b - vectorOffsetC); // Original had '-' for Y

                               currentPos.x += dir.x * speedCalc;
                               currentPos.y += dir.y * speedCalc;

                               // Wrap particles around boundaries (simple toroidal wrap)
                               // currentPos = fract(currentPos); // Keeps values in [0, 1) range
                               //if (currentPos.x < 0.0) currentPos.x += 1.0; // These actually cause seams...


                               dropCount += 0.001; // Increment lifetime (original increment value)
    } else { // Respawn particle
             // Respawn near initial position with some randomness
             currentPos.x = initialPos.x + particleRand * 0.01; // Smaller respawn spread?
             currentPos.y = initialPos.y + particleRand * 0.01;
             currentPos = fract(currentPos); // Ensure respawn is within bounds
             dropCount = 0.0; // Reset lifetime
    }

    // --- Calculate Alpha (ported from original VS) ---
    float alpha = 1.0;
    float fadeDuration = 0.05; // Duration of fade in/out relative to dropMax scale
    if (dropCount < fadeDuration) {
        alpha = dropCount / fadeDuration; // Fade in
    } else if (dropCount > (dropMax - fadeDuration)) {
        alpha = (dropMax - dropCount) / fadeDuration; // Fade out
    }
    alpha = clamp(alpha, 0.0, 1.0);
    //alpha = 1.0;


    // --- Write Output ---
    // Update dynamic state buffer
    dynamicData.particleState[absoluteIndex] = vec4(currentPos, dropCount, particleRand);

    // Write data needed for rendering
    renderData.renderInfo[absoluteIndex] = vec4(currentPos, alpha, 0.0); // pos.xy, alpha, unused
}