package md.leonis.crawler.moby.crawler;

import md.leonis.crawler.moby.FilesProcessor;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.model.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static md.leonis.crawler.moby.config.ConfigHolder.*;
import static md.leonis.crawler.moby.executor.Executor.RANDOM;
import static md.leonis.shingler.utils.FileUtils.saveAsJson;

public class TestCrawler extends AbstractCrawler {

    private int id = 0;

    private FilesProcessor processor;

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
    public Map<String, List<String>> loadPlatformsBindingMap() throws Exception {
        return null;
    }

    @Override
    public void savePlatformsBindingMap(Map<String, List<String>> map) throws Exception {

    }

    @Override
    public Map<String, List<String>> loadGamesBindingMap(String platformId, String mobyPlatformId) throws Exception {
        return null;
    }

    @Override
    public void saveGamesBindingMap(String platformId, String mobyPlatformId, Map<String, List<String>> map) throws Exception {

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
    public void saveGamesList(String platformId, List<GameEntry> games) throws Exception {
        saveAsJson(getGamesDir(getSource()), platformId, games);
    }

    @Override
    public void saveSupportData() {

    }

    @Override
    public FilesProcessor getProcessor() {
        return processor;
    }

    @Override
    public String getGamePage(String platformId, String gameId) {
        return null;
    }

    @Override
    public void setProcessor(FilesProcessor processor) {
        this.processor = processor;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
