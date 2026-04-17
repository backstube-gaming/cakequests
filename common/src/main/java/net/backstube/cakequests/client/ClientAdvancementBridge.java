package net.backstube.cakequests.client;

import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.data.QuestNodeDefinition;
import net.backstube.cakequests.data.QuestTabDefinition;
import net.backstube.cakequests.quest.QuestNodeState;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;

import java.lang.reflect.Field;
import java.util.Map;

public final class ClientAdvancementBridge {
    private static Field progressField;

    private ClientAdvancementBridge() {
    }

    public static QuestNodeState state(QuestTabDefinition tab, QuestNodeDefinition node) {
        if (ClientQuestGraphStore.fallbackMode()) {
            return QuestNodeState.AVAILABLE;
        }
        if (isComplete(node)) {
            return QuestNodeState.COMPLETE;
        }
        for (String parentId : node.parents()) {
            QuestNodeDefinition parent = tab.nodes().stream().filter(candidate -> candidate.id().equals(parentId)).findFirst().orElse(null);
            if (parent != null && !isComplete(parent)) {
                return QuestNodeState.LOCKED;
            }
        }
        return QuestNodeState.AVAILABLE;
    }

    public static boolean isComplete(QuestNodeDefinition node) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.player.connection == null) {
                return false;
            }
            ClientAdvancements advancements = minecraft.player.connection.getAdvancements();
            Advancement advancement = advancements.getAdvancements().get(node.advancement());
            if (advancement == null) {
                CakeQuests.LOGGER.debug("Quest node '{}' references missing client advancement {}", node.id(), node.advancement());
                return false;
            }
            AdvancementProgress progress = progress(advancements).get(advancement);
            return progress != null && progress.isDone();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            CakeQuests.LOGGER.debug("Unable to inspect client advancement progress", ex);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Advancement, AdvancementProgress> progress(ClientAdvancements advancements) throws ReflectiveOperationException {
        if (progressField == null) {
            progressField = ClientAdvancements.class.getDeclaredField("progress");
            progressField.setAccessible(true);
        }
        return (Map<Advancement, AdvancementProgress>) progressField.get(advancements);
    }
}
