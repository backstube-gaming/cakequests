package net.backstube.cakequests.forge;

import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.data.*;
import net.backstube.cakequests.net.QuestProgressSyncPacket;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class CakeQuestsProgressManager {
    public static final String ROOT_KEY = CakeQuests.MOD_ID + "_progress";
    private static final String COUNTS_KEY = "counts";
    private static final String COMPLETED_KEY = "completed";
    private static final Map<Item, List<RequirementBinding>> pickupByItem = new HashMap<>();
    private static final Map<Item, List<RequirementBinding>> craftByItem = new HashMap<>();
    private static QuestBookDefinition activeBook = QuestBookDefinition.EMPTY;

    private CakeQuestsProgressManager() {
    }

    public static void rebuild(QuestBookDefinition book) {
        activeBook = book;
        pickupByItem.clear();
        craftByItem.clear();
        for (QuestTabDefinition tab : book.tabs()) {
            for (QuestNodeDefinition node : tab.nodes()) {
                for (QuestEventRequirement requirement : node.events()) {
                    RequirementBinding binding = new RequirementBinding(tab.id(), node.id(), requirement.id(), requirement.type(), requirement.target(), requirement.count());
                    Map<Item, List<RequirementBinding>> targetIndex = requirement.type() == QuestEventType.ITEM_CRAFT ? craftByItem : pickupByItem;
                    indexRequirement(targetIndex, binding);
                }
            }
        }
        CakeQuests.LOGGER.info("Indexed {} pickup items and {} craft items for quest progress", pickupByItem.size(), craftByItem.size());
    }

    private static void indexRequirement(Map<Item, List<RequirementBinding>> index, RequirementBinding binding) {
        if (binding.target().tag()) {
            TagKey<Item> tag = TagKey.create(Registry.ITEM_REGISTRY, binding.target().id());
            Optional<HolderSet.Named<Item>> holders = Registry.ITEM.getTag(tag);
            if (holders.isEmpty()) {
                CakeQuests.LOGGER.warn("Quest requirement {}/{} references missing item tag #{}", binding.nodeKey(), binding.requirementId(), binding.target().id());
                return;
            }
            for (Holder<Item> holder : holders.get()) {
                index.computeIfAbsent(holder.value(), ignored -> new ArrayList<>()).add(binding);
            }
            return;
        }
        Optional<Item> item = Registry.ITEM.getOptional(binding.target().id());
        if (item.isEmpty()) {
            CakeQuests.LOGGER.warn("Quest requirement {}/{} references missing item {}", binding.nodeKey(), binding.requirementId(), binding.target().id());
            return;
        }
        index.computeIfAbsent(item.get(), ignored -> new ArrayList<>()).add(binding);
    }

    public static void handlePickup(ServerPlayer player, ItemStack stack) {
        handleItemEvent(player, stack, pickupByItem);
    }

    public static void handleCraft(ServerPlayer player, ItemStack stack) {
        handleItemEvent(player, stack, craftByItem);
    }

    private static void handleItemEvent(ServerPlayer player, ItemStack stack, Map<Item, List<RequirementBinding>> index) {
        if (stack.isEmpty() || index.isEmpty()) {
            return;
        }
        List<RequirementBinding> bindings = index.get(stack.getItem());
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (RequirementBinding binding : bindings) {
            changed |= addProgress(player, binding, stack.getCount());
        }
        if (changed) {
            CakeQuestsForgeNetwork.sendProgressTo(player);
        }
    }

    public static QuestProgressSyncPacket snapshot(ServerPlayer player) {
        CompoundTag root = root(player);
        CompoundTag countsTag = root.getCompound(COUNTS_KEY);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : countsTag.getAllKeys()) {
            long count = countsTag.getLong(key);
            if (count > 0) {
                counts.put(key, count);
            }
        }
        Set<String> completed = new LinkedHashSet<>();
        for (QuestTabDefinition tab : activeBook.tabs()) {
            for (QuestNodeDefinition node : tab.nodes()) {
                if (isNodeComplete(root, tab, node)) {
                    completed.add(nodeKey(tab.id(), node.id()));
                }
            }
        }
        return new QuestProgressSyncPacket(activeBook.hash(), counts, completed);
    }

    public static int reset(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getPersistentData().remove(ROOT_KEY);
            CakeQuestsForgeNetwork.sendProgressTo(player);
        }
        return players.size();
    }

    public static int grant(Collection<ServerPlayer> players, String id) {
        return mutate(players, id, Mutation.GRANT);
    }

    public static int revoke(Collection<ServerPlayer> players, String id) {
        return mutate(players, id, Mutation.REVOKE);
    }

    public static int grantTo(Collection<ServerPlayer> players, String id) {
        return mutate(players, id, Mutation.GRANT_TO);
    }

    private static int mutate(Collection<ServerPlayer> players, String id, Mutation mutation) {
        Target target = resolve(id);
        if (target == null) {
            return 0;
        }
        int changed = 0;
        for (ServerPlayer player : players) {
            if (mutate(player, target, mutation)) {
                changed++;
                CakeQuestsForgeNetwork.sendProgressTo(player);
            }
        }
        return changed;
    }

    private static boolean mutate(ServerPlayer player, Target target, Mutation mutation) {
        CompoundTag root = root(player);
        CompoundTag counts = counts(root);
        CompoundTag completed = completed(root);
        boolean changed = false;
        if (target.requirement() == null) {
            String nodeKey = nodeKey(target.tab().id(), target.node().id());
            if (mutation == Mutation.REVOKE) {
                changed |= completed.getBoolean(nodeKey);
                completed.remove(nodeKey);
                for (QuestEventRequirement requirement : target.node().events()) {
                    String key = requirementKey(target.tab().id(), target.node().id(), requirement.id());
                    changed |= counts.getLong(key) != 0L;
                    counts.remove(key);
                }
            } else {
                changed |= !completed.getBoolean(nodeKey);
                completed.putBoolean(nodeKey, true);
                for (QuestEventRequirement requirement : target.node().events()) {
                    String key = requirementKey(target.tab().id(), target.node().id(), requirement.id());
                    long old = counts.getLong(key);
                    counts.putLong(key, requirement.count());
                    changed |= old != requirement.count();
                }
            }
            return changed;
        }
        String key = requirementKey(target.tab().id(), target.node().id(), target.requirement().id());
        long old = counts.getLong(key);
        if (mutation == Mutation.REVOKE) {
            counts.remove(key);
            completed.remove(nodeKey(target.tab().id(), target.node().id()));
            return old != 0L;
        }
        counts.putLong(key, target.requirement().count());
        return old != target.requirement().count();
    }

    private static boolean addProgress(ServerPlayer player, RequirementBinding binding, long amount) {
        CompoundTag root = root(player);
        CompoundTag counts = counts(root);
        String key = requirementKey(binding.tabId(), binding.nodeId(), binding.requirementId());
        long old = counts.getLong(key);
        if (old >= binding.count()) {
            return false;
        }
        long updated = Math.min(binding.count(), old + Math.max(0L, amount));
        counts.putLong(key, updated);
        return updated != old;
    }

    private static boolean isNodeComplete(CompoundTag root, QuestTabDefinition tab, QuestNodeDefinition node) {
        if (completed(root).getBoolean(nodeKey(tab.id(), node.id()))) {
            return true;
        }
        if (node.events().isEmpty()) {
            return false;
        }
        CompoundTag counts = counts(root);
        boolean anyComplete = false;
        for (QuestEventRequirement requirement : node.events()) {
            boolean requirementComplete = counts.getLong(requirementKey(tab.id(), node.id(), requirement.id())) >= requirement.count();
            if (node.or() && requirementComplete) {
                return true;
            }
            if (!node.or() && !requirementComplete) {
                return false;
            }
            anyComplete |= requirementComplete;
        }
        return !node.or() || anyComplete;
    }

    private static Target resolve(String id) {
        String[] parts = id.split("/");
        if (parts.length < 2 || parts.length > 3) {
            return null;
        }
        for (QuestTabDefinition tab : activeBook.tabs()) {
            if (!tab.id().equals(parts[0])) {
                continue;
            }
            for (QuestNodeDefinition node : tab.nodes()) {
                if (!node.id().equals(parts[1])) {
                    continue;
                }
                if (parts.length == 2) {
                    return new Target(tab, node, null);
                }
                for (QuestEventRequirement requirement : node.events()) {
                    if (requirement.id().equals(parts[2])) {
                        return new Target(tab, node, requirement);
                    }
                }
            }
        }
        return null;
    }

    private static CompoundTag root(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_KEY, 10)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        CompoundTag root = persistent.getCompound(ROOT_KEY);
        if (!root.contains(COUNTS_KEY, 10)) {
            root.put(COUNTS_KEY, new CompoundTag());
        }
        if (!root.contains(COMPLETED_KEY, 10)) {
            root.put(COMPLETED_KEY, new CompoundTag());
        }
        persistent.put(ROOT_KEY, root);
        return root;
    }

    private static CompoundTag counts(CompoundTag root) {
        return root.getCompound(COUNTS_KEY);
    }

    private static CompoundTag completed(CompoundTag root) {
        return root.getCompound(COMPLETED_KEY);
    }

    private static String nodeKey(String tabId, String nodeId) {
        return tabId + "/" + nodeId;
    }

    private static String requirementKey(String tabId, String nodeId, String requirementId) {
        return tabId + "/" + nodeId + "/" + requirementId;
    }

    private enum Mutation {
        GRANT,
        REVOKE,
        GRANT_TO
    }

    private record RequirementBinding(String tabId, String nodeId, String requirementId, QuestEventType type,
                                      QuestItemTarget target, long count) {
        String nodeKey() {
            return tabId() + "/" + nodeId();
        }
    }

    private record Target(QuestTabDefinition tab, QuestNodeDefinition node, QuestEventRequirement requirement) {
    }
}
