package net.backstube.cakequests.forge;

import com.google.gson.JsonElement;
import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.data.QuestBookDefinition;
import net.backstube.cakequests.data.QuestGraphParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;

public class QuestGraphReloadListener extends SimpleJsonResourceReloadListener {
    private static QuestBookDefinition activeBook = QuestBookDefinition.EMPTY;

    public QuestGraphReloadListener() {
        super(QuestGraphParser.GSON, CakeQuests.MOD_ID + "/quest_graphs");
    }

    public static QuestBookDefinition activeBook() {
        return activeBook;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        activeBook = QuestGraphParser.parseTabs(elements);
        CakeQuests.LOGGER.info("Loaded quest graph {} with {} tabs from datapacks", activeBook.hash(), activeBook.tabs().size());
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            CakeQuestsForgeNetwork.sendToAll(activeBook);
        }
    }
}
