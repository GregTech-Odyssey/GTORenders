package com.suried.gtorenders.effect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.SplittableRandom;

@OnlyIn(Dist.CLIENT)
public final class ParticleRing {

    public static final float PIXEL = 1F / 16F;
    public static final int DATA_WIDTH = 1024;

    private final float majorRadius;
    private final float half;
    private final int count;
    private final float[] phase;
    private final float[] orbit;
    private final float[] driftFrequencyU;
    private final float[] driftFrequencyV;
    private final float[] driftPhaseU;
    private final float[] driftPhaseV;
    private final float[] spin;
    private final float[] spinPhase;
    private final float[] amplitudeU;
    private final float[] amplitudeV;
    private final float[] axisX;
    private final float[] axisY;
    private final float[] axisZ;
    private final int[] tone;
    private int dataTexture = -1;

    public ParticleRing(float majorRadius, float tubeRadius, int count, float minSpeed, float maxSpeed, long seed) {
        this.majorRadius = majorRadius;
        this.half = PIXEL / 2;
        this.count = count;
        phase = new float[count];
        orbit = new float[count];
        driftFrequencyU = new float[count];
        driftFrequencyV = new float[count];
        driftPhaseU = new float[count];
        driftPhaseV = new float[count];
        spin = new float[count];
        spinPhase = new float[count];
        amplitudeU = new float[count];
        amplitudeV = new float[count];
        axisX = new float[count];
        axisY = new float[count];
        axisZ = new float[count];
        tone = new int[count];
        var random = new SplittableRandom(seed);
        int[] order = new int[count];
        for (int i = 0; i < count; i++) order[i] = i;
        for (int i = count - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = order[i];
            order[i] = order[j];
            order[j] = swap;
        }
        float reach = Math.max(0, tubeRadius - half * 1.8F);
        for (int k = 0; k < count; k++) {
            int i = order[k];
            phase[i] = (float) ((k + random.nextDouble()) / count * Math.PI * 2);
            orbit[i] = (float) ((minSpeed + (maxSpeed - minSpeed) * random.nextDouble()) / majorRadius);
            driftFrequencyU[i] = (float) (0.03 + 0.07 * random.nextDouble());
            driftFrequencyV[i] = (float) (0.03 + 0.07 * random.nextDouble());
            driftPhaseU[i] = (float) (random.nextDouble() * Math.PI * 2);
            driftPhaseV[i] = (float) (random.nextDouble() * Math.PI * 2);
            double radius = reach * Math.sqrt(random.nextDouble());
            double angle = random.nextDouble() * Math.PI / 2;
            amplitudeU[i] = (float) (radius * Math.cos(angle));
            amplitudeV[i] = (float) (radius * Math.sin(angle));
            spin[i] = (float) ((0.06 + 0.2 * random.nextDouble()) * (random.nextBoolean() ? 1 : -1));
            spinPhase[i] = (float) (random.nextDouble() * Math.PI * 2);
            double z = random.nextDouble() * 2 - 1, azimuth = random.nextDouble() * Math.PI * 2, planar = Math.sqrt(1 - z * z);
            axisX[i] = (float) (planar * Math.cos(azimuth));
            axisY[i] = (float) (planar * Math.sin(azimuth));
            axisZ[i] = (float) z;
            tone[i] = random.nextInt(EffectPalette.SIZE);
        }
    }

    public int count() {
        return count;
    }

    public float majorRadius() {
        return majorRadius;
    }

    float half() {
        return half;
    }

    int dataTexture() {
        return dataTexture;
    }

    void dataTexture(int id) {
        dataTexture = id;
    }

    int dataRows() {
        return (count * 4 + DATA_WIDTH - 1) / DATA_WIDTH;
    }

    ByteBuffer createData() {
        var data = MemoryUtil.memCalloc(dataRows() * DATA_WIDTH * 16);
        for (int i = 0; i < count; i++) {
            data.putFloat(phase[i]).putFloat(orbit[i]).putFloat(driftFrequencyU[i]).putFloat(driftFrequencyV[i]);
            data.putFloat(driftPhaseU[i]).putFloat(driftPhaseV[i]).putFloat(amplitudeU[i]).putFloat(amplitudeV[i]);
            data.putFloat(spin[i]).putFloat(spinPhase[i]).putFloat(tone[i]).putFloat(0);
            data.putFloat(axisX[i]).putFloat(axisY[i]).putFloat(axisZ[i]).putFloat(0);
        }
        data.clear();
        return data;
    }
}
