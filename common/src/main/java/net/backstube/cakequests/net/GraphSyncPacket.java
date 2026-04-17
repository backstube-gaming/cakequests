package net.backstube.cakequests.net;

import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.data.QuestBookDefinition;
import net.backstube.cakequests.data.QuestGraphParser;
import net.backstube.cakequests.data.QuestMainConfig;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;

public record GraphSyncPacket(String hash, String json, String configJson) {
    public static GraphSyncPacket of(QuestBookDefinition book, QuestMainConfig config) {
        return new GraphSyncPacket(book.hash(), book.toJsonString(), config.toJsonString());
    }

    public static void encode(GraphSyncPacket packet, FriendlyByteBuf buffer) {
        byte[] bytes = packet.json.getBytes(StandardCharsets.UTF_8);
        byte[] configBytes = packet.configJson.getBytes(StandardCharsets.UTF_8);
        int totalBytes = bytes.length + configBytes.length;
        if (totalBytes > CakeQuests.MAX_SYNC_BYTES) {
            throw new IllegalArgumentException("Quest graph sync payload is too large: " + totalBytes);
        }
        buffer.writeUtf(packet.hash, 64);
        buffer.writeByteArray(bytes);
        buffer.writeByteArray(configBytes);
    }

    public static GraphSyncPacket decode(FriendlyByteBuf buffer) {
        String hash = buffer.readUtf(64);
        byte[] bytes = buffer.readByteArray(CakeQuests.MAX_SYNC_BYTES);
        byte[] configBytes = buffer.readByteArray(CakeQuests.MAX_SYNC_BYTES);
        return new GraphSyncPacket(hash, new String(bytes, StandardCharsets.UTF_8), new String(configBytes, StandardCharsets.UTF_8));
    }

    public QuestBookDefinition book() {
        return QuestGraphParser.parseBook(json);
    }

    public QuestMainConfig mainConfig() {
        return QuestGraphParser.parseMainConfig(configJson);
    }
}
