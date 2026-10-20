package com.suried.gtorenders.effect;

import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.SplittableRandom;

@OnlyIn(Dist.CLIENT)
public final class LinkEffects {

    private static final SplittableRandom RANDOM = new SplittableRandom(0x2545F4914F6CDD1DL);

    private LinkEffects() {}

    static float unit(long seed, int salt) {
        long z = seed + 0x9E3779B97F4A7C15L * (salt + 1);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 40) * 0x1.0p-24F;
    }

    public static void flow(double x0, double y0, double z0, double x1, double y1, double z1, int rgb, long seed, FlowStyle style, MoteLook look) {
        if (!RenderProFrame.open()) return;
        GpuMotes.rel(RenderProFrame.frameTime());
        int period = Math.max(1, style.period());
        int travel = Math.max(1, style.travel());
        int lanes = (travel + period - 1) / period + 1;
        float phase = unit(seed, 0) * period;
        float hashSeed = unit(seed, 1) * 1000;
        for (int k = 0; k < lanes; k++) {
            GpuMotes.write(GpuMotes.FLOW, x0, y0, z0, x1 - x0, y1 - y0, z1 - z0, 0, 0, style.sizeJitter(), style.jitter(), style.size(), style.alpha(), rgb,
                    GpuMotes.CONSTANT, 0, hashSeed, 0, period, travel, phase, k, look, false);
        }
    }

    static double pulseDuration(double length, PulseStyle style) {
        double v = Math.max(0.01, style.speed()), lag = Math.max(0.01, style.lag());
        double t = length / v + lag;
        for (int i = 0; i < 6; i++) {
            double e = Math.exp(-t / lag);
            double s = v * (t - lag * (1 - e)) - length;
            double ds = v * (1 - e);
            if (ds <= 1e-6) break;
            t -= s / ds;
        }
        return Math.max(1, t);
    }

    public static void pulse(double x0, double y0, double z0, double x1, double y1, double z1, int rgb, PulseStyle style, MoteLook look) {
        double now = RenderProFrame.now();
        if (Double.isNaN(now)) return;
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float duration = (float) pulseDuration(length, style);
        float lag = Math.max(0.01F, style.lag());
        GpuMotes.write(GpuMotes.PULSE, x0, y0, z0, dx, dy, dz, now, duration, style.speed(), lag, style.size(), style.alpha(), rgb, GpuMotes.CONSTANT, 0, 0,
                look.rollSpeed(), 0, 1, length, duration, look, false);
        float trailLife = Math.max(0.5F, style.trailLife());
        float w = style.size() * 0.45F;
        for (int i = 1; i <= style.trail(); i++) {
            float delay = i * style.trailStep();
            float a = 1 - delay / trailLife;
            if (a <= 0) break;
            double ox = (RANDOM.nextFloat() - 0.5F) * w, oy = (RANDOM.nextFloat() - 0.5F) * w, oz = (RANDOM.nextFloat() - 0.5F) * w;
            GpuMotes.write(GpuMotes.PULSE, x0 + ox, y0 + oy, z0 + oz, dx, dy, dz, now, duration + delay, style.speed(), lag, style.size(), style.alpha() * a, rgb,
                    GpuMotes.CONSTANT, 0, 0, look.rollSpeed(), delay, 0.7F + 0.3F * a, length, duration, look, false);
        }
        var arrival = style.arrival();
        if (arrival != null) BurstEffects.burstAt(x1, y1, z1, rgb, arrival, look, now + duration);
    }

    public static void beam(double x0, double y0, double z0, double x1, double y1, double z1, int rgb, BeamStyle style, MoteLook look) {
        double now = RenderProFrame.now();
        if (Double.isNaN(now)) return;
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 1e-4) return;
        float step = Math.max(0.05F, style.spacing());
        int n = Math.min(512, (int) (length / step));
        float hue = RANDOM.nextFloat();
        for (int i = 0; i <= n; i++) {
            double d = (i + (RANDOM.nextFloat() - 0.5F) * style.spacingJitter()) * step / length;
            if (d < 0 || d > 1) continue;
            int color = rgb;
            if (style.rainbow()) {
                int c = Mth.hsvToRgb(Mth.frac(hue + (float) d), 1, 1);
                int r = Math.min(255, (c >> 16 & 0xFF) + 102), g = Math.min(255, (c >> 8 & 0xFF) + 102), b = Math.min(255, (c & 0xFF) + 102);
                color = r << 16 | g << 8 | b;
            }
            GpuMotes.write(GpuMotes.BALLISTIC, x0 + dx * d, y0 + dy * d, z0 + dz * d, 0, 0, 0, now, Math.max(1, style.life()), 1, 0, style.size(),
                    style.alpha(), color, look.animated() ? GpuMotes.SHRINK : GpuMotes.CONSTANT, look.animated() ? 0 : GpuMotes.FADE, 0, look.rollSpeed(), 0, 0, 0, 0, look, false);
        }
    }
}
