package net.backstube.cakequests.net;

import net.minecraft.network.FriendlyByteBuf;

public record CheckQuestNodePacket(String graphHash, String tabId, String nodeId) {
    private static final int MAX_ID_LENGTH = 128;

    public static void encode(CheckQuestNodePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.graphHash(), MAX_ID_LENGTH);
        buffer.writeUtf(packet.tabId(), MAX_ID_LENGTH);
        buffer.writeUtf(packet.nodeId(), MAX_ID_LENGTH);
    }

    public static CheckQuestNodePacket decode(FriendlyByteBuf buffer) {
        return new CheckQuestNodePacket(
                buffer.readUtf(MAX_ID_LENGTH),
                buffer.readUtf(MAX_ID_LENGTH),
                buffer.readUtf(MAX_ID_LENGTH)
        );
    }
}
