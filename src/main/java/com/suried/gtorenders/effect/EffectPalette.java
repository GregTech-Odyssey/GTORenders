package com.suried.gtorenders.effect;

import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.SplittableRandom;

@OnlyIn(Dist.CLIENT)
public final class EffectPalette {

    public static final int SIZE = 24;
    private static final long SEED = 0x6A7E5L;
    private static final float HOT_CHANCE = 0.16F;
    private static final float HUE_JITTER = 0.045F;
    private static final int CACHE_LIMIT = 64;
    private static final Int2ObjectOpenHashMap<int[]> CACHE = new Int2ObjectOpenHashMap<>();

    private EffectPalette() {}

    public static int[] shades(int rgb) {
        rgb &= 0xFFFFFF;
        var colors = CACHE.get(rgb);
        if (colors == null) {
            if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();
            colors = build(rgb);
            CACHE.put(rgb, colors);
        }
        return colors;
    }

    private static int[] build(int rgb) {
        float red = (rgb >> 16 & 0xFF) / 255F, green = (rgb >> 8 & 0xFF) / 255F, blue = (rgb & 0xFF) / 255F;
        float max = Math.max(red, Math.max(green, blue)), min = Math.min(red, Math.min(green, blue));
        float delta = max - min;
        float hue = 0;
        if (delta > 0) {
            if (max == red) hue = ((green - blue) / delta) / 6F;
            else if (max == green) hue = ((blue - red) / delta + 2) / 6F;
            else hue = ((red - green) / delta + 4) / 6F;
            if (hue < 0) hue += 1;
        }
        float saturation = max == 0 ? 0 : delta / max;
        var random = new SplittableRandom(SEED);
        var colors = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            float h = hue + (float) (random.nextDouble() - 0.5) * 2 * HUE_JITTER;
            h -= Mth.floor(h);
            float s = Mth.clamp(saturation * (0.7F + 0.4F * (float) random.nextDouble()), 0, 1);
            float v = 0.72F + 0.28F * (float) random.nextDouble();
            int color = Mth.hsvToRgb(h, s, v);
            if (random.nextDouble() < HOT_CHANCE) {
                float white = 0.55F + 0.3F * (float) random.nextDouble();
                int r = (int) Mth.lerp(white, color >> 16 & 0xFF, 255);
                int g = (int) Mth.lerp(white, color >> 8 & 0xFF, 255);
                int b = (int) Mth.lerp(white, color & 0xFF, 255);
                color = r << 16 | g << 8 | b;
            }
            colors[i] = color;
        }
        return colors;
    }
}
