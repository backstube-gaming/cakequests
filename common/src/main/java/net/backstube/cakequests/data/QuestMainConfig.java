package net.backstube.cakequests.data;

import com.google.gson.JsonObject;

public record QuestMainConfig(String titleLabel, String subtitle) {
    public static final QuestMainConfig DEFAULT = new QuestMainConfig("Cake Quests", "Server");

    public static QuestMainConfig fromJson(JsonObject json) {
        if (json == null) {
            return DEFAULT;
        }
        return new QuestMainConfig(
                string(json, "title_label", DEFAULT.titleLabel),
                string(json, "subtitle", DEFAULT.subtitle)
        );
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
        return json;
    }

    public String toJsonString() {
        return QuestGraphParser.GSON.toJson(toJson());
    }
}
