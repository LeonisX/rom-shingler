package md.leonis.shingler.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public static LinkedHashMap<String, String> platforms = new LinkedHashMap<>();

    public static String platform;
    public static String collection;

    public static RomsCollection romsCollection;
    public static Map<String, Family> families;
    public static Map<String, GID> byHash;
    public static Map<String, GID> byTitle;

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

    //TODO bad solution
    public static List<String> selectedCollections;

}