package net.backstube.cakequests.data;

public final class QuestColor {
    public static final int TAB_DEFAULT = 0xFF4D83B5;
    public static final int NODE_DEFAULT = 0xFFB6B8C0;

    private QuestColor() {
    }

    public static int parse(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String clean = value.trim();
        if (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        try {
            if (clean.length() == 6) {
                return 0xFF000000 | Integer.parseUnsignedInt(clean, 16);
            }
            if (clean.length() == 8) {
                return (int) Long.parseLong(clean, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }

    public static String toHex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }
}
