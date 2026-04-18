package net.backstube.cakequests.data;

import java.util.Locale;

public enum QuestEventType {
    ITEM_PICKUP("item_pickup"),
    ITEM_CRAFT("item_craft"),
    CHECK("check");

    private final String id;

    QuestEventType(String id) {
        this.id = id;
    }

    public static QuestEventType parse(String value) {
        if (value != null) {
            String normalized = value.toLowerCase(Locale.ROOT);
            for (QuestEventType type : values()) {
                if (type.id.equals(normalized)) {
                    return type;
                }
            }
        }
        return ITEM_PICKUP;
    }

    public String id() {
        return id;
    }
}
