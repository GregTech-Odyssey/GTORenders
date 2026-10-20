package com.suried.gtorenders.effect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record RingShape(int strands, float height, float turns, float taper, float fadeIn, float fadeOut) {

    public static final RingShape CLOSED = new RingShape(0, 0, 0, 0, 0, 0);

    public static RingShape helix(int strands, float height, float turns, float taper, float fadeIn, float fadeOut) {
        return new RingShape(strands, height, turns, Math.max(0, Math.min(taper, 0.95F)), fadeIn, fadeOut);
    }
}
