package md.leonis.shingler;

import md.leonis.shingler.model.*;
import md.leonis.shingler.utils.*;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    public static Set<String> getOrphanNames() {
        Set<String> familyNames = families.values().stream().flatMap(f -> f.getMembers().stream().map(Name::getName)).collect(Collectors.toSet());
        Set<String> orphanedNames = romsCollection.getGidsMap().values().stream().map(GID::getTitle).collect(Collectors.toSet());
        orphanedNames.removeAll(familyNames);
        return orphanedNames;
    }

    public static void generateFamilies() {

        LOGGER.info("Reading list of games...");
        Platform plat = platformsByCpu.get(platform);
        List<Path> files = IOUtils.listFiles(romsCollection.getRomsPath());

        Set<String> orphanedNames = getOrphanNames();

        List<Name> names = files.stream().filter(f -> orphanedNames.contains(f.getFileName().toString())).map(f -> new Name(f.getFileName().toString(), false)).collect(Collectors.toList());

        File familyFile = fullFamiliesPath().toFile();

        /*if (familyFile.exists()) { //TODO not need
            LOGGER.info("Reading families from file {}...", familyFile);
            families = IOUtils.loadFamiliesAsJson(familyFile);
        } else {*/
            LOGGER.info("Generating families from scratch...");
            Map<String, List<Name>> namesList = names.stream().filter(n -> plat.nonHack(n.getName())).collect(Collectors.groupingBy(Name::getCleanName));

            namesList.entrySet().stream().filter(e -> e.getValue().size() != 1)
                    .forEach(e -> families.put(e.getKey(), new Family(e.getValue())));

            LOGGER.info("Saving families...");
            IOUtils.createDirectories(workFamiliesPath());
            IOUtils.serializeFamiliesAsJson(familyFile, families);
        //}

        generateTribes();
    }

    public static void generateTribes() {

        tribes = new HashMap<>();

        families.values().forEach(f -> {
            List<Family> familyList = tribes.get(f.getTribe());
            if (familyList == null) {
                tribes.put(f.getTribe(), new ArrayList<>(Collections.singletonList(f)));
            } else {
                familyList.add(f);
            }
        });
    }

    public static void archiveToFamilies() {

        LOGGER.info("Reading list of games...");
        List<File> archives = IOUtils.listFiles(romsCollection.getRomsPath().toFile()).stream().filter(f -> f.getName().endsWith(".7z")).collect(Collectors.toList());

        LOGGER.info("Get file names from archives...");
        Map<File, List<String>> map = archives.stream().collect(Collectors.toMap(Function.identity(), ListFilesa::listFiles));

        File familyFile = fullFamiliesPath().toFile();

        /*if (familyFile.exists()) {
            LOGGER.info("Reading families from file {}...", familyFile);
            families = IOUtils.loadFamiliesAsJson(familyFile);
        } else {*/
            LOGGER.info("Generating families based on GoodMerged source...");
            Map<String, List<Name>> namesList = new HashMap<>();
            map.forEach((key, value) -> namesList.put(StringUtils.stripExtension(key.getName()), value.stream().map(v -> new Name(v, false)).collect(Collectors.toList())));

            families = namesList.entrySet().stream()/*.filter(e -> e.getValue().size() != 1)*/
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new Family(e.getKey(), e.getValue(), FamilyType.FAMILY)));

            LOGGER.info("Saving families...");
            IOUtils.createDirectories(workFamiliesPath());
            IOUtils.serializeFamiliesAsJson(familyFile, families);
        //}

        generateTribes();
    }

    private static void processFamilies() {

        calculateRelations();

        LOGGER.info("Saving families...");
        IOUtils.serializeFamiliesAsJson(fullFamiliesPath().toFile(), families);
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
                boolean allJakkard = family.getMembers().stream().anyMatch(m -> m.getJakkardStatus() > 0);
                double percent = (k + 1) * 100.0 / families.size();
                if ((family.getRelations().size() == relationsCount && allJakkard) || family.getMembers().size() == 0) { // x * (x - 1) / 2
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

    /*private static void doCalculateRelations(Family family, double percent, boolean log) {

        family.getRelations().clear();
        family.getMembers().forEach(m -> m.setJakkardStatus(0));
        Platform platform = platformsByCpu.get(ConfigHolder.platform);

        AtomicInteger counter = new AtomicInteger(-1);

        ExecutorService service = Executors.newFixedThreadPool(8);
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (AtomicInteger i = new AtomicInteger(0); i.get() < family.size() - 1; i.getAndIncrement()) {

            tasks.add(() -> {
                try {
                    int id = counter.getAndIncrement();
                    *//*if (needToStop[0]) {
                        LOGGER.info("Execution was interrupted!");
                        if (percent == -1) {
                            needToStop[0] = false;
                        }
                        break;
                    }*//*

                    Name name1 = family.get(id);

                    long[] s1Set = ShingleUtils.loadFromCache(cache, fullShinglesPath().resolve(bytesToHex(byTitle.get(name1.getName()).getSha1()) + ".shg"));

                    for (int j = id + 1; j < family.size(); j++) {

                        Name name2 = family.get(j);

                        long[] s2Set = ShingleUtils.loadFromCache(cache, fullShinglesPath().resolve(bytesToHex(byTitle.get(name2.getName()).getSha1()) + ".shg"));

                        double jakkard = doCalculateJakkard(s1Set, s2Set);

                        if (platform.isGood(name2.getName())) {
                            synchronized (family.get(id)) {
                                name1.addJakkardStatus(jakkard);
                            }
                        } else {
                            //LOGGER.info("{} is bad", name2.getName());
                            synchronized (family.get(id)) {
                                name1.addJakkardStatus(100);
                            }
                        }
                        synchronized (family.get(j)) {
                            name2.addJakkardStatus(jakkard);
                        }

                        Result result = new Result(name1, name2, jakkard);
                        if (log) {
                            LOGGER.info("{}->{}: {}|{}", id, j, result, percent == -1 ? (id + 2.0) * 100 / family.size() : percent);
                        }

                        synchronized (family) {
                            family.addRelation(result);
                        }
                    }
                    name1.setDone(true);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
                return 0;
            });
        }

        try {
            List<Future<Integer>> results = service.invokeAll(tasks);
            for (Future<Integer> r : results) {
                System.out.println(r.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        service.shutdown();
    }*/

    private static void doCalculateRelations(Family family, double percent, boolean log) {

        family.getRelations().clear();
        family.getMembers().forEach(m -> m.setJakkardStatus(0));
        Platform platform = platformsByCpu.get(ConfigHolder.platform);

        for (int i = 0; i < family.size() - 1; i++) {

            if (needToStop[0]) {
                LOGGER.info("Execution was interrupted!");
                if (percent == -1) {
                    needToStop[0] = false;
                }
                break;
            }

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
                    LOGGER.info("{}->{}: {}|{}", i, j, result, percent == -1 ? (i + 2.0) * 100 / family.size() : percent);
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

        AtomicInteger k = new AtomicInteger(0);
        long[] s1Set = ShingleUtils.loadFromCache(cache, fullShinglesPath().resolve(bytesToHex(byTitle.get(name).getSha1()) + ".shg"));

        return families.values().stream().filter(e -> !e.getName().equals(ignore)).filter(e -> e.getType() == FamilyType.FAMILY).collect(Collectors.toMap(Function.identity(), family -> {
            if (log) {
                LOGGER.info("Comparing: {} with {}|{}", name, family.getName(), (k.incrementAndGet()) * 100.0 / families.size());
            }
            //TODO here if no shingle file (or NPE) - we don't see this error at all!!!
            long[] s2Set = ShingleUtils.loadFromCache(cache, fullShinglesPath().resolve(bytesToHex(byTitle.get(family.getMother().getName()).getSha1()) + ".shg"));

            return doCalculateJakkard(s1Set, s2Set);
        })).entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(candidates)
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

        if (family.getName().equals("Public Domain") || family.getName().equals("Multicarts Collection")
                || family.getName().equals("Wxn Collection") || family.getName().equals("VT03 Collection") || family.getName().equals("Multi-Game Pirate Carts")) {
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

    @Measured
    private static void recalculateJakkard(Family family) {
        family.getMembers().forEach(m -> {
            List<Result> results = family.getRelations().stream().filter(r -> r.getName1().equals(m) || r.getName2().equals(m)).collect(Collectors.toList());
            m.setJakkardStatus(results.stream().mapToDouble(Result::getJakkard).sum());
        });
    }

    // a < b = ((b-a)/a) * 100
    static double deviation(double d1, double d2) {
        return Math.abs(d2 - d1) / d1 * 100;
    }

    private static String normalize(String s) {
        List<String> chunks = new ArrayList<>();

        StringUtils.toChunks(s).forEach(c -> {
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

    public static String getCleanName(String s) {
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

    public static int calculateIndex(String name) {

        int index = 100;

        if (name.contains("(U)")) {
            index += 100;
        }
        if (name.contains("(W)")) {
            index += 99;
        }
        if (name.contains("(E)")) {
            index += 80;
        }
        if (name.contains("(F)")) {
            index += 70;
        }
        if (name.contains("(G)")) {
            index += 70;
        }
        if (name.contains("(J)")) {
            index += 60;
        }
        if (name.contains("[!]")) {
            index += 10;
        }
        if (name.contains("+")) {
            index += 2;
        }

        if (name.contains("[b")) {
            index -= 50;
        }
        if (name.contains("(PD)")) {
            index -= 45;
        }
        if (name.contains("(Hack") || name.contains("Hack)")) {
            index -= 45;
        }
        if (name.contains("[o")) {
            index -= 30;
        }
        if (name.contains("[h")) {
            index -= 20;
        }
        if (name.contains("(Prototype)")) {
            index -= 15;
        }
        if (name.contains("(Prototype ")) {
            index -= 15;
        }
        if (name.contains("(Sample)")) {
            index -= 15;
        }
        if (name.contains("(Menu)")) {
            index -= 15;
        }
        if (name.contains("[t")) {
            index -= 10;
        }
        if (name.contains("[p")) {
            index -= 10;
        }
        if (name.contains("[f")) {
            index -= 10;
        }
        if (name.contains("[T")) {
            index -= 10;
        }
        if (name.contains("[a")) {
            index -= 5;
        }
        return index;
    }
}
