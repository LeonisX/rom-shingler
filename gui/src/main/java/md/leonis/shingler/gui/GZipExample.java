package md.leonis.shingler.gui;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

public class GZipExample {

    public static void main(String[] args) {

        // compress a file
        Path source = Paths.get("/home/mkyong/test/sitemap.xml");
        Path target = Paths.get("/home/mkyong/test/sitemap.xml.gz");

        if (Files.notExists(source)) {
            System.err.printf("The path %s doesn't exist!", source);
            return;
        }

        try {
            GZipExample.compressGzip(source, target);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // copy file (FileInputStream) to GZIPOutputStream
    public static void compressGzip(Path source, Path target) throws IOException {

        try (GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(target.toFile()));
             FileInputStream fis = new FileInputStream(source.toFile())) {

            // copy file
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gos.write(buffer, 0, len);
            }
        }
    }

    public static void compressGzipNio(Path source, Path target) throws IOException {

        try (GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(target.toFile()))) {
            Files.copy(source, gos);
        }
    }
}