package md.leonis.shingler;

import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.zip.CRC32;

//TODO list[], write direct, read direct
//TODO fast intersect/union operations. TreeMap?
public class Main1024a {

    private static final int SHINGLE_LENGTH = 8;

    private static final String GAMES_DIR = "D:\\Downloads\\games\\";

    private static final int SHINGLE_MAP_THRESHOLD = 128;

    private static final List<Integer> SAMPLES = Arrays.asList(1, 16, 64, 256, 1024);

    private static final Map<Integer, File> SAMPLE_DIRS_MAP =
            SAMPLES.stream().collect(Collectors.toMap(Function.identity(), s -> new File(GAMES_DIR + "ssample" + s)));

    private static final Map<Integer, Map<String, long[]>> SHINGLE_MAP =
            SAMPLES.stream().collect(Collectors.toMap(Function.identity(), HashMap::new));

    private static final Cache<File, long[]> cache = new Cache<>(0, 0, 1600);
    //TODO investigate
    private static final SimpleCache<long[]> simpleCache = new SimpleCache<>();

    @SuppressWarnings("all")
    public static void main(String[] args) throws IOException {
        final File folder = new File(GAMES_DIR);

        SAMPLE_DIRS_MAP.values().forEach(File::mkdirs);

        List<File> files = listFilesForFolder(folder);

        generateShingles(files);

        //TODO revert
        //getSamples(files);

        List<Name> names = files.stream().map(f -> new Name(f, false)).collect(Collectors.toList());

        //TODO revert 1 or 16, 1 is the better solution
        //generateFamilies(names, 64, 20); // generate families
    }

    @SuppressWarnings("all")
    private static void generateShingles(List<File> files) throws IOException {

        System.out.println("Generating shingles...");

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);

            File shdFile = new File(SAMPLE_DIRS_MAP.get(1).getAbsolutePath() + File.separator + file.getName() + ".shg");

            if (shdFile.exists() && shdFile.length() > 0) {
                System.out.println(String.format(Locale.US, "Skipping: %s: %2.2f", file.getName(), ((i + 1) * 100.0 / files.size())));
                continue;
            }

            long[] shingles = toShingles(readFromFile(file));

