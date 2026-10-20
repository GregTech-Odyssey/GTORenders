package com.suried.gtorenders.effect;

import com.suried.gtorenders.GTORenders;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.GlStateManager;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@OnlyIn(Dist.CLIENT)
final class FeedbackProgram {

    private static final String[] VARYINGS = { "tfPosition", "tfColor", "tfUV" };
    private static final String FRAGMENT = "#version 150\nout vec4 fragColor;\nvoid main() {\n    fragColor = vec4(1.0);\n}\n";
    private static final String[] RING_ATTRIBUTES = { "Ring0", "Ring1", "Ring2", "Ring3", "Ring4", "Ring5", "Ring6", "Ring7" };

    final int id;
    final int[] ring = new int[RING_ATTRIBUTES.length];

    private FeedbackProgram(int id) {
        this.id = id;
        for (int i = 0; i < RING_ATTRIBUTES.length; i++) ring[i] = GL20.glGetAttribLocation(id, RING_ATTRIBUTES[i]);
    }

    int uniform(String name) {
        return GL20.glGetUniformLocation(id, name);
    }

    private static String source(String name) throws IOException {
        var manager = Minecraft.getInstance().getResourceManager();
        try (var stream = manager.getResourceOrThrow(GTORenders.id("shaders/effect/" + name)).open()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Nullable
    static FeedbackProgram load(String vertex) {
        try {
            String code = source(vertex).replace("//COMMON", source("ring_common.glsl"));
            int vertexShader = compile(GL20.GL_VERTEX_SHADER, code, vertex);
            int fragmentShader = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT, "feedback.fsh");
            int program = GlStateManager.glCreateProgram();
            GlStateManager.glAttachShader(program, vertexShader);
            GlStateManager.glAttachShader(program, fragmentShader);
            GL30.glTransformFeedbackVaryings(program, VARYINGS, GL30.GL_INTERLEAVED_ATTRIBS);
            GlStateManager.glLinkProgram(program);
            GlStateManager.glDeleteShader(vertexShader);
            GlStateManager.glDeleteShader(fragmentShader);
            if (GlStateManager.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
                GTORenders.LOGGER.error("GTORenders program {} failed to link: {}", vertex, GlStateManager.glGetProgramInfoLog(program, 32768));
                GlStateManager.glDeleteProgram(program);
                return null;
            }
            return new FeedbackProgram(program);
        } catch (IOException e) {
            GTORenders.LOGGER.error("GTORenders program {} failed to load", vertex, e);
            return null;
        }
    }

    private static int compile(int type, String code, String name) {
        int shader = GlStateManager.glCreateShader(type);
        GlStateManager.glShaderSource(shader, Collections.singletonList(code));
        GlStateManager.glCompileShader(shader);
        if (GlStateManager.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            GTORenders.LOGGER.error("GTORenders shader {} failed to compile: {}", name, GlStateManager.glGetShaderInfoLog(shader, 32768));
        }
        return shader;
    }
}
