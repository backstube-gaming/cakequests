package net.backstube.cakequests.net;

import net.minecraft.network.FriendlyByteBuf;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record QuestProgressSyncPacket(String hash, Map<String, Long> counts, Set<String> completedNodes) {
    private static final int MAX_PROGRESS_ENTRIES = 8192;

    public static void encode(QuestProgressSyncPacket packet, FriendlyByteBuf buffer) {
        if (packet.counts.size() + packet.completedNodes.size() > MAX_PROGRESS_ENTRIES) {
            throw new IllegalArgumentException("Quest progress sync payload has too many entries");
        }
        buffer.writeUtf(packet.hash, 64);
        buffer.writeVarInt(packet.counts.size());
        packet.counts.forEach((key, value) -> {
            buffer.writeUtf(key, 256);
            buffer.writeVarLong(value);
        });
        buffer.writeVarInt(packet.completedNodes.size());
        packet.completedNodes.forEach(key -> buffer.writeUtf(key, 256));
    }

    public static QuestProgressSyncPacket decode(FriendlyByteBuf buffer) {
        String hash = buffer.readUtf(64);
        int countSize = buffer.readVarInt();
        if (countSize > MAX_PROGRESS_ENTRIES) {
            throw new IllegalArgumentException("Quest progress sync has too many count entries: " + countSize);
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < countSize; i++) {
            counts.put(buffer.readUtf(256), buffer.readVarLong());
        }
        int completedSize = buffer.readVarInt();
        if (completedSize > MAX_PROGRESS_ENTRIES) {
            throw new IllegalArgumentException("Quest progress sync has too many completed entries: " + completedSize);
        }
        Set<String> completedNodes = new LinkedHashSet<>();
        for (int i = 0; i < completedSize; i++) {
            completedNodes.add(buffer.readUtf(256));
        }
        return new QuestProgressSyncPacket(hash, counts, completedNodes);
    }
}
