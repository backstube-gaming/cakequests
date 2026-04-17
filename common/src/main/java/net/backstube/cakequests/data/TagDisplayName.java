package net.backstube.cakequests.data;

import com.google.gson.JsonObject;

public record TagDisplayName(String translate, String fallback) {
    public static TagDisplayName fromJson(JsonObject json) {
        return new TagDisplayName(
                string(json, "translate", ""),
                string(json, "fallback", "")
        );
    }

    private static String string(JsonObject json, String key, String fallback) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return json.get(key).getAsString();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (!translate.isBlank()) {
            json.addProperty("translate", translate);
        }
        if (!fallback.isBlank()) {
            json.addProperty("fallback", fallback);
        }
        return json;
    }
}
