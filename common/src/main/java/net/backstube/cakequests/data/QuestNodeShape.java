package net.backstube.cakequests.data;

import java.util.Locale;

public enum QuestNodeShape {
    CIRCLE,
    SQUARE,
    DIAMOND;

    public static QuestNodeShape parse(String value) {
        if (value == null || value.isBlank()) {
            return CIRCLE;
        }
        try {
            return QuestNodeShape.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CIRCLE;
        }
    }
}
