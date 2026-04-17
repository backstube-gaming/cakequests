package net.backstube.cakequests.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record QuestNodeDefinition(
        String id,
        ResourceLocation advancement,
        QuestText title,
        QuestText subtitle,
        List<QuestText> description,
        QuestIcon icon,
        int x,
        int y,
        QuestNodeShape shape,
        int color,
        List<String> parents
) {
    static QuestNodeDefinition fromJson(JsonObject json) {
        String id = string(json, "id", "");
        ResourceLocation advancement = safeLocation(string(json, "advancement", "minecraft:story/root"));
        QuestText title = QuestText.fromJson(json.get("title"));
        QuestText subtitle = QuestText.fromJson(json.get("subtitle"));
        List<QuestText> description = new ArrayList<>();
        if (json.has("description") && json.get("description").isJsonArray()) {
            json.getAsJsonArray("description").forEach(line -> description.add(QuestText.fromJson(line)));
        }
        QuestIcon icon = QuestIcon.fromJson(json.has("icon") && json.get("icon").isJsonObject() ? json.getAsJsonObject("icon") : null);
        List<String> parents = stringList(json.has("parents") ? json.getAsJsonArray("parents") : json.has("dependencies") ? json.getAsJsonArray("dependencies") : null);
        return new QuestNodeDefinition(
                id,
                advancement,
                title,
                subtitle,
                List.copyOf(description),
                icon,
                integer(json, "x", 0),
                integer(json, "y", 0),
                QuestNodeShape.parse(string(json, "shape", "circle")),
                QuestColor.parse(string(json, "color", null), QuestColor.NODE_DEFAULT),
                List.copyOf(parents)
        );
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        try {
            return json.has(key) ? json.get(key).getAsInt() : fallback;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static ResourceLocation safeLocation(String value) {
        try {
            return new ResourceLocation(value);
        } catch (RuntimeException ex) {
            return new ResourceLocation("minecraft", "story/root");
        }
    }

    private static List<String> stringList(JsonArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            array.forEach(element -> {
                if (element.isJsonPrimitive()) {
                    list.add(element.getAsString());
                }
            });
        }
        return list;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("advancement", advancement.toString());
        json.add("title", title.toJson());
        if (subtitle != QuestText.EMPTY) {
            json.add("subtitle", subtitle.toJson());
        }
        JsonArray descriptionJson = new JsonArray();
        description.forEach(line -> descriptionJson.add(line.toJson()));
        json.add("description", descriptionJson);
        json.add("icon", icon.toJson());
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("shape", shape.name().toLowerCase());
        json.addProperty("color", QuestColor.toHex(color));
        JsonArray parentsJson = new JsonArray();
        parents.forEach(parentsJson::add);
        json.add("parents", parentsJson);
        return json;
    }
}
