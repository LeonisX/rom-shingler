package md.leonis.shingler.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class IOUtilsTest {

    @Test
    void findFiles() throws IOException {
        List<Path> results = IOUtils.findFiles(Paths.get("D:\\Downloads\\merged-roms"), "*.zip");
        System.out.println(results);

        // extract
        // compress
        for (Path p : results) {
            String name = p.getFileName().toString();
            Path tmp = Files.createTempDirectory("extr");
            ArchiveUtils.extract(p, tmp);
            List<Path> files = IOUtils.listFiles(tmp);

            String archiveName = p.toAbsolutePath().toString().replace("merged-roms", "merged-roms2");
            ArchiveUtils.compressZip(archiveName, files.stream().map(f -> f.toAbsolutePath().toString()).collect(Collectors.toList()));
        }
    }
}