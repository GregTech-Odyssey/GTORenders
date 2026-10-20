uniform sampler2D ParticleData;
uniform sampler2D PaletteAtlas;
uniform int ParticleCount;

in vec4 Ring0;
in vec4 Ring1;
in vec4 Ring2;
in vec4 Ring3;
in vec4 Ring4;
in vec4 Ring5;
in vec4 Ring6;

out vec3 tfPosition;
out vec4 tfColor;
out vec2 tfUV;

vec4 particle(int index, int part) {
    int texel = index * 4 + part;
    return texelFetch(ParticleData, ivec2(texel % 1024, texel / 1024), 0);
}

mat3 ringBasis(float axis) {
    if (axis < 0.5) return mat3(0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
    if (axis < 1.5) return mat3(1.0);
    return mat3(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0);
}

const float TAU = 6.28318530718;

float pathScale = 1.0;

float helixLength() {
    float around = Ring5.z * TAU * Ring2.y;
    return sqrt(around * around + Ring5.y * Ring5.y);
}

float helixFade(float along) {
    return smoothstep(0.0, max(Ring6.x, 0.0001), along) * smoothstep(0.0, max(Ring6.y, 0.0001), 1.0 - along);
}

float helixAlong(float sweep) {
    float taper = Ring5.w;
    if (taper < 0.0001) return sweep;
    float lead = 1.0 + taper;
    return (lead - sqrt(max(lead * lead - 4.0 * taper * sweep, 0.0))) / (2.0 * taper);
}

vec3 helixPoint(float strand, float sweep, float radial, float lift) {
    float theta = TAU * (strand / Ring5.x + Ring5.z * sweep);
    return Ring0.xyz + ringBasis(Ring2.x) * vec3(cos(theta) * radial, helixAlong(sweep) * Ring5.y + lift, sin(theta) * radial);
}

vec3 ringPoint(vec4 motion, vec4 drift) {
    float time = Ring0.w;
    float radial = Ring2.y + drift.z * sin(motion.z * time + drift.x);
    float lift = drift.w * sin(motion.w * time + drift.y);
    if (Ring5.x < 0.5) {
        float theta = motion.x + motion.y * time;
        return Ring0.xyz + ringBasis(Ring2.x) * vec3(cos(theta) * radial, lift, sin(theta) * radial);
    }
    float slot = motion.x / TAU * Ring5.x;
    float strand = floor(slot);
    float sweep = fract(slot - strand + motion.y * Ring2.y / helixLength() * time);
    pathScale = helixFade(helixAlong(sweep));
    return helixPoint(strand, sweep, radial, lift);
}

vec3 paletteColor(float tone) {
    return texelFetch(PaletteAtlas, ivec2(int(tone + 0.5), int(Ring1.w + 0.5)), 0).rgb;
}
