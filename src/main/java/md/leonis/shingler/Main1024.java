package md.leonis.shingler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

public class Main1024 {

    private static final int SHINGLE_LENGTH = 8;
    private static final int SAMPLE_PROBE = 256;

    private static final String GAMES_DIR = "D:\\Downloads\\games\\";

    private static final File SHD_FOLDER = new File(GAMES_DIR + "shd");

    private static final int SHINGLE_MAP_THRESHOLD = 128;

    private static final List<Integer> SAMPLES = Arrays.asList(16, 64, 256, 1024);

    private static final Map<Integer, File> SAMPLE_DIRS_MAP =
            SAMPLES.stream().collect(Collectors.toMap(Function.identity(), s -> new File(GAMES_DIR + "sample" + s)));

    private static final Map<Integer, Map<String, Set<Long>>> SHINGLE_MAP =
            SAMPLES.stream().collect(Collectors.toMap(Function.identity(), HashMap::new));

    public static void main(String[] args) throws IOException {
        final File folder = new File(GAMES_DIR);

        SAMPLE_DIRS_MAP.values().forEach(File::mkdirs);

        List<File> files = listFilesForFolder(folder);

        generateShingles(files);

        getSamples(files);

        List<Name> names = files.stream().map(f -> new Name(f, false)).collect(Collectors.toList());

        generateFamilies(names, 1024, 20); // generate families

    }

    @SuppressWarnings("all")
    private static void generateShingles(List<File> files) throws IOException {

        System.out.println("Generating shingles...");

        SHD_FOLDER.mkdir();

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);

            File shdFile = new File(SHD_FOLDER.getAbsolutePath() + File.separator + file.getName() + ".shg");

            if (shdFile.exists() && shdFile.length() > 0) {
                System.out.println(String.format(Locale.US, "Skipping: %s: %2.2f", file.getName(), ((i + 1) * 100.0 / files.size())));
                continue;
            }

            Set<Long> shingles = toShingles(readFromFile(file));

            System.out.println(String.format(Locale.US, "%s: %2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));
            serialize(shdFile, shingles);

            SAMPLE_DIRS_MAP.entrySet().forEach(e -> {
                int index = e.getKey();
                Set<Long> filteredShingles = shingles.stream().filter(s -> s % index == 0).collect(Collectors.toSet());

                if (index > SHINGLE_MAP_THRESHOLD) {
                    SHINGLE_MAP.get(index).put(file.getName(), filteredShingles);
                }

                File sampleFile = new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + file.getName() + ".shg");
                if (!sampleFile.exists()) {
                    serialize(sampleFile, shingles.stream().filter(s -> s % index == 0).collect(Collectors.toSet()));
                }
            });
        }
    }

    @SuppressWarnings("all")
    private static void getSamples(List<File> files) throws IOException {
        System.out.println("\nGetting samples from disk...");

        SHINGLE_MAP.forEach((key, value) -> {
            int index = key;
            File shinglesMapFile = new File("shinglesMap" + SAMPLE_PROBE);
            Map<String, Set<Long>> shinglesMap = (!shinglesMapFile.exists()) ? new HashMap<>() : readShingleMapFromFile(shinglesMapFile);
            if (index > SHINGLE_MAP_THRESHOLD && shinglesMap.isEmpty()) {
                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);

                    /*if (sampleFile.exists() && sampleFile.length() > 0) {
                        System.out.println(String.format(Locale.US, "Skipping: %s: %2.2f", file.getName(), ((i + 1) * 100.0 / files.size())));
                        continue;
                    }*/

                    System.out.println(String.format(Locale.US, "%s: %2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));

                    File srcSampleFolder = (index == SAMPLES.get(0)) ? SHD_FOLDER : SAMPLE_DIRS_MAP.get(index - 1);

                    Set<Long> shingles = readShdFromFile(new File(srcSampleFolder.getAbsolutePath() + File.separator + file.getName() + ".shg"));
                    Set<Long> filteredShingles = shingles.stream().filter(s -> s % index == 0).collect(Collectors.toSet());

                    shinglesMap.put(file.getName(), filteredShingles);

                    File sampleFile = new File(SAMPLE_DIRS_MAP.get(index).getAbsolutePath() + File.separator + file.getName() + ".shg");
                    if (!sampleFile.exists()) {
                        serialize(sampleFile, shingles.stream().filter(s -> s % SAMPLE_PROBE == 0).collect(Collectors.toSet()));
                    }
                }

                serialize(shinglesMapFile, shinglesMap);
                SHINGLE_MAP.replace(index, shinglesMap);
            }
        });
    }

    private static void generateFamilies(List<Name> names, int index, int jakkardIndex) throws IOException {

        System.out.println("\nComparing...");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("result.csv", true)));

        Map<String, List<Name>> families = names.stream().collect(Collectors.groupingBy(Name::getCleanName));

        ///TODO every family verify by SHD, drop < 80

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

                            Result result = new Result(name1.getName(), name2.getName(), relative, jakkard);

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
    }


    private static void compareAndCopy(List<Name> names, int index, int jakkardIndex) throws IOException {

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

                            Result result = new Result(name1.getName(), name2.getName(), relative, jakkard);

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
    }


    private static String getTitle(List<Name> names) {
        return names.stream().max(Comparator.comparing(Name::getIndex)).orElse(null).getName();
    }

    private static String getCleanTitle(List<Name> names) {
        return names.stream().max(Comparator.comparing(Name::getIndex)).orElse(null).getCleanName();
    }

    private static void serialize(File file, Object object) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //TODO unify
    @SuppressWarnings("unchecked")
    private static Map<String, Set<Long>> readShingleMapFromFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, Set<Long>> shinglesMap = (Map<String, Set<Long>>) ois.readObject();

            ois.close();
            fis.close();
            return shinglesMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //TODO unify
    @SuppressWarnings("unchecked")
    private static Set<Long> readShdFromFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Set<Long> result = (Set<Long>) ois.readObject();

            ois.close();
            fis.close();
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("all")
    private static Set<Long> toShingles(byte[] bytes) {
        Set<Long> hashes = new HashSet<>();

        for (int i = 0; i < bytes.length - SHINGLE_LENGTH + 1; i++) {
            CRC32 crc = new CRC32();
            crc.update(Arrays.copyOfRange(bytes, i, i + SHINGLE_LENGTH));
            long value = crc.getValue();
            if (value % SAMPLE_PROBE == 0) {
                hashes.add(crc.getValue());
            }
        }

        return hashes;
    }

    @SuppressWarnings("all")
    private static List<File> listFilesForFolder(final File folder) {
        return Arrays.stream(folder.listFiles()).filter(File::isFile).collect(Collectors.toList());
    }

    private static byte[] readFromFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }


    private static class Name {

        private File file;
        private boolean done;
        private int index = 100;

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

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getName() {
            return file.getName();
        }

        public String getCleanName() {
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

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }

    private static class Result {

        private String file1;
        private String file2;
        private double relative;
        private double jakkard;

        Result(String file1, String file2, double relative, double jakkard) {
            this.file1 = file1;
            this.file2 = file2;
            this.relative = relative;
            this.jakkard = jakkard;
        }

        public String getFile1() {
            return file1;
        }

        public String getFile2() {
            return file2;
        }

        public double getRelative() {
            return relative;
        }

        public double getJakkard() {
            return jakkard;
        }

        @Override
        public String toString() {
            return "\"" + file1 + "\",\"" + file2 + "\",\"" + relative + "\",\"" + jakkard + "\"";
        }
    }
}
