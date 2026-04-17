package net.backstube.cakequests.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record QuestNodeDefinition(
        String id,
        QuestText title,
        QuestText subtitle,
        List<QuestDescriptionElement> description,
        QuestIcon icon,
        boolean or,
        List<QuestEventRequirement> events,
        int x,
        int y,
        QuestNodeShape shape,
        int color,
        List<String> parents
) {
    static QuestNodeDefinition fromJson(JsonObject json) {
        String id = string(json, "id", "");
        QuestText title = QuestText.fromJson(json.get("title"));
        QuestText subtitle = QuestText.fromJson(json.get("subtitle"));
        List<QuestDescriptionElement> description = QuestDescriptionElement.listFromJson(json.get("description"));
        QuestIcon icon = QuestIcon.fromJson(json.has("icon") && json.get("icon").isJsonObject() ? json.getAsJsonObject("icon") : null);
        boolean or = json.has("or") && json.get("or").getAsBoolean();
        List<QuestEventRequirement> events = events(json);
        List<String> parents = stringList(json.has("parents") ? json.getAsJsonArray("parents") : json.has("dependencies") ? json.getAsJsonArray("dependencies") : null);
        return new QuestNodeDefinition(
                id,
                title,
                subtitle,
                List.copyOf(description),
                icon,
                or,
                List.copyOf(events),
                integer(json, "x", 0),
                integer(json, "y", 0),
                QuestNodeShape.parse(string(json, "shape", "circle")),
                QuestColor.parse(string(json, "color", null), QuestColor.NODE_DEFAULT),
                List.copyOf(parents)
        );
    }

    private static List<QuestEventRequirement> events(JsonObject json) {
        List<QuestEventRequirement> events = new ArrayList<>();
        if (json.has("events") && json.get("events").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("events");
            for (int i = 0; i < array.size(); i++) {
                if (array.get(i).isJsonObject()) {
                    events.add(QuestEventRequirement.fromJson(array.get(i).getAsJsonObject(), i));
                }
            }
        }
        return events;
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
        if (or) {
            json.addProperty("or", true);
        }
        if (!events.isEmpty()) {
            JsonArray eventsJson = new JsonArray();
            events.forEach(event -> eventsJson.add(event.toJson()));
            json.add("events", eventsJson);
        }
        json.add("title", title.toJson());
        if (subtitle != QuestText.EMPTY) {
            json.add("subtitle", subtitle.toJson());
        }
        JsonArray descriptionJson = new JsonArray();
        description.forEach(element -> descriptionJson.add(element.toJson()));
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
