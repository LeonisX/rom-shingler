package md.leonis.shingler.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Data
@NoArgsConstructor
// A shared object that stores general application settings
public class ConfigHolder {

    public static final List<Integer> DENOMINATORS = Arrays.asList(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024);

    public static final int defaultDenominator = 3; // 8

    private static int denominatorId;
    private static int denominator;

    static {
        setDenominatorId(defaultDenominator);
    }

    public static int getDenominatorId() {
        return denominatorId;
    }

    public static int getDenominator() {
        return denominator;
    }

    public static void setDenominatorId(int denominatorId) {
        ConfigHolder.denominatorId = denominatorId;
        ConfigHolder.denominator = DENOMINATORS.get(denominatorId);
    }

    //TODO config
    public static Path userHome = Paths.get(System.getProperty("user.home"));
    public static Path rootWorkDir = userHome.resolve("shingler");
    public static Path shinglesDir = rootWorkDir.resolve("shingles");
    public static Path collectionsDir = rootWorkDir.resolve("collections");
    public static Path familiesDir = rootWorkDir.resolve("families");

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
        return workShinglesPath().resolve("sample-" + denominator);
    }

    public static Path workCollectionsPath() {
        return collectionsDir.resolve(platform);
    }

    public static Path fullCollectionsPath() {
        return workCollectionsPath().resolve(collection);
    }

    public static Path workFamiliesPath() {
        return familiesDir.resolve(platform);
    }

    public static Path fullFamiliesPath() {
        return workFamiliesPath().resolve(collection + "-" + denominator);
    }

    public static Path fullFamilyRelationsPath() {
        return workFamiliesPath().resolve(collection + "-relations-" + denominator);
    }

    public static Path fullTribeRelationsPath() {
        return workFamiliesPath().resolve(collection + "-tribe-relations-" + denominator);
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

    public static Path outputDir = Paths.get("D:\\Downloads\\merged-roms");
}
