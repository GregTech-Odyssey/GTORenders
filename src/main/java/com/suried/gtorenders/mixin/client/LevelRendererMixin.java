package com.suried.gtorenders.mixin.client;

import com.suried.gtorenders.effect.MoteParticles;

import net.minecraft.client.renderer.LevelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @ModifyConstant(method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;",
                    constant = @Constant(doubleValue = 1024.0D))
    private double gtorenders$spawnDistance(double original) {
        return MoteParticles.SPAWN_DISTANCE * MoteParticles.SPAWN_DISTANCE;
    }
}
