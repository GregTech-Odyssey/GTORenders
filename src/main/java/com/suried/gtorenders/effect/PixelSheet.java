package com.suried.gtorenders.effect;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

/**
 * A flat sheet of pixel-glow squares in local block units lying in its own XY plane, uploaded once as static geometry.
 * Draw it on a body part through {@link BodyEffects#draw}.
 */
@OnlyIn(Dist.CLIENT)
public final class PixelSheet {

    private static final int STRIDE = 6;
    private final float[] pixels;
    private final float size, halo, core;
    private VertexBuffer haloBuffer, coreBuffer;

    private PixelSheet(float[] pixels, float size, float halo, float core) {
        this.pixels = pixels;
        this.size = size;
        this.halo = halo;
        this.core = core;
    }

    public static Builder builder(float pixel, float halo, float core) {
        return new Builder(pixel, halo, core);
    }

    public int count() {
        return pixels.length / STRIDE;
    }

    public float x(int index) {
        return pixels[index * STRIDE];
    }

    public float y(int index) {
        return pixels[index * STRIDE + 1];
    }

    public int rgb(int index) {
        int base = index * STRIDE;
        return Math.round(pixels[base + 2] * 255) << 16 | Math.round(pixels[base + 3] * 255) << 8 | Math.round(pixels[base + 4] * 255);
    }

    public float pixel() {
        return size;
    }

    VertexBuffer buffer(boolean corePass) {
        if (haloBuffer == null) {
            haloBuffer = upload(size * halo, false);
            coreBuffer = upload(size * halo * core, true);
        }
        return corePass ? coreBuffer : haloBuffer;
    }

    private VertexBuffer upload(float quad, boolean corePass) {
        int count = count();
        float s = quad * 0.5F;
        var builder = new BufferBuilder(count * 4 * DefaultVertexFormat.NEW_ENTITY.getVertexSize());
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        for (int i = 0; i < count; i++) {
            int base = i * STRIDE;
            float x = pixels[base], y = pixels[base + 1], r = pixels[base + 2], g = pixels[base + 3], b = pixels[base + 4], alpha = pixels[base + 5];
            if (corePass) {
                r += (1 - r) * 0.55F;
                g += (1 - g) * 0.55F;
                b += (1 - b) * 0.55F;
            }
            corner(builder, x - s, y - s, r, g, b, alpha, 0, 1);
            corner(builder, x + s, y - s, r, g, b, alpha, 1, 1);
            corner(builder, x + s, y + s, r, g, b, alpha, 1, 0);
            corner(builder, x - s, y + s, r, g, b, alpha, 0, 0);
        }
        var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.end());
        VertexBuffer.unbind();
        return buffer;
    }

    private static void corner(BufferBuilder builder, float x, float y, float r, float g, float b, float alpha, float u, float v) {
        builder.vertex(x, y, 0).color(r * alpha, g * alpha, b * alpha, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(0, 0, 1).endVertex();
    }

    public static final class Builder {

        private final float pixel, halo, core;
        private final FloatArrayList pixels = new FloatArrayList();

        private Builder(float pixel, float halo, float core) {
            this.pixel = pixel;
            this.halo = halo;
            this.core = core;
        }

        public Builder pixel(float x, float y, int rgb, float alpha) {
            pixels.add(x);
            pixels.add(y);
            pixels.add((rgb >> 16 & 0xFF) / 255F);
            pixels.add((rgb >> 8 & 0xFF) / 255F);
            pixels.add((rgb & 0xFF) / 255F);
            pixels.add(alpha);
            return this;
        }

        public PixelSheet build() {
            return new PixelSheet(pixels.toFloatArray(), pixel, halo, core);
        }
    }
}
