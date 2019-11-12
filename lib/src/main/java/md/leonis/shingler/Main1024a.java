package md.leonis.shingler;

import md.leonis.shingler.model.*;
import md.leonis.shingler.utils.MeasureMethodTest;
import md.leonis.shingler.utils.Measured;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.CRC32;

// NES (256 Kb) Up to 8 100% SAVE, Up to 32 SAFE, 64 relative SAFE, 256+ nonSAFE
// В любом случае, даже 1024 подходит для быстрой идентификации игры если она принадлежит группе
// Всё, что ниже 3-5 видимо можно относить к несовпадениям.

public class Main1024a {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main1024a.class);

    private static final int SHINGLE_LENGTH = 8;

    private static String GAMES_DIR = "D:\\Downloads\\games\\";
    private static String WORK_DIR = "D:\\Downloads\\games\\";

    private static final int SHINGLE_MAP_THRESHOLD = 127;

    static final List<Integer> SAMPLES = Arrays.asList(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024);

    private static final Map<Integer, File> SAMPLE_DIRS_MAP =
            SAMPLES.stream().collect(Collectors.toMap(Function.identity(), s -> new File(GAMES_DIR + "sample" + s)));

    private static final Map<Integer, Path> SAMPLE_DIRS_MAP2 =
            SAMPLES.stream().collect(Collectors.toMap(Function.identity(), s -> Paths.get(GAMES_DIR).resolve("sample" + s)));

    private static final Map<Integer, Map<String, long[]>> SHINGLE_MAP =
            SAMPLES.stream().collect(Collectors.toMap(Function.identity(), HashMap::new));

    static final Cache<File, long[]> cache = new Cache<>(0, 0, 1600);

    @SuppressWarnings("all")
    public static void main(String[] args) throws IOException {

        MeasureMethodTest.premain();

        final File folder = new File(GAMES_DIR);

        SAMPLE_DIRS_MAP.values().forEach(File::mkdirs);

        List<File> files = listFilesForFolder(folder);

        generateShingles(files);

        loadSamplesInCache(files);

        List<Name> names = files.stream().map(f -> new Name(f, false)).collect(Collectors.toList());

        //TODO revert 1 or 16, 1 is the better solution
        //generateFamilies(names, 64, 20); // generate families
    }

    public static void createDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            LOGGER.error("Can't create directory: {}", path.toString(), e);
        }
    }

    public static void createSampleDirs(Path path) {
        SAMPLES.stream().map(s -> path.resolve("sample" + s)).forEach(Main1024a::createDirectory);
    }

    @SuppressWarnings("all")
    private static void generateShingles(List<File> files) throws IOException {

        LOGGER.info("Generating shingles...");

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);

            File shdFile = new File(SAMPLE_DIRS_MAP.get(1).getAbsolutePath() + File.separator + file.getName() + ".shg");

            long[] shingles;

            if (shdFile.exists() && shdFile.length() > 0 && shdFile.length() % 8 == 0) {
                LOGGER.debug("Skipping: {}|{}", file.getName(), ((i + 1) * 100.0 / files.size()));
                shingles = loadShinglesFromFile(shdFile);
            } else {
                LOGGER.info("{}|{}", file.getName(), ((i + 1) * 100.0 / files.size()));
                shingles = toShingles(readFromFile(file));
                writeShinglesToFile(shingles, shdFile);
            }

            // Generate samples
            SAMPLE_DIRS_MAP.entrySet().forEach(e -> {
                int index = e.getKey();
                File sampleFile = new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + file.getName() + ".shg");
                if (index > 1 && (!sampleFile.exists() || sampleFile.length() == 0 || sampleFile.length() % 8 != 0)) {
                    long[] filteredShingles = filterArrays(shingles, index);

                    if (index > SHINGLE_MAP_THRESHOLD) {
                        SHINGLE_MAP.get(index).put(file.getName(), filteredShingles);
                    }
                    writeShinglesToFile(filteredShingles, sampleFile);
                }
            });
        }
    }

    public static void generateShinglesNio(RomsCollection collection, Path romsFolder, Path workDir) throws IOException {

        Map<String, GID> files = collection.getGids();
        LOGGER.info("Generating shingles...");

        int i = 0;
        for (Map.Entry<String, GID> entry : files.entrySet()) {

            long[] shingles = null;
            Path file = romsFolder.resolve(entry.getKey());
            Path shdFile = workDir.resolve("sample1").resolve(bytesToHex(entry.getValue().getSha1()) + ".shg");

            if (isCorrect(shdFile)) {
                LOGGER.debug("Skipping: {}|{}", file.toString(), ((i + 1) * 100.0 / files.size()));
            } else {
                LOGGER.info("{}|{}", file.toString(), ((i + 1) * 100.0 / files.size()));

                switch (collection.getType()) {
                    case PLAIN:
                        shingles = toShingles(readFromFile(file));
                        break;

                    case MERGED:
                        Path archiveFile = romsFolder.resolve(entry.getValue().getFamily());
                        shingles = toShingles(getFileFromArchive(archiveFile, entry.getKey()));
                        break;
                }

                writeShinglesToFile(shingles, shdFile);
            }

            // Generate samples
            for (Integer index : SAMPLES) {
                Path sampleFile = workDir.resolve("sample" + index).resolve(bytesToHex(entry.getValue().getSha1()) + ".shg");
                if (index > 1 && !isCorrect(sampleFile)) {
                    if (shingles == null) {
                        shingles = loadShinglesFromFile(shdFile);
                    }

                    // TODO actual some small files (PD) fail (empty) on 256 :(
                    long[] filteredShingles = filterArrays(shingles, index);

                    if (index > SHINGLE_MAP_THRESHOLD) {
                        SHINGLE_MAP.get(index).put(file.toString(), filteredShingles);
                    }
                    writeShinglesToFile(filteredShingles, sampleFile);
                }
            }

            i++;
        }
    }

    private static boolean isCorrect(Path path) {
        try {
            long size = Files.size(path);
            return Files.exists(path) && size > 0 && size % 8 == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("all")
    static void loadSamplesInCache(List<File> files) throws IOException {

        SHINGLE_MAP.forEach((key, value) -> {
            int index = key;
            if (index > 1) {

                File shinglesMapFile = new File("shinglesMap" + index);

                Map<String, long[]> shinglesMap = new HashMap<>();

                if (index > SHINGLE_MAP_THRESHOLD) {

                    if (shinglesMapFile.exists()) {
                        System.out.println(String.format("Getting sample%s from disk...", index));
                        shinglesMap = readShingleMapFromFile(shinglesMapFile);
                    } else {
                        System.out.println(String.format("Generating sample%s...", index));
                        for (int i = 0; i < files.size(); i++) {
                            File file = files.get(i);

                            File sampleFile = new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + file.getName() + ".shg");

                            if (!sampleFile.exists()) {

                                System.out.println(String.format(Locale.US, "%s: %2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));

                                File srcSampleFolder = SAMPLE_DIRS_MAP.get(index);

                                long[] shingles = loadShinglesFromCacheFile(new File(srcSampleFolder.getAbsolutePath() + File.separator + file.getName() + ".shg"));
                                long[] filteredShingles = filterArrays(shingles, index);

                                shinglesMap.put(file.getName(), filteredShingles);

                                if (!sampleFile.exists()) {
                                    writeShinglesToFile(filteredShingles, sampleFile);
                                }
                            } else {
                                System.out.println(String.format(Locale.US, "Reading: %s: %2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));
                                long[] filteredShingles = loadShinglesFromCacheFile(sampleFile);
                                shinglesMap.put(file.getName(), filteredShingles);
                            }
                        }
                        serialize(shinglesMapFile, shinglesMap);
                    }
                    SHINGLE_MAP.replace(index, shinglesMap);
                }
            }
        });
    }

    private static void generateFamilies(List<Name> names, int index, int jakkardIndex) {

        names = names.subList(500, 1500);

        File familyFile = new File("family" + index);

        Map<String, Family> families;
        if (familyFile.exists()) {
            System.out.println("\nReading families from file...");
            families = readFamiliesFromFile(familyFile);
        } else {
            System.out.println("\nGenerating families...");
            Map<String, List<Name>> namesList = names.stream().collect(Collectors.groupingBy(Name::getCleanName));

            families = namesList.entrySet().stream().filter(e -> e.getValue().size() != 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new Family(e.getValue())));

            calculateRelations(families, index, familyFile);

            serialize(familyFile, families);
        }

        int inFamily = families.values().stream().map(Family::size).mapToInt(Integer::intValue).sum();

        System.out.println("In family: " + inFamily);
        System.out.println("Not in family: " + (names.size() - inFamily));

        //TODO jakkardIndex
        drop(families, 30);

        System.out.println("In family: " + inFamily);
        System.out.println("Not in family: " + (names.size() - inFamily));

        //TODO merge jakkardIndex 20
        //TODO index 1 or 16. Better 1
        families = mergeFamilies(families, 30, 64);

        //TODO проходиться сиротами и смотреть куда пристроить.
    }

    private static Map<String, Family> mergeFamilies(Map<String, Family> families, double jakkardIndex, int index) {

        Map<Family, Family> mapping = new HashMap<>();

        List<Family> familyList = new ArrayList<>(families.values());

        int[] k = {0};
        for (int i = 0; i < familyList.size() - 1; i++) {
            Family family = familyList.get(i);

            if (family.isSkip()) {
                System.out.println(String.format("Skipping: %s... %2.2f%%", family.getName(), k[0] * 100.0 / families.size()));
                continue;
            }

            System.out.println(String.format("Comparing: %s... %2.2f%%", family.getName(), k[0] * 100.0 / families.size()));

            Name name1 = family.get(0);

            long[] s1Set;
            if (index > SHINGLE_MAP_THRESHOLD) {
                s1Set = SHINGLE_MAP.get(index).get(name1.getName());
            } else {
                s1Set = loadShinglesFromCacheFile(new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name1.getName() + ".shg"));
            }

            for (int j = i + 1; j < familyList.size(); j++) {

                Family family2 = familyList.get(j);
                Name name2 = family2.get(0);

                if (family2.isSkip()) {
                    continue;
                }

                long[] s2Set;
                if (index > SHINGLE_MAP_THRESHOLD) {
                    s2Set = SHINGLE_MAP.get(index).get(name2.getName());
                } else {
                    s2Set = loadShinglesFromCacheFile(new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name2.getName() + ".shg"));
                }

                long[] s1intersect = intersectArrays(s1Set, s2Set);
                long[] s1union = unionArrays(s1Set, s2Set);
                double relative = s1intersect.length * 100.0 / s1Set.length;
                double jakkard = s1intersect.length * 100.0 / s1union.length;

                Result result = new Result(name1, name2, relative, jakkard);
                //System.out.println(result);

                if (jakkard >= jakkardIndex) {
                    mapping.put(family2, family);
                }
            }
            k[0]++;
        }

        System.out.println(mapping);

        mapping.forEach((key, value) -> {
            value.getMembers().addAll(key.getMembers());
            familyList.remove(key);
        });

        Map<String, Family> families2 = familyList.stream().collect(Collectors.toMap(Family::getName, Function.identity()));

        calculateRelations(families2, 64, new File("family"));
        families2.values().forEach(f -> f.setName(f.getMother().getCleanName()));
        families2.values().forEach(Main1024a::recalculateJakkard);

        return families2;
    }

    static void calculateRelations(Map<String, Family> families, int index, File familyFile) {

        int[] k = {0};

        int[] save = {0};

        families.values().forEach(family -> {
            int relationsCount = family.getMembers().size() * (family.getMembers().size() - 1) / 2;
            if (family.getRelations().size() == relationsCount) { // x * (x - 1) / 2
                System.out.println(String.format("%nSkipping: %s... [%s] %2.3f%%", family.getName(), family.size(), (k[0] + 1) * 100.0 / families.size()));
                k[0]++;
            } else {

                System.out.println(String.format("%nComparing: %s... [%s] %2.3f%%", family.getName(), family.size(), (k[0] + 1) * 100.0 / families.size()));

                //family.getRelations().clear();

                for (int i = 0; i < family.size() - 1; i++) {

                    if (save[0] > 100000 * index) {
                        System.out.println("Saving family...");
                        serialize(familyFile, families);
                        save[0] = 0;
                    }

                    Name name1 = family.get(i);

                    if (family.hasAllRelations(name1, i)) {
                        System.out.println(String.format("Skipping all relations %s: %s", i, name1.getName()));
                        continue;
                    }

                    long[] s1Set;
                    // TODO use one cache, not two
                    if (index > SHINGLE_MAP_THRESHOLD) {
                        s1Set = SHINGLE_MAP.get(index).get(name1.getName());
                    } else {
                        s1Set = loadShinglesFromCacheFile(new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name1.getName() + ".shg"));
                    }

                    for (int j = i + 1; j < family.size(); j++) {

                        Name name2 = family.get(j);

                        if (family.containsRelation(name1, name2)) {
                            System.out.println(String.format("Skipping relation %s -> %s: %s -> %s", i, j, name1.getName(), name2.getName()));
                            continue;
                        }

                        if (name2.isDone()) {
                            continue;
                        }

                        long[] s2Set;
                        if (index > SHINGLE_MAP_THRESHOLD) {
                            s2Set = SHINGLE_MAP.get(index).get(name2.getName());
                        } else {
                            s2Set = loadShinglesFromCacheFile(new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name2.getName() + ".shg"));
                        }

                        long[] s1intersect = intersectArrays(s1Set, s2Set);
                        long[] s1union = unionArrays(s1Set, s2Set);
                        double relative = s1intersect.length * 100.0 / s1Set.length;
                        double jakkard = s1intersect.length * 100.0 / s1union.length;

                        name1.addRelativeStatus(relative);
                        name2.addRelativeStatus(relative);

                        name1.addJakkardStatus(jakkard);
                        name2.addJakkardStatus(jakkard);

                        Result result = new Result(name1, name2, relative, jakkard);
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
            m.setRelativeStatus(results.stream().mapToDouble(Result::getRelative).sum());
            m.setJakkardStatus(results.stream().mapToDouble(Result::getJakkard).sum());
        });
    }

    /*private static void compareAndCopy(List<Name> names, int index, int jakkardIndex) throws IOException {

        System.out.println("\nComparing...");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("result.csv", true)));

        names.forEach(name1 -> {
            List<Result> res = new ArrayList<>();

            int i = names.indexOf(name1);

            if (!name1.isDone()) {

                try {
                    List<Name> toCopy = new ArrayList<>();
                    toCopy.add(name1);

                    Set<Long> s1Set;
                    if (index > SHINGLE_MAP_THRESHOLD) {
                        s1Set = SHINGLE_MAP.get(index).get(name1.getName());
                    } else {
                        s1Set = readShdFromFile(new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name1.getName() + ".shg"));
                    }

                    if (i != names.size() - 1) {
                        for (int j = i + 1; j < names.size(); j++) {

                            Name name2 = names.get(j);

                            if (name2.isDone()) {
                                continue;
                            }

                            Set<Long> s2Set;
                            if (index > SHINGLE_MAP_THRESHOLD) {
                                s2Set = SHINGLE_MAP.get(index).get(name2.getName());
                            } else {
                                s2Set = readShdFromFile(new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name2.getName() + ".shg"));
                            }

                            Set<Long> s1intersect = new HashSet<>(s1Set);
                            Set<Long> s1union = new HashSet<>(s1Set);

                            s1intersect.retainAll(s2Set);
                            s1union.addAll(s2Set);
                            double relative = s1intersect.size() * 100.0 / s1Set.size();
                            double jakkard = s1intersect.size() * 100.0 / s1union.size();

                            Result result = new Result(name1, name2, relative, jakkard);

                            if (jakkard > jakkardIndex) { // 35
                                name2.setDone(true);
                                System.out.println(result);
                                toCopy.add(name2);
                            }

                            res.add(result);
                            out.println(result.toString());
                            out.flush();
                        }
                    }
                    name1.setDone(true);

                    String catName = getCleanTitle(toCopy);

                    File cat = Paths.get("D:\\Downloads\\games\\gata", catName).toFile();

                    if (cat.exists()) {
                        catName = getTitle(toCopy);
                        cat = Paths.get("D:\\Downloads\\games\\gata", catName).toFile();
                    }

                    cat.mkdir();
                    for (Name name : toCopy) {
                        Files.move(name.getFile().toPath(), Paths.get("D:\\Downloads\\games\\gata", catName, name.getFile().getName()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Skipping: " + name1.getName());
            }
        });

        out.close();
    }*/

    private static String getTitle(List<Name> names) {
        return names.stream().max(Comparator.comparing(Name::getIndex)).orElse(null).getName();
    }

    private static String getCleanTitle(List<Name> names) {
        return names.stream().max(Comparator.comparing(Name::getIndex)).orElse(null).getCleanName();
    }

    @Measured
    public static void serialize(File file, Object object) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //TODO unify
    @SuppressWarnings("unchecked")
    @Measured
    private static Map<String, long[]> readShingleMapFromFile(File file) {

        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, long[]> shinglesMap = (Map<String, long[]>) ois.readObject();

            ois.close();
            fis.close();
            return shinglesMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static long getFreeMemory() {
        long allocatedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
        return presumableFreeMemory / 1024 / 1024;
    }

    @SuppressWarnings("unchecked")
    @Measured
    static Map<String, Family> readFamiliesFromFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, Family> result = (Map<String, Family>) ois.readObject();

            ois.close();
            fis.close();
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Measured
    static Map<String, GID> readGIDsFromFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, GID> result = (Map<String, GID>) ois.readObject();

            ois.close();
            fis.close();
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Measured
    public static RomsCollection readCollectionFromFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            RomsCollection result = (RomsCollection) ois.readObject();

            ois.close();
            fis.close();
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //TODO unify
    @Measured
    private static long[] loadShinglesFromCacheFile(File file) {
        long[] cachedValue = cache.get(file);
        if (cachedValue != null) {
            return cachedValue;
        }

        long[] result = loadShinglesFromFile(file);
        if (getFreeMemory() < 250) {
            System.out.println(String.format("We have only %s Mb of RAM", getFreeMemory()));
            System.out.println("Cleaning Cache...");
            cache.cleanup();
            System.gc();
            System.out.println(String.format("Now we have %s Mb of RAM", getFreeMemory()));
        }
        cache.put(file, result);

        return result;
    }

    @Measured
    static long[] loadShinglesFromFile(File file) {
        int count = (int) file.length() / 8;
        long[] shingles = new long[count];
        try (FileChannel fc = FileChannel.open(file.toPath())) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            for (int i = 0; i < count; i++) {
                shingles[i] = mbb.getLong();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return shingles;
    }

    @Measured
    static long[] loadShinglesFromFile(Path file) {
        long size;
        try {
            size = Files.size(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int count = (int) size / 8;
        long[] shingles = new long[count];
        try (FileChannel fc = FileChannel.open(file)) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, size);
            for (int i = 0; i < count; i++) {
                shingles[i] = mbb.getLong();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return shingles;
    }

    @Measured
    static void writeShinglesToFile(long[] shingles, File file) {
        int count = shingles.length;
        try (FileChannel fc = FileChannel.open(file.toPath(), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, count * 8);
            for (long shingle : shingles) {
                mbb.putLong(shingle);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Measured
    static void writeShinglesToFile(long[] shingles, Path file) {
        int count = shingles.length;
        try (FileChannel fc = FileChannel.open(file, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, count * 8);
            for (long shingle : shingles) {
                mbb.putLong(shingle);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Measured
    private static long[] toShingles(byte[] bytes) {
        Set<Long> hashes = new HashSet<>();

        for (int i = 0; i < bytes.length - SHINGLE_LENGTH + 1; i++) {
            hashes.add(crc32(Arrays.copyOfRange(bytes, i, i + SHINGLE_LENGTH)));
        }

        return hashes.stream().sorted().mapToLong(l -> l).toArray();
    }

    public static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    public static byte[] md5(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha1(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Measured
    static long[] unionArrays(long[] arr1, long[] arr2) {
        long[] merge = new long[arr1.length + arr2.length];
        int i = 0, j = 0, k = 0;
        while (i < arr1.length && j < arr2.length) {
            if (arr1[i] < arr2[j]) {
                merge[k++] = arr1[i++];
            } else {
                merge[k++] = arr2[j++];
            }
        }
        while (i < arr1.length) {
            merge[k++] = arr1[i++];
        }
        while (j < arr2.length) {
            merge[k++] = arr2[j++];
        }
        return removeDuplicates(Arrays.copyOfRange(merge, 0, k));
    }

    @Measured
    static long[] intersectArrays(long[] arr1, long[] arr2) {
        long[] intersect = new long[Math.max(arr1.length, arr2.length)];
        int i = 0, j = 0, k = 0;
        while (i < arr1.length && j < arr2.length) {
            if (arr1[i] < arr2[j]) {
                i++;
            } else if (arr2[j] < arr1[i]) {
                j++;
            } else {
                intersect[k++] = arr1[i++];
            }
        }
        return Arrays.copyOfRange(intersect, 0, k);
    }

    /*@Measured
    static long[] intersectArrays0(long[] a, long[] b) {
        long[] c = new long[Math.max(a.length, b.length)];
        int k = 0;
        for (long n : b) {
            if (ArrayUtils.contains(a, n)) {
                c[k++] = n;
            }
        }

        return Arrays.copyOfRange(c, 0, k);
    }*/

    @Measured
    static long[] filterArrays(long[] a, int index) {
        long[] c = new long[a.length];
        int k = 0;
        for (long n : a) {
            if (n % index == 0) {
                c[k++] = n;
            }
        }
        return Arrays.copyOfRange(c, 0, k);
    }

    @Measured
    static long[] removeDuplicates(long[] arr) { // Only for sorted arrays
        if (arr.length < 2) {
            return arr;
        }
        int j = 0;
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] != arr[i + 1]) {
                arr[j++] = arr[i];
            }
        }
        arr[j++] = arr[arr.length - 1];
        return Arrays.copyOfRange(arr, 0, j);
    }

    @SuppressWarnings("all")
    @Measured
    public static List<File> listFilesForFolder(final File folder) {
        return Arrays.stream(folder.listFiles()).filter(File::isFile).collect(Collectors.toList());
    }

    @Measured
    public static List<Path> listFilesForFolder(final Path folder) {
        LOGGER.info("Getting a list of files...");
        List<Path> fileList = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileList.add(path);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Can't get list of files from dir: {}", folder.toString(), e);
        }
        return fileList;
    }

    @Measured
    private static byte[] readFromFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    @Measured
    private static byte[] readFromFile(Path file) throws IOException {
        return Files.readAllBytes(file);
    }

    @Measured
    public static Map<String, GID> filesToGid(List<Path> files) {

        LOGGER.info("Creating GIDs...");
        Map<String, GID> gidMap = new LinkedHashMap<>();

        try {
            for (Path path : files) {
                long size = Files.size(path);
                gidMap.put(path.getFileName().toString(), new GID(path.getFileName().toString(), size, null, null, null, null, null, null, null));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return gidMap;
    }

    @Measured
    public static Map<String, GID> mergedFilesToGid(List<Path> files) {

        LOGGER.info("Creating GIDs...");
        try {
            return files.stream().flatMap(Main1024a::listFiles).collect(Collectors.toMap(GID::getTitle, Function.identity()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<GID> listFiles(Path file) {
        try (SevenZFile archiveFile = new SevenZFile(file.toFile())) {
            return StreamSupport.stream(archiveFile.getEntries().spliterator(), false)
                    .map(e -> new GID(e.getName(), e.getSize(), e.getCrcValue(), null, null, null, null, null, file.getFileName().toString()));
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public static Map<String, GID> calculateHashes(List<Path> files) {

        LOGGER.info("Generating hashes...");
        Map<String, GID> gidMap = new LinkedHashMap<>();

        try {
            int i = 0;
            for (Path path : files) {
                LOGGER.info("Calculating hash sums for: {}|{}", path.getFileName(), ((i + 1) * 100.0 / files.size()));
                byte[] bytes = readFromFile(path);
                byte[] byteswh = Arrays.copyOfRange(bytes, 16, bytes.length);
                gidMap.put(path.getFileName().toString(), new GID(path.getFileName().toString(), Files.size(path), crc32(bytes), md5(bytes), sha1(bytes), crc32(byteswh), md5(byteswh), sha1(byteswh), null));

                i++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return gidMap;
    }

    public static Map<String, GID> calculateMergedHashes(List<Path> files) {

        LOGGER.info("Generating hashes...");
        Map<String, GID> gidMap = new LinkedHashMap<>();

        try {
            int i = 0;
            for (Path path : files) {
                LOGGER.info("Calculating hash sums for: {}|{}", path.getFileName(), ((i + 1) * 100.0 / files.size()));
                List<GID> gids = listFiles2(path);
                gids.forEach(gid -> gidMap.put(gid.getTitle(), gid));
                i++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return gidMap;
    }

    @Measured
    public static byte[] getFileFromArchive(Path file, String fileName) {

        try (SevenZFile sevenZFile = new SevenZFile(file.toFile())) {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            while (entry != null) {
                if (entry.getName().equals(fileName)) {
                    byte[] content = new byte[(int) entry.getSize()];
                    sevenZFile.read(content, 0, content.length);
                    return content;
                }
                entry = sevenZFile.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException(String.format("File %s not found in %s archive", fileName, file));
    }

    public static List<GID> listFiles2(Path file) {
        List<GID> result = new ArrayList<>();

        try (SevenZFile sevenZFile = new SevenZFile(file.toFile())) {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            while (entry != null) {
                byte[] content = new byte[(int) entry.getSize()];
                byte[] contentwh = Arrays.copyOfRange(content, 16, content.length);
                sevenZFile.read(content, 0, content.length);
                result.add(new GID(entry.getName(), entry.getSize(), entry.getCrcValue(), md5(content), sha1(content), crc32(contentwh), md5(contentwh), sha1(contentwh), file.getFileName().toString()));
                entry = sevenZFile.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
