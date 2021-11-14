package md.leonis.shingler.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GbScreensRenamer {

    private static final Path PATH = Paths.get("C:\\Users\\user\\Downloads\\Game Boy games");

    private static final Path NEW_PATH = PATH.resolve("renamed");

    public static void main(String[] args) throws IOException {

        List<Path> dirs = Files.walk(PATH).skip(1).filter(Files::isDirectory)
                .filter(p -> {
                    boolean isOk = true;
                    for (int i = 0; i < p.getNameCount(); i++) {
                        if (p.getName(i).toString().equals("renamed")) {
                            isOk = false;
                        }
                    }
                    return isOk;
                }).collect(Collectors.toList());


        List<String> serverPaths = new ArrayList<>();


        for (Path dir : dirs) {//System.out.println(dir);

            String dirName = EmuliciousScreensRenamer.normalize(dir.getName(dir.getNameCount() - 1).toString());

            List<Path> files = Files.walk(dir).filter(Files::isRegularFile).collect(Collectors.toList());

            for (int i = 1; i <= files.size(); i++) {
                //TODO rename
                Path file = files.get(i - 1);

                String fileName = file.getName(file.getNameCount() - 1).toString();
                fileName = dirName + "_" + i + getExt(fileName);

                System.out.println(fileName);

                String subDir = fileName.substring(0, 1).toLowerCase();

                if (!subDir.matches("^[a-z]{1}$")) {
                    System.out.println(subDir);
                    throw new RuntimeException(subDir);
                }

                //TODO copy
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

    private static String getExt(String fileName) {

        int lastPos = fileName.lastIndexOf(".");

        if (lastPos == -1) {
            return "";
        }

        return fileName.substring(lastPos);
    }
}
