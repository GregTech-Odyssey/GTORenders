package com.suried.gtorenders.effect;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@FunctionalInterface
public interface EffectSource {

    void collect(ClientLevel level, double time, double camX, double camY, double camZ);
}
