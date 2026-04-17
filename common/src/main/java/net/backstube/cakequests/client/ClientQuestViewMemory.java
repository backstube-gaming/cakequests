package net.backstube.cakequests.client;

import net.backstube.cakequests.data.QuestBookDefinition;

public final class ClientQuestViewMemory {
    private static Snapshot snapshot;

    private ClientQuestViewMemory() {
    }

    public static Snapshot snapshotFor(QuestBookDefinition book) {
        if (snapshot == null || !snapshot.graphHash().equals(book.hash())) {
            return null;
        }
        return snapshot;
    }

    public static void remember(QuestBookDefinition book, String tabId, double panX, double panY, double zoom) {
        snapshot = new Snapshot(book.hash(), tabId, panX, panY, zoom);
    }

    public static void clear() {
        snapshot = null;
    }

    public record Snapshot(String graphHash, String tabId, double panX, double panY, double zoom) {
    }
}
