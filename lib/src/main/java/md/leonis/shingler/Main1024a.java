package md.leonis.shingler;

import md.leonis.shingler.model.*;
import md.leonis.shingler.utils.IOUtils;
import md.leonis.shingler.utils.MeasureMethodTest;
import md.leonis.shingler.utils.Measured;
import md.leonis.shingler.utils.ShingleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static md.leonis.shingler.Main1024.serialize;
import static md.leonis.shingler.model.ConfigHolder.*;
import static md.leonis.shingler.utils.BinaryUtils.*;

// NES (256 Kb) Up to 8 100% SAVE, Up to 32 SAFE, 64 relative SAFE, 256+ nonSAFE
// В любом случае, даже 1024 подходит для быстрой идентификации игры если она принадлежит группе
// Всё, что ниже 3-5 видимо можно относить к несовпадениям.

public class Main1024a {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main1024a.class);

    private static String GAMES_DIR = "D:\\Downloads\\games\\";

    private static final int SHINGLE_MAP_THRESHOLD = 127;

    private static final List<Integer> SAMPLES = Arrays.asList(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024);

    private static final Map<Integer, File> SAMPLE_DIRS_MAP =
            SAMPLES.stream().collect(Collectors.toMap(Function.identity(), s -> new File(GAMES_DIR + "sample-" + s)));

    private static final Map<Integer, Map<String, int[]>> SHINGLE_MAP =
            SAMPLES.stream().collect(Collectors.toMap(Function.identity(), HashMap::new));

    static final Cache<String, int[]> CACHE = new Cache<>(0, 0, 2400);

    @SuppressWarnings("all")
    public static void main(String[] args) throws IOException {

        MeasureMethodTest.premain();

        final File folder = new File(GAMES_DIR);

        SAMPLE_DIRS_MAP.values().forEach(File::mkdirs);

        List<File> files = IOUtils.listFiles(folder);

        generateShingles(files);

        loadSamplesInCache(files);

        List<Name> names = files.stream().map(f -> new Name(f.getName(), false)).collect(Collectors.toList());

        //TODO revert 1 or 16, 1 is the better solution
        //generateFamilies(names, 64, 20); // generate families
    }

    public static void createSampleDirs(Path path) {
        SAMPLES.stream().map(s -> path.resolve("sample-" + s)).forEach(IOUtils::createDirectories);
    }

    @SuppressWarnings("all")
    private static void generateShingles(List<File> files) throws IOException {

        LOGGER.info("Generating shingles...");

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);

            File shdFile = new File(SAMPLE_DIRS_MAP.get(1).getAbsolutePath() + File.separator + file.getName() + ".shg");

            int[] shingles;

            if (isCorrect(shdFile.toPath())) {
                LOGGER.debug("Skipping: {}|{}", file.getName(), ((i + 1) * 100.0 / files.size()));
                shingles = ShingleUtils.load(shdFile);
            } else {
                LOGGER.info("{}|{}", file.getName(), ((i + 1) * 100.0 / files.size()));
                shingles = ShingleUtils.toShingles(IOUtils.loadBytes(file));
                ShingleUtils.save(shingles, shdFile);
            }

            // Generate samples
            SAMPLE_DIRS_MAP.entrySet().forEach(e -> {
                int index = e.getKey();
                File sampleFile = new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + file.getName() + ".shg");
                if (index > 1 && isNotCorrect(sampleFile.toPath())) {
                    int[] filteredShingles = filterArrays(shingles, index);

                    if (index > SHINGLE_MAP_THRESHOLD) {
                        SHINGLE_MAP.get(index).put(file.getName(), filteredShingles);
                    }
                    ShingleUtils.save(filteredShingles, sampleFile);
                }
            });
        }
    }

    /*public static void generateShinglesNio(RomsCollection collection, Path romsFolder, Path workDir) throws IOException {

        Set<Integer> restrictedShingles = new HashSet<>(platformsByCpu.get(platform).getRestrictedShingles());
        Map<String, GID> files = collection.getGidsMap();
        LOGGER.info("Generating shingles...");

        int i = 0;
        for (Map.Entry<String, GID> entry : files.entrySet()) {

            int[] shingles = null;
            Path file = romsFolder.resolve(entry.getKey());
            Path shdFile = workDir.resolve("sample-1").resolve(bytesToHex(entry.getValue().getSha1()) + ".shg");

            if (restrictedShingles.contains(1) || isCorrect(shdFile)) {
                LOGGER.debug("Skipping: {}|{}", file.toString(), ((i + 1) * 100.0 / files.size()));
            } else {
                LOGGER.info("{}|{}", file.toString(), ((i + 1) * 100.0 / files.size()));

                shingles = generateShingle(collection, romsFolder, file, entry);

                if (!restrictedShingles.contains(1)) {
                    ShingleUtils.save(shingles, shdFile);
                }
            }

            // Generate samples
            for (Integer index : SAMPLES) {
                Path sampleFile = workDir.resolve("sample-" + index).resolve(bytesToHex(entry.getValue().getSha1()) + ".shg");
                if (index > 1 && !restrictedShingles.contains(index) && !isCorrect(sampleFile)) {
                    if (shingles == null) {
                        if (Files.exists(shdFile)) {
                            shingles = ShingleUtils.load(shdFile);
                        } else {
                            shingles = generateShingle(collection, romsFolder, file, entry);
                        }
                    }

                    // TODO actual some small files (PD) fail (empty) on 256 :(
                    int[] filteredShingles = filterArrays(shingles, index);

                    if (index > SHINGLE_MAP_THRESHOLD) {
                        SHINGLE_MAP.get(index).put(file.toString(), filteredShingles);
                    }
                    ShingleUtils.save(filteredShingles, sampleFile);
                }
            }

            i++;
        }
    }*/

    public static void generateShinglesNioParallel(RomsCollection collection, Path romsFolder, Path workDir) {

        Set<Integer> restrictedShingles = new HashSet<>(platformsByCpu.get(platform).getRestrictedShingles());
        Map<String, GID> files = collection.getGidsMap();
        LOGGER.info("Generating shingles...");

        AtomicInteger counter = new AtomicInteger(-1);

        ExecutorService service = Executors.newFixedThreadPool(8);
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (Map.Entry<String, GID> entry : files.entrySet()) {

            tasks.add(() -> {
                try {
                    int i = counter.getAndIncrement();

                    int[] shingles = null;
                    Path file = romsFolder.resolve(entry.getKey());
                    Path shdFile = workDir.resolve("sample-1").resolve(bytesToHex(entry.getValue().getSha1()) + ".shg");

                    if (restrictedShingles.contains(1) || isCorrect(shdFile)) {
                        LOGGER.debug("Skipping: {}|{}", file.toString(), ((i + 1) * 100.0 / files.size()));
                    } else {
                        LOGGER.info("{}|{}", file.toString(), ((i + 1) * 100.0 / files.size()));

                        shingles = generateShingle(collection, romsFolder, file, entry);

                        if (!restrictedShingles.contains(1)) {
                            ShingleUtils.save(shingles, shdFile);
                        }
                    }

                    // Generate samples
                    for (Integer index : SAMPLES) {
                        Path sampleFile = workDir.resolve("sample-" + index).resolve(bytesToHex(entry.getValue().getSha1()) + ".shg");
                        if (index > 1 && !restrictedShingles.contains(index) && !isCorrect(sampleFile)) {
                            if (shingles == null) {
                                if (Files.exists(shdFile)) {
                                    shingles = ShingleUtils.load(shdFile);
                                } else {
                                    shingles = generateShingle(collection, romsFolder, file, entry);
                                }
                            }

                            // TODO actual some small files (PD) fail (empty) on 256 :(
                            int[] filteredShingles = filterArrays(shingles, index);

                            if (index > SHINGLE_MAP_THRESHOLD) {
                                SHINGLE_MAP.get(index).put(file.toString(), filteredShingles);
                            }
                            ShingleUtils.save(filteredShingles, sampleFile);
                        }
                    }
                    return 0;
                } catch (Throwable t) {
                    LOGGER.error(entry.getKey());
                    throw new RuntimeException(t);
                }
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
    }

    private static int[] generateShingle(RomsCollection collection, Path romsFolder, Path file, Map.Entry<String, GID> entry) throws IOException {
        switch (collection.getType()) {
            case PLAIN:
                return ShingleUtils.toShingles(IOUtils.loadBytes(file));

            case MERGED:
                Path archiveFile = romsFolder.resolve(entry.getValue().getFamily());
                return ShingleUtils.toShingles(IOUtils.loadBytesFromArchive(archiveFile, entry.getKey()));
        }
        return null;
    }

    private static boolean isCorrect(Path path) {
        try {
            long size = Files.size(path);
            return Files.exists(path) && size > 0 && size % 4 == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isNotCorrect(Path path) {
        try {
            long size = Files.size(path);
            return Files.notExists(path) || size == 0 || size % 4 != 0;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("all")
    static void loadSamplesInCache(List<File> files) throws IOException {

        SHINGLE_MAP.forEach((key, value) -> {
            int index = key;
            if (index > SHINGLE_MAP_THRESHOLD) {

                File shinglesMapFile = new File("shinglesMap" + index);
                Map<String, int[]> shinglesMap = new HashMap<>();

                if (shinglesMapFile.exists()) {
                    LOGGER.info(String.format("Getting sample%s from disk...", index));
                    shinglesMap = ShingleUtils.loadMap(shinglesMapFile);
                } else {
                    LOGGER.info(String.format("Generating sample%s...", index));
                    for (int i = 0; i < files.size(); i++) {
                        File file = files.get(i);

                        File sampleFile = new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + file.getName() + ".shg");

                        if (!sampleFile.exists()) {

                            LOGGER.info(String.format(Locale.US, "%s: %2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));

                            File srcSampleFolder = SAMPLE_DIRS_MAP.get(index);

                            int[] shingles = ShingleUtils.loadFromCache(CACHE, new File(srcSampleFolder.getAbsolutePath() + File.separator + file.getName() + ".shg"));
                            int[] filteredShingles = filterArrays(shingles, index);

                            shinglesMap.put(file.getName(), filteredShingles);

                            if (!sampleFile.exists()) {
                                ShingleUtils.save(filteredShingles, sampleFile);
                            }
                        } else {
                            LOGGER.info(String.format(Locale.US, "Reading: %s|%2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));
                            int[] filteredShingles = ShingleUtils.loadFromCache(CACHE, sampleFile);
                            shinglesMap.put(file.getName(), filteredShingles);
                        }
                    }
                    serialize(shinglesMapFile, shinglesMap);
                }
                SHINGLE_MAP.replace(index, shinglesMap);
            }
        });
    }

    private static void generateFamilies(List<Name> names, int index, int jakkardIndex) {

        names = names.subList(500, 1500);

        File familyFile = new File("family" + index);

        Map<String, Family> families;
        if (familyFile.exists()) {
            LOGGER.info("\nReading families from file...");
            families = IOUtils.loadFamiliesAsJson(familyFile);
        } else {
            LOGGER.info("\nGenerating families...");
            Map<String, List<Name>> namesList = names.stream().collect(Collectors.groupingBy(Name::getCleanName));

            families = namesList.entrySet().stream().filter(e -> e.getValue().size() != 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new Family(e.getValue())));

            calculateRelations(families, index);

            IOUtils.serializeFamiliesAsJson(familyFile, families);
        }

        int inFamily = families.values().stream().map(Family::size).mapToInt(Integer::intValue).sum();

        LOGGER.info("In family: " + inFamily);
        LOGGER.info("Not in family: " + (names.size() - inFamily));

        //TODO jakkardIndex
        drop(families, 30);

        LOGGER.info("In family: " + inFamily);
        LOGGER.info("Not in family: " + (names.size() - inFamily));

        //TODO merge jakkardIndex 20
        //TODO index 1 or 16. Better 1
        families = mergeFamilies(families, 30, 64);

        //TODO проходиться сиротами и смотреть куда пристроить.
    }

    private static void calculateRelations(Map<String, Family> families, int index) {

        AtomicInteger k = new AtomicInteger(0);

        for (Family family : families.values()) {

            if (needToStop[0]) {
                LOGGER.info("Saving families...");
                IOUtils.serializeFamiliesAsJson(fullFamiliesPath().toFile(), families);
                needToStop[0] = false;
                break;
            }

            int relationsCount = family.getMembers().size() * (family.getMembers().size() - 1) / 2;
            if (family.getRelations().size() == relationsCount) { // x * (x - 1) / 2
                LOGGER.debug(String.format("%nSkipping: %s... [%s]|%2.3f%%", family.getName(), family.size(), (k.incrementAndGet()) * 100.0 / families.size()));
            } else {

                LOGGER.info(String.format("%nComparing: %s... [%s]|%2.3f%%", family.getName(), family.size(), (k.incrementAndGet()) * 100.0 / families.size()));

                //family.getRelations().clear();

                for (int i = 0; i < family.size() - 1; i++) {

                    Name name1 = family.get(i);

                    if (family.hasAllRelations(name1, i)) {
                        LOGGER.debug(String.format("Skipping all relations %s: %s", i, name1.getName()));
                        continue;
                    }

                    int[] s1Set = ShingleUtils.loadFromCache(CACHE, new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name1.getName() + ".shg"));

                    for (int j = i + 1; j < family.size(); j++) {

                        Name name2 = family.get(j);

                        if (family.containsRelation(name1, name2)) {
                            LOGGER.debug(String.format("Skipping relation %s -> %s: %s -> %s", i, j, name1.getName(), name2.getName()));
                            continue;
                        }

                        if (name2.isDone()) {
                            continue;
                        }

                        int[] s2Set = ShingleUtils.loadFromCache(CACHE, new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name2.getName() + ".shg"));

                        int[] s1intersect = intersectArrays(s1Set, s2Set);
                        int[] s1union = unionArrays(s1Set, s2Set);
                        double jakkard = s1intersect.length * 100.0 / s1union.length;

                        name1.addJakkardStatus(jakkard);
                        name2.addJakkardStatus(jakkard);

                        Result result = new Result(name1, name2, jakkard);
                        LOGGER.info(i + "->" + j + ": " + result);

                        family.addRelation(result);
                    }
                    name1.setDone(true);
                }
            }
            family.selectMother();
        }
    }

    private static Map<String, Family> mergeFamilies(Map<String, Family> families, double jakkardIndex, int index) {

        Map<Family, Family> mapping = new HashMap<>();

        List<Family> familyList = new ArrayList<>(families.values());

        AtomicInteger k = new AtomicInteger(0);
        for (int i = 0; i < familyList.size() - 1; i++) {
            Family family = familyList.get(i);

            if (family.isSkip()) {
                LOGGER.debug(String.format("Skipping: %s...|%2.2f%%", family.getName(), k.get() * 100.0 / families.size()));
                continue;
            }

            LOGGER.info(String.format("Comparing: %s...|%2.2f%%", family.getName(), k.get() * 100.0 / families.size()));

            Name name1 = family.get(0);

            int[] s1Set;
            if (index > SHINGLE_MAP_THRESHOLD) {
                s1Set = SHINGLE_MAP.get(index).get(name1.getName());
            } else {
                s1Set = ShingleUtils.loadFromCache(CACHE, new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name1.getName() + ".shg"));
            }

            for (int j = i + 1; j < familyList.size(); j++) {

                Family family2 = familyList.get(j);
                Name name2 = family2.get(0);

                if (family2.isSkip()) {
                    continue;
                }

                int[] s2Set;
                if (index > SHINGLE_MAP_THRESHOLD) {
                    s2Set = SHINGLE_MAP.get(index).get(name2.getName());
                } else {
                    s2Set = ShingleUtils.loadFromCache(CACHE, new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name2.getName() + ".shg"));
                }

                int[] s1intersect = intersectArrays(s1Set, s2Set);
                int[] s1union = unionArrays(s1Set, s2Set);
                double jakkard = s1intersect.length * 100.0 / s1union.length;

                Result result = new Result(name1, name2, jakkard);
                //LOGGER.info(result);

                if (jakkard >= jakkardIndex) {
                    mapping.put(family2, family);
                }
            }
            k.incrementAndGet();
        }

        LOGGER.info(mapping.toString());

        mapping.forEach((key, value) -> {
            value.getMembers().addAll(key.getMembers());
            familyList.remove(key);
        });

        Map<String, Family> families2 = familyList.stream().collect(Collectors.toMap(Family::getName, Function.identity()));

        calculateRelations(families2, 64);
        families2.values().forEach(f -> f.setName(f.getMother().getCleanName()));
        families2.values().forEach(Main1024a::recalculateJakkard);

        return families2;
    }

    static void drop(Map<String, Family> families, double jakkardIndex) {

        families.values().forEach(family -> {
            List<Name> toDelete = getNonSiblings(family, jakkardIndex);

            toDelete.forEach(td -> LOGGER.info(String.format("Dropping: %s|%2.4f%%", td.getName(), td.getJakkardStatus() / family.size())));

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

        //TODO remove this
        if (family.getName().equals("Public Domain") || family.getName().equals("Multicarts Collection")
                || family.getName().equals("Wxn Collection") || family.getName().equals("VT03 Collection")) {
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

    private static String getTitle(List<Name> names) {
        return names.stream().max(Comparator.comparing(Name::getIndex)).orElse(null).getName();
    }

    private static String getCleanTitle(List<Name> names) {
        return names.stream().max(Comparator.comparing(Name::getIndex)).orElse(null).getCleanName();
    }

    @Measured
    public static Map<String, GID> GIDsFromFiles(List<Path> files) {

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
    public static Map<String, GID> GIDsFromMergedFile(List<Path> files) {

        LOGGER.info("Creating GIDs...");
        try {
            return files.stream().flatMap(IOUtils::loadGIDsFromArchive).collect(Collectors.toMap(GID::getTitle, Function.identity()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, GID> calculateHashes(List<Path> files) {

        LOGGER.info("Generating hashes...");
        Map<String, GID> gidMap = new LinkedHashMap<>();

        try {
            int i = 1;
            for (Path path : files) {
                LOGGER.info("Calculating hash sums for: {}|{}", path.getFileName(), (i * 100.0 / files.size()));
                byte[] bytes = IOUtils.loadBytes(path);
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
                List<GID> gids = IOUtils.loadHashedGIDsFromArchive(path);
                gids.forEach(gid -> gidMap.put(gid.getTitle(), gid));
                i++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return gidMap;
    }
}
