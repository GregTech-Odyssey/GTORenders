package com.suried.gtorenders.effect;

import com.suried.gtorenders.GTORenders;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Effects worn on player models: registered effects place {@link PixelSheet}s on body parts from a layer the library adds to every player renderer.
 * Sheets keep the part's real pose and are drawn after particles, once every entity batch is flushed, so depth decides what covers what.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTORenders.MOD_ID, value = Dist.CLIENT)
public final class BodyEffects {

    private static final ArrayList<Effect> EFFECTS = new ArrayList<>();
    private static final ArrayList<PixelSheet> SHEETS = new ArrayList<>();
    private static final ArrayList<Matrix4f> POSES = new ArrayList<>();
    private static float[] tints = new float[48];
    private static final Vector3f SCRATCH = new Vector3f();
    private static int count;
    private static boolean inLevel;

    private BodyEffects() {}

    @FunctionalInterface
    public interface Effect {

        void render(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, float partialTick, float ageInTicks);
    }

    public static void register(Effect effect) {
        if (!EFFECTS.contains(effect)) EFFECTS.add(effect);
    }

    public static boolean inLevel() {
        return inLevel;
    }

    public static void draw(PixelSheet sheet, PoseStack poseStack) {
        draw(sheet, poseStack, 1, 1, 1);
    }

    public static void draw(PixelSheet sheet, PoseStack poseStack, float red, float green, float blue) {
        if (!inLevel) return;
        if (count == POSES.size()) {
            POSES.add(new Matrix4f());
            SHEETS.add(null);
            if (tints.length < count * 3 + 3) tints = Arrays.copyOf(tints, tints.length * 2);
        }
        POSES.get(count).set(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
        tints[count * 3] = red;
        tints[count * 3 + 1] = green;
        tints[count * 3 + 2] = blue;
        SHEETS.set(count++, sheet);
    }

    public static boolean toWorld(PoseStack poseStack, float x, float y, float z, Vector3d out) {
        if (!inLevel) return false;
        var view = poseStack.last().pose().transformPosition(x, y, z, SCRATCH);
        RenderSystem.getInverseViewRotationMatrix().transform(view);
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        out.set(camera.x + view.x, camera.y + view.y, camera.z + view.z);
        return true;
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        var stage = event.getStage();
        if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
            inLevel = true;
            count = 0;
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            flush();
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            inLevel = false;
        }
    }

    private static void flush() {
        if (count == 0) return;
        RenderProTypes.ensureTextures();
        var projection = RenderSystem.getProjectionMatrix();
        pass(RenderProTypes.motes(), false, projection);
        pass(RenderProTypes.cores(), true, projection);
        for (int i = 0; i < count; i++) SHEETS.set(i, null);
        count = 0;
    }

    private static void pass(RenderType type, boolean core, Matrix4f projection) {
        type.setupRenderState();
        RenderSystem.disableCull();
        var shader = RenderSystem.getShader();
        if (shader != null) {
            for (int i = 0; i < count; i++) {
                var buffer = SHEETS.get(i).buffer(core);
                RenderSystem.setShaderColor(tints[i * 3], tints[i * 3 + 1], tints[i * 3 + 2], 1);
                buffer.bind();
                buffer.drawWithShader(POSES.get(i), projection, shader);
            }
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        RenderSystem.enableCull();
        type.clearRenderState();
    }

    private static final class Layer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

        private Layer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!inLevel) return;
            for (int i = 0, size = EFFECTS.size(); i < size; i++) EFFECTS.get(i).render(player, getParentModel(), poseStack, partialTick, ageInTicks);
        }
    }

    @Mod.EventBusSubscriber(modid = GTORenders.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {

        private Registration() {}

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer = event.getSkin(skin);
                if (renderer != null) renderer.addLayer(new Layer(renderer));
            }
        }
    }
}
