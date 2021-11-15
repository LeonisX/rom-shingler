package md.leonis.shingler.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static md.leonis.shingler.gui.EmuliciousScreensRenamer.*;

public class FusionScreensRenamer {

    private static final Path PATH = Paths.get("D:\\Emulsex\\Fusion_364rus\\Screens");

    private static final Path NEW_PATH = PATH.resolve("renamed");

    public static void main(String[] args) throws IOException {

        List<String> serverPaths = new ArrayList<>();

        Map<String, List<Path>> paths = new HashMap<>();

        Files.walk(PATH).filter(Files::isRegularFile).filter(p -> {
            boolean isOk = true;
            for (int i = 0; i < p.getNameCount(); i++) {
                if (p.getName(i).toString().equals("renamed")) {
                    isOk = false;
                }
            }
            return isOk;
        }).forEach(path -> {
            String fileName = removeIndex(getName(path.getName(path.getNameCount() - 1).toString()));
            if (paths.get(fileName) == null) {
                paths.put(fileName, new ArrayList<>(Collections.singletonList(path)));
            } else {
                paths.get(fileName).add(path);
            }
        });

        for (Map.Entry<String, List<Path>> entry : paths.entrySet()) {
            for (int i = 1; i <= entry.getValue().size(); i++) {
                Path file = entry.getValue().get(i - 1);
                String fileName = file.getName(file.getNameCount() - 1).toString();
                fileName = normalize(entry.getKey()) + "_" + i + getExt(fileName);
                System.out.println(fileName);

                //TODO copy
                String subDir = fileName.substring(0, 1).toLowerCase();

                if (!subDir.matches("^[a-z]{1}$")) {
                    System.out.println(subDir);
                    throw new RuntimeException(subDir);
                }

                Path newPath = NEW_PATH.resolve(subDir);

                Files.createDirectories(newPath);

                newPath = newPath.resolve(fileName);
                //System.out.println(newPath);

                if (!Files.exists(newPath)) {
                    Files.copy(file, newPath);
                }

                serverPaths.add(fileName);
            }
        }

        Files.write(NEW_PATH.resolve("list.txt"), serverPaths);
    }

    public static String removeIndex(String fileName) {
        return fileName.substring(0, fileName.length() - 3);
    }
}
