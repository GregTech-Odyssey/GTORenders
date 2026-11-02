package com.suried.gtorenders.effect;

import com.suried.gtorenders.GTORenders;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

@OnlyIn(Dist.CLIENT)
public final class RenderProTypes {

    public static final ResourceLocation GLOW_TEXTURE = GTORenders.id("effect/glow");
    public static final ResourceLocation WHITE_TEXTURE = GTORenders.id("effect/white");
    public static final ResourceLocation MOTE_TEXTURE = GTORenders.id("effect/mote");
    private static final int GLOW_SIZE = 16;
    private static final int GLOW_STEPS = 5;
    private static boolean texturesReady;

    private RenderProTypes() {}

    public static RenderType voxels() {
        return RenderType.entitySolid(WHITE_TEXTURE);
    }

    public static RenderType glows() {
        return RenderType.eyes(GLOW_TEXTURE);
    }

    public static RenderType motes() {
        return RenderType.eyes(MOTE_TEXTURE);
    }

    public static RenderType cores() {
        return RenderType.eyes(WHITE_TEXTURE);
    }

    public static RenderType bodyGlows() {
        return Types.BODY_GLOWS;
    }

    public static RenderType bodyCores() {
        return Types.BODY_CORES;
    }

    public static void ensureTextures() {
        if (texturesReady) return;
        texturesReady = true;
        var glow = new NativeImage(GLOW_SIZE, GLOW_SIZE, false);
        float center = (GLOW_SIZE - 1) / 2F;
        for (int x = 0; x < GLOW_SIZE; x++) {
            for (int y = 0; y < GLOW_SIZE; y++) {
                float dx = (x - center) / (GLOW_SIZE / 2F), dy = (y - center) / (GLOW_SIZE / 2F);
                float falloff = Math.max(0, 1 - (float) Math.sqrt(dx * dx + dy * dy));
                float stepped = (float) Math.ceil(falloff * falloff * GLOW_STEPS) / GLOW_STEPS;
                glow.setPixelRGBA(x, y, Math.round(stepped * 255) << 24 | 0xFFFFFF);
            }
        }
        var mote = new NativeImage(GLOW_SIZE, GLOW_SIZE, false);
        for (int x = 0; x < GLOW_SIZE; x++) {
            for (int y = 0; y < GLOW_SIZE; y++) {
                int a = glow.getPixelRGBA(x, y) >>> 24;
                mote.setPixelRGBA(x, y, a << 24 | a << 16 | a << 8 | a);
            }
        }
        var white = new NativeImage(1, 1, false);
        white.setPixelRGBA(0, 0, 0xFFFFFFFF);
        var textures = Minecraft.getInstance().getTextureManager();
        textures.register(GLOW_TEXTURE, new DynamicTexture(glow));
        textures.register(WHITE_TEXTURE, new DynamicTexture(white));
        textures.register(MOTE_TEXTURE, new DynamicTexture(mote));
    }

    private static final class Types extends RenderType {

        private static final RenderType BODY_GLOWS = depthGlow("gtorenders_body_glows", GLOW_TEXTURE);
        private static final RenderType BODY_CORES = depthGlow("gtorenders_body_cores", WHITE_TEXTURE);

        private Types() {
            super(null, null, null, 0, false, false, null, null);
        }

        private static RenderType depthGlow(String name, ResourceLocation texture) {
            return create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                    CompositeState.builder().setShaderState(RENDERTYPE_EYES_SHADER).setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
        }
    }
}
