package md.leonis.shingler.utils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import javafx.util.Pair;
import lombok.Data;
import lombok.SneakyThrows;
import md.leonis.shingler.CSV;
import md.leonis.shingler.model.ConfigHolder;
import md.leonis.shingler.model.Family;
import md.leonis.shingler.model.FamilyType;
import md.leonis.shingler.model.Name;
import md.leonis.shingler.model.dto.TiviStructure;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static md.leonis.shingler.model.ConfigHolder.*;
import static md.leonis.shingler.utils.TiviApiUtils.loadTiviGames;
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

    private static List<CSV.MySqlStructure> readCsv(boolean force) {

        /*try {
            File file = getInputPath().resolve("lists").resolve("base_" + platform + ".csv").toFile(); // Already escaped to &amp;, ...
            if (!file.exists()) {
                //createGamesList(file);
                LOGGER.warn("{} is absent", file);
                LOGGER.warn("Please, export table as CSV from MySQL DB, next delete in Excel all fields, except \"sid\", \"name\", \"cpu\", \"game\", \"rom\"");
                return new ArrayList<>();
            }
            CsvSchema schema = new CsvMapper().schemaFor(CSV.MySqlStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
            MappingIterator<CSV.MySqlStructure> iter = new CsvMapper().readerFor(CSV.MySqlStructure.class).with(schema).readValues(file);
            return iter.readAll().stream().peek(s -> s.setOldName(s.getName())).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/

        try {
            List<TiviStructure> structure = loadTiviGames(platform, force);
            return structure.stream().map(s -> new CSV.MySqlStructure(s.getSid(), s.getName(), s.getName(), s.getCpu(), s.getGame(), s.getRom())).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.warn("Can't read table from TiVi: " + platform, e);
            return new ArrayList<>();
        }
    }

    private static void saveCsv(List<CSV.MySqlStructure> structure, File file) {
        try {
            CsvSchema schema = new CsvMapper().schemaFor(CSV.MySqlStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
            new CsvMapper().writerFor(CSV.MySqlStructure.class).with(schema).writeValues(file).writeAll(structure).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<CSV.RenamedStructure> readRenamedCsv() {
        try {
            File file = getInputPath().resolve(platform + "_renamed.csv").toFile();
            if (file.exists()) {
                CsvSchema schema = new CsvMapper().schemaFor(CSV.RenamedStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
                MappingIterator<CSV.RenamedStructure> iter = new CsvMapper().readerFor(CSV.RenamedStructure.class).with(schema).readValues(file);
                return iter.readAll().stream().peek(r -> {
                    r.setNewName(StringUtils.escapeChars(r.getNewName()));
                    r.setOldName(StringUtils.escapeChars(r.getOldName()));
                }).collect(Collectors.toList());
            } else {
                LOGGER.warn("{} is absent", file);
                return new ArrayList<>();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveRenamedCsv(List<CSV.RenamedStructure> renamed) {
        try {
            File file = getInputPath().resolve(platform + "_renamed_final.csv").toFile();
            CsvSchema schema = new CsvMapper().schemaFor(CSV.RenamedStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
            new CsvMapper().writerFor(CSV.RenamedStructure.class).with(schema).writeValues(file).writeAll(renamed).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<CSV.AddedStructure> readAddedCsv() {
        try {
            File file = getInputPath().resolve(platform + "_added.csv").toFile();
            if (!file.exists()) {
                createAddedList(file);
            }
            CsvSchema schema = new CsvMapper().schemaFor(CSV.AddedStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
            MappingIterator<CSV.AddedStructure> iter = new CsvMapper().readerFor(CSV.AddedStructure.class).with(schema).readValues(file);
            return iter.readAll().stream().peek(r -> r.setName(StringUtils.escapeChars(r.getName()))).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveAddedCsv(List<CSV.AddedStructure> added, File file) {
        try {
            CsvSchema schema = new CsvMapper().schemaFor(CSV.AddedStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
            new CsvMapper().writerFor(CSV.AddedStructure.class).with(schema).writeValues(file).writeAll(added).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<CSV.ValidationStructure> readValidationCsv() {
        try {
            File file = getInputPath().resolve("base_" + platform + ".csv").toFile();
            CsvSchema schema = new CsvMapper().schemaFor(CSV.ValidationStructure.class).withColumnSeparator(';').withoutHeader().withQuoteChar('"');
            MappingIterator<CSV.ValidationStructure> iter = new CsvMapper().readerFor(CSV.ValidationStructure.class).with(schema).readValues(file);
            return iter.readAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path xlsUpdatePath() {
        return getInputPath().resolve("base_" + platform + ".xlsx");
    }

    private static Path xlsCreatePath() {
        return getInputPath().resolve("base_" + platform + "_create.xlsx");
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

        try (FileOutputStream outputStream = new FileOutputStream(path.toFile())) {
            workbook.write(outputStream);
            workbook.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<CSV.MySqlStructure> readXls(Path path) {

        int r = -1, c = -1;

        try {
            File file = path.toFile();
            FileInputStream excelFile = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(excelFile);
            Sheet datatypeSheet = workbook.getSheetAt(0);

            List<CSV.MySqlStructure> records = new ArrayList<>();

            for (Row currentRow : datatypeSheet) {
                r = currentRow.getRowNum();
                CSV.MySqlStructure record = new CSV.MySqlStructure();
                record.setName(readValue(currentRow.getCell(1)));

                c = 0;
                if (currentRow.getCell(c) != null) {
                    record.setSid(readValue(currentRow.getCell(c)));
                } else {
                    LOGGER.warn("Empty SID for {}", record.getName());
                    record.setSid(getSid(record.getName()));
                }

                c = 2;
                if (currentRow.getCell(c) != null) {
                    String value = readValue(currentRow.getCell(c));
                    record.setCpu(value.equals("") ? StringUtils.cpu(record.getName()) : value);
                } else {
                    record.setCpu(StringUtils.cpu(record.getName()));
                }
                c = 3;
                if (currentRow.getCell(c) != null) {
                    record.setGame(readValue(currentRow.getCell(c)));
                }
                c = 4;
                if (currentRow.getCell(c) != null) {
                    record.setRom(readValue(currentRow.getCell(c)));
                }
                c = 5;
                if (currentRow.getCell(c) != null) {
                    record.setOldName(readValue(currentRow.getCell(c)));
                }
                records.add(record);
            }
            excelFile.close();
            workbook.close();
            return records;
        } catch (Exception e) {
            LOGGER.error("[{}, {}]", r, c);
            throw new RuntimeException(e);
        }
    }

    private static String readValue(Cell cell) {
        if (cell.getCellType() == CellType.BLANK) {
            return "";
        } else if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return Integer.toString(Double.valueOf(cell.getNumericCellValue()).intValue());
        } else {
            throw new RuntimeException("Unsupported cell type: " + cell.getCellType());
        }
    }

    @SneakyThrows
    public static void createCleanedXls() {

        FileUtils.createDirectories(getInputPath());

        // import CSV original
        List<CSV.MySqlStructure> records = readCsv(true);

        // clean name, rom; calc cpu if not
        records.forEach(r -> {
            if (isBlank(r.getCpu())) {
                r.setCpu(StringUtils.cpu(r.getName()));
            }
            r.setGame("");
            r.setRom("");
        });

        // save as excel
        FileUtils.backupFile(xlsUpdatePath());
        writeXls(xlsUpdatePath(), records);

        // Added
        records = readAddedCsv().stream().map(CSV.MySqlStructure::new).collect(Collectors.toList());

        FileUtils.backupFile(xlsCreatePath());
        writeXls(xlsCreatePath(), records);

        LOGGER.info("Done");
    }

    // html+dump+force63+split
    @SneakyThrows
    public static void generatePageAndRomsForTvRoms() {

        TiViLists lists = new TiViLists();

        boolean needToSeparate = tribes.keySet().size() > DIR_SIZE;
        AtomicInteger g = new AtomicInteger(1);

        //lists.getSynonyms().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(System.out::println);

        tribes.keySet().stream().sorted().forEach(t -> {

            String sourceArchiveName = StringUtils.addExt(t, "7z");
            Path sourceArchive = getOutputPath().resolve(platform).resolve(sourceArchiveName);

            // compress all families to one tribe
            /*if (tribes.get(t).size() > 1) {
                LOGGER.info("Compressing tribe {}", t);
                List<String> members = tribes.get(t).stream().flatMap(f -> f.getMembers().stream()).map(Name::getName).sorted().collect(Collectors.toList());

                ArchiveUtils.compress7z(sourceArchive.toAbsolutePath().toString(), members, -1);
            }*/

            int fileSize = (int) FileUtils.fileSize(sourceArchive) / 1024;

            String platformGroup = needToSeparate ? String.format("%s%s", platform, (int) Math.ceil(g.getAndIncrement() / DIR_SEPARATE_SIZE)) : platform;
            Path destPath = getOutputPath().resolve(platform).resolve("games").resolve(platformGroup);
            if (isIO) {
                FileUtils.createDirectories(destPath);
            }

            List<String> members = tribes.get(t).stream().flatMap(f -> f.getMembers().stream()).map(Name::getName).sorted().collect(Collectors.toList());

            if (fileSize < MAX_SIZE || members.size() <= 1) {

                String destArchiveName = StringUtils.normalize7z(StringUtils.stripExtension(sourceArchiveName), "7z");
                Path destArchive = destPath.resolve(destArchiveName);

                if (isIO) {
                    FileUtils.copyFile(sourceArchive, destArchive);
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

                final int archivesCount = new Double(Math.ceil(fileSize * 1.0 / MAX_SIZE)).intValue();
                final int membersCount = members.size() / archivesCount;
                final AtomicInteger counter = new AtomicInteger();

                final Collection<List<String>> result = members.stream()
                        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / membersCount)).values();

                int i = 1;
                for (List<String> chunk : result) {

                    sourceArchiveName = String.format("%s (part %s).7z", t, i);
                    String destArchiveName = StringUtils.normalize(String.format("%s part %s", t, i).replace("_", " "), "7z");

                    if (isIO) {
                        //TODO fix this - hack for SNES games - hashcode collizion
                        chunk = chunk.stream().distinct().collect(Collectors.toList());

                        ArchiveUtils.compress7z(destArchiveName, chunk, i++);

                        sourceArchive = getOutputPath().resolve(platform).resolve(destArchiveName);
                        Path destArchive = destPath.resolve(destArchiveName);
                        fileSize = (int) FileUtils.fileSize(sourceArchive) / 1024;
                        FileUtils.copyFile(sourceArchive, destArchive);
                        FileUtils.deleteFile(sourceArchive);
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

        FileUtils.saveToFile(getInputPath().resolve(platform + "_games.htm"), formatHead() + String.join("\n", lists.getHtmlLines()) + FOOT);
        FileUtils.saveToFile(getInputPath().resolve(platform + "_games.txt"), lists.getTxtLines());
        //FileUtils.saveToFile(getInputPath().resolve(platform + "_games_update.sql"), lists.getUpdateLines());
        FileUtils.saveToFile(getInputPath().resolve(platform + "_games_unmapped.txt"), lists.getUnmappedLines());
        FileUtils.saveToFile(getInputPath().resolve(platform + "_games_unmapped_families.txt"), lists.getUnmappedFamilies());
        FileUtils.saveToFile(getInputPath().resolve(platform + "_games_unmapped_names.txt"), lists.getUnmappedNames().stream().sorted().map(r -> lists.getNormalizedMap().get(r).getName()).collect(Collectors.toList()));
        //FileUtils.saveToFile(getInputPath().resolve(platform + "_create.sql"), lists.getCreateLines().stream().sorted().map(r -> formatCreate(getSid(r), r, StringUtils.cpu(r), null, null, false, warnings)).collect(Collectors.toList()));
        FileUtils.saveToFile(getInputPath().resolve(platform + "_htaccess.txt"), lists.getHtAccessLines());

        // save as excel
        FileUtils.backupFile(xlsUpdatePath());
        writeXls(xlsUpdatePath(), lists.getRecords());

        FileUtils.backupFile(xlsUpdatePath());
        writeXls(xlsCreatePath(), lists.getAddedRecords());
    }

    private static void processFamily(TiViLists lists, Family f, String shortRomPath) {

        //String romPath = formatRomPath(shortRomPath);
        String familyName = StringUtils.escapeChars(norm2(f.getName()));
        String normalizedFamilyName = StringUtils.normalize(familyName);
        if (!lists.getUnmappedNames().contains(normalizedFamilyName)) {

            String syn = lists.getSynonyms().get(normalizedFamilyName);
            if (syn != null && lists.getNormalizedMap().get(syn) != null) {
                CSV.MySqlStructure record = lists.getNormalizedMap().get(syn);
                record.setName(StringUtils.escapeChars(f.getName()));
                record.setCpu(StringUtils.cpu(f.getName()));
                record.setGame(shortRomPath);
                lists.getUnmappedNames().remove(normalizedFamilyName);
                //lists.getUpdateLines().add(formatUpdateQuery(platform, romPath, familyName));
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
            //lists.getUpdateLines().add(formatUpdateQuery(platform, romPath, familyName));
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

    public static void validateRoms() {

        LOGGER.info("Validation is started...");

        // Read CSV
        List<CSV.ValidationStructure> records = readValidationCsv();

        for (CSV.ValidationStructure r : records) {

            String name = StringEscapeUtils.unescapeHtml4(r.getName());

            // Validate availability
            if (org.apache.commons.lang3.StringUtils.isBlank(r.getGame())) {
                LOGGER.warn("!!!" + name + ": Game is absent :(");
                continue;
            }

            if (org.apache.commons.lang3.StringUtils.isBlank(r.getRom())) {
                LOGGER.warn("!!!" + name + ": Rom is absent :(");
                continue;
            }

            Path gamePath;
            Path romPath;

            // Download if need / save
            // name
            try {
                String uri = StringEscapeUtils.unescapeHtml4(r.getGame());
                gamePath = getOutputPath().resolve(platform).resolve("valid-game").resolve(uri.replace(romsUrl, ""));

                if (!Files.exists(gamePath)) {
                    FileUtils.createDirectories(gamePath.getParent());
                    IOUtils.downloadFromUrl(new URL(uri), gamePath);
                }

            } catch (Exception e) {
                LOGGER.error("!!!" + name + ": " + e.getMessage());
                continue;
            }

            // rom
            try {
                String uri = ConfigHolder.uniqueRomsUrl + StringEscapeUtils.unescapeHtml4(r.getRom());
                romPath = getOutputPath().resolve(platform).resolve("valid-rom").resolve(uri.replace(uniqueRomsUrl, ""));

                if (!Files.exists(romPath)) {
                    IOUtils.downloadFromUrl(new URL(uri), romPath);
                }

            } catch (Exception e) {
                LOGGER.error("!!!" + name + ": " + e.getMessage());
                continue;
            }

            // Validate inside
            List<String> gamesList = ArchiveUtils.listFiles(gamePath);
            List<String> romsList = ArchiveUtils.listFiles(romPath);

            if (romsList.size() != 1) {
                LOGGER.warn("!!!" + name + ": wrong roms count: " + romsList.size());
                continue;
            }

            if (!gamesList.containsAll(romsList)) {
                LOGGER.warn("!!!" + name + ": the rom is not in the game archive :(");
            }
        }

        LOGGER.info("Validation is finished...");
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
        //private List<String> updateLines = new ArrayList<>();
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

    /*private static String formatUpdateQuery(String platform, String romPath, String familyName) {
        return String.format("UPDATE `base_%s` SET game='%s' WHERE name='%s';", platform, romPath, familyName);
    }*/

    private static final Map<String, String> GROUP_MAP = new HashMap<>();

    static {
        GROUP_MAP.put("public domain", "pd");
        GROUP_MAP.put("public domain+public domain (slide shows)", "pd_slides");
        GROUP_MAP.put("public domain (gbs player)", "pd_gbs");
        GROUP_MAP.put("public domain (books)", "pd_books");
        GROUP_MAP.put("public domain (infocom games)", "pd_infocom");
        GROUP_MAP.put("multicarts collection", "multi");
        GROUP_MAP.put("vt03 collection", "vt03");
        GROUP_MAP.put("wxn collection", "wxn");
    }

    private static Path getUniquePath() {
        return getOutputPath().resolve(platform).resolve("roms");
    }

    private static void createAddedList(File file) {

        LOGGER.warn("Create added CSV list");
        List<CSV.AddedStructure> addedStructures = new ArrayList<>();
        //a-z
        addedStructures.addAll(families.values().stream().flatMap(f -> f.getMembers().stream()
                .filter(n -> platformsByCpu.get(platform).nonHack(n.getName()) && platformsByCpu.get(platform).nonPD(n.getName()))
                .collect(Collectors.groupingBy(Name::getCleanName)).keySet().stream()
                .map(name -> new CSV.AddedStructure(getSid(name), name))
        ).sorted(Comparator.comparing(CSV.AddedStructure::getName)).collect(Collectors.toList()));
        //PD
        addedStructures.addAll(families.values().stream().flatMap(f -> f.getMembers().stream()
                .filter(n -> platformsByCpu.get(platform).isPD(n.getName()))
                .collect(Collectors.groupingBy(Name::getPdCleanName)).keySet().stream()
                .map(name -> new CSV.AddedStructure(getSid(name), name))
        ).sorted(Comparator.comparing(CSV.AddedStructure::getName)).collect(Collectors.toList()));
        //hack
        addedStructures.addAll(families.values().stream().flatMap(f -> f.getMembers().stream()
                .filter(n -> platformsByCpu.get(platform).isHack(n.getName()))
                .collect(Collectors.groupingBy(Name::getHackCleanName)).keySet().stream()
                .map(name -> new CSV.AddedStructure(getSid(name), name))
        ).sorted(Comparator.comparing(CSV.AddedStructure::getName)).collect(Collectors.toList()));
        // save
        saveAddedCsv(addedStructures, file);
    }

    private static void createGamesList(File file) {

        LOGGER.warn("Create games CSV list");
        List<CSV.MySqlStructure> structures = new ArrayList<>();
        //a-z
        structures.addAll(families.values().stream().flatMap(f -> f.getMembers().stream()
                .filter(n -> platformsByCpu.get(platform).nonHack(n.getName()) && platformsByCpu.get(platform).nonPD(n.getName()))
                .collect(Collectors.groupingBy(Name::getCleanName)).keySet().stream()
                .map(name -> new CSV.MySqlStructure(new CSV.AddedStructure(getSid(name), name)))
        ).sorted(Comparator.comparing(CSV.MySqlStructure::getName)).collect(Collectors.toList()));
        //PD
        structures.addAll(families.values().stream().flatMap(f -> f.getMembers().stream()
                .filter(n -> platformsByCpu.get(platform).isPD(n.getName()))
                .collect(Collectors.groupingBy(Name::getPdCleanName)).keySet().stream()
                .map(name -> new CSV.MySqlStructure(new CSV.AddedStructure(getSid(name), name)))
        ).sorted(Comparator.comparing(CSV.MySqlStructure::getName)).collect(Collectors.toList()));
        //hack
        structures.addAll(families.values().stream().flatMap(f -> f.getMembers().stream()
                .filter(n -> platformsByCpu.get(platform).isHack(n.getName()))
                .collect(Collectors.groupingBy(Name::getHackCleanName)).keySet().stream()
                .map(name -> new CSV.MySqlStructure(new CSV.AddedStructure(getSid(name), name)))
        ).sorted(Comparator.comparing(CSV.MySqlStructure::getName)).collect(Collectors.toList()));
        // save
        saveCsv(structures, file);
    }

    //(list+dump+force63)
    public static void getAllUniqueRoms() {

        List<String> games = FileUtils.loadTextFile(getInputPath().resolve(platform + "_games.txt"));

        TiViLists lists = new TiViLists();

        Path uniquePath = getUniquePath();

        if (isIO) {
            FileUtils.createDirectories(uniquePath);
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
            int fileSize = isIO ? (int) FileUtils.fileSize(destZipRom) / 1024 : -1;
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

        FileUtils.saveToFile(getInputPath().resolve(platform + "_roms.htm"), formatHead() + String.join("\n", lists.getHtmlLines()) + FOOT);
        FileUtils.saveToFile(getInputPath().resolve(platform + "_roms.txt"), lists.getTxtLines())/*.stream().sorted().collect(Collectors.toList()))*/; // mess with hacks, pd
        //FileUtils.saveToFile(getInputPath().resolve(platform + "_roms_update.sql"), lists.getUpdateLines());
        FileUtils.saveToFile(getInputPath().resolve(platform + "_roms_unmapped.txt"), lists.getUnmappedLines());
        FileUtils.saveToFile(getInputPath().resolve(platform + "_roms_unmapped_families.txt"), lists.getUnmappedFamilies());
        FileUtils.saveToFile(getInputPath().resolve(platform + "_roms_unmapped_names.txt"), lists.getUnmappedNames().stream().sorted().map(r -> lists.getNormalizedMap().get(r).getName()).collect(Collectors.toList()));

        List<String> fams = new ArrayList<>();
        families.values().stream().collect(Collectors.toMap(Family::getName, f -> f.getMembers().stream().map(Name::getCleanName).distinct().sorted()))
                .entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(f -> {
            fams.add(f.getKey());
            f.getValue().map(r -> "    " + r).forEach(fams::add);
        });

        FileUtils.saveToFile(getInputPath().resolve(platform + "_families_to_games.txt"), fams);

        List<Pair<String, String>> pairs = new ArrayList<>();
        families.values().stream().collect(Collectors.toMap(Family::getName, f -> f.getMembers().stream().map(Name::getCleanName).distinct().sorted()))
                .entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(f -> f.getValue().forEach(v -> {
            if (!v.equals(f.getKey())) {
                pairs.add(new Pair<>(v, f.getKey()));
            }
        }));

        FileUtils.saveToFile(getInputPath().resolve(platform + "_games_to_families.txt"), pairs.stream().map(p -> p.getKey() + "\t\t" + p.getValue()).sorted().collect(Collectors.toList()));

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
        String dir = GROUP_MAP.get(StringUtils.stripExtension(groupName.toLowerCase()));
        if (dir == null) {
            dir = StringUtils.normalize(groupName.replace("_", " "));
        }
        dir = String.format("%s_%s%s", platform, dir, num);

        Path destZipRom = getUniquePath().resolve(dir).resolve(zipRomName);

        if (isIO) {
            ArchiveUtils.compressZip(destZipRom.toAbsolutePath().toString(), Collections.singletonList(sourceRom.toAbsolutePath().toString()), i.get());
        }

        String shortRomPath = formatShortUniqueRomPath(dir, zipRomName);
        int fileSize = isIO ? (int) FileUtils.fileSize(destZipRom) / 1024 : -1;
        lists.getHtmlLines().add(formatTableCell(formatUniqueRomPath(shortRomPath), sourceRomName, fileSize));
        lists.getTxtLines().add(shortRomPath);

        processRomFamily(lists, name, f, shortRomPath, games);
    }

    private static void processRomFamily(TiViLists lists, Name n, Family f, String shortRomPath, List<String> games) {

        //String romPath = formatRomPath(shortRomPath);
        String name = n.getCleanName();
        String normalizedFamilyName = StringUtils.normalize(StringUtils.escapeChars(name));
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
                record.setName(StringUtils.escapeChars(name));
                record.setCpu(StringUtils.cpu(name));
                record.setRom(shortRomPath);
                lists.getUnmappedNames().remove(normalizedFamilyName);
                //lists.getUpdateLines().add(String.format("UPDATE `base_%s` SET rom='%s' WHERE name='%s';", platform, romPath, escapeQuotes(n.getCleanName())));

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
            //lists.getUpdateLines().add(String.format("UPDATE `base_%s` SET rom='%s' WHERE name='%s';", platform, romPath, escapeQuotes(n.getCleanName())));
        }

        CSV.MySqlStructure record = lists.getAddedNormalizedMap().get(normalizedFamilyName);
        if (record != null) {
            record.setRom(shortRomPath);
            addGameIfNeed(record, f, games);
        }
    }

    private static void addGameIfNeed(CSV.MySqlStructure record, Family f, List<String> games) {

        if (org.apache.commons.lang3.StringUtils.isBlank(record.getGame())) {
            String archiveName = StringUtils.normalize(StringUtils.stripExtension(f.getName()), "7z");
            String fullArchiveName = games.stream().filter(g -> g.endsWith("/" + archiveName)).findFirst().orElse(null);
            if (null != fullArchiveName) {
                record.setGame(fullArchiveName);
            } else {
                LOGGER.warn("Game isn't found: {}; {}", f.getName(), archiveName);
            }
        }
    }

    public static void createUpdateQueries(boolean isFinal) {

        List<String> warnings = new ArrayList<>();
        List<String> screenWarnings = new ArrayList<>();
        List<CSV.RenamedStructure> renamed = new ArrayList<>();

        List<CSV.MySqlStructure> originRecords = readCsv(false);
        List<CSV.MySqlStructure> records = readXls(xlsUpdatePath());

        Set<String> copied = new HashSet<>();
        Set<String> names = new HashSet<>();
        Set<String> cpus = new HashSet<>();

        List<String> updateList = new ArrayList<>();
        for (int k = 0; k < originRecords.size(); k++) {

            String name = records.get(k).getName().trim();
            validateName(name, warnings, names);

            String cpu = records.get(k).getCpu().trim();

            if (isBlank(cpu) && isFinal) {
                warnings.add("CPU is blank for " + cpu);
            } else {
                validateCpu(cpu, warnings, cpus);
            }

            Set<String> sids = new HashSet<>(Arrays.asList(getSid(name), getSid(originRecords.get(k).getName()), records.get(k).getSid()));
            if (sids.size() != 1) {
                screenWarnings.add(originRecords.get(k).getName() + " -> " + records.get(k).getSid() + ": " + name);
            }

            if (isBlank(records.get(k).getGame()) && isFinal) {
                warnings.add("Game is blank for " + name);
            }

            if (isBlank(records.get(k).getRom()) && isFinal) {
                warnings.add("Rom is blank for " + name);
            }

            Map<String, String> vals = new LinkedHashMap<>();
            if (!originRecords.get(k).getName().trim().equals(name)) {
                vals.put("name", name);
                renamed.add(new CSV.RenamedStructure(getSid(name), name, originRecords.get(k).getName()));
            }
            if (!getSid(originRecords.get(k).getName()).equals(getSid(records.get(k).getName()))) {
                vals.put("sid", getSid(records.get(k).getName()));
            }
            if (!originRecords.get(k).getGame().trim().equals(records.get(k).getGame().trim())) {
                if (isNotBlank(records.get(k).getGame())) {
                    vals.put("game", formatRomPath(records.get(k).getGame().trim()));
                }
            }
            if (!originRecords.get(k).getRom().trim().equals(records.get(k).getRom().trim())) {
                if (isNotBlank(records.get(k).getRom())) {
                    vals.put("rom", records.get(k).getRom().trim());
                }
            }

            if (isFinal) {
                copyFinal(records.get(k).getGame(), "games", copied);
                copyFinal(records.get(k).getRom(), "roms", copied);
            }

            vals.put("cpu", cpu);

            vals.put("modified", Long.toString(new Date().getTime() / 1000));
            String sets = vals.entrySet().stream().map(e -> String.format("`%s`='%s'", e.getKey(), e.getValue())).collect(Collectors.joining(", "));
            updateList.add(String.format("UPDATE `base_%s` SET %s WHERE name='%s';", platform, sets, originRecords.get(k).getName()));
        }
        FileUtils.saveToFile(getInputPath().resolve(platform + "_update.sql"), String.join("\n", updateList));

        // Added
        records = readXls(xlsCreatePath());

        List<String> createList = new ArrayList<>();
        for (CSV.MySqlStructure record : records) {
            createList.add(formatCreate(record.getSid(), record.getName().trim(), record.getCpu().trim(), record.getGame().trim(), record.getRom().trim(), isFinal, warnings, names, cpus));

            if (isFinal) {
                copyFinal(record.getGame(), "games", copied);
                copyFinal(record.getRom(), "roms", copied);
            }
        }
        FileUtils.saveToFile(getInputPath().resolve(platform + "_create.sql"), String.join("\n", createList));

        saveRenamedCsv(renamed);

        if (!warnings.isEmpty()) {
            LOGGER.warn("WARNINGS:");
            warnings.forEach(LOGGER::warn);
        }

        if (!screenWarnings.isEmpty()) {
            LOGGER.warn("SCREENSHOTS NEED TO FIX:");
            screenWarnings.forEach(LOGGER::warn);
        }

        LOGGER.info("Done");
    }

    private static void validateName(String name, List<String> warnings, Set<String> names) {

        if (name.contains("'")) {
            warnings.add("Unescaped character ' in: " + name);
        }

        Set<Integer> amps = getAllIndexes(name, "&amp;");
        amps.addAll(getAllIndexes(name, "&rsquo;"));

        if (!getAllIndexes(name, "&").equals(amps)) {
            warnings.add("Unescaped character & in: " + name);
        }

        if (names.contains(name)) {
            warnings.add("Duplicate name: " + name);
        } else {
            names.add(name);
        }
    }

    private static void validateCpu(String cpu, List<String> warnings, Set<String> cpus) {

        if (cpus.contains(cpu)) {
            warnings.add("Duplicate cpu: " + cpu);
        } else {
            cpus.add(cpu);
        }
    }

    private static Set<Integer> getAllIndexes(String name, String substr) {
        Set<Integer> result = new HashSet<>();
        int index = name.indexOf(substr);
        while (index >= 0) {
            result.add(index);
            index = name.indexOf(substr, index + 1);
        }
        return result;
    }

    private static void copyFinal(String source, String path, Set<String> copied) {
        if (isIO && org.apache.commons.lang3.StringUtils.isNotBlank(source)) {
            Path sourceFile = getOutputPath().resolve(platform).resolve(path).resolve(source.trim());
            Path destFile = getOutputPath().resolve(platform + "-final").resolve(path).resolve(source.trim());
            if (Files.exists(sourceFile)) {
                if (Files.notExists(destFile.getParent())) {
                    FileUtils.createDirectories(destFile.getParent());
                }
                FileUtils.copyFile(sourceFile, destFile);
            } else if (!copied.contains(source)) {
                LOGGER.warn("File isn't found: {}", sourceFile.toAbsolutePath().toString());
            }
            copied.add(source);
        }
    }

    private static String formatCreate(String sid, String name, String cpu, String game, String rom, boolean isFinal, List<String> warnings, Set<String> names, Set<String> cpus) {

        // n INC: SELECT MAX(n) AS max FROM base_".$catcpu

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

        validateName(name, warnings, names);

        if (isBlank(sid) && isFinal) {
            warnings.add("SID is blank for " + name);
        }

        if (isBlank(cpu) && isFinal) {
            warnings.add("CPU is blank for " + name);
        } else {
            validateCpu(cpu, warnings, cpus);
        }

        if (isNotBlank(game)) {
            game = formatRomPath(game).trim();
        } else if (isFinal) {
            warnings.add("Game is blank for " + name);
        }

        if (isNotBlank(rom)) {
            rom = rom.trim();
        } else if (isFinal) {
            warnings.add("Rom is blank for " + name);
        }

        return String.format(format, platform, platform, modified, modified, sid.trim(), cpu, name, region.trim(), game, rom);
    }

    public static String getSid(String name) {
        name = StringEscapeUtils.unescapeHtml4(name.replace("&rsquo;", "'")); // &amp;, ...
        String restricted = "'\"().,&!?$@#%^*=/\\[];:|<>{}";

        for (char c : restricted.toCharArray()) {
            name = name.replace("" + c, "");
        }

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
