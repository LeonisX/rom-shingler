package md.leonis.crawler.moby.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import md.leonis.crawler.moby.FilesProcessor;
import md.leonis.crawler.moby.HttpProcessor;
import md.leonis.crawler.moby.crawler.Crawler;
import md.leonis.crawler.moby.crawler.TestCrawler;
import md.leonis.crawler.moby.crawler.YbomCrawler;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.executor.TestExecutor;
import md.leonis.crawler.moby.model.Activity;
import md.leonis.crawler.moby.model.Platform;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
// A shared object that stores general application settings
public class ConfigHolder {

    public static List<Platform> platforms = new ArrayList<>();
    public static Map<String, Platform> platformsById = new LinkedHashMap<>();
    public static Activity activity;

    public static void setPlatforms(List<Platform> platforms) {
        ConfigHolder.platforms = platforms;
        platformsById = platforms.stream().collect(Collectors.toMap(Platform::getId, p -> p,
                (p1, p2) -> p1, LinkedHashMap::new));
    }

    private static String source = "";

    public static void setSource(String source) {
        ConfigHolder.source = source;

        sourceDir = home.resolve(source);
        gamesDir = sourceDir.resolve("games");
        cacheDir = sourceDir.resolve("cache");
        pagesDir = sourceDir.resolve("pages");
    }

    public static String getSource() {
        return source;
    }

    public static Path home = Paths.get(".");
    public static Path sourceDir = home.resolve(source);
    public static Path gamesDir = sourceDir.resolve("games");
    public static Path cacheDir = sourceDir.resolve("cache");
    public static Path pagesDir = sourceDir.resolve("pages");

    public static Crawler getCrawler() {

        switch (getSource()) {
            case "moby":
                return new YbomCrawler(4);
            case "test":
                Queue<FileEntry> queue = new ConcurrentLinkedQueue<>();
                List<HttpProcessor> processors = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    processors.add(new HttpProcessor(i, queue, new TestExecutor(), 1, 1));
                }
                return new TestCrawler(new FilesProcessor(processors));
            default:
                throw new RuntimeException("Unknown source: " + getSource());
        }
    }

    public static Map<String, List<Throwable>> errorsMap;
}
