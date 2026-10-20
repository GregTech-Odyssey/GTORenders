package com.suried.gtorenders.effect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public final class EffectClock {

    private static final Map<Object, EffectClock> CLOCKS = new WeakHashMap<>();
    private static final double MAX_STEP = 20;

    private final float riseTicks;
    private final float fallTicks;
    private float intensity;
    private double time;
    private double lastNow = Double.NaN;
    private int color = -1;

    private EffectClock(float riseTicks, float fallTicks) {
        this.riseTicks = riseTicks;
        this.fallTicks = fallTicks;
    }

    public static EffectClock of(Object owner, float riseTicks, float fallTicks) {
        var clock = CLOCKS.get(owner);
        if (clock == null) {
            clock = new EffectClock(riseTicks, fallTicks);
            CLOCKS.put(owner, clock);
        }
        return clock;
    }

    public void update(boolean active, double now) {
        double step = Double.isNaN(lastNow) ? 0 : Math.max(0, Math.min(now - lastNow, MAX_STEP));
        lastNow = now;
        if (active) intensity = (float) Math.min(1, intensity + step / riseTicks);
        else intensity = (float) Math.max(0, intensity - step / fallTicks);
        float speed = intensity * intensity * (3 - 2 * intensity);
        time += step * speed;
    }

    public boolean visible() {
        return intensity > 0;
    }

    public float intensity() {
        return intensity;
    }

    public double time() {
        return time;
    }

    public int color(int current, int fallback) {
        if (current != -1) color = current;
        return color == -1 ? fallback : color;
    }
}
