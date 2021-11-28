package md.leonis.crawler.moby.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import md.leonis.crawler.moby.model.Platform;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
// A shared object that stores general application settings
public class ConfigHolder {

    public static List<Platform> platforms = new ArrayList<>();
    public static Map<String, Platform> platformsById = new LinkedHashMap<>();

    public static void setPlatforms(List<Platform> platforms) {
        ConfigHolder.platforms = platforms;
        platformsById = platforms.stream().collect(Collectors.toMap(Platform::getId, p -> p,
                (p1, p2) -> p1, LinkedHashMap::new));
    }

    public static String source = "moby";

    public static Path home = Paths.get(".");
    public static Path sourceDir = home.resolve(source);
    public static Path gamesDir = sourceDir.resolve("games");
    public static Path cacheDir = sourceDir.resolve("cache");
    public static Path pagesDir = sourceDir.resolve("pages");

    //TODO config
    public static Path userHome = Paths.get(System.getProperty("user.home"));
    public static Path rootWorkDir = userHome.resolve("shingler");
    public static Path shinglesDir = rootWorkDir.resolve("shingles");
    public static Path collectionsDir = rootWorkDir.resolve("collections");
    public static Path familiesDir = rootWorkDir.resolve("families");


    //TODO notifications
    public static Boolean[] needToStop = new Boolean[]{false};

    public static Set<String> runningTasks = new HashSet<>();

    public static void registerRunningTask(String name) {
        runningTasks.add(name);
    }

    public static void unRegisterRunningTask(String name) {
        runningTasks.remove(name);
        if (runningTasks.isEmpty()) {
            needToStop[0] = false;
        }
    }
}
