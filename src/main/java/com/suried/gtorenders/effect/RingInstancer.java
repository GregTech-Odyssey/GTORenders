package com.suried.gtorenders.effect;

import com.suried.gtorenders.GTORenders;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;

@OnlyIn(Dist.CLIENT)
final class RingInstancer {

    static final int RIBBON_SEGMENTS = 192;
    private static final int RECORD_BYTES = 112;
    private static final int VERTEX_BYTES = 36;
    private static final int VOXEL_VERTICES = 12;
    private static final int QUAD_VERTICES = 4;
    private static final int PALETTE_ROWS = 64;
    private static final int MAX_GROUPS = 16;
    private static final int FULL_BRIGHT = 240;
    private static final int NO_OVERLAY_V = 10;
    private static final ParticleRing[] GROUP_RING = new ParticleRing[MAX_GROUPS];
    private static final int[] GROUP_FIRST = new int[MAX_GROUPS];
    private static final int[] GROUP_SIZE = new int[MAX_GROUPS];
    private static final int[][] PALETTES = new int[PALETTE_ROWS][];
    private static final ByteBuffer PALETTE_PIXELS = MemoryUtil.memAlloc(EffectPalette.SIZE * PALETTE_ROWS * 4);
    private static int state = -1;
    private static FeedbackProgram voxelProgram;
    private static FeedbackProgram glowProgram;
    private static FeedbackProgram ribbonProgram;
    private static int groups;
    private static int paletteRows;
    private static int records;
    private static int[] commandGroup = new int[64];
    private static int[] commandRow = new int[64];
    private static ByteBuffer table = MemoryUtil.memAlloc(64 * RECORD_BYTES);
    private static int tableBuffer = -1;
    private static int paletteTexture = -1;
    private static int simulateVao = -1;
    private static final Output VOXELS = new Output();
    private static final Output GLOWS = new Output();

    private RingInstancer() {}

    private static final class Output {

        private int buffer = -1;
        private int capacity;
        private int vertices;
        private int vao = -1;
        private int program = -1;

        private void reserve(int count) {
            if (buffer == -1) buffer = GlStateManager._glGenBuffers();
            if (count <= capacity) return;
            capacity = Math.max(count, capacity * 2);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) capacity * VERTEX_BYTES, GL15.GL_DYNAMIC_COPY);
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

    static boolean available() {
        if (state < 0) {
            var caps = GL.getCapabilities();
            boolean supported = caps.OpenGL33 || caps.GL_ARB_instanced_arrays;
            if (supported) {
                voxelProgram = FeedbackProgram.load("ring_voxel.vsh");
                glowProgram = FeedbackProgram.load("ring_glow.vsh");
                ribbonProgram = FeedbackProgram.load("ring_ribbon.vsh");
            }
            state = supported && voxelProgram != null && glowProgram != null && ribbonProgram != null ? 1 : 0;
            if (state == 0) GTORenders.LOGGER.error("GTORenders is unavailable on this OpenGL context, machine effects are disabled");
        }
        return state == 1;
    }

    private static void resetUnpack() {
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
    }

