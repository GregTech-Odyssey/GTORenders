package com.suried.gtorenders.effect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Drop-in replacement for simple mod particles: a mote with drag and gravity, or a mote sliding to a target, computed on the GPU.
 * Mod particle providers call this with their own sprite and return no particle, so every existing addParticle call site keeps its look.
 */
@OnlyIn(Dist.CLIENT)
public final class MoteParticles {

    public static final int SHRINK = GpuMotes.SHRINK, PULSE = GpuMotes.PULSE_SIZE, CONSTANT = GpuMotes.CONSTANT;
    public static final int FADE = 1, THROUGH_WALLS = 2;

    private MoteParticles() {}

    public static void spawn(double x, double y, double z, double vx, double vy, double vz, float drag, float gravity, float life, float size, int sizeCurve,
                             int rgb, float alpha, int flags, MoteLook look) {
        if (life <= 0 || size <= 0 || alpha <= 0) return;
        double now = RenderProFrame.now();
        if (Double.isNaN(now)) return;
        GpuMotes.write(GpuMotes.BALLISTIC, x, y, z, vx, vy, vz, now, life, drag, gravity, size, alpha, rgb, sizeCurve, flags & FADE, 0, look.rollSpeed(), 0, 0,
                0, 0, look, (flags & THROUGH_WALLS) != 0);
    }

    public static void slide(double x0, double y0, double z0, double x1, double y1, double z1, float life, float size, int sizeCurve, int rgb, float alpha,
                             int flags, MoteLook look) {
        if (life <= 0 || size <= 0 || alpha <= 0) return;
        double now = RenderProFrame.now();
        if (Double.isNaN(now)) return;
        GpuMotes.write(GpuMotes.SLIDE, x0, y0, z0, x1 - x0, y1 - y0, z1 - z0, now, life, 1, 0, size, alpha, rgb, sizeCurve, flags & FADE, 0, look.rollSpeed(),
                0, 0, 0, 0, look, (flags & THROUGH_WALLS) != 0);
    }
}
