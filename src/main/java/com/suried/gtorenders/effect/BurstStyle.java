package com.suried.gtorenders.effect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BurstStyle(int count, int life, float radius, float spreadXZ, float spreadY, float rise, float size, float alpha) {}
