package net.backstube.cakequests.net;

import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.data.QuestBookDefinition;
import net.backstube.cakequests.data.QuestGraphParser;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;

public record GraphSyncPacket(String hash, String json) {
    public static GraphSyncPacket of(QuestBookDefinition book) {
        return new GraphSyncPacket(book.hash(), book.toJsonString());
    }

    public static void encode(GraphSyncPacket packet, FriendlyByteBuf buffer) {
        byte[] bytes = packet.json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > CakeQuests.MAX_SYNC_BYTES) {
            throw new IllegalArgumentException("Quest graph sync payload is too large: " + bytes.length);
        }
        buffer.writeUtf(packet.hash, 64);
        buffer.writeByteArray(bytes);
    }

    public static GraphSyncPacket decode(FriendlyByteBuf buffer) {
        String hash = buffer.readUtf(64);
        byte[] bytes = buffer.readByteArray(CakeQuests.MAX_SYNC_BYTES);
        return new GraphSyncPacket(hash, new String(bytes, StandardCharsets.UTF_8));
    }

    public QuestBookDefinition book() {
        return QuestGraphParser.parseBook(json);
    }
}
