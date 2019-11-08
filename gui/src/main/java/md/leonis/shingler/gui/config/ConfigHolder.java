package md.leonis.shingler.gui.config;

import javafx.stage.DirectoryChooser;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
// A shared object that stores general application settings
public class ConfigHolder {

    private final int wordsToLearnCount = 20;

    private final Map<String, File> initialDirs = new HashMap<>();

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
