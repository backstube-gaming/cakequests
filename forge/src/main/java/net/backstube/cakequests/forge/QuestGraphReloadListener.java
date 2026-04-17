package net.backstube.cakequests.forge;

import com.google.gson.JsonElement;
import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.data.QuestBookDefinition;
import net.backstube.cakequests.data.QuestGraphParser;
import net.backstube.cakequests.data.QuestMainConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class QuestGraphReloadListener extends SimpleJsonResourceReloadListener {
    private static QuestBookDefinition activeBook = QuestBookDefinition.EMPTY;
    private static QuestMainConfig activeConfig = QuestMainConfig.DEFAULT;

    public QuestGraphReloadListener() {
        super(QuestGraphParser.GSON, CakeQuests.MOD_ID + "/quest_graphs");
    }

    public static QuestBookDefinition activeBook() {
        return activeBook;
    }

    public static QuestMainConfig activeConfig() {
        return activeConfig;
    }

    private static QuestMainConfig loadMainConfig(ResourceManager resourceManager) {
        ResourceLocation id = CakeQuests.id("cakequests-main.json");
        if (!resourceManager.hasResource(id)) {
            return QuestMainConfig.DEFAULT;
        }
        try {
            Resource resource = resourceManager.getResource(id);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return QuestMainConfig.fromJson(QuestGraphParser.GSON.fromJson(reader, com.google.gson.JsonObject.class));
            } finally {
                resource.close();
            }
        } catch (Exception ex) {
            CakeQuests.LOGGER.warn("Using default Cake Quests main config because {} could not be read", id, ex);
            return QuestMainConfig.DEFAULT;
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        activeConfig = loadMainConfig(resourceManager);
        activeBook = QuestGraphParser.parseTabs(elements);
        CakeQuests.LOGGER.info("Loaded quest graph {} with {} tabs from datapacks", activeBook.hash(), activeBook.tabs().size());
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            CakeQuestsForgeNetwork.sendToAll(activeBook, activeConfig);
        }
    }
}
