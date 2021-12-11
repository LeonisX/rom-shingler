package md.leonis.crawler.moby.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import md.leonis.crawler.moby.FilesProcessor;
import md.leonis.crawler.moby.HttpProcessor;
import md.leonis.crawler.moby.controller.GamesBindingController;
import md.leonis.crawler.moby.crawler.Crawler;
import md.leonis.crawler.moby.crawler.TestCrawler;
import md.leonis.crawler.moby.crawler.YbomCrawler;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.executor.TestExecutor;
import md.leonis.crawler.moby.model.Activity;
import md.leonis.crawler.moby.model.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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

    public static Map<String, List<String>> platformsBindingMap;
    public static List<Map.Entry<String, List<String>>> platformsBindingMapEntries;

    public static void setPlatforms(List<Platform> platforms) {
        ConfigHolder.platforms = platforms;
        platformsById = platforms.stream().collect(Collectors.toMap(Platform::getId, p -> p,
                (p1, p2) -> p1, LinkedHashMap::new));
    }

    private static String source = "";

    public static void setSource(String source) {
        ConfigHolder.source = source;
    }

    public static String getSource() {
        return source;
    }

    public static Path home = Paths.get(".");

    public static Path getSourceDir(String source) {
        return home.resolve(source);
    }

    public static Path getGamesDir(String source) {
        return home.resolve(source).resolve("games");
    }

    public static Path getCacheDir(String source) {
        return home.resolve(source).resolve("cache");
    }

    public static Path getPagesDir(String source) {
        return home.resolve(source).resolve("pages");
    }

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

    public static GamesBindingController.Structure tiviStructure;
    public static GamesBindingController.Structure mobyStructure;

    public static Map<String, List<Throwable>> errorsMap;


    public static String apiPath;
    public static String sitePath;
    public static String localSitePath;
    public static String serverSecret;

    public static void loadProtectedProperties() throws IOException {
        try (InputStream inputStream = Files.newInputStream(home.resolve("protected.properties"))){
            Properties prop = new Properties();
            prop.load(inputStream);
            String apiDir = prop.getProperty("api.dir") + "/";
            sitePath = prop.getProperty("site.path") + "/";
            localSitePath = prop.getProperty("local.site.path") + "/";
            serverSecret = prop.getProperty("server.secret");
            apiPath = sitePath + apiDir;
        }
    }
}
