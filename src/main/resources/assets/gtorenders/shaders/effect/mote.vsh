#version 150

uniform float Now;
uniform vec3 CamHi;
uniform vec3 CamLo;
uniform vec3 CameraLeft;
uniform vec3 CameraUp;
uniform float CullDistance;
uniform int CorePass;
uniform int Premultiply;
uniform sampler2D SpriteFrames;

in vec4 Ring0;
in vec4 Ring1;
in vec4 Ring2;
in vec4 Ring3;
in vec4 Ring4;
in vec4 Ring5;
in vec4 Ring6;
in vec4 Ring7;

out vec3 tfPosition;
out vec4 tfColor;
out vec2 tfUV;

const vec2 CORNERS[4] = vec2[](vec2(1.0, -1.0), vec2(-1.0, -1.0), vec2(-1.0, 1.0), vec2(1.0, 1.0));
const int BALLISTIC = 0;
const int SLIDE = 1;
const int PULSE = 2;
const int BURST = 3;
const int FLOW = 4;
const int FADE = 1;
const int ANIMATED = 4;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float edge(float f, float e) {
    return f < e ? f / e : (f > 1.0 - e ? (1.0 - f) / e : 1.0);
}

void hide() {
    tfPosition = vec3(0.0, -100000.0, 0.0);
    tfColor = vec4(0.0);
    tfUV = vec2(0.0);
}

float pulseDistance(float t, float speed, float lag) {
    return speed * (t - lag * (1.0 - exp(-t / lag)));
}

void main() {
    int motion = int(Ring2.w + 0.5);
    int flags = int(Ring6.y + 0.5);
    vec3 origin = (Ring1.xyz - CamHi) + (Ring0.xyz - CamLo);
    float age = Now - Ring0.w;
    float life = max(Ring1.w, 0.0001);
    float f = age / life;
    float alpha = Ring3.w;
    float sizeMul = 1.0;
    vec3 p = origin;
    if (motion == FLOW) {
        float period = max(Ring7.x, 1.0);
        float travel = max(Ring7.y, 1.0);
        float slot = floor((Now + Ring7.z) / period) - Ring7.w;
        float emitted = slot * period - Ring7.z;
        f = (Now - emitted) / travel;
        if (f < 0.0 || f >= 1.0) {
            hide();
            return;
        }
        float s = Ring6.z * 131.0 + slot * 0.6180339;
        vec3 j0 = vec3(hash(s + 1.0), hash(s + 2.0), hash(s + 3.0)) - 0.5;
        vec3 j1 = vec3(hash(s + 4.0), hash(s + 5.0), hash(s + 6.0)) - 0.5;
        p = mix(origin + j0 * Ring3.y, origin + Ring2.xyz + j1 * Ring3.y, f);
        sizeMul = 1.0 + Ring3.x * hash(s + 7.0);
        alpha *= edge(f, 0.18);
        f = 0.5;
    } else {
        if (age < 0.0 || f >= 1.0) {
            hide();
            return;
        }
        if (motion == SLIDE) {
            p = origin + Ring2.xyz * f;
        } else if (motion == PULSE) {
            float delay = Ring7.x;
            float duration = Ring7.w;
            float t = age - delay;
            if (t < 0.0 || t > duration) {
                hide();
                return;
            }
            float len = Ring7.z;
            float d = len > 0.0001 ? min(len, pulseDistance(t, Ring3.x, max(Ring3.y, 0.01))) / len : 0.0;
            p = origin + Ring2.xyz * d;
            sizeMul = Ring7.y;
        } else if (motion == BURST) {
            float ease = 1.0 - (1.0 - f) * (1.0 - f);
            p = origin + Ring2.xyz * ease + vec3(0.0, Ring3.y * age, 0.0);
            alpha *= min(1.0, age / 1.5);
        } else {
            float d = Ring3.x;
            float move, fall;
            if (abs(1.0 - d) < 0.000001) {
                move = age;
                fall = age * (age + 1.0) * 0.5;
            } else {
                float dn = pow(d, age);
                move = (1.0 - dn) / (1.0 - d);
                fall = (age - d * move) / (1.0 - d);
            }
            p = origin + Ring2.xyz * move - vec3(0.0, Ring3.y * fall, 0.0);
        }
        if ((flags & FADE) != 0) alpha *= 1.0 - f;
    }
    if (dot(p, p) > CullDistance * CullDistance || alpha <= 0.004) {
        hide();
        return;
    }
    int curve = int(Ring6.x + 0.5);
    float scale = curve == 0 ? 1.0 - f : (curve == 1 ? (f < 0.5 ? f * 2.0 : 2.0 - f * 2.0) : 1.0);
    float size = Ring3.z * sizeMul * scale;
    vec3 rgb = Ring4.rgb;
    if (CorePass != 0) {
        if (Ring4.a <= 0.0) {
            hide();
            return;
        }
        size *= Ring4.a;
        rgb = mix(rgb, vec3(1.0), 0.55);
    }
    vec2 corner = CORNERS[gl_VertexID];
    float angle = motion == FLOW ? 0.0 : Ring6.z + Ring6.w * age;
    if (angle != 0.0) {
        float c = cos(angle), sn = sin(angle);
        corner = vec2(corner.x * c - corner.y * sn, corner.x * sn + corner.y * c);
    }
    tfPosition = p + (CameraLeft * corner.x + CameraUp * corner.y) * size * 0.5;
    tfColor = Premultiply != 0 ? vec4(rgb * alpha, alpha) : vec4(rgb, alpha);
    vec4 rect = Ring5;
    if ((flags & ANIMATED) != 0) {
        float frames = max(Ring5.y, 1.0);
        float index = min(frames - 1.0, floor(age * (frames - 1.0) / life));
        rect = texelFetch(SpriteFrames, ivec2(int(index), int(Ring5.x)), 0);
    }
    vec2 base = CORNERS[gl_VertexID];
    tfUV = vec2(base.x > 0.0 ? rect.x : rect.z, base.y > 0.0 ? rect.y : rect.w);
}
