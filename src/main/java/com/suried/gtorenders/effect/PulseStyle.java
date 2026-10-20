package com.suried.gtorenders.effect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record PulseStyle(float speed, float lag, int trail, float trailStep, float trailLife, float size, float alpha, @Nullable BurstStyle arrival) {}
