package net.backstube.cakequests.forge;

import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.client.ClientQuestGraphStore;
import net.backstube.cakequests.data.QuestBookDefinition;
import net.backstube.cakequests.net.GraphSyncPacket;
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
    private static final String PROTOCOL = "1";
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
    }

    public static void sendTo(ServerPlayer player, QuestBookDefinition book) {
        if (channel != null && channel.isRemotePresent(player.connection.getConnection())) {
            channel.send(PacketDistributor.PLAYER.with(() -> player), GraphSyncPacket.of(book));
        }
    }

    public static void sendToAll(QuestBookDefinition book) {
        if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
            net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()
                    .forEach(player -> sendTo(player, book));
        }
    }

    private static void handleGraphSync(GraphSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                QuestBookDefinition book = packet.book();
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientQuestGraphStore.acceptServerBook(book));
            } catch (RuntimeException ex) {
                CakeQuests.LOGGER.warn("Ignoring malformed quest graph sync packet {}", packet.hash(), ex);
            }
        });
        context.setPacketHandled(true);
    }
}
