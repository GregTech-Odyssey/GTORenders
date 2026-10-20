package com.suried.gtorenders.effect;

import com.suried.gtorenders.GTORenders;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTORenders.MOD_ID, value = Dist.CLIENT)
public final class RenderProFrame {

    private static final double TWO_PI = Math.PI * 2;
    private static final ArrayList<RingCommand> RINGS = new ArrayList<>();
    private static final ArrayList<EffectSource> SOURCES = new ArrayList<>();
    private static int ringCount;
    private static boolean collecting;
    private static double frameTime;
    private static ClientLevel lastLevel;

    private RenderProFrame() {}

    public static final class RingCommand {

        ParticleRing ring;
        float x, y, z;
        Direction.Axis axis;
        double time;
        float scale;
        int[] palette;
        float spriteSize;
        int spriteStrength;
        RingStyle style;
        int baseColor;
        float intensity;
        RingShape shape;
    }

    public static boolean collecting(Level level) {
        return collecting && level == Minecraft.getInstance().level && RingInstancer.available();
    }

    static boolean open() {
        return collecting && Minecraft.getInstance().level != null;
    }

    public static void addSource(EffectSource source) {
        if (!SOURCES.contains(source)) SOURCES.add(source);
    }

    public static double now() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return Double.NaN;
        return mc.level.getGameTime() + mc.getFrameTime();
    }

    public static double frameTime() {
        return frameTime;
    }

    public static int lastMoteCount() {
        return GpuMotes.lastDrawn();
    }

    public static int activeTimedEffects() {
        return GpuMotes.live(frameTime);
    }

    static float breathe(RingCommand command) {
        return 0.9F + 0.1F * Mth.sin((float) (command.time * 0.21 % TWO_PI));
    }

    public static void ring(ParticleRing ring, RingStyle style, double worldX, double worldY, double worldZ, Direction.Axis axis,
                            double time, float intensity, int baseColor, int[] palette) {
        ring(ring, style, RingShape.CLOSED, worldX, worldY, worldZ, axis, time, intensity, baseColor, palette);
    }

    public static void ring(ParticleRing ring, RingStyle style, RingShape shape, double worldX, double worldY, double worldZ, Direction.Axis axis,
                            double time, float intensity, int baseColor, int[] palette) {
        if (ringCount == RINGS.size()) RINGS.add(new RingCommand());
        var command = RINGS.get(ringCount++);
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float scale = intensity * intensity * (3 - 2 * intensity);
        command.ring = ring;
        command.x = (float) (worldX - camera.x);
        command.y = (float) (worldY - camera.y);
        command.z = (float) (worldZ - camera.z);
        command.axis = axis;
        command.time = time;
        command.scale = scale;
        command.palette = palette;
        command.spriteSize = ParticleRing.PIXEL * style.spriteScale() * scale;
        command.spriteStrength = (int) (style.spriteStrength() * intensity);
        command.style = style;
        command.baseColor = baseColor;
        command.intensity = intensity;
        command.shape = shape;
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        var stage = event.getStage();
        if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
            collecting = true;
            ringCount = 0;
            GpuMotes.beginFrame();
            frameTime = now();
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            var level = Minecraft.getInstance().level;
            if (level != null && !SOURCES.isEmpty()) {
                var cam = event.getCamera().getPosition();
                for (int i = 0; i < SOURCES.size(); i++) SOURCES.get(i).collect(level, frameTime, cam.x, cam.y, cam.z);
            }
            collecting = false;
            flush(event);
            flushMotes(event);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level != lastLevel) {
            GpuMotes.clear();
            ParticleEmitters.clear();
            lastLevel = level;
        }
        if (level == null || mc.isPaused()) return;
        ParticleEmitters.tick(level);
    }

    private static void flushMotes(RenderLevelStageEvent event) {
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(event.getPoseStack().last().pose());
        RenderSystem.applyModelViewMatrix();
        GpuMotes.flush(frameTime, event.getCamera());
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static void flush(RenderLevelStageEvent event) {
        if (ringCount == 0) return;
        RenderProTypes.ensureTextures();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(event.getPoseStack().last().pose());
        RenderSystem.applyModelViewMatrix();
        RingInstancer.draw(RINGS, ringCount, event.getCamera());
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        ringCount = 0;
    }
}
