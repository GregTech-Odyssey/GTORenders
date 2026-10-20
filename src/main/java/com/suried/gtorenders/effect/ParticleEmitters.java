package com.suried.gtorenders.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;

/**
 * Vanilla particle emitters for effects that need the original particle textures: one-shot gaussian bursts and timed emitters ticked on the client.
 */
@OnlyIn(Dist.CLIENT)
public final class ParticleEmitters {

    static final int MAX_EMITTERS = 1024;

    private static final class Emitter {

        ParticleOptions particle;
        double x, y, z, dx, dy, dz, speed;
        int ticks, chance;
    }

    private static final ArrayList<Emitter> ACTIVE = new ArrayList<>();
    private static final ArrayList<Emitter> POOL = new ArrayList<>();

    private ParticleEmitters() {}

    public static void burst(ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        var level = Minecraft.getInstance().level;
        if (level != null) spawn(level, particle, x, y, z, count, dx, dy, dz, speed);
    }

    public static void emitter(ParticleOptions particle, double x, double y, double z, int ticks, int chance, double dx, double dy, double dz,
                               double speed) {
        if (ticks <= 0 || ACTIVE.size() >= MAX_EMITTERS) return;
        var e = POOL.isEmpty() ? new Emitter() : POOL.remove(POOL.size() - 1);
        e.particle = particle;
        e.x = x;
        e.y = y;
        e.z = z;
        e.dx = dx;
        e.dy = dy;
        e.dz = dz;
        e.speed = speed;
        e.ticks = ticks;
        e.chance = Math.max(1, chance);
        ACTIVE.add(e);
    }

    static void tick(ClientLevel level) {
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            var e = ACTIVE.get(i);
            if (e.chance <= 1 || level.random.nextInt(e.chance) == 0) spawn(level, e.particle, e.x, e.y, e.z, 1, e.dx, e.dy, e.dz, e.speed);
            if (--e.ticks <= 0) remove(i);
        }
    }

    static void clear() {
        for (int i = ACTIVE.size() - 1; i >= 0; i--) remove(i);
    }

    private static void remove(int i) {
        int last = ACTIVE.size() - 1;
        var e = ACTIVE.get(i);
        ACTIVE.set(i, ACTIVE.get(last));
        ACTIVE.remove(last);
        e.particle = null;
        POOL.add(e);
    }

    private static void spawn(ClientLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz,
                              double speed) {
        var random = level.random;
        if (count == 0) {
            level.addParticle(particle, false, x, y, z, speed * dx, speed * dy, speed * dz);
            return;
        }
        for (int i = 0; i < count; i++) {
            level.addParticle(particle, false, x + random.nextGaussian() * dx, y + random.nextGaussian() * dy, z + random.nextGaussian() * dz,
                    random.nextGaussian() * speed, random.nextGaussian() * speed, random.nextGaussian() * speed);
        }
    }
}
