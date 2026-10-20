package com.suried.gtorenders.geometry;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * Axis-aligned sprite faces in pixel units (16 per block), tiled on the 16-pixel grid so liquids and panels line up across blocks.
 * Writes position, color, uv, overlay, light and normal; elements missing from the buffer's format are skipped by the buffer.
 */
@OnlyIn(Dist.CLIENT)
public final class SpriteQuads {

    public static final int DOWN = 1, UP = 2, NORTH = 4, SOUTH = 8, WEST = 16, EAST = 32;
    public static final int SIDES = NORTH | SOUTH | WEST | EAST;
    public static final int ALL = SIDES | UP | DOWN;

    private static final float P = 1F / 16F;

    private SpriteQuads() {}

    public static int bit(Direction face) {
        return 1 << face.get3DDataValue();
    }

    public static void box(VertexConsumer buffer, Matrix4f pose, TextureAtlasSprite sprite, float x0, float y0, float z0, float x1, float y1, float z1,
                           int faces, int argb, int light) {
        for (var face : Direction.values()) {
            if ((faces & bit(face)) != 0) face(buffer, pose, sprite, face, x0, y0, z0, x1, y1, z1, argb, light);
        }
    }

    public static void face(VertexConsumer buffer, Matrix4f pose, TextureAtlasSprite sprite, Direction face, float x0, float y0, float z0, float x1,
                            float y1, float z1, int argb, int light) {
        switch (face) {
            case UP -> horizontal(buffer, pose, sprite, x0, z0, x1, z1, y1, true, argb, light);
            case DOWN -> horizontal(buffer, pose, sprite, x0, z0, x1, z1, y0, false, argb, light);
            case NORTH -> vertical(buffer, pose, sprite, face, x0, x1, y0, y1, z0, argb, light);
            case SOUTH -> vertical(buffer, pose, sprite, face, x0, x1, y0, y1, z1, argb, light);
            case WEST -> vertical(buffer, pose, sprite, face, z0, z1, y0, y1, x0, argb, light);
            case EAST -> vertical(buffer, pose, sprite, face, z0, z1, y0, y1, x1, argb, light);
        }
    }

    private static void horizontal(VertexConsumer buffer, Matrix4f pose, TextureAtlasSprite sprite, float x0, float z0, float x1, float z1, float y,
                                   boolean up, int argb, int light) {
        for (float a = x0; a < x1;) {
            float ta = tile(a), a1 = Math.min(x1, ta + 16);
            for (float b = z0; b < z1;) {
                float tb = tile(b), b1 = Math.min(z1, tb + 16);
                float u0 = sprite.getU(a - ta), u1 = sprite.getU(a1 - ta), v0 = sprite.getV(b - tb), v1 = sprite.getV(b1 - tb);
                if (up) {
                    vertex(buffer, pose, a, y, b, u0, v0, argb, light, 0, 1, 0);
                    vertex(buffer, pose, a, y, b1, u0, v1, argb, light, 0, 1, 0);
                    vertex(buffer, pose, a1, y, b1, u1, v1, argb, light, 0, 1, 0);
                    vertex(buffer, pose, a1, y, b, u1, v0, argb, light, 0, 1, 0);
                } else {
                    vertex(buffer, pose, a, y, b, u0, v0, argb, light, 0, -1, 0);
                    vertex(buffer, pose, a1, y, b, u1, v0, argb, light, 0, -1, 0);
                    vertex(buffer, pose, a1, y, b1, u1, v1, argb, light, 0, -1, 0);
                    vertex(buffer, pose, a, y, b1, u0, v1, argb, light, 0, -1, 0);
                }
                b = b1;
            }
            a = a1;
        }
    }

    private static void vertical(VertexConsumer buffer, Matrix4f pose, TextureAtlasSprite sprite, Direction face, float h0, float h1, float y0,
                                 float y1, float plane, int argb, int light) {
        float nx = face.getStepX(), nz = face.getStepZ();
        for (float a = h0; a < h1;) {
            float ta = tile(a), a1 = Math.min(h1, ta + 16);
            for (float y = y0; y < y1;) {
                float ty = tile(y), yb = Math.min(y1, ty + 16);
                float u0 = sprite.getU(a - ta), u1 = sprite.getU(a1 - ta), v0 = sprite.getV(16 - (yb - ty)), v1 = sprite.getV(16 - (y - ty));
                switch (face) {
                    case NORTH -> quad(buffer, pose, argb, light, nx, nz, a, y, plane, a, yb, plane, a1, yb, plane, a1, y, plane, u0, v1, u0, v0, u1, v0, u1, v1);
                    case SOUTH -> quad(buffer, pose, argb, light, nx, nz, a1, y, plane, a1, yb, plane, a, yb, plane, a, y, plane, u1, v1, u1, v0, u0, v0, u0, v1);
                    case WEST -> quad(buffer, pose, argb, light, nx, nz, plane, y, a1, plane, yb, a1, plane, yb, a, plane, y, a, u1, v1, u1, v0, u0, v0, u0, v1);
                    default -> quad(buffer, pose, argb, light, nx, nz, plane, y, a, plane, yb, a, plane, yb, a1, plane, y, a1, u0, v1, u0, v0, u1, v0, u1, v1);
                }
                y = yb;
            }
            a = a1;
        }
    }

    private static float tile(float v) {
        return (float) Math.floor(v / 16F + 1e-4F) * 16F;
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose, int argb, int light, float nx, float nz, float xa, float ya, float za, float xb,
                             float yb, float zb, float xc, float yc, float zc, float xd, float yd, float zd, float ua, float va, float ub, float vb,
                             float uc, float vc, float ud, float vd) {
        vertex(buffer, pose, xa, ya, za, ua, va, argb, light, nx, 0, nz);
        vertex(buffer, pose, xb, yb, zb, ub, vb, argb, light, nx, 0, nz);
        vertex(buffer, pose, xc, yc, zc, uc, vc, argb, light, nx, 0, nz);
        vertex(buffer, pose, xd, yd, zd, ud, vd, argb, light, nx, 0, nz);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, float x, float y, float z, float u, float v, int argb, int light, float nx,
                               float ny, float nz) {
        buffer.vertex(pose, x * P, y * P, z * P).color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
    }
}
