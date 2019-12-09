package md.leonis.shingler.utils;

import md.leonis.shingler.ListFilesa;
import md.leonis.shingler.model.Family;
import md.leonis.shingler.model.FamilyType;
import md.leonis.shingler.model.Name;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static md.leonis.shingler.model.ConfigHolder.*;
import static org.slf4j.LoggerFactory.getLogger;

public class TiviUtils {

    private static final Logger LOGGER = getLogger(TiviUtils.class);

    private static int MAX_SIZE = 60000; // 65525, but separate archives are bigger

    // html+dump+force63+split
    public static void generatePageAndRomsForTvRoms() {

        String head = "<html>\n<head>\n" +
                "  <title>Игры Nintendo/Dendy GoodNES 3.23b</title>\n" +
                "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                "  <META content=Sinoel name=author>\n" +
                "</head>\n" +
                "<body bgcolor=\"#FFFFFF\" text=\"#000000\">\n" +
                "  <a href=\"..\"><<на главную страницу</a><br>\n" +
                "  <table width=\"95%\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" bgcolor=\"#FFFFFF\" bordercolor=\"#000000\">\n";

        String foot = "  </table>\n</body>\n</html>";

        Set<String> dbGamesSet = new HashSet<>(IOUtils.loadTextFile(Paths.get("lists").resolve(platform + ".txt")));

        Path destPath = outputDir.resolve(platform).resolve("games");
        IOUtils.createDirectories(destPath);

        List<String> lines = new ArrayList<>();
        List<String> linesTxt = new ArrayList<>();
        List<String> unmappedLines = new ArrayList<>();
        List<String> updateLines = new ArrayList<>();

        tribes.keySet().stream().sorted().forEach(t -> {

            String sourceArchiveName = StringUtils.addExt(t, "7z");
            Path sourceArchive = outputDir.resolve(platform).resolve(sourceArchiveName);
            int fileSize = (int) IOUtils.fileSize(sourceArchive) / 1024;

            if (fileSize < MAX_SIZE) {

                String destArchiveName = StringUtils.removeSpecialChars(sourceArchiveName.replace("_", " ")); // remove special symbols
                destArchiveName = StringUtils.force63(destArchiveName);
                Path destArchive = destPath.resolve(destArchiveName);

                IOUtils.copyFile(sourceArchive, destArchive);

                String romPath = formatRomPath(destArchiveName);

                lines.add(formatTableCell(romPath, sourceArchiveName, fileSize));
                linesTxt.add(romPath);

                for (Family f : tribes.get(t)) {
                    String familyName = formatFamilyName(f);
                    if (!dbGamesSet.contains(familyName)) {
                        unmappedLines.add(romPath);
                    } else {
                        dbGamesSet.remove(familyName);
                        updateLines.add(formatUpdateQuery(romPath, familyName));
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
                for(List<String> chunk: result) {

                    //TODO
                    sourceArchiveName = String.format("%s (part %s).7z", t, i);
                    String destArchiveName = StringUtils.removeSpecialChars(String.format("%s part %s.7z", t, i).replace("_", " ")); // remove special symbols
                    destArchiveName = StringUtils.force63(destArchiveName);

                    ArchiveUtils.compress7z(destArchiveName, chunk, i++);

                    sourceArchive = outputDir.resolve(platform).resolve(destArchiveName);
                    Path destArchive = destPath.resolve(destArchiveName);
                    fileSize = (int) IOUtils.fileSize(sourceArchive) / 1024;
                    IOUtils.copyFile(sourceArchive, destArchive);
                    IOUtils.deleteFile(sourceArchive);

                    String romPath = formatRomPath(destArchiveName);

                    lines.add(formatTableCell(romPath, sourceArchiveName, fileSize));
                    linesTxt.add(romPath);
                    //TODO detect correct part
                    for (Family f : tribes.get(t)) {
                        String familyName = formatFamilyName(f);
                        if (!dbGamesSet.contains(familyName)) {
                            unmappedLines.add(romPath);
                        } else {
                            dbGamesSet.remove(familyName);
                            updateLines.add(formatUpdateQuery(romPath, familyName));
                        }
                    }
                }
            }
        });

        IOUtils.saveToFile(platform + "_games.htm", head + String.join("\n", lines) + foot);
        IOUtils.saveToFile(platform + "_games.txt", linesTxt);
        IOUtils.saveToFile(platform + "_games_update.sql", updateLines);
        IOUtils.saveToFile(platform + "_games_unmapped.txt", unmappedLines);
        IOUtils.saveToFile(platform + "_games_unmapped_names.txt", dbGamesSet.stream().sorted().collect(Collectors.toList()));
    }

    private static String formatRomPath(String destArchiveName) {
        return String.format("http://tv-roms.narod.ru/games/%s/%s", platform, destArchiveName);
    }

    private static String formatTableCell(String romPath, String sourceArchiveName, int fileSize) {
        return String.format("    <tr><td><a href=\"%s\">%s</a></td><td>%s Kb</td></tr>", romPath, sourceArchiveName, fileSize);
    }



    private static String formatUpdateQuery(String romPath, String familyName) {
        return String.format("UPDATE `base_%s` SET game='%s' WHERE name='%s';", platform, romPath, familyName);
    }

    private static String formatFamilyName(Family family) {
        return family.getName().replace("&", "&amp;").replace("'", "&rsquo;").replace(".7z", "");
    }

    //(list+dump+force63)
    public static void getAllUniqueRoms() {

        Set<String> names = new HashSet<>(IOUtils.loadTextFile(Paths.get("lists").resolve(platform + ".txt")));

        Path uniquePath = outputDir.resolve(platform).resolve("roms");

        IOUtils.createDirectories(uniquePath);

        List<String> lines = new ArrayList<>();
        List<String> unmappedLines = new ArrayList<>();
        List<String> updateLines = new ArrayList<>();

        // TODO process groups somehow too - get every
        families.values().stream().filter(f -> f.getType() == FamilyType.GROUP).forEach(f -> System.out.println("Skipping group: " + f.getName()));

        Map<Name, Family> familyMap = new HashMap<>();

        for (Family family: families.values().stream().filter(f -> f.getType() == FamilyType.FAMILY).collect(Collectors.toList())) {
            Name name = family.getMembers().stream().filter(n -> ListFilesa.nonHack(n.getName()))
                    .collect(Collectors.groupingBy(Name::getCleanName))
                    .values().stream().map(l -> l.stream().sorted(Comparator.comparing(Name::getIndex).reversed()).findFirst().orElse(null)).findFirst().orElse(null);
            familyMap.put(name, family);
        }

        AtomicInteger i = new AtomicInteger(0);
        familyMap.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getName())).forEach(e -> {
            String familyName = formatFamilyName(e.getValue());
            String sourceRomName = e.getKey().getName();
            String zipRomName = StringUtils.replaceExt(e.getValue().getName().replace(".7z", ""), "zip");
            zipRomName = StringUtils.removeSpecialChars(zipRomName.replace("_", " ")); // remove special symbols
            zipRomName = StringUtils.force63(zipRomName);

            Path sourceRom = romsCollection.getRomsPath().resolve(sourceRomName);
            Path renamedRom = uniquePath.resolve(zipRomName);

            ArchiveUtils.compressZip(renamedRom.toAbsolutePath().toString(), Collections.singletonList(sourceRom.toAbsolutePath().toString()), i.getAndIncrement());
            lines.add(String.format("%s/%s", platform, zipRomName));

            String romPath = String.format("http://cominf0.narod.ru/emularity/%s/%s", platform, zipRomName);

            if (!names.contains(familyName)) {
                unmappedLines.add(romPath);
            } else {
                names.remove(familyName);
                updateLines.add(String.format("UPDATE `base_%s` SET rom='%s' WHERE name='%s';", platform, romPath, familyName));
            }
        });

        IOUtils.saveToFile(platform + "_roms.txt", lines);
        IOUtils.saveToFile(platform + "_roms_update.sql", updateLines);
        IOUtils.saveToFile(platform + "_roms_unmapped.txt", unmappedLines);
        IOUtils.saveToFile(platform + "_roms_unmapped_names.txt", names.stream().sorted().collect(Collectors.toList()));
    }
}
