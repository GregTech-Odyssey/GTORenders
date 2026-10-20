#version 150

//COMMON

mat3 spinMatrix(vec3 a, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    float t = 1.0 - c;
    return mat3(
        t * a.x * a.x + c, t * a.x * a.y + s * a.z, t * a.x * a.z - s * a.y,
        t * a.x * a.y - s * a.z, t * a.y * a.y + c, t * a.y * a.z + s * a.x,
        t * a.x * a.z + s * a.y, t * a.y * a.z - s * a.x, t * a.z * a.z + c);
}

const vec2 CORNERS[4] = vec2[](vec2(-1.0, -1.0), vec2(1.0, -1.0), vec2(1.0, 1.0), vec2(-1.0, 1.0));

void main() {
    int index = gl_InstanceID % ParticleCount;
    vec4 motion = particle(index, 0);
    vec4 drift = particle(index, 1);
    vec4 spin = particle(index, 2);
    vec3 spinAxis = particle(index, 3).xyz;
    mat3 rotation = spinMatrix(spinAxis, spin.x * Ring0.w + spin.y);
    vec3 center = ringPoint(motion, drift);
    int face = gl_VertexID / 4;
    vec2 corner = CORNERS[gl_VertexID - face * 4];
    vec3 normal = rotation[face];
    if (dot(normal, center) > 0.0) normal = -normal;
    vec3 tangent = rotation[(face + 1) % 3];
    vec3 bitangent = cross(normal, tangent);
    tfPosition = center + (normal + corner.x * tangent + corner.y * bitangent) * Ring1.x * pathScale;
    float shade = 0.75 + 0.25 * normal.y + 0.05 * (abs(normal.z) - abs(normal.x));
    tfColor = vec4(paletteColor(spin.z) * shade, 1.0);
    tfUV = vec2(0.5);
}
