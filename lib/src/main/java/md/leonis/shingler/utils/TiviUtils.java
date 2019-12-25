package md.leonis.shingler.utils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import javafx.util.Pair;
import lombok.Data;
import md.leonis.shingler.CSV;
import md.leonis.shingler.model.Family;
import md.leonis.shingler.model.FamilyType;
import md.leonis.shingler.model.Name;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static md.leonis.shingler.model.ConfigHolder.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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

    public static boolean isIO = true;

    private static List<CSV.MySqlStructure> readCsv() {

        try {
            File file = inputDir.resolve("lists").resolve("base_" + platform + ".csv").toFile(); // Already escaped to &amp;, ...
            CsvSchema schema = new CsvMapper().schemaFor(CSV.MySqlStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
            MappingIterator<CSV.MySqlStructure> iter = new CsvMapper().readerFor(CSV.MySqlStructure.class).with(schema).readValues(file);
            return iter.readAll().stream().peek(s -> s.setOldName(s.getName())).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<CSV.RenamedStructure> readRenamedCsv() {

        try {
            File file = inputDir.resolve(platform + "_renamed.csv").toFile();
            CsvSchema schema = new CsvMapper().schemaFor(CSV.RenamedStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
            MappingIterator<CSV.RenamedStructure> iter = new CsvMapper().readerFor(CSV.RenamedStructure.class).with(schema).readValues(file);
            return iter.readAll().stream().peek(r -> {
                r.setNewName(StringUtils.escapeChars(r.getNewName()));
                r.setOldName(StringUtils.escapeChars(r.getOldName()));
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveRenamedCsv(List<CSV.RenamedStructure> renamed) {
        try {
            File file = inputDir.resolve(platform + "_renamed_final.csv").toFile();
            CsvSchema schema = new CsvMapper().schemaFor(CSV.RenamedStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
            new CsvMapper().writerFor(CSV.RenamedStructure.class).with(schema).writeValues(file).writeAll(renamed).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<CSV.AddedStructure> readAddedCsv() {

        try {
            File file = inputDir.resolve(platform + "_added.csv").toFile();
            CsvSchema schema = new CsvMapper().schemaFor(CSV.AddedStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
            MappingIterator<CSV.AddedStructure> iter = new CsvMapper().readerFor(CSV.AddedStructure.class).with(schema).readValues(file);
            return iter.readAll().stream().peek(r -> r.setName(StringUtils.escapeChars(r.getName()))).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path xlsUpdatePath() {
        return inputDir.resolve("base_" + platform + ".xlsx");
    }

    private static Path xlsCreatePath() {
        return inputDir.resolve("base_" + platform + "_create.xlsx");
    }

    private static void writeXls(Path path, List<CSV.MySqlStructure> records) {

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Sheet-1");

        int rowNum = 0;
        LOGGER.info("Creating excel: {}", path);

        for (CSV.MySqlStructure record : records) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(record.getSid());
            row.createCell(1).setCellValue(record.getName());
            row.createCell(2).setCellValue(record.getCpu());
            row.createCell(3).setCellValue(record.getGame());
            row.createCell(4).setCellValue(record.getRom());
            Cell cell = row.createCell(5);
            cell.setCellValue(record.getOldName());
            if (!record.getName().equals(record.getOldName())) {
                Font font = workbook.createFont();
                font.setColor(HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());
                cell.getCellStyle().setFont(font);
            }
        }
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(path.toFile());
            workbook.write(outputStream);
            workbook.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<CSV.MySqlStructure> readXls(Path path) {

        try {
            FileInputStream excelFile = new FileInputStream(path.toFile());
            Workbook workbook = new XSSFWorkbook(excelFile);
            Sheet datatypeSheet = workbook.getSheetAt(0);

            List<CSV.MySqlStructure> records = new ArrayList<>();

            for (Row currentRow : datatypeSheet) {
                CSV.MySqlStructure record = new CSV.MySqlStructure();
                record.setName(readValue(currentRow.getCell(1)));

                if (currentRow.getCell(0) != null) {
                    record.setSid(readValue(currentRow.getCell(0)));
                } else {
                    LOGGER.warn("Empty SID for {}", record.getName());
                    record.setSid(getSid(record.getName()));
                }

                if (currentRow.getCell(2) != null) {
                    String value = readValue(currentRow.getCell(2));
                    record.setCpu(value.equals("") ? StringUtils.cpu(record.getName()) : value);
                } else {
                    record.setCpu(StringUtils.cpu(record.getName()));
                }
                if (currentRow.getCell(3) != null) {
                    record.setGame(readValue(currentRow.getCell(3)));
                }
                if (currentRow.getCell(4) != null) {
                    record.setRom(readValue(currentRow.getCell(4)));
                }
                if (currentRow.getCell(5) != null) {
                    record.setOldName(readValue(currentRow.getCell(5)));
                }
                records.add(record);
            }
            return records;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String readValue(Cell cell) {
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return Integer.toString(Double.valueOf(cell.getNumericCellValue()).intValue());
        } else {
            throw new RuntimeException("Unsupported cell type: " + cell.getCellType());
        }
    }

    public static void createCleanedXls() {

        // import CSV original
        List<CSV.MySqlStructure> records = readCsv();

        // clean name, rom; calc cpu if not
        records.forEach(r -> {
            if (isBlank(r.getCpu())) {
                r.setCpu(StringUtils.cpu(r.getName()));
            }
            r.setGame("");
            r.setRom("");
        });

        // save as excel
        IOUtils.backupFile(xlsUpdatePath());
        writeXls(xlsUpdatePath(), records);

        // Added
        records = readAddedCsv().stream().map(CSV.MySqlStructure::new).collect(Collectors.toList());

        IOUtils.backupFile(xlsCreatePath());
        writeXls(xlsCreatePath(), records);

        LOGGER.info("Done");
    }

    // html+dump+force63+split
    public static void generatePageAndRomsForTvRoms() {

        TiViLists lists = new TiViLists();

        boolean needToSeparate = tribes.keySet().size() > DIR_SIZE;
        AtomicInteger g = new AtomicInteger(1);

        //lists.getSynonyms().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(System.out::println);

        tribes.keySet().stream().sorted().forEach(t -> {

            String sourceArchiveName = StringUtils.addExt(t, "7z");
            Path sourceArchive = outputDir.resolve(platform).resolve(sourceArchiveName);

            // compress all families to one tribe
            /*if (tribes.get(t).size() > 1) {
                LOGGER.info("Compressing tribe {}", t);
                List<String> members = tribes.get(t).stream().flatMap(f -> f.getMembers().stream()).map(Name::getName).sorted().collect(Collectors.toList());

                ArchiveUtils.compress7z(sourceArchive.toAbsolutePath().toString(), members, -1);
            }*/

            int fileSize = (int) IOUtils.fileSize(sourceArchive) / 1024;

            String platformGroup = needToSeparate ? String.format("%s%s", platform, (int) Math.ceil(g.getAndIncrement() / DIR_SEPARATE_SIZE)) : platform;
            Path destPath = outputDir.resolve(platform).resolve("games").resolve(platformGroup);
            if (isIO) {
                IOUtils.createDirectories(destPath);
            }

            if (fileSize < MAX_SIZE) {

                String destArchiveName = StringUtils.normalize(sourceArchiveName);
                Path destArchive = destPath.resolve(destArchiveName);

                if (isIO) {
                    IOUtils.copyFile(sourceArchive, destArchive);
                }

                String shortRomPath = formatShortRomPath(platformGroup, destArchiveName);

                lists.getHtmlLines().add(formatTableCell(formatRomPath(shortRomPath), sourceArchiveName, fileSize));
                lists.getTxtLines().add(shortRomPath);

                for (Family f : tribes.get(t)) {
                    processFamily(lists, f, shortRomPath);
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
                    String destArchiveName = StringUtils.normalize(String.format("%s part %s", t, i).replace("_", " "), "7z");

                    if (isIO) {
                        ArchiveUtils.compress7z(destArchiveName, chunk, i++);

                        sourceArchive = outputDir.resolve(platform).resolve(destArchiveName);
                        Path destArchive = destPath.resolve(destArchiveName);
                        fileSize = (int) IOUtils.fileSize(sourceArchive) / 1024;
                        IOUtils.copyFile(sourceArchive, destArchive);
                        IOUtils.deleteFile(sourceArchive);
                    }

                    String shortRomPath = formatShortRomPath(platformGroup, destArchiveName);

                    lists.getHtmlLines().add(formatTableCell(formatRomPath(shortRomPath), sourceArchiveName, fileSize));
                    lists.getTxtLines().add(shortRomPath);
                    //TODO detect correct part
                    for (Family f : tribes.get(t)) {
                        processFamily(lists, f, shortRomPath);
                    }
                }
            }
        });

        IOUtils.saveToFile(inputDir.resolve(platform + "_games.htm"), formatHead() + String.join("\n", lists.getHtmlLines()) + FOOT);
        IOUtils.saveToFile(inputDir.resolve(platform + "_games.txt"), lists.getTxtLines());
        IOUtils.saveToFile(inputDir.resolve(platform + "_games_update.sql"), lists.getUpdateLines());
        IOUtils.saveToFile(inputDir.resolve(platform + "_games_unmapped.txt"), lists.getUnmappedLines());
        IOUtils.saveToFile(inputDir.resolve(platform + "_games_unmapped_families.txt"), lists.getUnmappedFamilies());
        IOUtils.saveToFile(inputDir.resolve(platform + "_games_unmapped_names.txt"), lists.getUnmappedNames().stream().sorted().map(r -> lists.getNormalizedMap().get(r).getName()).collect(Collectors.toList()));
        IOUtils.saveToFile(inputDir.resolve(platform + "_create.sql"), lists.getCreateLines().stream().sorted().map(r -> formatCreate(getSid(r), r, StringUtils.cpu(r), null, null, false)).collect(Collectors.toList()));
        IOUtils.saveToFile(inputDir.resolve(platform + "_htaccess.txt"), lists.getHtAccessLines());

        // save as excel
        IOUtils.backupFile(xlsUpdatePath());
        writeXls(xlsUpdatePath(), lists.getRecords());

        IOUtils.backupFile(xlsUpdatePath());
        writeXls(xlsCreatePath(), lists.getAddedRecords());
    }

    private static void processFamily(TiViLists lists, Family f, String shortRomPath) {

        String romPath = formatRomPath(shortRomPath);
        String familyName = escapeQuotes(norm2(f.getName()));
        String normalizedFamilyName = StringUtils.normalize(familyName);
        if (!lists.getUnmappedNames().contains(normalizedFamilyName)) {

            String syn = lists.getSynonyms().get(normalizedFamilyName);
            if (syn != null && lists.getNormalizedMap().get(syn) != null) {
                CSV.MySqlStructure record = lists.getNormalizedMap().get(syn);
                record.setName(f.getName());
                record.setCpu(StringUtils.cpu(f.getName()));
                record.setGame(shortRomPath);
                lists.getUnmappedNames().remove(normalizedFamilyName);
                lists.getUpdateLines().add(formatUpdateQuery(platform, romPath, familyName));
                lists.getHtAccessLines().add(String.format("RewriteRule ^game/%s/%s.html$ game/%s/%s.html [L,R=301]", platform, toRegex(cryptTitle(lists.getSynonymsNames().get(syn))), platform, toRegex(StringUtils.cpu(f.getName()))));
            } else {
                boolean isCreated = lists.getCreated().contains(normalizedFamilyName);
                if (isCreated) {
                    lists.getCreateLines().add(familyName);
                    lists.getUnmappedNames().remove(normalizedFamilyName);
                } else {
                    lists.getUnmappedFamilies().add(f.getName());
                    lists.getUnmappedLines().add(shortRomPath);
                }
            }
        } else {
            CSV.MySqlStructure record = lists.getNormalizedMap().get(normalizedFamilyName);
            record.setGame(shortRomPath);
            lists.getUnmappedNames().remove(normalizedFamilyName);
            lists.getUpdateLines().add(formatUpdateQuery(platform, romPath, familyName));
        }

        CSV.MySqlStructure record = lists.getAddedNormalizedMap().get(normalizedFamilyName);
        if (record != null) {
            record.setGame(shortRomPath);
        }
    }

    private static String norm2(String s) {
        if (s.startsWith("Archer Maclean&rsquo;s ")) {
            s = s.replace("Archer Maclean&rsquo;s ", "");
        }
        if (s.startsWith("Disney&rsquo;s ")) {
            s = s.replace("Disney&rsquo;s ", "");
        }
        if (s.startsWith("Fox&rsquo;s ")) {
            s = s.replace("Fox&rsquo;s ", "");
        }
        if (s.startsWith("Capcom&rsquo;s ")) {
            s = s.replace("Capcom&rsquo;s ", "");
        }
        return s;
    }

    private static String toRegex(String s) {
        return s.replaceAll("[\\[\\]{}()*+?.,\\\\^$|#\\s]", "\\\\$0");
    }

    @Data
    static class TiViLists {

        private List<CSV.MySqlStructure> records;
        private Map<String, CSV.MySqlStructure> normalizedMap;

        private List<CSV.MySqlStructure> addedRecords;
        private Map<String, CSV.MySqlStructure> addedNormalizedMap;

        private Map<String, String> synonyms = new HashMap<>();
        private Map<String, String> synonymsNames = new HashMap<>();

        private List<String> htmlLines = new ArrayList<>();
        private List<String> txtLines = new ArrayList<>();
        private List<String> updateLines = new ArrayList<>();
        private List<String> unmappedLines = new ArrayList<>();
        private List<String> unmappedFamilies = new ArrayList<>();
        private Set<String> unmappedNames;

        private List<String> htAccessLines = new ArrayList<>();

        private Set<String> created;
        private List<String> createLines = new ArrayList<>();

        TiViLists() {
            this.records = readXls(xlsUpdatePath());
            this.normalizedMap = records.stream().collect(Collectors.toMap(r -> StringUtils.normalize(r.getName()), Function.identity(), (r1, r2) -> r1));
            this.unmappedNames = new HashSet<>(normalizedMap.keySet());

            this.addedRecords = readXls(xlsCreatePath());
            this.addedNormalizedMap = addedRecords.stream().collect(Collectors.toMap(r -> StringUtils.normalize(r.getName()), Function.identity(), (r1, r2) -> r1));

            List<CSV.RenamedStructure> renamed = readRenamedCsv();
            renamed.forEach(r -> {
                synonyms.put(StringUtils.normalize(r.getNewName()), StringUtils.normalize(r.getOldName()));
                synonymsNames.put(StringUtils.normalize(r.getOldName()), r.getOldName());
            });

            created = readAddedCsv().stream().map(s -> StringUtils.normalize(s.getName())).collect(Collectors.toSet());
            //TODO deleted and generate
        }
    }

    private static String formatHead() {
        return String.format(HEAD, platformsByCpu.get(platform).getTitle());
    }

    private static String formatShortRomPath(String platform, String destArchiveName) {
        return String.format("%s/%s", platform, destArchiveName);
    }

    private static String formatRomPath(String shortRomPath) {
        return romsUrl + shortRomPath;
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
        GROUP_MAP.put("wxn collection", "wxn");
    }

    private static Path getUniquePath() {
        return outputDir.resolve(platform).resolve("roms");
    }

    //(list+dump+force63)
    public static void getAllUniqueRoms() {

        List<String> games = IOUtils.loadTextFile(inputDir.resolve(platform + "_games.txt"));

        TiViLists lists = new TiViLists();

        Path uniquePath = getUniquePath();

        if (isIO) {
            IOUtils.createDirectories(uniquePath);
        }

        LOGGER.info("Processing families...");

        Map<Name, Family> familyMap = new HashMap<>();
        boolean needToSeparate = families.keySet().size() > DIR_SIZE;
        AtomicInteger g = new AtomicInteger(1);

        for (Family family : families.values().stream().filter(f -> f.getType() == FamilyType.FAMILY).collect(Collectors.toList())) {
            family.getMembers().stream().filter(n -> platformsByCpu.get(platform).nonHack(n.getName()))
                    .collect(Collectors.groupingBy(Name::getCleanName))
                    .values().stream().map(l -> l.stream().max(Comparator.comparing(Name::getIndex)).orElse(null)).forEach(name -> familyMap.put(name, family));
        }

        //familyMap.keySet().stream().sorted(Comparator.comparing(Name::getName)).forEach(System.out::println);
        //System.out.println("=============================== getNormalizedMap");
        //lists.getNormalizedMap().keySet().stream().sorted().forEach(System.out::println);


        AtomicInteger j = new AtomicInteger(0);
        familyMap.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getName())).forEach(e -> {
            String sourceRomName = e.getKey().getName();
            //TODO collizions
            String zipRomName = StringUtils.normalize(e.getKey().getCleanName(), "zip");

            String platformGroup = needToSeparate ? String.format("%s%s", platform, (int) Math.ceil(g.getAndIncrement() / DIR_SEPARATE_SIZE)) : platform;

            Path sourceRom = romsCollection.getRomsPath().resolve(sourceRomName);
            Path destZipRom = uniquePath.resolve(platformGroup).resolve(zipRomName);

            if (isIO) {
                ArchiveUtils.compressZip(destZipRom.toAbsolutePath().toString(), Collections.singletonList(sourceRom.toAbsolutePath().toString()), j.getAndIncrement());
            }

            String shortRomPath = formatShortUniqueRomPath(platformGroup, zipRomName);
            int fileSize = isIO ? (int) IOUtils.fileSize(destZipRom) / 1024 : -1;
            lists.getHtmlLines().add(formatTableCell(formatUniqueRomPath(shortRomPath), sourceRomName, fileSize));
            lists.getTxtLines().add(shortRomPath);

            processRomFamily(lists, e.getKey(), e.getValue(), shortRomPath, games);
        });

        LOGGER.info("Processing groups...");

        AtomicInteger i = new AtomicInteger(0);
        families.values().stream().filter(f -> f.getType() == FamilyType.GROUP).forEach(f -> processGroup(lists, f.getName(), f, f.getMembers().stream().sorted(Comparator.comparing(Name::getName)).collect(Collectors.toList()), i, games, f.getMembers().size() > DIR_SIZE));

        LOGGER.info("Processing hacks...");
        Map<Name, Family> hackedFamilyMap = new HashMap<>();

        families.values().stream().map(f -> {
            Family newFamily = new Family(f);
            newFamily.setMembers(f.getMembers().stream().filter(n -> platformsByCpu.get(platform).isHack(n.getName())).sorted(Comparator.comparing(Name::getName)).collect(Collectors.toList()));
            return newFamily;
        }).filter(f -> !f.getMembers().isEmpty()).forEach(f -> f.getMembers().forEach(m -> hackedFamilyMap.put(m, f)));

        lists.getHtmlLines().add("    <tr><td></td></tr>");
        lists.getHtmlLines().add("    <tr><td><b>Hacks</b></td></tr>");
        lists.getTxtLines().add("");
        lists.getTxtLines().add("Hacks");

        boolean separateHacks = hackedFamilyMap.size() > DIR_SIZE;
        AtomicInteger k = new AtomicInteger(0);
        hackedFamilyMap.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getName())).forEach(e -> processMember(lists, "hack", e.getValue(), e.getKey(), k, games, separateHacks));

        //List<Name> hacks = families.values().stream().flatMap(f -> f.getMembers().stream()).filter(n -> platformsByCpu.get(platform).isHack(n.getName())).sorted(Comparator.comparing(Name::getName)).collect(Collectors.toList());
        //processGroup(lists, new Family("hack", null, null), hacks, i, games);

        IOUtils.saveToFile(inputDir.resolve(platform + "_roms.htm"), formatHead() + String.join("\n", lists.getHtmlLines()) + FOOT);
        IOUtils.saveToFile(inputDir.resolve(platform + "_roms.txt"), lists.getTxtLines())/*.stream().sorted().collect(Collectors.toList()))*/; // mess with hacks, pd
        IOUtils.saveToFile(inputDir.resolve(platform + "_roms_update.sql"), lists.getUpdateLines());
        IOUtils.saveToFile(inputDir.resolve(platform + "_roms_unmapped.txt"), lists.getUnmappedLines());
        IOUtils.saveToFile(inputDir.resolve(platform + "_roms_unmapped_families.txt"), lists.getUnmappedFamilies());
        IOUtils.saveToFile(inputDir.resolve(platform + "_roms_unmapped_names.txt"), lists.getUnmappedNames().stream().sorted().map(r -> lists.getNormalizedMap().get(r).getName()).collect(Collectors.toList()));

        List<String> fams = new ArrayList<>();
        families.values().stream().collect(Collectors.toMap(Family::getName, f -> f.getMembers().stream().map(Name::getCleanName).distinct().sorted()))
                .entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(f -> {
            fams.add(f.getKey());
            f.getValue().map(r -> "    " + r).forEach(fams::add);
        });

        IOUtils.saveToFile(inputDir.resolve(platform + "_families_to_games.txt"), fams);

        List<Pair<String, String>> pairs = new ArrayList<>();
        families.values().stream().collect(Collectors.toMap(Family::getName, f -> f.getMembers().stream().map(Name::getCleanName).distinct().sorted()))
                .entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(f -> f.getValue().forEach(v -> {
            if (!v.equals(f.getKey())) {
                pairs.add(new Pair<>(v, f.getKey()));
            }
        }));

        IOUtils.saveToFile(inputDir.resolve(platform + "_games_to_families.txt"), pairs.stream().map(p -> p.getKey() + "\t\t" + p.getValue()).sorted().collect(Collectors.toList()));

        // save as excel
        writeXls(xlsUpdatePath(), lists.getRecords());
        writeXls(xlsCreatePath(), lists.getAddedRecords());

        // update queries
        createUpdateQueries(false);

        LOGGER.info("Done");
    }

    private static void processGroup(TiViLists lists, String groupName, Family f, List<Name> members, AtomicInteger i, List<String> games, boolean needToSeparate) {

        String title = Character.toUpperCase(groupName.charAt(0)) + groupName.substring(1) + ":";
        lists.getHtmlLines().add("    <tr><td></td></tr>");
        lists.getHtmlLines().add(String.format("    <tr><td><b>%s</b></td></tr>", title));
        lists.getTxtLines().add("");
        lists.getTxtLines().add(title);

        members.forEach(n -> processMember(lists, groupName, f, n, i, games, needToSeparate));
    }

    private static void processMember(TiViLists lists, String groupName, Family f, Name name, AtomicInteger i, List<String> games, boolean needToSeparate) {

        String num = needToSeparate ? "" + (int) Math.ceil((i.getAndIncrement() + 1) / DIR_SEPARATE_SIZE) : "";

        String sourceRomName = name.getName();
        String zipRomName = StringUtils.normalize(StringUtils.replaceExt(sourceRomName, "zip"));

        Path sourceRom = romsCollection.getRomsPath().resolve(sourceRomName);
        String dir = GROUP_MAP.get(groupName.toLowerCase());
        if (dir == null) {
            dir = StringUtils.normalize(groupName.replace("_", " ")).toLowerCase();
        }
        dir = String.format("%s_%s%s", platform, dir, num);

        Path destZipRom = getUniquePath().resolve(dir).resolve(zipRomName);

        if (isIO) {
            ArchiveUtils.compressZip(destZipRom.toAbsolutePath().toString(), Collections.singletonList(sourceRom.toAbsolutePath().toString()), i.get());
        }

        String shortRomPath = formatShortUniqueRomPath(dir, zipRomName);
        int fileSize = isIO ? (int) IOUtils.fileSize(destZipRom) / 1024 : -1;
        lists.getHtmlLines().add(formatTableCell(formatUniqueRomPath(shortRomPath), sourceRomName, fileSize));
        lists.getTxtLines().add(shortRomPath);

        processRomFamily(lists, name, f, shortRomPath, games);
    }

    private static void processRomFamily(TiViLists lists, Name n, Family f, String shortRomPath, List<String> games) {

        String romPath = formatRomPath(shortRomPath);
        String name = n.getCleanName();
        String normalizedFamilyName = StringUtils.normalize(escapeQuotes(name));
        /*if (n.getName().startsWith("R.B")) { // for debug
            List<String> u = lists.getUnmappedNames().stream().filter(us -> us.startsWith("R")).sorted().collect(Collectors.toList());
            System.out.println(u);
            List<String> us = lists.getNormalizedMap().keySet().stream().filter(usa -> usa.startsWith("R")).sorted().collect(Collectors.toList());
            System.out.println(us);
        }*/
        if (!lists.getUnmappedNames().contains(normalizedFamilyName)) {
            String syn = lists.getSynonyms().get(normalizedFamilyName);
            if (syn != null && lists.getNormalizedMap().get(syn) != null) {
                CSV.MySqlStructure record = lists.getNormalizedMap().get(syn);
                record.setName(name);
                record.setCpu(StringUtils.cpu(name));
                record.setRom(shortRomPath);
                lists.getUnmappedNames().remove(normalizedFamilyName);
                lists.getUpdateLines().add(String.format("UPDATE `base_%s` SET rom='%s' WHERE name='%s';", platform, romPath, escapeQuotes(n.getCleanName())));

                addGameIfNeed(record, f, games);
            } else {
                lists.getUnmappedFamilies().add(n.getCleanName());
                lists.getUnmappedLines().add(shortRomPath);
            }
        } else {
            CSV.MySqlStructure record = lists.getNormalizedMap().get(normalizedFamilyName);
            record.setRom(shortRomPath);
            addGameIfNeed(record, f, games);
            lists.getUnmappedNames().remove(normalizedFamilyName);
            lists.getUpdateLines().add(String.format("UPDATE `base_%s` SET rom='%s' WHERE name='%s';", platform, romPath, escapeQuotes(n.getCleanName())));
        }

        CSV.MySqlStructure record = lists.getAddedNormalizedMap().get(normalizedFamilyName);
        if (record != null) {
            record.setRom(shortRomPath);
            addGameIfNeed(record, f, games);
        }
    }

    private static void addGameIfNeed(CSV.MySqlStructure record, Family f, List<String> games) {

        if (org.apache.commons.lang3.StringUtils.isBlank(record.getGame())) {
            String archiveName = StringUtils.normalize(f.getName(), "7z");
            String fullArchiveName = games.stream().filter(g -> g.endsWith("/" + archiveName)).findFirst().orElse(null);
            if (null != fullArchiveName) {
                record.setGame(fullArchiveName);
            } else {
                LOGGER.warn("Game isn't found: {}; {}", f, archiveName);
            }
        }
    }

    public static void createUpdateQueries(boolean isFinal) {

        List<String> warnings = new ArrayList<>();
        List<CSV.RenamedStructure> renamed = new ArrayList<>();

        List<CSV.MySqlStructure> originRecords = readCsv();
        List<CSV.MySqlStructure> records = readXls(xlsUpdatePath());

        Set<String> copied = new HashSet<>();

        List<String> updateList = new ArrayList<>();
        for (int k = 0; k < originRecords.size(); k++) {

            if (isBlank(records.get(k).getCpu()) && isFinal) {
                warnings.add("CPU is blank for " + records.get(k).getCpu());
            }

            if (isBlank(records.get(k).getCpu()) && isFinal) {
                warnings.add("Game is blank for " + records.get(k).getGame());
            }

            if (isBlank(records.get(k).getCpu()) && isFinal) {
                warnings.add("Rom is blank for " + records.get(k).getRom());
            }

            Map<String, String> vals = new LinkedHashMap<>();
            if (!originRecords.get(k).getName().equals(records.get(k).getName())) {
                vals.put("name", records.get(k).getName().trim());
                renamed.add(new CSV.RenamedStructure(getSid(records.get(k).getName()), records.get(k).getName(), originRecords.get(k).getName()));
            }
            if (!getSid(originRecords.get(k).getName()).equals(getSid(records.get(k).getName()))) {
                vals.put("sid", getSid(records.get(k).getName()));
            }
            if (!originRecords.get(k).getGame().equals(records.get(k).getGame())) {
                if (isNotBlank(records.get(k).getGame())) {
                    vals.put("game", formatRomPath(records.get(k).getGame().trim()));
                }
            }
            if (!originRecords.get(k).getRom().equals(records.get(k).getRom())) {
                if (isNotBlank(records.get(k).getRom())) {
                    vals.put("rom", formatUniqueRomPath(records.get(k).getRom().trim()));
                }
            }

            if (isFinal) {
                copyFinal(records.get(k).getGame(), "games", copied);
                copyFinal(records.get(k).getRom(), "roms", copied);
            }

            vals.put("cpu", records.get(k).getCpu().trim());

            vals.put("modified", Long.toString(new Date().getTime() / 1000));
            String sets = vals.entrySet().stream().map(e -> String.format("`%s`='%s'", e.getKey(), escapeQuotes(e.getValue()))).collect(Collectors.joining(", "));
            updateList.add(String.format("UPDATE `base_%s` SET %s WHERE name='%s';", platform, sets, escapeQuotes(originRecords.get(k).getName())));
        }
        IOUtils.saveToFile(inputDir.resolve(platform + "_update.sql"), String.join("\n", updateList));

        // Added
        records = readXls(xlsCreatePath());

        List<String> createList = new ArrayList<>();
        for (CSV.MySqlStructure record : records) {
            createList.add(formatCreate(record.getSid(), record.getName(), record.getCpu(), record.getGame(), record.getRom(), isFinal));

            if (isFinal) {
                copyFinal(record.getGame(), "games", copied);
                copyFinal(record.getRom(), "roms", copied);
            }
        }
        IOUtils.saveToFile(inputDir.resolve(platform + "_create.sql"), String.join("\n", createList));

        saveRenamedCsv(renamed);

        if (!warnings.isEmpty()) {
            LOGGER.warn("UPDATE warnings:");
            warnings.forEach(LOGGER::warn);
        }

        LOGGER.info("Done");
    }

    private static void copyFinal(String source, String path, Set<String> copied) {
        if (isIO && org.apache.commons.lang3.StringUtils.isNotBlank(source)) {
            Path sourceFile = outputDir.resolve(platform).resolve(path).resolve(source.trim());
            Path destFile = outputDir.resolve(platform + "-final").resolve(path).resolve(source.trim());
            if (Files.exists(sourceFile)) {
                if (Files.notExists(destFile.getParent())) {
                    IOUtils.createDirectories(destFile.getParent());
                }
                IOUtils.copyFile(sourceFile, destFile);
            } else if (!copied.contains(source)) {
                LOGGER.warn("File isn't found: {}", sourceFile.toAbsolutePath().toString());
            }
            copied.add(source);
        }
    }

    private static String formatCreate(String sid, String name, String cpu, String game, String rom, boolean isFinal) {

        //TODO n INC: SELECT MAX(n) AS max FROM base_".$catcpu

        long modified = new Date().getTime() / 1000;

        String format = "INSERT INTO `base_%s` VALUES (99999, '%s', '%s','%s', '%s', '%s'," + // n, platform, created, modified, sid, cpu
                "'%s'," + // name
                "'',''," + // descript, keywords
                "'%s'," + // region
                "'',''," + // publisher, developer
                "'','','0'," + // god, god1, ngamers
                "'',''," + //type, genre
                "'','','','','','',''," + // images 1-7
                "'','','','','','',''," + // images 8-14
                "'%s', 0, '', 0, '%s','yes',0," + //game, ?, music, ?, rom, playable
                "'',''," + // analog, drname
                "'',''," + // cros, serie
                "'',''," + // text1, text2
                "'0', '0','0', '0', 0);"; //rating, user ratings, viewes, ?, act //TODO add act: ", 'yes'"

        String region = "";

        if (isBlank(sid) && isFinal) {
            LOGGER.warn("SID is blank for {}", name);
        }

        if (isBlank(cpu) && isFinal) {
            LOGGER.warn("CPU is blank for {}", name);
        }

        if (isNotBlank(game)) {
            game = formatRomPath(game).trim();
        } else if (isFinal) {
            LOGGER.warn("Game is blank for {}", name);
        }

        if (isNotBlank(rom)) {
            rom = formatUniqueRomPath(rom).trim();
        } else if (isFinal) {
            LOGGER.warn("Rom is blank for {}", name);
        }

        return String.format(format, platform, platform, modified, modified, sid.trim(), cpu.trim(), escapeQuotes(name.trim()), region.trim(), game, rom);
    }

    public static String getSid(String name) {
        if (isBlank(name)) {
            return "";
        }
        String sid = name.toLowerCase().substring(0, 1);
        if (platformsByCpu.get(platform).isPD(name)) {
            sid = "pd";
        } else if (platformsByCpu.get(platform).isHack(name)) {
            sid = "hak";
        } else if (sid.matches("\\d")) {
            sid = "num";
        }
        return sid;
    }

    private static String formatShortUniqueRomPath(String platform, String destArchiveName) {
        return String.format("%s/%s", platform, destArchiveName);
    }

    private static String formatUniqueRomPath(String shortRomPath) {
        return uniqueRomsUrl + shortRomPath;
    }

    private static String cryptTitle(String title) {
        title = title.replace("/", "--");
        title = title.replace("_", "__");
        title = title.replace(" ", "_");
        title = title.replace("'", "&rsquo;");
        /*if ((strpos($s, 'а') > 0) || (strpos($s, 'е') > 0)) {
            $st = 'Rus--' . ruslat($st);
        }*/ //TODO

        title = title.replace("&quot;", "-quote-");
        title = title.replace("&rsquo;", "=");
        title = title.replace("&amp;", "&");
        title = title.replace("&", "__n__");
        title = title.replace("#", "_diez_");
        title = title.replace("*", "_star_");
        title = title.replace("+", "-plus-");
        title = title.replace("%", "_procent_");
        title = title.replace("?", "");
        return title;
    }
}
