package net.backstube.cakequests.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CakeQuestsForgeEvents {
    @SubscribeEvent
    public void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new QuestGraphReloadListener());
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            CakeQuestsForgeNetwork.sendTo(player, QuestGraphReloadListener.activeBook(), QuestGraphReloadListener.activeConfig());
        }
    }

    @SubscribeEvent
    public void playerCloned(PlayerEvent.Clone event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            player.getPersistentData().put(CakeQuestsProgressManager.ROOT_KEY, event.getOriginal().getPersistentData().getCompound(CakeQuestsProgressManager.ROOT_KEY).copy());
        }
    }

    @SubscribeEvent
    public void itemPickedUp(PlayerEvent.ItemPickupEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            CakeQuestsProgressManager.handlePickup(player, event.getStack());
        }
    }

    @SubscribeEvent
    public void itemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            CakeQuestsProgressManager.handleCraft(player, event.getCrafting());
        }
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CakeQuestsCommands.register(event.getDispatcher());
    }
}
