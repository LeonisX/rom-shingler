package md.leonis.shingler.gui.config;

import javafx.stage.DirectoryChooser;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
// A shared object that stores general application settings
public class ConfigHolder {

    //TODO config
    public static Path userHome = Paths.get(System.getProperty("user.home"));
    public static Path rootWorkDir = userHome.resolve("shingler");
    public static Path shinglesDir = rootWorkDir.resolve("shingles");
    public static Path collectionsDir = rootWorkDir.resolve("collections");

    public static LinkedHashMap<String, String> platforms = new LinkedHashMap<>();

    public static String platform;
    public static String collection;

    public static Path workCollectionsDir() {
        return collectionsDir.resolve(platform);
    }

    public static List<String> selectedCollections;

    private final int wordsToLearnCount = 20;

    private final Map<String, File> initialDirs = new HashMap<>();

    //TODO not here
    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    public DirectoryChooser getDirectoryChooser(String title) {
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(initialDirs.get(title));
        return directoryChooser;
    }

    public void saveInitialDir(DirectoryChooser directoryChooser, File file) {
        if (null != file) {
            initialDirs.put(directoryChooser.getTitle(), file);
        }
    }
}
