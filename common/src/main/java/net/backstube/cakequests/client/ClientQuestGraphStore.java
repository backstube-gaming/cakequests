package net.backstube.cakequests.client;

import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.data.QuestBookDefinition;
import net.backstube.cakequests.data.QuestGraphParser;
import net.backstube.cakequests.data.QuestMainConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ClientQuestGraphStore {
    private static QuestBookDefinition activeBook = QuestBookDefinition.EMPTY;
    private static QuestMainConfig mainConfig = QuestMainConfig.DEFAULT;
    private static boolean fallbackMode = true;

    private ClientQuestGraphStore() {
    }

    public static QuestBookDefinition activeBook() {
        return activeBook;
    }

    public static boolean fallbackMode() {
        return fallbackMode;
    }

    public static String titleLabel() {
        return mainConfig.titleLabel();
    }

    public static String subtitle() {
        return mainConfig.subtitle();
    }

    public static void acceptServerBook(QuestBookDefinition book, QuestMainConfig config) {
        fallbackMode = false;
        activeBook = book;
        mainConfig = config;
        CakeQuests.LOGGER.info("Loaded server quest graph {} with {} tabs", book.hash(), book.tabs().size());
    }

    public static void loadFallbackGraphs() {
        loadBundledMainConfig();
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

    private static void loadBundledMainConfig() {
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(ClientQuestGraphStore.class.getClassLoader().getResourceAsStream("data/" + CakeQuests.MOD_ID + "/cakequests-main.json")),
                StandardCharsets.UTF_8
        )) {
            mainConfig = QuestMainConfig.fromJson(QuestGraphParser.GSON.fromJson(reader, com.google.gson.JsonObject.class));
        } catch (RuntimeException | IOException ex) {
            mainConfig = QuestMainConfig.DEFAULT;
            CakeQuests.LOGGER.warn("Using default Cake Quests main config", ex);
        }
    }
}
