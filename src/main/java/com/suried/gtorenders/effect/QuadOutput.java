package com.suried.gtorenders.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

/**
 * A GPU buffer of quads (position, color, uv as floats) drawn through the shader a render type sets up. Attributes are bound by name,
 * falling back to the iris_ names, so shader pack programs read the same data as the vanilla ones.
 */
@OnlyIn(Dist.CLIENT)
final class QuadOutput {

    static final int VERTEX_BYTES = 36;
    private static final int FULL_BRIGHT = 240;
    private static final int NO_OVERLAY_V = 10;

    int buffer = -1, capacity, vertices;
    private int vao = -1, program = -1;

    void reserve(int count) {
        if (buffer == -1) buffer = GlStateManager._glGenBuffers();
        if (count <= capacity) return;
        capacity = Math.max(count, capacity * 2);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) capacity * VERTEX_BYTES, GL15.GL_DYNAMIC_COPY);
    }

    void upload(ByteBuffer data, int count) {
        if (buffer == -1) buffer = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
        capacity = vertices = count;
    }

    static void uniforms(ShaderInstance shader) {
        for (int i = 0; i < 12; i++) shader.setSampler("Sampler" + i, RenderSystem.getShaderTexture(i));
        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
        if (shader.COLOR_MODULATOR != null) shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        if (shader.FOG_START != null) shader.FOG_START.set(RenderSystem.getShaderFogStart());
        if (shader.FOG_END != null) shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        if (shader.FOG_COLOR != null) shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        if (shader.FOG_SHAPE != null) shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        if (shader.TEXTURE_MATRIX != null) shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        if (shader.GAME_TIME != null) shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        if (shader.SCREEN_SIZE != null) {
            var window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }
        RenderSystem.setupShaderLights(shader);
        shader.apply();
    }

    void draw(ShaderInstance shader) {
        bindFor(shader);
        int indices = vertices / 4 * 6;
        var sequential = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        sequential.bind(indices);
        RenderSystem.drawElements(GL11.GL_TRIANGLES, indices, sequential.type().asGLType);
        GlStateManager._glBindVertexArray(0);
        BufferUploader.invalidate();
        shader.clear();
    }

    private void bindFor(ShaderInstance shader) {
        int id = shader.getId();
        if (vao == -1 || program != id) {
            if (vao != -1) GlStateManager._glDeleteVertexArrays(vao);
            program = id;
            vao = GlStateManager._glGenVertexArrays();
            GlStateManager._glBindVertexArray(vao);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            pointer(id, "Position", 3, 0);
            pointer(id, "Color", 4, 12);
            pointer(id, "UV0", 2, 28);
        } else {
            GlStateManager._glBindVertexArray(vao);
        }
        int overlay = attribute(id, "UV1");
        if (overlay >= 0) GL30.glVertexAttribI2i(overlay, 0, NO_OVERLAY_V);
        int light = attribute(id, "UV2");
        if (light >= 0) GL30.glVertexAttribI2i(light, FULL_BRIGHT, FULL_BRIGHT);
        int normal = attribute(id, "Normal");
        if (normal >= 0) GL20.glVertexAttrib3f(normal, 0, 1, 0);
    }

    private static int attribute(int program, String name) {
        int location = GL20.glGetAttribLocation(program, name);
        return location >= 0 ? location : GL20.glGetAttribLocation(program, "iris_" + name);
    }

    private static void pointer(int program, String name, int size, int offset) {
        int location = attribute(program, name);
        if (location < 0) return;
        GL20.glEnableVertexAttribArray(location);
        GL20.glVertexAttribPointer(location, size, GL11.GL_FLOAT, false, VERTEX_BYTES, offset);
    }
}
