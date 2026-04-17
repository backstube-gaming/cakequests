package net.backstube.cakequests.client;

import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.data.QuestBookDefinition;
import net.backstube.cakequests.data.QuestGraphParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClientQuestGraphStore {
    private static QuestBookDefinition activeBook = QuestBookDefinition.EMPTY;
    private static boolean fallbackMode = true;

    private ClientQuestGraphStore() {
    }

    public static QuestBookDefinition activeBook() {
        return activeBook;
    }

    public static boolean fallbackMode() {
        return fallbackMode;
    }

    public static void acceptServerBook(QuestBookDefinition book) {
        fallbackMode = false;
        activeBook = book;
        CakeQuests.LOGGER.info("Loaded server quest graph {} with {} tabs", book.hash(), book.tabs().size());
    }

    public static void loadFallbackGraphs() {
        Path root = Path.of("config", CakeQuests.MOD_ID, "quest_graphs");
        List<net.backstube.cakequests.data.QuestTabDefinition> tabs = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            activeBook = QuestBookDefinition.EMPTY;
            fallbackMode = true;
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> {
                        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                            String id = root.relativize(path).toString().replace('\\', '/').replace(".json", "");
                            tabs.add(QuestGraphParser.parseTab(new net.minecraft.resources.ResourceLocation(CakeQuests.MOD_ID, id), reader));
                        } catch (RuntimeException | IOException ex) {
                            CakeQuests.LOGGER.warn("Skipping fallback quest graph {}", path, ex);
                        }
                    });
        } catch (IOException ex) {
            CakeQuests.LOGGER.warn("Failed to read fallback quest graph directory {}", root, ex);
        }
        activeBook = QuestBookDefinition.of(tabs);
        fallbackMode = true;
        CakeQuests.LOGGER.info("Loaded fallback quest graph {} with {} tabs", activeBook.hash(), activeBook.tabs().size());
    }

    public static void clearToFallback() {
        loadFallbackGraphs();
    }
}
