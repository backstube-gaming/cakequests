package net.backstube.cakequests.client;

import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.data.QuestBookDefinition;
import net.backstube.cakequests.data.QuestMainConfig;
import net.backstube.cakequests.data.TagDisplayName;
import net.minecraft.resources.ResourceLocation;

public final class ClientQuestGraphStore {
    private static QuestBookDefinition activeBook = QuestBookDefinition.EMPTY;
    private static QuestMainConfig mainConfig = QuestMainConfig.DEFAULT;

    private ClientQuestGraphStore() {
    }

    public static QuestBookDefinition activeBook() {
        return activeBook;
    }

    public static String titleLabel() {
        return mainConfig.titleLabel();
    }

    public static String subtitle() {
        return mainConfig.subtitle();
    }

    public static TagDisplayName tagName(ResourceLocation tag) {
        return mainConfig.tagNames().get(tag);
    }

    public static void acceptServerBook(QuestBookDefinition book, QuestMainConfig config) {
        activeBook = book;
        mainConfig = config;
        ClientQuestProgressStore.clear();
        CakeQuests.LOGGER.info("Loaded server quest graph {} with {} tabs", book.hash(), book.tabs().size());
    }

    public static void clear() {
        activeBook = QuestBookDefinition.EMPTY;
        mainConfig = QuestMainConfig.DEFAULT;
        ClientQuestViewMemory.clear();
        ClientQuestProgressStore.clear();
    }
}