    private static int texture(int internalFormat, int width, int height, int type, ByteBuffer pixels) {
        int id = GlStateManager._genTexture();
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(id);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
        resetUnpack();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, GL11.GL_RGBA, type, pixels);
        return id;
    }

    private static void ensureData(ParticleRing ring) {
        if (ring.dataTexture() != -1) return;
        var data = ring.createData();
        ring.dataTexture(texture(GL30.GL_RGBA32F, ParticleRing.DATA_WIDTH, ring.dataRows(), GL11.GL_FLOAT, data));
        MemoryUtil.memFree(data);
    }

    private static int paletteRow(int[] palette) {
        for (int i = 0; i < paletteRows; i++) {
            if (PALETTES[i] == palette) return i;
        }
        if (paletteRows == PALETTE_ROWS) return PALETTE_ROWS - 1;
        int row = paletteRows++;
        PALETTES[row] = palette;
        int base = row * EffectPalette.SIZE * 4;
        for (int i = 0; i < EffectPalette.SIZE; i++) {
            int rgb = palette[i];
            PALETTE_PIXELS.put(base + i * 4, (byte) (rgb >> 16));
            PALETTE_PIXELS.put(base + i * 4 + 1, (byte) (rgb >> 8));
            PALETTE_PIXELS.put(base + i * 4 + 2, (byte) rgb);
            PALETTE_PIXELS.put(base + i * 4 + 3, (byte) 0xFF);
        }
        return row;
    }

    private static void putColor(long address, int rgb, int strength) {
        MemoryUtil.memPutFloat(address, (rgb >> 16 & 0xFF) * strength / 65025F);
        MemoryUtil.memPutFloat(address + 4, (rgb >> 8 & 0xFF) * strength / 65025F);
        MemoryUtil.memPutFloat(address + 8, (rgb & 0xFF) * strength / 65025F);
        MemoryUtil.memPutFloat(address + 12, 0);
    }

    private static void buildTable(List<RenderProFrame.RingCommand> commands, int count) {
        groups = 0;
        paletteRows = 0;
        if (commandGroup.length < count) {
            commandGroup = new int[count * 2];
            commandRow = new int[count * 2];
        }
        for (int c = 0; c < count; c++) {
            var command = commands.get(c);
            int group = -1;
            for (int g = 0; g < groups; g++) {
                if (GROUP_RING[g] == command.ring) {
                    group = g;
                    break;
                }
            }
            if (group < 0) {
                group = groups < MAX_GROUPS ? groups++ : MAX_GROUPS - 1;
                GROUP_RING[group] = command.ring;
                GROUP_SIZE[group] = 0;
            }
            GROUP_SIZE[group]++;
            commandGroup[c] = group;
            commandRow[c] = paletteRow(command.palette);
        }
        int first = 0;
        for (int g = 0; g < groups; g++) {
            GROUP_FIRST[g] = first;
            first += GROUP_SIZE[g];
            GROUP_SIZE[g] = 0;
            ensureData(GROUP_RING[g]);
        }
        records = count;
        if (table.capacity() < count * RECORD_BYTES) table = MemoryUtil.memRealloc(table, count * 2 * RECORD_BYTES);
        long base = MemoryUtil.memAddress(table);
        for (int c = 0; c < count; c++) {
            var command = commands.get(c);
            int g = commandGroup[c];
            int slot = GROUP_FIRST[g] + GROUP_SIZE[g]++;
            long address = base + (long) slot * RECORD_BYTES;
            var style = command.style;
            float breathe = RenderProFrame.breathe(command);
            MemoryUtil.memPutFloat(address, command.x);
            MemoryUtil.memPutFloat(address + 4, command.y);
            MemoryUtil.memPutFloat(address + 8, command.z);
            MemoryUtil.memPutFloat(address + 12, (float) command.time);
            MemoryUtil.memPutFloat(address + 16, command.ring.half() * command.scale);
            MemoryUtil.memPutFloat(address + 20, command.spriteSize);
            MemoryUtil.memPutFloat(address + 24, command.spriteStrength / 255F);
            MemoryUtil.memPutFloat(address + 28, commandRow[c]);
            MemoryUtil.memPutFloat(address + 32, command.axis.ordinal());
            MemoryUtil.memPutFloat(address + 36, command.ring.majorRadius());
            MemoryUtil.memPutFloat(address + 40, style.hazeWidth());
            MemoryUtil.memPutFloat(address + 44, style.coreWidth());
            putColor(address + 48, command.baseColor, (int) (style.hazeStrength() * command.intensity * breathe));
            putColor(address + 64, command.baseColor, (int) (style.coreStrength() * command.intensity * breathe));
            var shape = command.shape;
            MemoryUtil.memPutFloat(address + 80, shape.strands());
            MemoryUtil.memPutFloat(address + 84, shape.height());
            MemoryUtil.memPutFloat(address + 88, shape.turns());
            MemoryUtil.memPutFloat(address + 92, shape.taper());
            MemoryUtil.memPutFloat(address + 96, shape.fadeIn());
            MemoryUtil.memPutFloat(address + 100, shape.fadeOut());
            MemoryUtil.memPutFloat(address + 104, 0);
            MemoryUtil.memPutFloat(address + 108, 0);
        }
        if (tableBuffer == -1) tableBuffer = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, tableBuffer);
        table.position(0);
        table.limit(count * RECORD_BYTES);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, table, GL15.GL_STREAM_DRAW);
        table.clear();
        if (paletteTexture == -1) {
            PALETTE_PIXELS.clear();
            paletteTexture = texture(GL11.GL_RGBA8, EffectPalette.SIZE, PALETTE_ROWS, GL11.GL_UNSIGNED_BYTE, PALETTE_PIXELS);
        } else {
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(paletteTexture);
            resetUnpack();
            PALETTE_PIXELS.position(0);
            PALETTE_PIXELS.limit(EffectPalette.SIZE * paletteRows * 4);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, EffectPalette.SIZE, paletteRows, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PALETTE_PIXELS);
            PALETTE_PIXELS.clear();
        }
    }

    private static void pointRing(FeedbackProgram program, int firstRecord, int divisor) {
        for (int i = 0; i < program.ring.length; i++) {
            int location = program.ring[i];
            if (location < 0) continue;
            GL20.glEnableVertexAttribArray(location);
            GL20.glVertexAttribPointer(location, 4, GL11.GL_FLOAT, false, RECORD_BYTES, (long) firstRecord * RECORD_BYTES + i * 16L);
            GL33.glVertexAttribDivisor(location, divisor);
        }
    }

    private static void useParticles(FeedbackProgram program) {
        GlStateManager._glUseProgram(program.id);
        GL20.glUniform1i(program.uniform("ParticleData"), 0);
        GL20.glUniform1i(program.uniform("PaletteAtlas"), 1);
        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        GlStateManager._bindTexture(paletteTexture);
    }

    private static int capture(Output output, int offsetVertices, int vertices, int instances) {
        int count = vertices * instances;
        GL30.glBindBufferRange(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, output.buffer, (long) offsetVertices * VERTEX_BYTES, (long) count * VERTEX_BYTES);
        GL30.glBeginTransformFeedback(GL11.GL_POINTS);
        GL31.glDrawArraysInstanced(GL11.GL_POINTS, 0, vertices, instances);
        GL30.glEndTransformFeedback();
        return count;
    }

    private static void simulate(Camera camera) {
        int voxelVertices = 0, spriteVertices = 0;
        for (int g = 0; g < groups; g++) {
            int instances = GROUP_SIZE[g] * GROUP_RING[g].count();
            voxelVertices += instances * VOXEL_VERTICES;
            spriteVertices += instances * QUAD_VERTICES;
        }
        int ribbonVertices = records * RIBBON_SEGMENTS * 2 * QUAD_VERTICES;
        VOXELS.reserve(voxelVertices);
        GLOWS.reserve(spriteVertices + ribbonVertices);
        if (simulateVao == -1) simulateVao = GlStateManager._glGenVertexArrays();
        GlStateManager._glBindVertexArray(simulateVao);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, tableBuffer);
        GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
        int voxelOffset = 0, glowOffset = 0;
        useParticles(voxelProgram);
        int voxelCount = voxelProgram.uniform("ParticleCount");
        for (int g = 0; g < groups; g++) {
            var ring = GROUP_RING[g];
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(ring.dataTexture());
            GL20.glUniform1i(voxelCount, ring.count());
            pointRing(voxelProgram, GROUP_FIRST[g], ring.count());
            voxelOffset += capture(VOXELS, voxelOffset, VOXEL_VERTICES, GROUP_SIZE[g] * ring.count());
        }
        useParticles(glowProgram);
        var left = camera.getLeftVector();
        var up = camera.getUpVector();
        GL20.glUniform3f(glowProgram.uniform("CameraLeft"), left.x(), left.y(), left.z());
        GL20.glUniform3f(glowProgram.uniform("CameraUp"), up.x(), up.y(), up.z());
        int glowCount = glowProgram.uniform("ParticleCount");
        for (int g = 0; g < groups; g++) {
            var ring = GROUP_RING[g];
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(ring.dataTexture());
            GL20.glUniform1i(glowCount, ring.count());
            pointRing(glowProgram, GROUP_FIRST[g], ring.count());
            glowOffset += capture(GLOWS, glowOffset, QUAD_VERTICES, GROUP_SIZE[g] * ring.count());
        }
        GlStateManager._glUseProgram(ribbonProgram.id);
        GL20.glUniform1i(ribbonProgram.uniform("Segments"), RIBBON_SEGMENTS);
        pointRing(ribbonProgram, 0, RIBBON_SEGMENTS * 2);
        glowOffset += capture(GLOWS, glowOffset, QUAD_VERTICES, records * RIBBON_SEGMENTS * 2);
        GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
        GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, 0);
        GlStateManager._glUseProgram(0);
        GlStateManager._glBindVertexArray(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        BufferUploader.invalidate();
        VOXELS.vertices = voxelOffset;
        GLOWS.vertices = glowOffset;
    }

    private static void present(Output output, RenderType type, boolean additive) {
        if (output.vertices == 0) return;
        type.setupRenderState();
        var shader = RenderSystem.getShader();
        if (shader == null) {
            type.clearRenderState();
            return;
        }
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
        if (additive) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        }
        output.bindFor(shader);
        int indices = output.vertices / 4 * 6;
        var sequential = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        sequential.bind(indices);
        RenderSystem.drawElements(GL11.GL_TRIANGLES, indices, sequential.type().asGLType);
        GlStateManager._glBindVertexArray(0);
        BufferUploader.invalidate();
        shader.clear();
        type.clearRenderState();
    }

    static void draw(List<RenderProFrame.RingCommand> commands, int count, Camera camera) {
        if (count == 0) return;
        buildTable(commands, count);
        simulate(camera);
        present(VOXELS, RenderProTypes.voxels(), false);
        present(GLOWS, RenderProTypes.glows(), true);
    }
}
