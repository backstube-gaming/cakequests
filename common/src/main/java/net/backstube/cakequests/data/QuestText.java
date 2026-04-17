package net.backstube.cakequests.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public record QuestText(Component component) {
    public static final QuestText EMPTY = new QuestText(TextComponent.EMPTY);

    public static QuestText literal(String text) {
        return new QuestText(new TextComponent(text));
    }

    public static QuestText fromJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return EMPTY;
        }
        try {
            if (element.isJsonPrimitive()) {
                return literal(element.getAsString());
            }
            Component component = Component.Serializer.fromJson(element);
            return new QuestText(component == null ? TextComponent.EMPTY : component);
        } catch (RuntimeException ex) {
            return literal(element.toString());
        }
    }

    public JsonElement toJson() {
        return component instanceof TextComponent text && text.getSiblings().isEmpty() && text.getStyle().isEmpty()
                ? new JsonPrimitive(text.getText())
                : Component.Serializer.toJsonTree(component);
    }
}
