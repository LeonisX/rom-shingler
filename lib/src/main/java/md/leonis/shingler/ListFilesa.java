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

import static md.leonis.shingler.utils.BinaryUtils.*;

public class ListFilesa {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListFilesa.class);

    private static final Cache<String, long[]> cache = new Cache<>(0, 0, 2400);

    private static Path userHome = Paths.get(System.getProperty("user.home"));
    private static Path rootWorkDir = userHome.resolve("shingler");
    private static Path shinglesDir = rootWorkDir.resolve("shingles");
    private static Path collectionsDir = rootWorkDir.resolve("collections");
    private static Path familiesDir = rootWorkDir.resolve("families");

    private static String collection = "GoodNES 3.14";
    private static String platform = "nes";
    private static RomsCollection romsCollection;
    private static Map<String, GID> byHash;
    private static Map<String, GID> byTitle;

    private static Path romsDir = Paths.get("D:\\Downloads\\games314");

    public static void main(String[] args) throws IOException {

        MeasureMethodTest.premain();
        generateFamilies();
    }

    // Need shingles
    private static void generateFamilies() {

        LOGGER.info("Reading list of games...");
        List<File> archives = IOUtils.listFiles(romsDir.toFile()).stream().filter(f -> f.getName().endsWith(".7z")).collect(Collectors.toList());

        LOGGER.info("Reading list of games...");
        romsCollection = IOUtils.loadCollection(workCollectionsDir().resolve(collection).toFile());
        byHash = romsCollection.getGids().values().stream().collect(Collectors.toMap(h -> bytesToHex(h.getSha1()), Function.identity()));
        byTitle = romsCollection.getGids().values().stream().collect(Collectors.toMap(GID::getTitle, Function.identity()));

        LOGGER.info("Get file names from archives...");
        Map<File, List<String>> map = archives.stream().collect(Collectors.toMap(Function.identity(), ListFilesa::listFiles));

        //TODO not need :(
        //LOGGER.info("Preparing cache...");
        //Main1024a.loadSamplesInCache(map.values().stream().flatMap(v -> v.stream().map(File::new)).collect(Collectors.toList()));

        //TODO this code is needed when generate families from scratch
        /*LOGGER.info("Creating list of unique games...");
        Map<File, List<String>> filteredMap = new HashMap<>();

        // really unique
        map.forEach((key, value) -> {
            List<String> names = value.stream().filter(ListFilesa::nonHack)
                    .map(s -> s.substring(0, s.lastIndexOf('.')))
                    .map(ListFilesa::normalize)
                    .distinct()
                    .filter(v -> !v.equals(normalize(key.getName().replace(".7z", ""))))
                    .collect(Collectors.toList());
            filteredMap.put(key, names);
        });*/

        //saveUniqueListToFile(filteredMap);

        LOGGER.info("Generating families...");
        generateFamilies(map, 8, 30);

        //otherStatistics(map);
    }

    private static void generateFamilies(Map<File, List<String>> map, int index, int jakkardIndex) {

        Main1024a.cache.fullCleanup();

        IOUtils.createDirectories(familiesDir.resolve(platform));
        File familyFile = familiesDir.resolve(platform).resolve(collection + index).toFile();

        Map<String, Family> families;
        if (familyFile.exists()) {
            System.out.println(String.format("%nReading families from file %s...", familyFile));
            families = IOUtils.loadFamilies(familyFile);
        } else {
            System.out.println("\nGenerating families...");
            Map<String, List<Name>> namesList = new HashMap<>();
            map.forEach((key, value) -> namesList.put(key.getName(), value.stream().map(v -> new Name(new File(v), false)).collect(Collectors.toList())));

            families = namesList.entrySet().stream()/*.filter(e -> e.getValue().size() != 1)*/
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new Family(e.getKey(), e.getValue())));
        }

        calculateRelations(families, index, familyFile);

        System.out.println("Saving family...");
        IOUtils.serialize(familyFile, families);

        int total = (int) map.values().stream().mapToLong(Collection::size).sum();
        int inFamily = families.values().stream().map(Family::size).mapToInt(Integer::intValue).sum();

        System.out.println("Total: " + total);
        System.out.println("In family: " + inFamily);
        System.out.println("Not in family: " + (total - inFamily));

        //Main1024a.saveDropCsv(families, index, 50);

        //TODO jakkardIndex
        /*Main1024a.drop(families, 50);
        inFamily = families.values().stream().map(Main1024a.Family::size).mapToInt(Integer::intValue).sum();

        System.out.println("Total: " + total);
        System.out.println("In family: " + inFamily);
        System.out.println("Not in family: " + (total - inFamily));*/

        System.out.println("==");
        //TODO merge jakkardIndex 20
        //TODO index 1 or 16. Better 1
        //families = mergeFamilies(families, 30, 64);

        //TODO проходиться сиротами и смотреть куда пристроить.
    }

    static void calculateRelations(Map<String, Family> families, int index, File familyFile) {

        Path sampleDir = shinglesDir.resolve(platform).resolve("sample" + index);

        int[] k = {0};
        int[] save = {0};

        families.values().forEach(family -> {

            if (save[0] > 100000 * index) {
                System.out.println("Saving family...");
                IOUtils.serialize(familyFile, families);
                save[0] = 0;
            }

            int relationsCount = family.getMembers().size() * (family.getMembers().size() - 1) / 2;
            if (family.getRelations().size() == relationsCount) { // x * (x - 1) / 2
                System.out.println(String.format("%nSkipping: %s... [%s] %2.3f%%", family.getName(), family.size(), (k[0] + 1) * 100.0 / families.size()));
                k[0]++;
            } else {

                System.out.println(String.format("%nComparing: %s... [%s] %2.3f%%", family.getName(), family.size(), (k[0] + 1) * 100.0 / families.size()));
                family.getRelations().clear();

                for (int i = 0; i < family.size() - 1; i++) {

                    Name name1 = family.get(i);

                    long[] s1Set = ShingleUtils.loadFromCache(cache, sampleDir.resolve(bytesToHex(byTitle.get(name1.getName()).getSha1()) + ".shg"));

                    for (int j = i + 1; j < family.size(); j++) {

                        Name name2 = family.get(j);

                        long[] s2Set = ShingleUtils.loadFromCache(cache, sampleDir.resolve(bytesToHex(byTitle.get(name2.getName()).getSha1()) + ".shg"));

                        long[] s1intersect = intersectArrays(s1Set, s2Set);
                        long[] s1union = unionArrays(s1Set, s2Set);
                        double jakkard = s1intersect.length * 100.0 / s1union.length;

                        name1.addJakkardStatus(jakkard);
                        name2.addJakkardStatus(jakkard);

                        Result result = new Result(name1, name2, jakkard);
                        System.out.println(i + "->" + j + ": " + result);

                        family.addRelation(result);
                        save[0]++;
                    }
                    name1.setDone(true);
                }
                family.setMother(family.getMembers().stream().max(Comparator.comparing(Name::getJakkardStatus)).orElse(null));
                k[0]++;
            }
        });
    }

    static void drop(Map<String, Family> families, double jakkardIndex) {

        families.values().forEach(family -> {
            List<Name> toDelete = deleteNonSiblings(family, jakkardIndex);

            toDelete.forEach(td -> System.out.println(String.format("Dropping: %s (%2.4f%%)", td.getName(), td.getJakkardStatus() / family.size())));

            family.setRelations(family.getRelations().stream().filter(r -> toDelete.contains(r.getName1()) || toDelete.contains(r.getName2())).collect(Collectors.toList()));
            family.getRelationsCount().forEach((key, value) ->
                    family.getRelationsCount().replace(key, (int) family.getRelations().stream().filter(r -> r.getName1().getName().equals(key)).count())
            );
            family.setIndividualRelations(family.getIndividualRelations().stream().filter(r -> toDelete.stream().anyMatch(d -> r.startsWith(d.getName()))).collect(Collectors.toSet()));

            family.getMembers().removeAll(toDelete);

            recalculateJakkard(family);
        });
    }

    private static List<Name> deleteNonSiblings(Family fam, double jakkardIndex) {
        Family family = new Family(fam);

        List<Name> deleted = new ArrayList<>();

        //TODO revert
        if (family.getName().equals("Public Domain.7z") || family.getName().equals("Multicarts Collection.7z")
                || family.getName().equals("Wxn Collection.7z") || family.getName().equals("VT03 Collection.7z")) {
            return deleted;
        }

        double status = family.getJakkardStatus(0);
        double k = 100 / status;

        for (int i = 1; i < family.size(); i++) {
            if (family.getJakkardStatus(i) * k < jakkardIndex) {
                deleted.add(family.get(i));
            }
        }

        return deleted;
    }

    static void saveDropCsv(Map<String, Family> families, int index, double jakkardIndex) {

        List<String> toDelete = families.values().stream().flatMap(family -> getNonSiblings(family, jakkardIndex).stream()
                .sorted(Comparator.comparing(Name::getJakkardStatus))
                .map(n -> String.format("\"%s\";\"%s\";\"%2.4f\"", family.getName().replace(".7z", ""), n.getName(), n.getJakkardStatus() / family.size()))).collect(Collectors.toList());

        try {
            Files.write(Paths.get("low-jakkard" + index + ".csv"), toDelete, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Name> getNonSiblings(Family family, double jakkardIndex) {

        List<Name> deleted = new ArrayList<>();

        if (family.getName().equals("Public Domain.7z") || family.getName().equals("Multicarts Collection.7z")
                || family.getName().equals("Wxn Collection.7z") || family.getName().equals("VT03 Collection.7z")) {
            return deleted;
        }

        double status = family.getJakkardStatus(0);
        double k = 100 / status;

        for (int i = 1; i < family.size(); i++) {
            if (family.getJakkardStatus(i) * k < jakkardIndex) {
                deleted.add(family.get(i));
            }
        }

        return deleted;
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

    private static void otherStatistics(Map<File, List<String>> map) {
        System.out.println("Generating families...");
        Main1024a.SAMPLES.forEach(index -> {
            if (!new File("low-jakkard" + index + ".csv").exists())
                generateFamilies(map, index, 30);
        });

        System.out.println("Calculating Jakkard deviations for families...");
        Map<String, Family> families1 = IOUtils.loadFamilies(new File("list-family" + 1));

        for (int i = 1; i < Main1024a.SAMPLES.size(); i++) {
            measureJakkard(Main1024a.SAMPLES.get(i), families1);
        }
    }

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
                    System.out.println(String.format("%s:%s %2.2f->%2.2f(%2.4f%%)", family1.getName(), family1.get(j).getName(), j1, jX, deviation));
                }
                maxDeviation = Math.max(maxDeviation, deviation);
                sumDeviation += deviation;
            }
            medDeviation += sumDeviation / family1.size();
        }
        System.out.println(String.format("%s. Max: %2.4f%%; Med: %2.4f%%", index, maxDeviation, medDeviation / families.size()));
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

    public static Path workCollectionsDir() {
        return collectionsDir.resolve(platform);
    }
}
