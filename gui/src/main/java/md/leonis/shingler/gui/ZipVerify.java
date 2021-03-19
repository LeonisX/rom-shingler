package md.leonis.shingler.gui;

import md.leonis.shingler.utils.ArchiveUtils;
import md.leonis.shingler.utils.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ZipVerify {

    private static final Path ROOT = Paths.get("C:\\Users\\user\\shingler\\merged-roms\\snes\\roms\\snes_Super_Mario_World-Hacks1");

    public static void main(String[] args) throws IOException {

        List<Path> files = IOUtils.findFiles(ROOT, "*.zip");

        for (Path path : files) {
            List<String> results = ArchiveUtils.list(path);
            //List<String> paths = Objects.requireNonNull(results).stream().filter(p -> p.startsWith("Path = ")).collect(Collectors.toList());
            List<String> methods = Objects.requireNonNull(results).stream().filter(p -> p.startsWith("Method = ")).collect(Collectors.toList());
            long filteredMethods = methods.stream().filter(p -> p.equals("Method = Deflate")).count();

            if (methods.size() != 1) {
                System.out.println("Compilation: " + path);
            }

            if (filteredMethods != methods.size()) {
                System.out.println("Non Deflate: " + path + ": " + methods);
            }
        }

        System.out.println("Done");
    }
}
