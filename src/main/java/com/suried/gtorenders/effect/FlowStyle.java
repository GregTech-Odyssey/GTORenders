package com.suried.gtorenders.effect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record FlowStyle(int period, int travel, float size, float sizeJitter, float jitter, float alpha) {}
