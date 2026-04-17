package net.backstube.cakequests.client;

import net.backstube.cakequests.data.QuestNodeDefinition;
import net.backstube.cakequests.data.QuestTabDefinition;
import net.backstube.cakequests.quest.QuestNodeState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ClientQuestProgressStore {
    private static final Map<String, Long> counts = new HashMap<>();
    private static final Set<String> completedNodes = new HashSet<>();
    private static String graphHash = "";

    private ClientQuestProgressStore() {
    }

    public static void accept(String hash, Map<String, Long> newCounts, Set<String> newCompletedNodes) {
        graphHash = hash;
        counts.clear();
        counts.putAll(newCounts);
        completedNodes.clear();
        completedNodes.addAll(newCompletedNodes);
    }

    public static void clear() {
        graphHash = "";
        counts.clear();
        completedNodes.clear();
    }

    public static QuestNodeState state(QuestTabDefinition tab, QuestNodeDefinition node) {
        if (isComplete(tab.id(), node.id())) {
            return QuestNodeState.COMPLETE;
        }
        for (String parentId : node.parents()) {
            if (!isComplete(tab.id(), parentId)) {
                return QuestNodeState.LOCKED;
            }
        }
        return QuestNodeState.AVAILABLE;
    }

    public static boolean isComplete(String tabId, String nodeId) {
        return completedNodes.contains(nodeKey(tabId, nodeId));
    }

    public static long count(String tabId, String nodeId, String requirementId) {
        return counts.getOrDefault(requirementKey(tabId, nodeId, requirementId), 0L);
    }

    public static String graphHash() {
        return graphHash;
    }

    public static String nodeKey(String tabId, String nodeId) {
        return tabId + "/" + nodeId;
    }

    public static String requirementKey(String tabId, String nodeId, String requirementId) {
        return tabId + "/" + nodeId + "/" + requirementId;
    }
}