            System.out.println(String.format(Locale.US, "%s: %2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));
            serialize(shdFile, shingles);

            SAMPLE_DIRS_MAP.entrySet().forEach(e -> {
                int index = e.getKey();
                long[] filteredShingles = filterArrays(shingles, index);

                if (index > SHINGLE_MAP_THRESHOLD) {
                    SHINGLE_MAP.get(index).put(file.getName(), filteredShingles);
                }

                File sampleFile = new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + file.getName() + ".shg");
                if (!sampleFile.exists()) {
                    serialize(sampleFile, filteredShingles);
                }
            });
        }
    }

    @SuppressWarnings("all")
    private static void getSamples(List<File> files) throws IOException {

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

                                File srcSampleFolder = SAMPLE_DIRS_MAP.get(index - 1);

                                long[] shingles = readShdFromFile(new File(srcSampleFolder.getAbsolutePath() + File.separator + file.getName() + ".shg"));
                                long[] filteredShingles = filterArrays(shingles, index);

                                shinglesMap.put(file.getName(), filteredShingles);

                                if (!sampleFile.exists()) {
                                    serialize(sampleFile, filteredShingles);
                                }
                            } else {
                                System.out.println(String.format(Locale.US, "Skipping: %s: %2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));
                                long[] filteredShingles = readShdFromFile(sampleFile);
                                shinglesMap.put(file.getName(), filteredShingles);
                            }
                        }
                        serialize(shinglesMapFile, shinglesMap);
                        SHINGLE_MAP.replace(index, shinglesMap);
                    }
                }
            }
        });
    }

    private static void generateFamilies(List<Name> names, int index, int jakkardIndex) {

        names = names.subList(500, 1500);

        File familyFile = new File("family");

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
                s1Set = readShdFromFile(new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name1.getName() + ".shg"));
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
                    s2Set = readShdFromFile(new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name2.getName() + ".shg"));
                }

                long[] s1intersect = intersectArrays(s1Set, s2Set);
                long[] s1union = mergeArrays(s1Set, s2Set);
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
                System.out.println(String.format("%nSkipping: %s... [%s] %2.3f", family.getName(), family.size(), k[0] * 100.0 / families.size()));
                k[0]++;
            } else {

                System.out.println(String.format("%nComparing: %s... [%s] %2.3f", family.getName(), family.size(), k[0] * 100.0 / families.size()));

                //family.getRelations().clear();

                for (int i = 0; i < family.size() - 1; i++) {

                    if (save[0] > 100) {
                        System.out.println("Saving family...");
                        serialize(familyFile, families);
                        save[0] = 0;
                    }

                    Name name1 = family.get(i);

                    long[] s1Set;
                    if (index > SHINGLE_MAP_THRESHOLD) {
                        s1Set = SHINGLE_MAP.get(index).get(name1.getName());
                    } else {
                        s1Set = readShdFromFile(new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name1.getName() + ".shg"));
                    }

                    for (int j = i + 1; j < family.size(); j++) {

                        Name name2 = family.get(j);

                        if (family.containsRelation(name1, name2)) {
                            continue;
                        }

                        if (name2.isDone()) {
                            continue;
                        }

                        long[] s2Set;
                        if (index > SHINGLE_MAP_THRESHOLD) {
                            s2Set = SHINGLE_MAP.get(index).get(name2.getName());
                        } else {
                            s2Set = readShdFromFile(new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + name2.getName() + ".shg"));
                        }

                        long[] s1intersect = intersectArrays(s1Set, s2Set);
                        long[] s1union = mergeArrays(s1Set, s2Set);
                        double relative = s1intersect.length * 100.0 / s1Set.length;
                        double jakkard = s1intersect.length * 100.0 / s1union.length;

                        name1.addRelativeStatus(relative);
                        name2.addRelativeStatus(relative);

                        name1.addJakkardStatus(jakkard);
                        name2.addJakkardStatus(jakkard);

                        Result result = new Result(name1, name2, relative, jakkard);
                        System.out.println(i + "->" + j + ": " + result);

                        family.addRelation(result);
                    }
                    name1.setDone(true);
                    save[0]++;
                }
                family.setMother(family.getMembers().stream().max(Comparator.comparing(Name::getJakkardStatus)).orElse(null));
                k[0]++;
            }
        });
    }

    private static List<Name> deleteNonSiblings(Family fam, double jakkardIndex) {
        Family family = new Family(fam);

        List<Name> deleted = new ArrayList<>();

        double status = family.getJakkardStatus(family.size() - 1);

        while (status < jakkardIndex && family.size() > 1) {
            Name name = family.get(family.size() - 1);
            deleted.add(name);
            family.getMembers().remove(family.size() - 1);
            // recalculate jakkard
            family.setRelations(family.getRelations().stream().filter(r -> !r.getName1().equals(name) && !r.getName2().equals(name)).collect(Collectors.toList()));

            recalculateJakkard(family);

            status = family.getJakkardStatus(family.size() - 1);
        }

        /*if (!deleted.isEmpty()) {
            toDelete.addAll(deleted);
        }*/
        return deleted;
    }

    static void drop(Map<String, Family> families, double jakkardIndex) {

        families.values().forEach(family -> {
            List<Name> toDelete = deleteNonSiblings(family, jakkardIndex);

            toDelete.forEach(td -> System.out.println(String.format("Dropping: %s (%2.4f)", td.getName(), td.jakkardStatus / family.size())));
            family.setRelations(family.getRelations().stream().filter(r -> toDelete.contains(r.getName1()) || toDelete.contains(r.getName2())).collect(Collectors.toList()));

            family.getMembers().removeAll(toDelete);

            recalculateJakkard(family);
        });
    }

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

    static void serialize(File file, Object object) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //TODO unify
    @SuppressWarnings("unchecked")
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

    //TODO unify
    private static long[] readShdFromFile(File file) {
        long[] cachedValue = cache.get(file);
        if (cachedValue != null) {
            return cachedValue;
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            long[] result = (long[]) ois.readObject();
            if (getFreeMemory() < 250) {
                System.out.println(String.format("We have only %s Mb of RAM", getFreeMemory()));
                System.out.println("Cleaning Cache...");
                cache.cleanup();
                System.gc();
                System.out.println(String.format("Now we have %s Mb of RAM", getFreeMemory()));
            }
            cache.put(file, result);

            ois.close();
            fis.close();
            return result;
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

    private static long[] toShingles(byte[] bytes) {
        Set<Long> hashes = new HashSet<>();

        for (int i = 0; i < bytes.length - SHINGLE_LENGTH + 1; i++) {
            CRC32 crc = new CRC32();
            crc.update(Arrays.copyOfRange(bytes, i, i + SHINGLE_LENGTH));
            hashes.add(crc.getValue());
        }

        return hashes.stream().sorted().mapToLong(l -> l).toArray();
    }

    //TODO use mergeArrays
    /*static long[] unionArrays(long[] a, long[] b) {
        long[] c = new long[a.length + b.length];
        int k = 0;
        for (long n : a) {
            c[k++] = n;
        }
        for (long n : b) {
            c[k++] = n;
        }
        Arrays.sort(c);
        return removeDuplicates(c);
    }*/

    static long[] mergeArrays(long arr1[], long arr2[]) {
        long[] merge = new long[arr1.length + arr2.length];
        int i = 0, j = 0, k = 0;
        while (i < arr1.length && j < arr2.length) {
            if (arr1[i] < arr2[j]) {
                merge[k++] = arr1[i++];
            } else {
                merge[k++] = arr2[j++];
            }
        }
        while (i < arr1.length)
            merge[k++] = arr1[i++];
        while (j < arr2.length)
            merge[k++] = arr2[j++];
        return ArrayUtils.subarray(removeDuplicates(merge), 0, k);
    }

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
        return ArrayUtils.subarray(intersect, 0, k);
    }

    /*static long[] intersectArrays0(long[] a, long[] b) {
        long[] c = new long[Math.max(a.length, b.length)];
        int k = 0;
        for (long n : b) {
            if (ArrayUtils.contains(a, n)) {
                c[k++] = n;
            }
        }

        return ArrayUtils.subarray(c, 0, k);
    }*/

    static long[] filterArrays(long[] a, int index) {
        long[] c = new long[a.length];
        int k = 0;
        for (long n : a) {
            if (n % index == 0) {
                c[k++] = n;
            }
        }
        return ArrayUtils.subarray(c, 0, k);
    }

    static long[] removeDuplicates(long[] arr) { // Only for sorted arrays
        int j = 0;
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] != arr[i + 1]) {
                arr[j++] = arr[i];
            }
        }
        arr[j++] = arr[arr.length - 1];
        return ArrayUtils.subarray(arr, 0, j);
    }

    @SuppressWarnings("all")
    private static List<File> listFilesForFolder(final File folder) {
        return Arrays.stream(folder.listFiles()).filter(File::isFile).collect(Collectors.toList());
    }

    private static byte[] readFromFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }


    static class Family implements Serializable, Cloneable {

        private static final long serialVersionUID = -3737249230697805286L;

        private String name;
        private List<Name> members;
        private Name mother;
        private List<Result> relations;

        private boolean skip = false;

        Family(String name, List<Name> members) {
            this.members = members;
            this.name = name;
            relations = new ArrayList<>();
        }

        Family(List<Name> members) {
            this.members = members;
            name = members.get(0).getCleanName();
            relations = new ArrayList<>();
        }

        Family(Family family) {
            setName(family.getName());
            setMembers(new ArrayList<>(family.getMembers()));
            setMother(family.getMother());
            setRelations(new ArrayList<>(family.getRelations()));
            setSkip(family.isSkip());
            this.members.sort((d1, d2) -> Double.compare(d2.getJakkardStatus(), d1.getJakkardStatus()));
        }

        @Override
        public String toString() {
            return name + ": " + members;
        }

        boolean isSkip() {
            return skip;
        }

        void setSkip(boolean skip) {
            this.skip = skip;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        List<Name> getMembers() {
            return members;
        }

        void setMembers(List<Name> members) {
            this.members = members;
        }

        Name getMother() {
            return mother;
        }

        void setMother(Name mother) {
            this.mother = mother;
        }

        List<Result> getRelations() {
            return relations;
        }

        void setRelations(List<Result> relations) {
            this.relations = relations;
        }

        int size() {
            return members.size();
        }

        Name get(int i) {
            return members.get(i);
        }

        void addRelation(Result result) {
            relations.add(result);
        }

        boolean containsRelation(Name name1, Name name2) {
            return relations.stream().filter(r -> r.getName1().getName().equals(name1.getName())).anyMatch(r -> r.getName2().getName().equals(name2.getName()));
        }

        double getJakkardStatus(int index) {
            if (members.size() < 2) {
                return 0;
            }
            Name name = members.get(index);
            return name.getJakkardStatus() / (members.size() - 1);
        }

    }

    static class Name implements Serializable {

        private static final long serialVersionUID = 292207904602980582L;

        private File file;
        private boolean done;
        private int index = 100;

        private double jakkardStatus = 0;
        private double relativeStatus = 0;

        Name(File file, boolean done) {
            this.file = file;
            this.done = done;

            if (file.getName().contains("(U)")) {
                index += 100;
            }
            if (file.getName().contains("(W)")) {
                index += 99;
            }
            if (file.getName().contains("(E)")) {
                index += 80;
            }
            if (file.getName().contains("(F)")) {
                index += 70;
            }
            if (file.getName().contains("(G)")) {
                index += 70;
            }
            if (file.getName().contains("(J)")) {
                index += 60;
            }
            if (file.getName().contains("[b")) {
                index -= 50;
            }
            if (file.getName().contains("[a")) {
                index -= 5;
            }
            if (file.getName().contains("[h")) {
                index -= 20;
            }
            if (file.getName().contains("[t")) {
                index -= 10;
            }
            if (file.getName().contains("[p")) {
                index -= 10;
            }
            if (file.getName().contains("[f")) {
                index -= 10;
            }
            if (file.getName().contains("[T")) {
                index -= 10;
            }
            if (file.getName().contains("[!]")) {
                index += 10;
            }
            if (file.getName().contains("(PD)")) {
                index -= 45;
            }
            if (file.getName().contains("(Hack") || file.getName().contains("Hack)")) {
                index -= 45;
            }
            if (file.getName().contains("+")) {
                index -= 2;
            }
        }

        @Override
        public String toString() {
            return file.getName() + ": " + jakkardStatus;
        }

        int getIndex() {
            return index;
        }

        void setIndex(int index) {
            this.index = index;
        }

        File getFile() {
            return file;
        }

        void setFile(File file) {
            this.file = file;
        }

        String getName() {
            return file.getName();
        }

        String getCleanName() {
            String result = file.getName();
            int braceIndex = result.indexOf("(");
            if (braceIndex > 0) {
                result = result.substring(0, braceIndex);
            }
            braceIndex = result.indexOf("[");
            if (braceIndex > 0) {
                result = result.substring(0, braceIndex);
            }
            return result.trim();
        }

        boolean isDone() {
            return done;
        }

        void setDone(boolean done) {
            this.done = done;
        }

        double getJakkardStatus() {
            return jakkardStatus;
        }

        void setJakkardStatus(double jakkardStatus) {
            this.jakkardStatus = jakkardStatus;
        }

        public double getRelativeStatus() {
            return relativeStatus;
        }

        void setRelativeStatus(double relativeStatus) {
            this.relativeStatus = relativeStatus;
        }

        void addRelativeStatus(double status) {
            relativeStatus += status;
        }

        void addJakkardStatus(double status) {
            jakkardStatus += status;
        }
    }

    private static class Result implements Serializable {

        private static final long serialVersionUID = -472204242580854693L;

        private Name name1;
        private Name name2;
        private double relative;
        private double jakkard;

        Result(Name name1, Name name2, double relative, double jakkard) {
            this.name1 = name1;
            this.name2 = name2;
            this.relative = relative;
            this.jakkard = jakkard;
        }

        Name getName1() {
            return name1;
        }

        Name getName2() {
            return name2;
        }

        double getRelative() {
            return relative;
        }

        double getJakkard() {
            return jakkard;
        }

        @Override
        public String toString() {
            return "\"" + name1.getName() + "\",\"" + name2.getName() + "\",\"" + relative + "\",\"" + jakkard + "\"";
        }
    }
}
