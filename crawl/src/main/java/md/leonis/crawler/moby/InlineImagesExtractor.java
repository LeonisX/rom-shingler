package md.leonis.crawler.moby;

import org.apache.commons.compress.utils.FileNameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class InlineImagesExtractor {

    private static final boolean PREPEND_INDEX = false;
    private static final File HTML_FILE = new File("H:\\Downloads\\html\\Great Giana Sisters - TiVi форум.html");
    private static final String BASE_URI = "http://tv-games.ru/forum/blog.php?b=542";

    public static void main(String[] args) throws Exception {

        extractImages();

        //renameFiles();
    }

    private static void extractImages() throws IOException, URISyntaxException {

        Document doc = Jsoup.parse(HTML_FILE, null, BASE_URI);

        Set<String> b64 = new HashSet<>();

        int index = 1;
        for (Element img : doc.select("img[src]")) {
            String src = img.attr("src");
            if (src.startsWith("data:image")) {
                String source = img.attr("data-savepage-src");
                String name = new File(new URI(source).getPath()).getName();
                String[] chunks = src.split(";base64,");
                String type = chunks[0].split("/")[1];
                if (type.equals("jpeg")) {
                    type = "jpg";
                }
                if (!name.endsWith("." + type)) {
                    name = name + "." + type;
                }
                if (PREPEND_INDEX) {
                    name = String.format("%03d" , index++) + "-" + name;
                }

                if (!b64.contains(chunks[1])) {
                    byte[] base64 = Base64.getDecoder().decode(chunks[1]);
                    Path dir = HTML_FILE.toPath().getParent().resolve("im");

                    name = getNextAvailableFilename(dir, name);

                    Files.createDirectories(dir);
                    Files.write(dir.resolve(name), base64);
                    b64.add(chunks[1]);
                }
            }
        }
    }

    public static String getNextAvailableFilename(Path dir, String filename) {

        if (!Files.exists(dir.resolve(filename))) {
            return filename;
        }

        String alternateFilename;
        int fileNameIndex = 1;
        do {
            fileNameIndex += 1;
            alternateFilename = createNumberedFilename(filename, fileNameIndex);
        } while (Files.exists(dir.resolve(alternateFilename)));

        return alternateFilename;
    }

    private static String createNumberedFilename(String filename, int number) {

        String plainName = FileNameUtils.getBaseName(filename);
        String extension = FileNameUtils.getExtension(filename);
        return String.format("%s-%s.%s", plainName, number, extension);
    }

    private static void renameFiles() throws IOException {
        Path path = Paths.get("C:\\Users\\user\\Downloads\\im");
        Path out = path.resolve("out");
        Files.createDirectories(out);

        List<Path> files = Files.list(path).map(Path::toFile)
                .sorted(Comparator.comparing(File::lastModified))
                .map(File::toPath)  // remove this line if you would rather work with a List<File> instead of List<Path>
                .collect(Collectors.toList());

        for (int i = 1; i <= files.size(); i++) {
            Path file = files.get(i - 1);
            Files.copy(file, out.resolve(i + "." + FileNameUtils.getExtension(file.getFileName().toString())));
        }
    }
}
