package md.leonis.shingler;

import md.leonis.shingler.model.*;
import md.leonis.shingler.utils.IOUtils;
import md.leonis.shingler.utils.MeasureMethodTest;
import md.leonis.shingler.utils.Measured;
import md.leonis.shingler.utils.ShingleUtils;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static md.leonis.shingler.model.ConfigHolder.*;
import static md.leonis.shingler.utils.BinaryUtils.*;

public class ListFilesa {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListFilesa.class);

    private static final Cache<String, long[]> cache = new Cache<>(0, 0, 2400);

    public static void main(String[] args) {

        MeasureMethodTest.premain();

        ConfigHolder.setDenominatorId(3); // 8
        ConfigHolder.platform = "nes";
        ConfigHolder.collection = "_GoodNES 3.23b (test)"; // "GoodNES 3.14"

        ConfigHolder.romsCollection = new RomsCollection();
        ConfigHolder.romsCollection.setTitle(collection);
        ConfigHolder.romsCollection.setPlatform(platform);
        ConfigHolder.romsCollection.setType(CollectionType.PLAIN);
        ConfigHolder.romsCollection.setRomsPathString("D:\\Downloads\\games-work"); // "D:\\Downloads\\games314";
        //ConfigHolder.romsCollection = IOUtils.loadCollection(ConfigHolder.workCollectionsDir().resolve(collection).toFile());

        //generateFamilies();
        archiveToFamilies();

        processFamilies();

        //otherStatistics(map);
    }

    public static void generateFamilies() {

        LOGGER.info("Reading list of games...");
        List<Path> files = IOUtils.listFiles(romsCollection.getRomsPath());

        Set<String> familyNames = families.values().stream().flatMap(f -> f.getMembers().stream().map(Name::getName)).collect(Collectors.toSet());
        Set<String> orphanedNames = romsCollection.getGidsMap().values().stream().map(GID::getTitle).collect(Collectors.toSet());
        orphanedNames.removeAll(familyNames);

        //List<Name> names = files.stream().map(f -> new Name(f.toFile(), false)).collect(Collectors.toList());
        List<Name> names = files.stream().filter(f -> orphanedNames.contains(f.getFileName().toString())).map(f -> new Name(f.toFile(), false)).collect(Collectors.toList());

        File familyFile = fullFamiliesPath().toFile();

        if (familyFile.exists()) { //TODO not need
            LOGGER.info("Reading families from file {}...", familyFile);
            families = IOUtils.loadFamilies(familyFile);
        } else {
            LOGGER.info("Generating families from scratch...");
        }

        LOGGER.info("Generating families...");
        Map<String, List<Name>> namesList = names.stream().filter(n -> nonHack(n.getName())).collect(Collectors.groupingBy(Name::getCleanName));

        /*families = namesList.entrySet().stream().filter(e -> e.getValue().size() != 1)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new Family(e.getValue())));*/

        namesList.entrySet().stream().filter(e -> e.getValue().size() != 1)
                .forEach(e -> families.put(e.getKey(), new Family(e.getValue())));

        LOGGER.info("Saving families...");
        IOUtils.createDirectories(workFamiliesPath());
        IOUtils.serializeFamiliesAsJson(familyFile, families);

        //saveUniqueListToFile(filteredMap);
    }

    public static void archiveToFamilies() {

        LOGGER.info("Reading list of games...");
        List<File> archives = IOUtils.listFiles(romsCollection.getRomsPath().toFile()).stream().filter(f -> f.getName().endsWith(".7z")).collect(Collectors.toList());

        LOGGER.info("Get file names from archives...");
        Map<File, List<String>> map = archives.stream().collect(Collectors.toMap(Function.identity(), ListFilesa::listFiles));

        File familyFile = fullFamiliesPath().toFile();

        if (familyFile.exists()) {
            LOGGER.info("Reading families from file {}...", familyFile);
            families = IOUtils.loadFamilies(familyFile);
        } else {
            LOGGER.info("Generating families based on GoodMerged source...");
            Map<String, List<Name>> namesList = new HashMap<>();
            map.forEach((key, value) -> namesList.put(key.getName(), value.stream().map(v -> new Name(new File(v), false)).collect(Collectors.toList())));

            families = namesList.entrySet().stream()/*.filter(e -> e.getValue().size() != 1)*/
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new Family(e.getKey(), e.getValue(), FamilyType.FAMILY)));

            LOGGER.info("Saving families...");
            IOUtils.createDirectories(workFamiliesPath());
            IOUtils.serializeFamiliesAsJson(familyFile, families);
        }
    }

    private static void processFamilies() {

        calculateRelations();

        LOGGER.info("Saving families...");
        IOUtils.serializeFamiliesAsJson(fullFamiliesPath().toFile(), families);

        //saveDropCsv(families, 50);

        //TODO jakkardIndex
        //Main1024a.drop(families, 50);
        //TODO merge jakkardIndex 20
        //TODO index 1 or 16. Better 1
        //families = mergeFamilies(families, 30, 64);

        //TODO проходиться сиротами и смотреть куда пристроить.
    }

    public static void cleanRelations() {
        families.values().forEach(f -> f.setRelations(new ArrayList<>()));
    }

    public static void calculateRelations() {

        Main1024a.cache.fullCleanup();

        byTitle = romsCollection.getGidsMap().values().stream().collect(Collectors.toMap(GID::getTitle, Function.identity()));

        int k = 0;

        for (Family family : families.values()) {

            if (needToStop[0]) {
                LOGGER.info("Execution was interrupted!");
                /*LOGGER.info("Saving families...");
                IOUtils.serializeFamiliesAsJson(fullFamiliesPath().toFile(), families);*/
                needToStop[0] = false;
                break;
            }

            if (family.getType() == FamilyType.FAMILY) {
                int relationsCount = family.getMembers().size() * (family.getMembers().size() - 1) / 2;
                double percent = (k + 1) * 100.0 / families.size();
                if (family.getRelations().size() == relationsCount) { // x * (x - 1) / 2
                    LOGGER.debug("Skipping: {}... [{}]|{}", family.getName(), family.size(), percent);
                } else {
                    LOGGER.info("Comparing: {}... [{}]|{}", family.getName(), family.size(), percent);
                    doCalculateRelations(family, percent, true);
                }
            }
            k++;
            family.selectMother();
        }
    }

    public static void calculateRelations(Family family) {
        calculateRelations(family, true);
    }

    public static void calculateRelations(Family family, boolean log) {

        if (family.getType() == FamilyType.FAMILY) {
            Main1024a.cache.fullCleanup();

            byTitle = romsCollection.getGidsMap().values().stream().collect(Collectors.toMap(GID::getTitle, Function.identity()));

            if (log) {
                LOGGER.info("Comparing: {}... [{}]", family.getName(), family.size());
            }
            doCalculateRelations(family, -1, log);
            family.selectMother();
        }
    }

    private static void doCalculateRelations(Family family, double percent, boolean log) {

        family.getRelations().clear();
        family.getMembers().forEach(m -> m.setJakkardStatus(0));
        Platform platform = platformsByCpu.get(ConfigHolder.platform);

        for (int i = 0; i < family.size() - 1; i++) {

            Name name1 = family.get(i);

            long[] s1Set = ShingleUtils.loadFromCache(cache, fullShinglesPath().resolve(bytesToHex(byTitle.get(name1.getName()).getSha1()) + ".shg"));

            for (int j = i + 1; j < family.size(); j++) {

                Name name2 = family.get(j);

                long[] s2Set = ShingleUtils.loadFromCache(cache, fullShinglesPath().resolve(bytesToHex(byTitle.get(name2.getName()).getSha1()) + ".shg"));

                double jakkard = doCalculateJakkard(s1Set, s2Set);

                if (platform.isGood(name2.getName())) {
                    name1.addJakkardStatus(jakkard);
                } else {
                    //LOGGER.info("{} is bad", name2.getName());
                    name1.addJakkardStatus(100);
                }
                name2.addJakkardStatus(jakkard);

                Result result = new Result(name1, name2, jakkard);
                if (log) {
                    LOGGER.info("{}->{}: {}|{}", i, j, result, percent == -1 ? (i + 1.0) * 100 / family.size() : percent);
                }

                family.addRelation(result);
            }
            name1.setDone(true);
        }
    }

    private static double doCalculateJakkard(long[] s1Set, long[] s2Set) {
        long[] s1intersect = intersectArrays(s1Set, s2Set);
        long[] s1union = unionArrays(s1Set, s2Set);
        return s1intersect.length * 100.0 / s1union.length;
    }

    public static Map<Family, Double> calculateRelations(String name) {
        return calculateRelations(name, false);
    }

    public static Map<Family, Double> calculateRelations(String name, boolean log) {
        return calculateRelations(name, "", log);
    }

    public static Map<Family, Double> calculateRelations(String name, String ignore, boolean log) {

        Main1024a.cache.fullCleanup();
        byTitle = romsCollection.getGidsMap().values().stream().collect(Collectors.toMap(GID::getTitle, Function.identity()));

        int[] k = {0};
        long[] s1Set = ShingleUtils.loadFromCache(cache, fullShinglesPath().resolve(bytesToHex(byTitle.get(name).getSha1()) + ".shg"));

        return families.values().stream().filter(e -> !e.getName().equals(ignore)).filter(e -> e.getType() == FamilyType.FAMILY).collect(Collectors.toMap(Function.identity(), family -> {
            if (log) {
                LOGGER.info("Comparing: {} with {}|{}", name, family.getName(), (k[0]++ + 1) * 100.0 / families.size());
            }
            //TODO here if no shingle file (or NPE) - we don't see this error at all!!!
            long[] s2Set = ShingleUtils.loadFromCache(cache, fullShinglesPath().resolve(bytesToHex(byTitle.get(family.getMother().getName()).getSha1()) + ".shg"));

            return doCalculateJakkard(s1Set, s2Set);
        })).entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(8)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, LinkedHashMap::new));
    }

    static void drop(Map<String, Family> families, double jakkardIndex) {

        families.values().forEach(family -> {
            List<Name> toDelete = getNonSiblings(family, jakkardIndex);

            toDelete.forEach(td -> LOGGER.info("Dropping: {}|{}", td.getName(), td.getJakkardStatus() / family.size()));

            family.setRelations(family.getRelations().stream().filter(r -> toDelete.contains(r.getName1()) || toDelete.contains(r.getName2())).collect(Collectors.toList()));
            family.getRelationsCount().forEach((key, value) ->
                    family.getRelationsCount().replace(key, (int) family.getRelations().stream().filter(r -> r.getName1().getName().equals(key)).count())
            );
            family.setIndividualRelations(family.getIndividualRelations().stream().filter(r -> toDelete.stream().anyMatch(d -> r.startsWith(d.getName()))).collect(Collectors.toSet()));

            family.getMembers().removeAll(toDelete);

            recalculateJakkard(family);
        });
    }

    private static List<Name> getNonSiblings(Family family, double jakkardIndex) {

        List<Name> deleted = new ArrayList<>();

        if (family.getName().equals("Public Domain.7z") || family.getName().equals("Multicarts Collection.7z")
                || family.getName().equals("Wxn Collection.7z") || family.getName().equals("VT03 Collection.7z") || family.getName().equals("Multi-Game Pirate Carts.7z")) {
            return deleted;
        }

        double status = family.getMother().getJakkardStatus();
        double k = 100 / status;

        for (int i = 1; i < family.size(); i++) {
            if (family.getJakkardStatus(i) * k < jakkardIndex) {
                deleted.add(family.get(i));
            }
        }

        return deleted;
    }

    static void saveDropCsv(Map<String, Family> families, double jakkardIndex) {

        List<String> toDelete = families.values().stream().flatMap(family -> getNonSiblings(family, jakkardIndex).stream()
                .sorted(Comparator.comparing(Name::getJakkardStatus))
                .map(n -> String.format("\"%s\";\"%s\";\"%2.4f\"", family.getName().replace(".7z", ""), n.getName(), n.getJakkardStatus() / family.size()))).collect(Collectors.toList());

        try {
            Files.write(Paths.get("low-jakkard" + getDenominator() + ".csv"), toDelete, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Measured
    private static void recalculateJakkard(Family family) {
        family.getMembers().forEach(m -> {
            List<Result> results = family.getRelations().stream().filter(r -> r.getName1().equals(m) || r.getName2().equals(m)).collect(Collectors.toList());
            m.setJakkardStatus(results.stream().mapToDouble(Result::getJakkard).sum());
        });
    }

    private static void saveUniqueListToFile(Map<File, List<String>> filteredMap) throws IOException {
        List<String> lines = new ArrayList<>();

        filteredMap.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getName())).forEach(e -> {
            lines.add(e.getKey().getName().replace(".7z", ""));
            if (e.getValue().size() >= 1) {
                e.getValue().forEach(v -> lines.add("    " + v));
            }
            lines.add("");
        });

        Files.write(Paths.get("unique.txt"), lines);
    }

    /*private static void otherStatistics(Map<File, List<String>> map) {
        LOGGER.info("Generating families...");
        Main1024a.SAMPLES.forEach(index -> {
            if (!new File("low-jakkard" + index + ".csv").exists())
                archiveToFamilies(map, index, 30);
        });

        LOGGER.info("Calculating Jakkard deviations for families...");
        Map<String, Family> families1 = IOUtils.loadFamilies(new File("list-family" + 1));

        for (int i = 1; i < Main1024a.SAMPLES.size(); i++) {
            measureJakkard(Main1024a.SAMPLES.get(i), families1);
        }
    }*/

    private static void measureJakkard(Integer index, Map<String, Family> fams1) {

        List<Family> families1 = new ArrayList<>(fams1.values());

        Map<String, Family> families = IOUtils.loadFamilies(new File("list-family" + index));
        List<Family> familiesX = new ArrayList<>(families.values());

        double maxDeviation = 0;
        double medDeviation = 0;

        for (int i = 0; i < families1.size(); i++) {
            Family family1 = families1.get(i);
            Family familyX = familiesX.get(i);

            double sumDeviation = 0;

            for (int j = 0; j < family1.size(); j++) {

                double j1 = family1.getJakkardStatus(j);
                double jX = familyX.getJakkardStatus(j);
                double deviation = deviation(j1, jX);
                if (Double.valueOf(deviation).isNaN()) {
                    deviation = 0;
                }
                if (deviation > 10 && j1 > 3) {
                    LOGGER.info(String.format("%s:%s %2.2f->%2.2f(%2.4f%%)|%s", family1.getName(), family1.get(j).getName(), j1, jX, deviation, (i + 1.0) / families1.size()));
                }
                maxDeviation = Math.max(maxDeviation, deviation);
                sumDeviation += deviation;
            }
            medDeviation += sumDeviation / family1.size();
        }
        LOGGER.info(String.format("%s. Max: %2.4f%%; Med: %2.4f%%", index, maxDeviation, medDeviation / families.size()));
    }

    // a < b = ((b-a)/a) * 100
    static double deviation(double d1, double d2) {
        return Math.abs(d2 - d1) / d1 * 100;
    }

    private static boolean nonHack(String s) {
        return !s.contains("(Hack)") && !s.contains("(Hack ") && !s.contains(" Hack)");
    }

    private static String normalize(String s) {
        List<String> chunks = new ArrayList<>();

        toChunks(s).forEach(c -> {
            if (
                    !c.matches("^\\[.+\\]$") &&
                            !c.matches("^\\(.+\\)$")


                    /*!c.matches("^\\[[abhfopt]\\d+]$") &&
                            !c.matches("^\\[[b][abc]]$") &&
                            !c.matches("^\\[[a][abcdef]]$") &&
                            !c.matches("^\\[!p]$") &&
                            !c.matches("^\\[T[\\-+].+]$") &&
                            !c.matches("^\\(PRG[0123]\\)$") &&
                            !c.matches("^\\(CHR[0123]\\)$") &&
                            !c.matches("^\\(REV[012ABC]\\)$") &&
                            !c.matches("^\\(REV1\\.[123x]\\)$") &&
                            !c.matches("^\\[hFFE]$") &&
                            !c.matches("^\\[hM\\d\\d\\d]$") &&

                            !c.matches("^\\[hM\\d{2}]$") &&

                            !c.matches("^\\((JU|UE|PC10|VC|VS|GC|Ch|M3|M4|M5|Chi|ChS|ChT|Eng|KC|[WJUERKGFICS]|Tw|Sw|Beta|Prototype|Prototype1|Prototype-1|Prototype2|Prototype-2|Sample|Decrypted|PAL|NTSC|Unl|Wxn|Gamepad|GBA e-Reader|Player 1 Mode|Player 2 Mode|GBA-Capcom Classics|FDS Conversion|FDS Conversion,Kaiser)\\)$") &&

                            !c.matches("^\\((Eternal Life|No Timer|Direction|2 Joys|E-GC|J-GC|J-AC|E-VC|J-VC|No-Bug|Dev Version|FDS Conversion, Kaiser Hacked|FDS Conversion, CHR-ROM version|Arabic|Maxi 15 Pack Version|With +4 Menu|START|Sound|Defaced by NA|NTSC by Ian Bell & David Braben)\\)$") &&

                            !c.matches("^\\((Newer Prototype|Older Prototype|Test Version|Wisdom Tree|SRAM Saving|Title & Weapon-Magic Exploit by Parasyte|Title by Parasyte v1.0|Weapon-Magic Exploit by Parasyte|Earlier|Open Bus|Cutscene Extra Animation|Ladies)\\)$") &&

                            !c.matches("^\\((7 Lasers and Double Bullets|Gun-Nac|Heavy Barrel|SRAM|UBI Soft|Jackal|SELECT\\+START|Jackie Chan|No Time|V5.0 CHR 6.0|V5.0 CHR 1.3|by dragon2snow|Push START|SOMARI-W|Demo Cart|Coolboy Version|NTSC by nfzxyd|Dragon)\\)$") &&

                            !c.matches("^\\((CH88|6 Lifebars|Sachen-HES|Hwang Shinwei,RCM|Sachen-NTSC|Sachen-PAL|Sachen-Hacker|Sachen-FC|MGC-008|Beta1|Panesian|Color Dreams|YOKO-Y1|Final|V1995|VT-482|ZR006|Blue Version|REV.B)\\)$") &&

                            !c.matches("^\\((JY-011|NT-851|YH2001|MD102|NT-328|YY-030|M1274|16 Player|8 Player|Time|Menu|9 Lives|50 Lives|100 Lives|Namco|AbabSoft|Dung Dung No 1)\\)$") &&


                            !c.matches("^\\((Sachen|Sachen-Chinese|Sachen-English|Bunch|Hacker|Hwang Shinwei|RCM Group|SuperGame|Gluk Video|Gluk Video, NTDEC|Camerica|Aladdin|Shin Data|Rex-Soft|Rex Soft|Subor|Tengen|Sugar Softec|Sachen-JAP|Sachen-USA|AGCI|HES|Joy Van|AVE|KaSing)\\)$") &&

                            !c.matches("^\\((GameTec)\\)$") &&


                            !c.matches("^\\(1[98]\\d\\d\\)$") &&
                            !c.matches("^\\([vV][123456]\\.\\da?\\)$") &&

                            !c.matches("^\\(Mapper \\d+\\)$") &&
                            !c.matches("^\\(NJ0\\d\\d\\)$") &&
                            !c.matches("^\\(ES-\\d+\\)$") &&

                            !c.matches("^\\(Hack\\)$") &&
                            !c.matches("^\\(Hack .+\\)$") &&
                            !c.matches("^\\(.+ Hack\\)$") &&

                            !c.matches("^\\[[!U]]$")*/
            ) {
                chunks.add(c);
            }
        });

        return String.join(" ", chunks);
    }

    static List<String> toChunks(String string) {
        List<String> chunks = new ArrayList<>();

        int openSB = 0;
        int openBR = 0;

        StringBuilder chunk = new StringBuilder();

        char[] chars = string.toCharArray();
        for (char c : chars) {
            switch (c) {
                case '[':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.toString().trim());
                        chunk = new StringBuilder();
                    }
                    openSB++;
                    break;
                case ']':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.append(']').toString().trim());
                        chunk = new StringBuilder();
                    }
                    openSB--;
                    break;
                case '(':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.toString().trim());
                        chunk = new StringBuilder();
                    }
                    openBR++;
                    break;
                case ')':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.append(')').toString().trim());
                        chunk = new StringBuilder();
                    }
                    openBR--;
                    break;
            }
            chunk.append(c);
        }
        if (chunk.length() > 0) {
            chunks.add(chunk.toString());
        }
        return chunks;
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

    public static List<String> listFiles(File file) {
        try (SevenZFile archiveFile = new SevenZFile(file)) {
            return StreamSupport.stream(archiveFile.getEntries().spliterator(), false).map(SevenZArchiveEntry::getName).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
