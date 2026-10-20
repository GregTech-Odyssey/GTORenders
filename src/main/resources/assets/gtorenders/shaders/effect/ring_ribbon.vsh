#version 150

//COMMON

uniform int Segments;

vec3 ribbonPoint(mat3 basis, int segment, out vec3 side, float width) {
    float angle = TAU * float(segment) / float(Segments);
    vec3 point = Ring0.xyz + basis * vec3(cos(angle) * Ring2.y, 0.0, sin(angle) * Ring2.y);
    vec3 tangent = basis * vec3(-sin(angle), 0.0, cos(angle));
    side = cross(tangent, point);
    side *= width / max(length(side), 0.0001);
    return point;
}

vec3 helixRibbonPoint(float strand, float sweep, out vec3 side, float width) {
    vec3 point = helixPoint(strand, sweep, Ring2.y, 0.0);
    vec3 ahead = helixPoint(strand, sweep + 0.001, Ring2.y, 0.0);
    side = cross(ahead - point, point);
    side *= width * helixFade(helixAlong(sweep)) / max(length(side), 0.0001);
    return point;
}

void main() {
    int local = gl_InstanceID % (Segments * 2);
    int layer = local / Segments;
    int segment = local - layer * Segments;
    float width = layer == 0 ? Ring2.z : Ring2.w;
    vec3 sideA;
    vec3 sideB;
    vec3 a;
    vec3 b;
    if (Ring5.x < 0.5) {
        mat3 basis = ringBasis(Ring2.x);
        a = ribbonPoint(basis, segment, sideA, width);
        b = ribbonPoint(basis, segment + 1, sideB, width);
    } else {
        int strands = int(Ring5.x + 0.5);
        int perStrand = Segments / strands;
        int strand = min(segment / perStrand, strands - 1);
        int piece = segment - strand * perStrand;
        a = helixRibbonPoint(float(strand), float(piece) / float(perStrand), sideA, width);
        b = helixRibbonPoint(float(strand), float(piece + 1) / float(perStrand), sideB, width);
    }
    vec3 quad[4] = vec3[](a + sideA, a - sideA, b - sideB, b + sideB);
    vec2 uv[4] = vec2[](vec2(0.0, 0.5), vec2(1.0, 0.5), vec2(1.0, 0.5), vec2(0.0, 0.5));
    int corner = gl_VertexID;
    if (dot(cross(quad[1] - quad[0], quad[2] - quad[0]), quad[0]) > 0.0) corner = 3 - corner;
    tfPosition = quad[corner];
    tfUV = uv[corner];
    tfColor = vec4(layer == 0 ? Ring3.rgb : Ring4.rgb, 1.0);
}
