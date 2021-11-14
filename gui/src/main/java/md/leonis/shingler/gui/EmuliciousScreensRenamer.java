package md.leonis.shingler.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class EmuliciousScreensRenamer {

    private static final Path PATH = Paths.get("D:\\Emulsex\\Emulicious-with-Java\\screenshots");

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

    public static String normalize(String fileName) {

        if (fileName.startsWith("The ")) {
            fileName = fileName.replaceFirst("The ", "");
        }

        fileName = fileName
                .replace(" - ", "_")
                .replace("-", "_")
                .replace("'", "")
                .replace(",", "")
                .replace(".", "")
                .replace("(", "")
                .replace(")", "")
                .replace("\"", "")
                .replace(" & ", "_")
                .replace("!", "")
                .replace(" The ", " ")
                .replace(" ", "_")
                .replace("__", "_");

        if (!fileName.matches("^[a-zA-Z0-9_]+$")) {
            System.out.println(fileName);
            throw new RuntimeException(fileName);
        }

        return fileName;
    }

    public static String removeIndex(String fileName) {
        return fileName.replaceFirst("-[0-9]+$", "");
    }

    public static String getName(String fileName) {
        return fileName.substring(0, fileName.length() - getExt(fileName).length());
    }

    public static String getExt(String fileName) {

        int lastPos = fileName.lastIndexOf(".");

        if (lastPos == -1) {
            return "";
        }

        return fileName.substring(lastPos);
    }
}
