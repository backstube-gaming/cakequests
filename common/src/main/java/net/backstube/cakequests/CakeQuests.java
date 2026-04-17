package net.backstube.cakequests;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class CakeQuests {
    public static final String MOD_ID = "cakequests";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_SYNC_BYTES = 512 * 1024;

    private CakeQuests() {
    }

    public static void init() {
        LOGGER.info("Cake Quests initialized");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
