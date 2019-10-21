package md.leonis.shingler;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ListFiles {

    public static void main(String[] args) throws IOException {

        List<File> files = Arrays.asList(Objects.requireNonNull(new File("D:\\Downloads\\Nintendo Famicom - GoodNES ROMS v3.23b Merged").listFiles()));

        Map<File, List<String>> map = files.stream().collect(Collectors.toMap(Function.identity(), ListFiles::listFiles));

        List<String> lines = new ArrayList<>();

        map.forEach((key, value) -> {
            List<String> names = value.stream().map(ListFiles::normalize).distinct().collect(Collectors.toList());
            map.replace(key, names);
        });


        map.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getName())).forEach(e -> {
            lines.add(e.getKey().getName());
            e.getValue().forEach(v -> lines.add("  " + v));
        });

        Files.write(Paths.get("s"), lines);
    }

    //TODO https://stackoverflow.com/questions/524548/regular-expression-to-detect-semi-colon-terminated-c-for-while-loops/524624#524624
    private static String normalize(String s) {
        s = s.replace(")(", ") (");
        s = s.replace("][", "] [");
        s = s.replace(")[", ") [");
        s = s.replace("](", "] (");
        List<String> chunks = new ArrayList<>();

        Arrays.stream(s.split(" ")).forEach(c -> {
            if (
                    !c.matches("^\\[[abhfopt]\\d+]$") &&
                            !c.matches("^\\[T[\\-\\+].+\\]$") &&
                            !c.matches("^\\(PRG[0123]\\)$") &&
                            !c.matches("^\\[hFFE]$") &&
                            !c.matches("^\\[hM\\d{2}\\]$") &&

                            !c.matches("^\\((JU|PC10|VS|GC|Ch|M3|[JUERK])\\)$") &&

                            !c.matches("^\\[[!U]]$")
            ) {
                chunks.add(c);
            }
        });

        return String.join(" ", chunks);
    }

    private static String getCleanName(String s) {
        int braceIndex = s.indexOf("(");
        if (braceIndex > 0) {
            s = s.substring(0, braceIndex);
        }
        braceIndex = s.indexOf("[");
        if (braceIndex > 0) {
            s = s.substring(0, braceIndex);
        }
        return s.trim();
    }

    private static List<String> listFiles(File file) {
        try (SevenZFile archiveFile = new SevenZFile(file)) {
            return StreamSupport.stream(archiveFile.getEntries().spliterator(), false).map(SevenZArchiveEntry::getName).map(s -> s.substring(0, s.lastIndexOf('.'))).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
