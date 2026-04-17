package net.backstube.cakequests.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record QuestTabDefinition(String id, boolean enabled, QuestText title, int tabColor,
                                 List<QuestNodeDefinition> nodes) {
    static QuestTabDefinition fromJson(String fallbackId, JsonObject json) {
        JsonObject tab = unwrapTab(json);
        String id = tab.has("id") ? tab.get("id").getAsString() : fallbackId;
        boolean enabled = !tab.has("enabled") || tab.get("enabled").getAsBoolean();
        QuestText title = QuestText.fromJson(tab.get("title"));
        int tabColor = QuestColor.parse(tab.has("tab_color") ? tab.get("tab_color").getAsString() : null, QuestColor.TAB_DEFAULT);
        List<QuestNodeDefinition> nodes = new ArrayList<>();
        if (enabled && tab.has("nodes") && tab.get("nodes").isJsonArray()) {
            tab.getAsJsonArray("nodes").forEach(element -> {
                if (element.isJsonObject()) {
                    nodes.add(QuestNodeDefinition.fromJson(element.getAsJsonObject()));
                }
            });
        }
        return new QuestTabDefinition(id, enabled, title, tabColor, List.copyOf(nodes));
    }

    private static JsonObject unwrapTab(JsonObject json) {
        if (json.has("tabs") && json.get("tabs").isJsonArray() && json.getAsJsonArray("tabs").size() > 0) {
            return json.getAsJsonArray("tabs").get(0).getAsJsonObject();
        }
        return json;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("enabled", enabled);
        json.add("title", title.toJson());
        json.addProperty("tab_color", QuestColor.toHex(tabColor));
        JsonArray nodesJson = new JsonArray();
        nodes.forEach(node -> nodesJson.add(node.toJson()));
        json.add("nodes", nodesJson);
        return json;
    }
}
