package com.suried.gtorenders.effect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record RingStyle(float spriteScale, int spriteStrength, float hazeWidth, int hazeStrength, float coreWidth, int coreStrength) {}
