package net.backstube.cakequests.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public record QuestMainConfig(String titleLabel, String subtitle, Map<ResourceLocation, TagDisplayName> tagNames) {
    public static final QuestMainConfig DEFAULT = new QuestMainConfig("Cake Quests", "Server", Map.of());

    public static QuestMainConfig fromJson(JsonObject json) {
        if (json == null) {
            return DEFAULT;
        }
        return new QuestMainConfig(
                string(json, "title_label", DEFAULT.titleLabel),
                string(json, "subtitle", DEFAULT.subtitle),
                tagNames(json)
        );
    }

    private static Map<ResourceLocation, TagDisplayName> tagNames(JsonObject json) {
        Map<ResourceLocation, TagDisplayName> names = new LinkedHashMap<>();
        if (!json.has("tag_names") || !json.get("tag_names").isJsonObject()) {
            return names;
        }
        JsonObject tagNames = json.getAsJsonObject("tag_names");
        for (String key : tagNames.keySet()) {
            try {
                if (tagNames.get(key).isJsonObject()) {
                    names.put(new ResourceLocation(key), TagDisplayName.fromJson(tagNames.getAsJsonObject(key)));
                }
            } catch (RuntimeException ignored) {
            }
        }
        return Map.copyOf(names);
    }

    private static String string(JsonObject json, String key, String fallback) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            String value = json.get(key).getAsString();
            return value.isBlank() ? fallback : value;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("title_label", titleLabel);
        json.addProperty("subtitle", subtitle);
        if (!tagNames.isEmpty()) {
            JsonObject tagNamesJson = new JsonObject();
            tagNames.forEach((id, text) -> tagNamesJson.add(id.toString(), text.toJson()));
            json.add("tag_names", tagNamesJson);
        }
        return json;
    }

    public String toJsonString() {
        return QuestGraphParser.GSON.toJson(toJson());
    }
}
