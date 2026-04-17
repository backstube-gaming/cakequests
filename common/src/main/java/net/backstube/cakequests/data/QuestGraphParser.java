package net.backstube.cakequests.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.backstube.cakequests.CakeQuests;
import net.minecraft.resources.ResourceLocation;

import java.io.Reader;
import java.util.*;

public final class QuestGraphParser {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private QuestGraphParser() {
    }

    public static QuestBookDefinition parseBook(String json) {
        JsonObject object = GSON.fromJson(json, JsonObject.class);
        QuestBookDefinition book = QuestBookDefinition.fromJson(object == null ? new JsonObject() : object);
        validate(book, "sync");
        return book;
    }

    public static QuestMainConfig parseMainConfig(String json) {
        JsonObject object = GSON.fromJson(json, JsonObject.class);
        return QuestMainConfig.fromJson(object == null ? new JsonObject() : object);
    }

    public static QuestTabDefinition parseTab(ResourceLocation id, Reader reader) {
        JsonObject object = GSON.fromJson(reader, JsonObject.class);
        QuestTabDefinition tab = QuestTabDefinition.fromJson(id.getPath().replace('/', '_'), object == null ? new JsonObject() : object);
        validateTab(tab, id.toString());
        return tab;
    }

    public static QuestBookDefinition parseTabs(Map<ResourceLocation, JsonElement> elements) {
        List<QuestTabDefinition> tabs = new ArrayList<>();
        elements.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                if (entry.getValue().isJsonObject()) {
                    QuestTabDefinition tab = QuestTabDefinition.fromJson(entry.getKey().getPath().replace('/', '_'), entry.getValue().getAsJsonObject());
                    if (validateTab(tab, entry.getKey().toString())) {
                        tabs.add(tab);
                    }
                }
            } catch (RuntimeException ex) {
                CakeQuests.LOGGER.warn("Skipping invalid quest graph {}", entry.getKey(), ex);
            }
        });
        return QuestBookDefinition.of(tabs);
    }

    public static boolean validate(QuestBookDefinition book, String source) {
        boolean ok = true;
        Set<String> tabIds = new HashSet<>();
        for (QuestTabDefinition tab : book.tabs()) {
            if (!tabIds.add(tab.id())) {
                CakeQuests.LOGGER.warn("Duplicate quest tab id '{}' in {}", tab.id(), source);
                ok = false;
            }
            ok &= validateTab(tab, source + "/" + tab.id());
        }
        return ok;
    }

    private static boolean validateTab(QuestTabDefinition tab, String source) {
        boolean ok = true;
        if (tab.id().isBlank()) {
            CakeQuests.LOGGER.warn("Quest tab in {} has a blank id", source);
            ok = false;
        }
        Set<String> nodeIds = new HashSet<>();
        for (QuestNodeDefinition node : tab.nodes()) {
            if (node.id().isBlank()) {
                CakeQuests.LOGGER.warn("Quest node in {} has a blank id", source);
                ok = false;
            }
            if (!nodeIds.add(node.id())) {
                CakeQuests.LOGGER.warn("Duplicate quest node id '{}' in {}", node.id(), source);
                ok = false;
            }
            Set<String> eventIds = new HashSet<>();
            for (QuestEventRequirement event : node.events()) {
                if (event.id().isBlank()) {
                    CakeQuests.LOGGER.warn("Quest node '{}'/{} has a blank event requirement id", tab.id(), node.id());
                    ok = false;
                }
                if (!eventIds.add(event.id())) {
                    CakeQuests.LOGGER.warn("Quest node '{}'/{} has duplicate event requirement id '{}'", tab.id(), node.id(), event.id());
                    ok = false;
                }
                if (event.count() < 1L) {
                    CakeQuests.LOGGER.warn("Quest node '{}'/{} event '{}' has invalid count {}", tab.id(), node.id(), event.id(), event.count());
                    ok = false;
                }
            }
        }
        for (QuestNodeDefinition node : tab.nodes()) {
            for (String parent : node.parents()) {
                if (!nodeIds.contains(parent)) {
                    CakeQuests.LOGGER.warn("Quest node '{}'/{} references missing parent '{}'", tab.id(), node.id(), parent);
                    ok = false;
                }
            }
        }
        return ok;
    }
}
