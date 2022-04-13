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
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class InlineImagesExtractor {

    private static final boolean PREPEND_INDEX = true;

    public static void main(String[] args) throws IOException, URISyntaxException {

        File file = new File("C:\\Users\\user\\Documents\\gata\\src\\books-add\\baidu.html");
        Document doc = Jsoup.parse(file, null, "http://tv-games.ru/forum/blog.php?b=1562");

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
                    Path dir = file.toPath().getParent().resolve("im");

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
}
