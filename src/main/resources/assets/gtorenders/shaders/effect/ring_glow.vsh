#version 150

//COMMON

uniform vec3 CameraLeft;
uniform vec3 CameraUp;

const vec2 CORNERS[4] = vec2[](vec2(1.0, -1.0), vec2(-1.0, -1.0), vec2(-1.0, 1.0), vec2(1.0, 1.0));

void main() {
    int index = gl_InstanceID % ParticleCount;
    vec4 motion = particle(index, 0);
    vec4 drift = particle(index, 1);
    vec4 spin = particle(index, 2);
    vec2 corner = CORNERS[gl_VertexID];
    vec3 center = ringPoint(motion, drift);
    tfPosition = center + (CameraLeft * corner.x + CameraUp * corner.y) * Ring1.y * pathScale;
    tfColor = vec4(paletteColor(spin.z) * Ring1.z * pathScale, 1.0);
    tfUV = vec2(corner.x > 0.0 ? 0.0 : 1.0, corner.y > 0.0 ? 0.0 : 1.0);
}
