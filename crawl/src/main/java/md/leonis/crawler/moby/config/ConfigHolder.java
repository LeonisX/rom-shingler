package md.leonis.shingler.gui.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import md.leonis.shingler.model.Family;
import md.leonis.shingler.model.GID;
import md.leonis.shingler.model.Platform;
import md.leonis.shingler.model.RomsCollection;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Data
@NoArgsConstructor
// A shared object that stores general application settings
public class ConfigHolder {

    public static final List<Integer> SHINGLES_LEVELS = Arrays.asList(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024);

    public static final int defaultShinglesLevel = 8; // id == 3

    private static int shinglesLevelId;
    private static int shinglesLevel;

    static {
        setShinglesLevel(defaultShinglesLevel);
    }

    public static void setShinglesLevelId(int shinglesLevelId) {
        ConfigHolder.shinglesLevelId = shinglesLevelId;
        ConfigHolder.shinglesLevel = SHINGLES_LEVELS.get(shinglesLevelId);
    }

    public static int getShinglesLevelId() {
        return shinglesLevelId;
    }

    public static int getShinglesLevel() {
        return shinglesLevel;
    }

    public static void setShinglesLevel(int shinglesLevel) {
        ConfigHolder.shinglesLevelId = SHINGLES_LEVELS.indexOf(shinglesLevel);
        ConfigHolder.shinglesLevel = shinglesLevel;
    }

    //TODO config
    public static Path userHome = Paths.get(System.getProperty("user.home"));
    public static Path rootWorkDir = userHome.resolve("shingler");
    public static Path shinglesDir = rootWorkDir.resolve("shingles");
    public static Path collectionsDir = rootWorkDir.resolve("collections");
    public static Path familiesDir = rootWorkDir.resolve("families");

    public static String romsUrl = "http://tv-roms.narod.ru/games/";
    public static String uniqueRomsUrl = "http://cominf0.narod.ru/mame/";

    public static LinkedHashMap<String, Platform> platforms = new LinkedHashMap<>();
    public static HashMap<String, Platform> platformsByCpu = new HashMap<>();

    public static String platform;
    public static String collection;

    public static RomsCollection romsCollection;
    public static Map<String, Family> families;
    public static Map<String, List<Family>> tribes;
    public static BooleanProperty familiesModified = new SimpleBooleanProperty(false);
    public static Map<String, GID> byHash;
    public static Map<String, GID> byTitle;

    public static Map<Family, Map<Family, Double>> familyRelations;
    public static BooleanProperty familyRelationsModified = new SimpleBooleanProperty(false);

    //TODO from config, may be bind to platform or collection
    public static double jakkard = 50;

    public static Path workShinglesPath() {
        return shinglesDir.resolve(platform);
    }

    public static Path fullShinglesPath() {
        return workShinglesPath().resolve("sample-" + shinglesLevel);
    }

    public static Path workCollectionsPath() {
        return collectionsDir.resolve(platform);
    }

    public static Path fullCollectionsPath() {
        return workCollectionsPath().resolve(collection);
    }

    public static Path getInputPath() {
        return rootWorkDir.resolve("input");
    }

    public static Path getOutputPath() {
        return rootWorkDir.resolve("merged-roms");
    }

    public static Path workFamiliesPath() {
        return familiesDir.resolve(platform);
    }

    public static Path fullFamiliesPath() {
        return workFamiliesPath().resolve(collection + "-" + shinglesLevel);
    }

    public static Path fullFamilyRelationsPath() {
        return workFamiliesPath().resolve(collection + "-relations-" + shinglesLevel);
    }

    public static Path fullTribeRelationsPath() {
        return workFamiliesPath().resolve(collection + "-tribe-relations-" + shinglesLevel);
    }

    //TODO bad solution
    public static List<String> selectedCollections;

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

    public static int candidates = 48;
    public static int showCandidates = 8;
}
