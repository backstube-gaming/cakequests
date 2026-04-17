package net.backstube.cakequests.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.backstube.cakequests.CakeQuests;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record QuestBookDefinition(List<QuestTabDefinition> tabs, String hash) {
    public static final QuestBookDefinition EMPTY = new QuestBookDefinition(List.of(), "empty");

    public static QuestBookDefinition of(List<QuestTabDefinition> tabs) {
        QuestBookDefinition temp = new QuestBookDefinition(tabs.stream()
                .filter(QuestTabDefinition::enabled)
                .sorted(Comparator.comparing(QuestBookDefinition::tabSortTitle, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(QuestTabDefinition::id))
                .toList(), "");
        return new QuestBookDefinition(temp.tabs, hash(temp.toJson().toString()));
    }

    private static String tabSortTitle(QuestTabDefinition tab) {
        String title = tab.title().component().getString();
        return title.isBlank() ? tab.id() : title;
    }

    public static QuestBookDefinition fromJson(JsonObject json) {
        if (!json.has("tabs") || !json.get("tabs").isJsonArray()) {
            return of(List.of(QuestTabDefinition.fromJson("main", json)));
        }
        List<QuestTabDefinition> tabs = new ArrayList<>();
        for (var element : json.getAsJsonArray("tabs")) {
            if (element.isJsonObject()) {
                tabs.add(QuestTabDefinition.fromJson("tab_" + tabs.size(), element.getAsJsonObject()));
            }
        }
        return of(tabs);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 8 && i < bytes.length; i++) {
                builder.append(String.format("%02x", bytes[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            CakeQuests.LOGGER.warn("SHA-256 unavailable, falling back to graph length hash", ex);
            return Integer.toHexString(value.length());
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        JsonArray tabsJson = new JsonArray();
        tabs.forEach(tab -> tabsJson.add(tab.toJson()));
        json.add("tabs", tabsJson);
        return json;
    }

    public String toJsonString() {
        return QuestGraphParser.GSON.toJson(toJson());
    }
}
