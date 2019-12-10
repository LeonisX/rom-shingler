package md.leonis.shingler.utils;

import md.leonis.shingler.model.Family;
import md.leonis.shingler.model.FamilyType;
import md.leonis.shingler.model.Name;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static md.leonis.shingler.model.ConfigHolder.*;
import static org.slf4j.LoggerFactory.getLogger;

public class TiviUtils {

    private static final Logger LOGGER = getLogger(TiviUtils.class);

    private static final String HEAD = "<html>%n<head>%n" +
            "  <title>Игры %s</title>%n" +
            "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">%n" +
            "  <META content=Sinoel name=author>%n" +
            "</head>%n" +
            "<body bgcolor=\"#FFFFFF\" text=\"#000000\">%n" +
            "  <a href=\"..\"><<на главную страницу</a><br>%n" +
            "  <table width=\"95%%\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" bgcolor=\"#FFFFFF\" bordercolor=\"#000000\">%n";

    private static final String FOOT = "  </table>\n</body>\n</html>";

    private static final int MAX_SIZE = 60000; // 65525, but separate archives are bigger
    private static final int DIR_SIZE = 1000;
    private static final double DIR_SEPARATE_SIZE = 986.0;

    // html+dump+force63+split
    public static void generatePageAndRomsForTvRoms() {

        Map<String, String> normalizedMap = new HashSet<>(IOUtils.loadTextFile(Paths.get("lists").resolve(platform + ".txt")))
                .stream().collect(Collectors.toMap(StringUtils::normalize, Function.identity()));
        Set<String> dbGamesSet = normalizedMap.keySet();

        List<String> lines = new ArrayList<>();
        List<String> linesTxt = new ArrayList<>();
        List<String> unmappedLines = new ArrayList<>();
        List<String> updateLines = new ArrayList<>();

        boolean needToSeparate = tribes.keySet().size() > DIR_SIZE;
        AtomicInteger g = new AtomicInteger(1);

        tribes.keySet().stream().sorted().forEach(t -> {

            String sourceArchiveName = StringUtils.addExt(t, "7z");
            Path sourceArchive = outputDir.resolve(platform).resolve(sourceArchiveName);
            int fileSize = (int) IOUtils.fileSize(sourceArchive) / 1024;

            String platformGroup = needToSeparate ? String.format("%s%s", platform, (int) Math.ceil(g.getAndIncrement() / DIR_SEPARATE_SIZE)) : platform;
            Path destPath = outputDir.resolve(platform).resolve("games").resolve(platformGroup);
            IOUtils.createDirectories(destPath);

            if (fileSize < MAX_SIZE) {

                String destArchiveName = StringUtils.normalize(sourceArchiveName);
                Path destArchive = destPath.resolve(destArchiveName);

                IOUtils.copyFile(sourceArchive, destArchive);

                String romPath = formatRomPath(platformGroup, destArchiveName);

                lines.add(formatTableCell(romPath, sourceArchiveName, fileSize));
                linesTxt.add(romPath);

                for (Family f : tribes.get(t)) {
                    String familyName = escapeQuotes(f.getName());
                    if (!dbGamesSet.contains(StringUtils.normalize(familyName))) {
                        unmappedLines.add(romPath);
                    } else {
                        dbGamesSet.remove(StringUtils.normalize(familyName));
                        updateLines.add(formatUpdateQuery(platform, romPath, familyName));
                    }
                }
            } else {
                //TODO split  by letters or families, don't tear them
                LOGGER.info("Splitting {}", t);
                List<String> members = tribes.get(t).stream().flatMap(f -> f.getMembers().stream()).map(Name::getName).sorted().collect(Collectors.toList());

                final int archivesCount = new Double(Math.ceil(fileSize * 1.0 / MAX_SIZE)).intValue();
                final int membersCount = members.size() / archivesCount;
                final AtomicInteger counter = new AtomicInteger();

                final Collection<List<String>> result = members.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / membersCount)).values();

                int i = 1;
                for (List<String> chunk : result) {

                    sourceArchiveName = String.format("%s (part %s).7z", t, i);
                    String destArchiveName = StringUtils.normalize(String.format("%s part %s.7z", t, i).replace("_", " "));

                    ArchiveUtils.compress7z(destArchiveName, chunk, i++);

                    sourceArchive = outputDir.resolve(platform).resolve(destArchiveName);
                    Path destArchive = destPath.resolve(destArchiveName);
                    fileSize = (int) IOUtils.fileSize(sourceArchive) / 1024;
                    IOUtils.copyFile(sourceArchive, destArchive);
                    IOUtils.deleteFile(sourceArchive);

                    String romPath = formatRomPath(platformGroup, destArchiveName);

                    lines.add(formatTableCell(romPath, sourceArchiveName, fileSize));
                    linesTxt.add(romPath);
                    //TODO detect correct part
                    for (Family f : tribes.get(t)) {
                        String familyName = escapeQuotes(f.getName());
                        if (!dbGamesSet.contains(StringUtils.normalize(familyName))) {
                            unmappedLines.add(romPath);
                        } else {
                            dbGamesSet.remove(StringUtils.normalize(familyName));
                            updateLines.add(formatUpdateQuery(platform, romPath, familyName));
                        }
                    }
                }
            }
        });

        IOUtils.saveToFile(platform + "_games.htm", formatHead() + String.join("\n", lines) + FOOT);
        IOUtils.saveToFile(platform + "_games.txt", linesTxt);
        IOUtils.saveToFile(platform + "_games_update.sql", updateLines);
        IOUtils.saveToFile(platform + "_games_unmapped.txt", unmappedLines);
        IOUtils.saveToFile(platform + "_games_unmapped_names.txt", dbGamesSet.stream().sorted().map(normalizedMap::get).collect(Collectors.toList()));
    }

    private static String formatHead() {
        return String.format(HEAD, platformsByCpu.get(platform).getTitle());
    }

    private static String formatRomPath(String platform, String destArchiveName) {
        return String.format("http://tv-roms.narod.ru/games/%s/%s", platform, destArchiveName);
    }

    private static String formatTableCell(String romPath, String sourceArchiveName, int fileSize) {
        return String.format("    <tr><td><a href=\"%s\">%s</a></td><td>%s Kb</td></tr>", romPath, sourceArchiveName, fileSize);
    }

    private static String formatUpdateQuery(String platform, String romPath, String familyName) {
        return String.format("UPDATE `base_%s` SET game='%s' WHERE name='%s';", platform, romPath, familyName);
    }

    private static String escapeQuotes(String family) {
        return family.replace("&", "&amp;").replace("'", "&rsquo;");
    }

    private static final Map<String, String> GROUP_MAP = new HashMap<>();

    static {
        GROUP_MAP.put("public domain", "pd");
        GROUP_MAP.put("multicarts collection", "multi");
        GROUP_MAP.put("vt03 collection", "vt03");
        GROUP_MAP.put("wxn collection", "multi");
    }

    //(list+dump+force63)
    public static void getAllUniqueRoms() {

        Map<String, String> normalizedMap = new HashSet<>(IOUtils.loadTextFile(Paths.get("lists").resolve(platform + ".txt")))
                .stream().collect(Collectors.toMap(StringUtils::normalize, Function.identity()));
        Set<String> names = normalizedMap.keySet();

        Path uniquePath = outputDir.resolve(platform).resolve("roms");

        IOUtils.createDirectories(uniquePath);

        List<String> lines = new ArrayList<>();
        List<String> linesTxt = new ArrayList<>();
        List<String> unmappedLines = new ArrayList<>();
        List<String> updateLines = new ArrayList<>();

        LOGGER.info("Processing families...");

        Map<Name, Family> familyMap = new HashMap<>();
        boolean needToSeparate = families.keySet().size() > DIR_SIZE;
        AtomicInteger g = new AtomicInteger(1);

        for (Family family : families.values().stream().filter(f -> f.getType() == FamilyType.FAMILY).collect(Collectors.toList())) {
            Name name = family.getMembers().stream()/*.filter(n -> ListFilesa.nonHack(n.getName()))*/
                    .collect(Collectors.groupingBy(Name::getCleanName))
                    .values().stream().flatMap(l -> l.stream().sorted(Comparator.comparing(Name::getIndex).reversed())).findFirst().orElse(null);
            familyMap.put(name, family);
        }

        AtomicInteger j = new AtomicInteger(0);
        familyMap.entrySet().stream().sorted(Comparator.comparing(e -> e.getValue().getName())).forEach(e -> {
            String familyName = escapeQuotes(e.getValue().getName());
            String sourceRomName = e.getKey().getName();
            String zipRomName = StringUtils.normalize(StringUtils.addExt(e.getValue().getName(), "zip"));

            String platformGroup = needToSeparate ? String.format("%s%s", platform, (int) Math.ceil(g.getAndIncrement() / DIR_SEPARATE_SIZE)) : platform;

            Path sourceRom = romsCollection.getRomsPath().resolve(sourceRomName);
            Path destZipRom = uniquePath.resolve(platformGroup).resolve(zipRomName);

            ArchiveUtils.compressZip(destZipRom.toAbsolutePath().toString(), Collections.singletonList(sourceRom.toAbsolutePath().toString()), j.getAndIncrement());

            String romPath = formatUniqueRomPath(platformGroup, zipRomName);
            int fileSize = (int) IOUtils.fileSize(destZipRom) / 1024;
            lines.add(formatTableCell(romPath, sourceRomName, fileSize));
            linesTxt.add(String.format("%s/%s", platformGroup, zipRomName));

            if (!names.contains(StringUtils.normalize(familyName))) {
                unmappedLines.add(romPath);
            } else {
                names.remove(StringUtils.normalize(familyName));
                updateLines.add(String.format("UPDATE `base_%s` SET rom='%s' WHERE name='%s';", platformGroup, romPath, familyName));
            }
        });

        LOGGER.info("Processing groups...");

        AtomicInteger i = new AtomicInteger(0);
        families.values().stream().filter(f -> f.getType() == FamilyType.GROUP)
                .forEach(f -> {
                    String title = Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1) + ":";
                    lines.add("    <tr><td></td></tr>");
                    lines.add(String.format("    <tr><td><b>%s</b></td></tr>", title));
                    linesTxt.add("");
                    linesTxt.add(title);

                    f.getMembers().forEach(n -> {
                        String familyName = f.getName();
                        String sourceRomName = n.getName();
                        String zipRomName = StringUtils.normalize(StringUtils.replaceExt(sourceRomName, "zip"));

                        Path sourceRom = romsCollection.getRomsPath().resolve(sourceRomName);
                        String dir = GROUP_MAP.get(f.getName().toLowerCase());
                        if (dir != null) {
                            dir = String.format("%s_%s", platform, dir);
                        } else {
                            dir = StringUtils.normalize(f.getName().replace("_", " ")).toLowerCase();
                        }
                        Path destZipRom = uniquePath.resolve(dir).resolve(zipRomName);

                        ArchiveUtils.compressZip(destZipRom.toAbsolutePath().toString(), Collections.singletonList(sourceRom.toAbsolutePath().toString()), i.getAndIncrement());

                        String romPath = formatUniqueRomPath(dir, zipRomName);
                        int fileSize = (int) IOUtils.fileSize(destZipRom) / 1024;
                        lines.add(formatTableCell(romPath, sourceRomName, fileSize));
                        linesTxt.add(String.format("%s/%s", dir, zipRomName));

                        if (!names.contains(StringUtils.normalize(familyName))) {
                            unmappedLines.add(romPath);
                        } else {
                            names.remove(StringUtils.normalize(familyName));
                            updateLines.add(String.format("UPDATE `base_%s` SET rom='%s' WHERE name='%s';", platform, romPath, escapeQuotes(familyName)));
                        }
                    });
                });

        IOUtils.saveToFile(platform + "_roms.htm", formatHead() + String.join("\n", lines) + FOOT);
        IOUtils.saveToFile(platform + "_roms.txt", linesTxt);
        IOUtils.saveToFile(platform + "_roms_update.sql", updateLines);
        IOUtils.saveToFile(platform + "_roms_unmapped.txt", unmappedLines);
        IOUtils.saveToFile(platform + "_roms_unmapped_names.txt", names.stream().sorted().map(normalizedMap::get).collect(Collectors.toList()));
    }

    private static String formatUniqueRomPath(String platform, String destArchiveName) {
        return String.format("http://cominf0.narod.ru/emularity/%s/%s", platform, destArchiveName);
    }
}
