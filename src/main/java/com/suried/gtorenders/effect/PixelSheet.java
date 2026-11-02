package com.suried.gtorenders.effect;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.lwjgl.system.MemoryUtil;


/**
 * A flat sheet of pixel-glow squares in local block units lying in its own XY plane, uploaded once as static geometry.
 * Draw it on a body part through {@link BodyEffects#draw}.
 */
@OnlyIn(Dist.CLIENT)
public final class PixelSheet {

    private static final int STRIDE = 6;
    private final float[] pixels;
    private final float size, halo, core;
    private static final Emitter EMITTER = new Emitter();
    private QuadOutput haloBuffer, coreBuffer;

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

    QuadOutput buffer(boolean corePass) {
        if (haloBuffer == null) {
            haloBuffer = upload(size * halo, false);
            coreBuffer = upload(size * halo * core, true);
        }
        return corePass ? coreBuffer : haloBuffer;
    }

    void emit(VertexConsumer consumer, PoseStack.Pose pose, boolean corePass, float red, float green, float blue) {
        EMITTER.consumer = consumer;
        EMITTER.pose = pose;
        EMITTER.red = red;
        EMITTER.green = green;
        EMITTER.blue = blue;
        quads(corePass ? size * halo * core : size, corePass ? 0.15F : 0, false, EMITTER);
        EMITTER.consumer = null;
        EMITTER.pose = null;
    }

    private QuadOutput upload(float quad, boolean corePass) {
        int count = count();
        var data = MemoryUtil.memAlloc(count * 4 * QuadOutput.VERTEX_BYTES);
        quads(quad, corePass ? 0.55F : 0, true, (x, y, r, g, b, alpha, u, v) -> data.putFloat(x).putFloat(y).putFloat(0).putFloat(r).putFloat(g).putFloat(b).putFloat(alpha).putFloat(u).putFloat(v));
        data.flip();
        var output = new QuadOutput();
        output.upload(data, count * 4);
        MemoryUtil.memFree(data);
        return output;
    }

    private void quads(float quad, float whiten, boolean premultiply, Corner out) {
        float s = quad * 0.5F;
        for (int i = 0, count = count(); i < count; i++) {
            int base = i * STRIDE;
            float x = pixels[base], y = pixels[base + 1], r = pixels[base + 2], g = pixels[base + 3], b = pixels[base + 4], alpha = pixels[base + 5];
            r += (1 - r) * whiten;
            g += (1 - g) * whiten;
            b += (1 - b) * whiten;
            if (premultiply) {
                r *= alpha;
                g *= alpha;
                b *= alpha;
            }
            out.accept(x - s, y - s, r, g, b, alpha, 0, 1);
            out.accept(x + s, y - s, r, g, b, alpha, 1, 1);
            out.accept(x + s, y + s, r, g, b, alpha, 1, 0);
            out.accept(x - s, y + s, r, g, b, alpha, 0, 0);
        }
    }

    @FunctionalInterface
    private interface Corner {

        void accept(float x, float y, float r, float g, float b, float alpha, float u, float v);
    }

    private static final class Emitter implements Corner {

        VertexConsumer consumer;
        PoseStack.Pose pose;
        float red, green, blue;
        private final float[] quad = new float[32];
        private int corners;

        @Override
        public void accept(float x, float y, float r, float g, float b, float alpha, float u, float v) {
            int base = corners * 8;
            quad[base] = x;
            quad[base + 1] = y;
            quad[base + 2] = r * red;
            quad[base + 3] = g * green;
            quad[base + 4] = b * blue;
            quad[base + 5] = alpha;
            quad[base + 6] = u;
            quad[base + 7] = v;
            if (++corners < 4) return;
            corners = 0;
            for (int c = 0; c < 4; c++) vertex(c, 1);
            for (int c = 3; c >= 0; c--) vertex(c, -1);
        }

        private void vertex(int corner, float facing) {
            int base = corner * 8;
            consumer.vertex(pose.pose(), quad[base], quad[base + 1], 0).color(quad[base + 2], quad[base + 3], quad[base + 4], quad[base + 5])
                    .uv(quad[base + 6], quad[base + 7]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.normal(), 0, 0, facing).endVertex();
        }
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
