package net.backstube.cakequests.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;

public final class FtbSnbtQuestImporter {
    private static final int COMPOUND = 10;
    private static final int STRING = 8;

    private FtbSnbtQuestImporter() {
    }

    public static JsonObject importChapter(String namespace, String fallbackId, String snbt) throws CommandSyntaxException {
        CompoundTag chapter = TagParser.parseTag(snbt);
        String tabId = safeId(chapter.contains("id", STRING) ? chapter.getString("id") : fallbackId);
        JsonObject tab = new JsonObject();
        tab.addProperty("id", tabId);
        tab.addProperty("enabled", true);
        tab.add("title", text(chapter.contains("title", STRING) ? chapter.getString("title") : fallbackId));
        tab.addProperty("tab_color", "#4D83B5");
        JsonArray nodes = new JsonArray();
        ListTag quests = chapter.getList("quests", COMPOUND);
        for (int i = 0; i < quests.size(); i++) {
            CompoundTag quest = quests.getCompound(i);
            nodes.add(importQuest(namespace, tabId, quest));
        }
        tab.add("nodes", nodes);
        return tab;
    }

    private static JsonObject importQuest(String namespace, String tabId, CompoundTag quest) {
        String id = safeId(quest.contains("id", STRING) ? quest.getString("id") : "quest");
        JsonObject node = new JsonObject();
        node.addProperty("id", id);
        node.add("title", text(quest.contains("title", STRING) ? quest.getString("title") : id));
        node.add("description", description(quest));
        node.add("icon", icon(quest));
        node.addProperty("x", (int) Math.round(quest.getDouble("x") * 48.0D));
        node.addProperty("y", (int) Math.round(quest.getDouble("y") * 48.0D));
        node.addProperty("shape", "circle");
        node.addProperty("color", "#B6B8C0");
        node.add("parents", stringArray(quest.getList("dependencies", STRING)));
        return node;
    }

    private static JsonArray description(CompoundTag quest) {
        JsonArray description = new JsonArray();
        ListTag text = quest.getList("description", STRING);
        for (int i = 0; i < text.size(); i++) {
            description.add(text(text.getString(i)));
        }
        return description;
    }

    private static JsonObject icon(CompoundTag quest) {
        JsonObject icon = new JsonObject();
        String item = "minecraft:book";
        ListTag tasks = quest.getList("tasks", COMPOUND);
        if (!tasks.isEmpty()) {
            CompoundTag firstTask = tasks.getCompound(0);
            if (firstTask.contains("item", STRING)) {
                item = firstTask.getString("item");
            } else if (firstTask.contains("icon", STRING)) {
                item = firstTask.getString("icon");
            }
        }
        icon.addProperty("item", item.contains(":") ? item : "minecraft:book");
        return icon;
    }

    private static JsonObject text(String value) {
        JsonObject text = new JsonObject();
        text.addProperty("text", stripFormatting(value));
        return text;
    }

    private static JsonArray stringArray(ListTag list) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < list.size(); i++) {
            array.add(safeId(list.getString(i)));
        }
        return array;
    }

    private static String safeId(String id) {
        String clean = id.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        return clean.isBlank() ? "quest" : clean;
    }

    private static String stripFormatting(String value) {
        return value == null ? "" : value.replaceAll("(?i)&[0-9a-fk-or]", "");
    }
}
