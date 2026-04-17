package net.backstube.cakequests.data;

import net.minecraft.resources.ResourceLocation;

public record QuestItemTarget(ResourceLocation id, boolean tag) {
    public static QuestItemTarget parse(String value) {
        if (value == null || value.isBlank()) {
            return new QuestItemTarget(new ResourceLocation("minecraft", "air"), false);
        }
        boolean tag = value.startsWith("#");
        String location = tag ? value.substring(1) : value;
        try {
            return new QuestItemTarget(new ResourceLocation(location), tag);
        } catch (RuntimeException ex) {
            return new QuestItemTarget(new ResourceLocation("minecraft", "air"), false);
        }
    }

    public String toJsonValue() {
        return tag ? "#" + id : id.toString();
    }
}
