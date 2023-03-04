package md.leonis.crawler.moby;

import md.leonis.shingler.utils.FileUtils;
import md.leonis.shingler.utils.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static md.leonis.shingler.utils.StringUtils.unescapeChars;

public class AbimeConverter {

    private static final Path rootPath = Paths.get("H:\\download\\hol.abime.net");
    private static final Path mapsPath = rootPath.resolve("pic_full").resolve("gamemap");

    public static void main(String[] args) throws IOException {
        Map<String, String> names = new HashMap<>();

        FileUtils.createDirectories(rootPath.resolve("gamemap"));

        try (Stream<Path> stream = Files.walk(mapsPath)) {
            stream.filter(Files::isRegularFile)
                    .filter(f -> !f.toString().toLowerCase().endsWith("wd3"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String[] chunks = fileName.split("_");
                        String gameName = names.get(chunks[0]);
                        if (gameName == null) {
                            gameName = getGameName(chunks[0]);
                            names.put(chunks[0], gameName);
                        }
                        //simplify, rename, copy
                        String simpleName = fileName.replace("gamemap", "");
                        String newFileName = String.format("%s_%s", normalizeImageName(gameName), simpleName);
                        //System.out.println(chunks[0] + ": " + newFileName);
                        Path newPath = rootPath.resolve("gamemap").resolve(newFileName);
                        /*try {
                            Files.copy(path, newPath);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }*/


                    });
        }
        names.values().stream().distinct().sorted().forEach(System.out::println);
        Files.write(rootPath.resolve("list.txt"), names.values().stream().distinct().sorted().collect(Collectors.toList()));
    }

    private static String getGameName(String chunk) {
        try {
            Path pagePath = rootPath.resolve(chunk).resolve("gamemap");
            Document doc = Jsoup.parse(Files.lines(pagePath).collect(Collectors.joining()));
            return doc.head().getElementsByTag("TITLE").get(0).text().replace(" : Hall Of Light - The database of Amiga games", "").trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String normalizeImageName(String name) {
        return StringUtils.cleanString(unescapeChars(name)).replace(" ", "_");
    }
}
