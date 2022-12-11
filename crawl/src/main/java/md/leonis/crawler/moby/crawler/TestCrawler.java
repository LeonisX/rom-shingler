package md.leonis.crawler.moby.crawler;

import md.leonis.crawler.moby.FilesProcessor;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.executor.Executor;
import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.model.Platform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static md.leonis.crawler.moby.config.ConfigHolder.*;
import static md.leonis.crawler.moby.executor.Executor.RANDOM;
import static md.leonis.shingler.utils.FileUtils.saveAsJson;

public class TestCrawler extends AbstractCrawler {

    private int id = 0;

    public TestCrawler(FilesProcessor processor) {
        this.processor = processor;
    }

    @Override
    public List<Platform> parsePlatformsList() {
        return null;
    }

    @Override
    public List<Platform> loadPlatformsList() {
        return null;
    }

    @Override
    public void savePlatformsList(List<Platform> platforms) throws Exception {
        saveAsJson(getSourceDir(getSource()), "platforms", platforms);
    }

    @Override
    public Map<String, List<String>> loadPlatformsBindingMap() {
        return null;
    }

    @Override
    public void savePlatformsBindingMap(Map<String, List<String>> map) {

    }

    @Override
    public Map<String, List<String>> loadGamesBindingMap(String platformId, String sourcePlatformId) {
        return null;
    }

    @Override
    public void saveGamesBindingMap(String platformId, String sourcePlatformId, Map<String, List<String>> map) {

    }

    @Override
    public void parseGameEntry(GameEntry entry) {
        long sleep = 1000;
        sleep(sleep);
        boolean err = RANDOM.nextBoolean();
        boolean err2 = false;
        if (err || err2) {
            throw new RuntimeException(entry.getTitle());
        }
        for (int i = 0; i < new Random().nextInt(2) + 1; i++) {
            processor.add(new FileEntry(entry.getPlatformId(), "ROOT://test.me", "/images/" + entry.getGameId() + "_" + id++, "referrer"));
        }
    }

    @Override
    public List<GameEntry> parseGamesList(String platformId) {
        return null;
    }

    @Override
    public List<GameEntry> loadGamesList(String platformId) {
        List<GameEntry> result = new ArrayList<>();
        for (int i = 0; i < new Random().nextInt(10) + 15; i++) {
            result.add(new GameEntry(platformId, String.valueOf(i), "title" + i));
        }
        return result;
    }

    @Override
    public void saveGamesList(String platformId, List<GameEntry> games, GameEntry currentGame) throws Exception {
        saveAsJson(getGamesDir(getSource()), platformId, games);
    }

    @Override
    public void saveSupportData() {

    }

    @Override
    public String getGamePage(String platformId, String gameId) {
        return null;
    }

    @Override
    public void fileConsumer(FileEntry fileEntry) {
    }

    @Override
    public boolean isPrependPlatformId() {
        return false;
    }

    @Override
    public Path getFilePath(FileEntry fileEntry) {
        return null;
    }

    @Override
    public Path getFilePath(String platformId, String host, String uri) {
        return null;
    }

    @Override
    public Executor getExecutor() {
        return null;
    }

    @Override
    public String getHost() {
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
