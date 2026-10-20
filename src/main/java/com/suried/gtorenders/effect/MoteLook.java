package com.suried.gtorenders.effect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

/**
 * How a mote looks: the GTORenders pixel glow (with an optional bright core), or a mod's own particle sprite drawn with additive blending.
 */
@OnlyIn(Dist.CLIENT)
public record MoteLook(@Nullable MoteSprite sprite, boolean animated, float rollSpeed, float core) {

    public static final MoteLook GLOW = new MoteLook(null, false, 0, 0.3F);

    public static MoteLook glow(float core) {
        return new MoteLook(null, false, 0, core);
    }

    public static MoteLook sprite(MoteSprite sprite) {
        return new MoteLook(sprite, false, 0, 0);
    }

    public static MoteLook animated(MoteSprite sprite) {
        return new MoteLook(sprite, true, 0, 0);
    }

    public static MoteLook spinning(MoteSprite sprite, float rollSpeed) {
        return new MoteLook(sprite, false, rollSpeed, 0);
    }
}
