package com.suried.gtorenders;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(GTORenders.MOD_ID)
public final class GTORenders {

    public static final String MOD_ID = "gtorenders";
    public static final Logger LOGGER = LoggerFactory.getLogger("GTORenders");

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
