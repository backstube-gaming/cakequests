package net.backstube.cakequests.data;

import com.google.gson.JsonObject;
import net.backstube.cakequests.CakeQuests;

public record QuestEventRequirement(String id, QuestEventType type, QuestItemTarget target, long count) {
    public static QuestEventRequirement fromJson(JsonObject json, int index) {
        if (json.has("nbt") || json.has("match_nbt") || json.has("weak_nbt_match")) {
            CakeQuests.LOGGER.warn("Ignoring unsupported NBT matching fields on quest event requirement '{}'", string(json, "id", "event_" + index));
        }
        String id = string(json, "id", "event_" + index);
        QuestEventType type = QuestEventType.parse(string(json, "type", "item_pickup"));
        QuestItemTarget target = QuestItemTarget.parse(string(json, "item", "minecraft:air"));
        long count = Math.max(1L, longValue(json, "count", 1L));
        return new QuestEventRequirement(id, type, target, count);
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
    }

    private static long longValue(JsonObject json, String key, long fallback) {
        try {
            return json.has(key) ? json.get(key).getAsLong() : fallback;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("type", type.id());
        json.addProperty("item", target.toJsonValue());
        json.addProperty("count", count);
        return json;
    }
}
