package com.suried.gtorenders.effect;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Whether an Iris/Oculus shader pack is active, looked up through the Iris API by reflection so no shader mod is required.
 * Mods use it to switch custom core shaders, which shader packs do not support, to vanilla render types.
 */
@OnlyIn(Dist.CLIENT)
public final class ShaderPacks {

    private static MethodHandle inUse;
    private static Object api;
    private static boolean resolved;

    private ShaderPacks() {}

    public static boolean inUse() {
        if (!resolved) {
            resolved = true;
            try {
                var type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = type.getMethod("getInstance").invoke(null);
                inUse = MethodHandles.publicLookup().findVirtual(type, "isShaderPackInUse", MethodType.methodType(boolean.class));
            } catch (ReflectiveOperationException | LinkageError e) {
                inUse = null;
            }
        }
        if (inUse == null) return false;
        try {
            return (boolean) inUse.invoke(api);
        } catch (Throwable e) {
            return false;
        }
    }
}
