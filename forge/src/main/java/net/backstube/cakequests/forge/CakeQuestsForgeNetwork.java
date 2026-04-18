package net.backstube.cakequests.forge;

import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.client.ClientQuestGraphStore;
import net.backstube.cakequests.client.ClientQuestProgressStore;
import net.backstube.cakequests.data.QuestBookDefinition;
import net.backstube.cakequests.data.QuestMainConfig;
import net.backstube.cakequests.net.CheckQuestNodePacket;
import net.backstube.cakequests.net.GraphSyncPacket;
import net.backstube.cakequests.net.QuestProgressSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class CakeQuestsForgeNetwork {
    private static final String PROTOCOL = "4";
    private static SimpleChannel channel;

    private CakeQuestsForgeNetwork() {
    }

    public static void setup(FMLCommonSetupEvent event) {
        channel = NetworkRegistry.newSimpleChannel(
                CakeQuests.id("graph_sync"),
                () -> PROTOCOL,
                NetworkRegistry.acceptMissingOr(PROTOCOL),
                NetworkRegistry.acceptMissingOr(PROTOCOL)
        );
        channel.registerMessage(0, GraphSyncPacket.class, GraphSyncPacket::encode, GraphSyncPacket::decode,
                CakeQuestsForgeNetwork::handleGraphSync, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        channel.registerMessage(1, QuestProgressSyncPacket.class, QuestProgressSyncPacket::encode, QuestProgressSyncPacket::decode,
                CakeQuestsForgeNetwork::handleProgressSync, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        channel.registerMessage(2, CheckQuestNodePacket.class, CheckQuestNodePacket::encode, CheckQuestNodePacket::decode,
                CakeQuestsForgeNetwork::handleCheckQuestNode, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendCheckCompletion(String graphHash, String tabId, String nodeId) {
        if (channel != null) {
            channel.send(PacketDistributor.SERVER.noArg(), new CheckQuestNodePacket(graphHash, tabId, nodeId));
        }
    }

    public static void sendTo(ServerPlayer player, QuestBookDefinition book, QuestMainConfig config) {
        if (channel != null && channel.isRemotePresent(player.connection.getConnection())) {
            channel.send(PacketDistributor.PLAYER.with(() -> player), GraphSyncPacket.of(book, config));
            sendProgressTo(player);
        }
    }

    public static void sendProgressTo(ServerPlayer player) {
        if (channel != null && channel.isRemotePresent(player.connection.getConnection())) {
            channel.send(PacketDistributor.PLAYER.with(() -> player), CakeQuestsProgressManager.snapshot(player));
        }
    }

    public static void sendToAll(QuestBookDefinition book, QuestMainConfig config) {
        if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
            net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()
                    .forEach(player -> sendTo(player, book, config));
        }
    }

    private static void handleGraphSync(GraphSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                QuestBookDefinition book = packet.book();
                QuestMainConfig config = packet.mainConfig();
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientQuestGraphStore.acceptServerBook(book, config));
            } catch (RuntimeException ex) {
                CakeQuests.LOGGER.warn("Ignoring malformed quest graph sync packet {}", packet.hash(), ex);
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleProgressSync(QuestProgressSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientQuestProgressStore.accept(packet.hash(), packet.counts(), packet.completedNodes()));
            } catch (RuntimeException ex) {
                CakeQuests.LOGGER.warn("Ignoring malformed quest progress sync packet {}", packet.hash(), ex);
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleCheckQuestNode(CheckQuestNodePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                CakeQuestsProgressManager.completeCheck(sender, packet.graphHash(), packet.tabId(), packet.nodeId());
            }
        });
        context.setPacketHandled(true);
    }
}
