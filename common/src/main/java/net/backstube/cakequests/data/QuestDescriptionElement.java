package net.backstube.cakequests.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record QuestDescriptionElement(Kind kind, QuestText text, ResourceLocation image) {
    public static QuestDescriptionElement text(QuestText text) {
        return new QuestDescriptionElement(Kind.TEXT, text, null);
    }

    public static QuestDescriptionElement image(ResourceLocation image) {
        return new QuestDescriptionElement(Kind.IMAGE, QuestText.EMPTY, image);
    }

    public static List<QuestDescriptionElement> listFromJson(JsonElement element) {
        List<QuestDescriptionElement> description = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return description;
        }
        element.getAsJsonArray().forEach(entry -> description.addAll(fromJson(entry)));
        return description;
    }

    private static List<QuestDescriptionElement> fromJson(JsonElement element) {
        List<QuestDescriptionElement> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return result;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("image")) {
                result.add(image(safeLocation(object.get("image").getAsString())));
                return result;
            }
            if (object.has("extra") && object.get("extra").isJsonArray()) {
                try {
                    Component base = Component.Serializer.fromJson(object);
                    if (base != null && !base.getString().isEmpty()) {
                        result.add(text(new QuestText(base)));
                        return result;
                    }
                } catch (RuntimeException ignored) {
                    // Fall back to the legacy extra array below.
                }
                object.getAsJsonArray("extra").forEach(extra -> result.add(text(QuestText.fromJson(extra))));
                return result;
            }
        }
        result.add(text(QuestText.fromJson(element)));
        return result;
    }

    private static ResourceLocation safeLocation(String value) {
        try {
            return new ResourceLocation(value);
        } catch (RuntimeException ex) {
            return new ResourceLocation("minecraft", "textures/missingno.png");
        }
    }

    public JsonElement toJson() {
        if (kind == Kind.IMAGE) {
            JsonObject object = new JsonObject();
            object.addProperty("image", image.toString());
            return object;
        }
        return text.toJson();
    }

    public enum Kind {
        TEXT,
        IMAGE
    }
}
